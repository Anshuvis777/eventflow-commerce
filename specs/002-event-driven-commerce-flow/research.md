# Research: Event-Driven Commerce Flow

**Date**: 2026-08-18
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

All technical unknowns were resolved by inspecting the existing codebase (no NEEDS CLARIFICATION markers remain). This document records the decisions and the evidence behind them.

## Decision 1: Topic split — 4 per-domain topics

**Decision**: Split the single `business-events` topic into `orders`, `payments`, `inventory`, `shipments`.

**Rationale**: Real-world Kafka choreography uses per-domain topics so each service owns its event stream and consumers subscribe only to what they need. The user explicitly requested "like actual real world". Current state (verified in source): all 4 core services call `kafkaTemplate.send("business-events", ...)` and only `incident-detector` consumes it.

**Alternatives considered**:
- Keep 1 topic + event-type filtering — rejected: couples all domains, every consumer sees every event, no per-domain retention/partitioning.
- Publish to both `business-events` AND new topics — rejected: duplicate writes, no benefit once incident-detector is updated.

**Impact**: `incident-detector`'s `@KafkaListener(topics = "business-events")` must change to listen on all 4 topics (it is the observability consumer).

## Decision 2: New event classes in eventflow-common

**Decision**: Add `PaymentProcessedEvent` and `InventoryReservedEvent` to `eventflow-common/src/main/java/com/eventflow/common/event/`, and register them in `BaseEvent`'s `@JsonSubTypes`.

**Rationale**: The chain needs success events to trigger the next stage. Verified existing events: `OrderPlacedEvent`, `PaymentFailedEvent`, `InventoryReleasedEvent`, `OrderCancelledEvent`, `ShipmentCreatedEvent`, `ShipmentDeliveredEvent`. There is no success event for payment or inventory.

**Impact**: `BaseEvent` `@JsonSubTypes` must gain two entries (`PaymentProcessed`, `InventoryReserved`).

## Decision 3: OrderPlacedEvent carries customer identity

**Decision**: Add `customerEmail` and `customerName` fields to `OrderPlacedEvent`, populated by `OrderService` from `OrderEntity` (which already stores `customerEmail`/`customerName`).

**Rationale**: The notification-service needs the customer's email to send notifications. `OrderPlacedEvent` is the only event that originates where customer identity is known. Verified: `OrderEntity` has `customerEmail`/`customerName` columns; `OrderService.createOrder` currently does NOT set them on the event.

**Impact**: `OrderService.createOrder` sets the two new fields. Downstream events (PaymentFailed, ShipmentCreated, etc.) do NOT carry email — the notification-service learns `orderId → email` from `OrderPlaced` and reuses it for the rest of the chain.

## Decision 4: notification-service email lookup strategy

**Decision**: notification-service maintains an `orderId → customerEmail` mapping in its own DB table (`notification_recipient`), populated when it consumes `OrderPlaced`. All subsequent events for that order look up the email by `orderId`.

**Rationale**: Avoids polluting every event schema with email. Events are keyed by `orderId` (verified: `kafkaTemplate.send(topic, orderId.toString(), event)`), so `orderId` is the natural correlation key. If no email is known for an order, the notification is logged and skipped (no crash).

**Alternatives considered**:
- Add email to every event — rejected: 4 event schemas polluted, payment/inventory/shipping services would need to carry email they don't own.
- Synchronous REST lookup to order-service — rejected: adds coupling + latency to an async chain.

## Decision 5: Idempotency (at-least-once delivery)

**Decision**: Every consumer guards against duplicate processing:
- payment-service: skip if a payment already exists for `orderId`
- inventory-service: skip if stock already reserved for `orderId` + `productId`
- shipping-service: skip if a shipment already exists for `orderId`
- notification-service: skip if a notification already recorded for `event_id`

**Rationale**: Kafka guarantees at-least-once delivery; without idempotency, redelivery creates duplicate payments/reservations/shipments/emails. Verified: existing services have no idempotency guards.

## Decision 6: SMTP via Spring Mail

**Decision**: Use `spring-boot-starter-mail` with configurable SMTP settings via env vars (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`). Default to Mailtrap-style dev settings; production can point at Gmail/SendGrid SMTP.

**Rationale**: Standard library approach, no third-party SaaS SDK. Matches the spec assumption "SMTP provider is configurable".

**Impact**: `.env` gains `MAIL_*` vars; notification-service `application.yml` maps them to `spring.mail.*`.

## Decision 7: Failure isolation + retry policy

**Decision**: Email sending failures are caught and recorded (status `RETRYING`, then `FAILED` after 3 attempts with backoff) but never rethrown into the Kafka consumer — the consumer commits the offset so the core chain is never blocked by mail.

**Rationale**: Spec FR-019 requires notification failure not to block payment/inventory/shipping. FR-027 (clarification) fixes the retry budget at 3 attempts with backoff; the `retryCount` field on the notification record tracks attempts.

**Impact**: `NotificationEntity` has `retryCount` (default 0, max 3). Status flow: `RETRYING` (≤3 attempts) → `SENT` or `FAILED`.

## Decision 7b: Email coverage (post-clarification)

**Decision**: The notification-service handles 7 email types. Beyond the original 5, two were added via `/speckit-clarify`: an "Insufficient Stock" email triggered by the new `InventoryReservationFailedEvent` (published by inventory-service when stock is unavailable), and an "Order Cancelled" email triggered by the existing `OrderCancelledEvent` (published by order-service).

**Rationale**: Closes the gap between the spec's edge cases ("customer is notified") and the email list. Both events already exist or are required for the chain, so no extra infrastructure is needed — just two more templates and one new event class.

**Impact**: New `InventoryReservationFailedEvent` in `eventflow-common`; inventory-service publishes it on reservation failure; notification-service maps it to the "Insufficient Stock" template.

## Decision 8: Deployment wiring

**Decision**: notification-service joins `docker/compose.yml` under the `core` profile on port `8085`, reuses the Aiven CA bake step in its Dockerfile, and gets an nginx route `/api/v1/notifications → notification-service:8085`. Dashboard health grid gains a "Notification Service" entry; `EmailPage` is wired to the real `notificationApi` (replacing mock data).

**Rationale**: Consistent with the existing 4 core services (verified compose + nginx + DashboardPage patterns). Port 8085 is free.

## Best practices applied

- **Choreography over orchestration**: each service reacts to events; no central coordinator, no synchronous cross-service calls.
- **Correlation**: `orderId` is the correlation key across the whole chain (matches existing keying).
- **Consumer groups**: each service uses its own consumer group so it can scale independently.
- **Trusted packages**: notification-service Kafka consumer config sets `spring.json.trusted.packages: com.eventflow.common.event` (same as incident-detector).