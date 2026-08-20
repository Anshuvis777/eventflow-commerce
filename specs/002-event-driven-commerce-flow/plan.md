# Implementation Plan: Event-Driven Commerce Flow

**Branch**: `002-event-driven-commerce-flow` | **Date**: 2026-08-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-event-driven-commerce-flow/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Convert the EventFlow Commerce core from a single-topic publish-only model into a real-world Kafka event-choreography pipeline. Split the single `business-events` topic into four per-domain topics (`orders`, `payments`, `inventory`, `shipments`). Each core service reacts to the previous stage's event: payment-service auto-processes on `OrderPlaced`, inventory-service reserves stock on `PaymentProcessed`, shipping-service creates a shipment on `InventoryReserved`. A new `notification-service` consumes all four topics and sends SMTP emails (order confirmed, payment received, shipped, delivered, payment failed, insufficient stock, order cancelled) with idempotent processing and a 3-attempt retry-with-backoff policy. The notification service is wired into docker-compose and the nginx dashboard proxy, and the dashboard health grid + email page are updated to reflect it.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.4.4 (parent POM), Maven 3.9.9 multi-module

**Primary Dependencies**: Spring Web, Spring Data JPA, Spring Kafka, Spring Mail (NEW for notification-service), Flyway, MapStruct 1.6.3, Lombok, `eventflow-common` (shared events, BaseEntity, ApiResponse, CorrelationIdFilter, GlobalExceptionHandler)

**Storage**: Neon PostgreSQL — shared `eventflow` database for all core services (consistent with existing services; no DB-per-service)

**Testing**: JUnit 5, Mockito, Testcontainers (real Kafka + PostgreSQL), RestAssured contract tests

**Target Platform**: Docker containers (`eclipse-temurin:21-jre-alpine`), docker-compose with profiles `[core, ai, all]`, nginx reverse proxy

**Project Type**: Microservices (web services) — event-driven backend + React dashboard

**Performance Goals**: Full chain (order → payment → inventory → shipment) completes in < 30s; each stage email sent < 30s after its triggering event

**Constraints**: Aiven Kafka with SASL_SSL + private CA (CA already baked into all Dockerfiles); `mem_limit: 350m` per service; single shared PostgreSQL; at-least-once Kafka delivery requires idempotent consumers

**Scale/Scope**: Small demo platform — 5 core services (order, payment, inventory, shipping, notification) + 3 incident services (ai profile) + dashboard

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Reuse `eventflow-common` (MANDATORY) | ✅ PASS | New events (`PaymentProcessedEvent`, `InventoryReservedEvent`) added to common; notification-service depends on common |
| Clean Architecture package layout (MANDATORY) | ✅ PASS | All services follow `entity/repository/dto/mapper/service/controller`; notification-service follows same layout |
| Test-First (NON-NEGOTIABLE) | ✅ PASS | Contract tests + Testcontainers integration tests planned for every consumer and the notification service |
| Integration Testing with Real Infrastructure | ✅ PASS | Testcontainers Kafka + PostgreSQL for consumer tests; RestAssured for API contracts |
| Simplicity & Single Shared DB | ⚠️ DEVIATION | Adds a 5th core service (notification-service). Justified in Complexity Tracking. Single shared DB maintained |

## Project Structure

### Documentation (this feature)

```text
specs/002-event-driven-commerce-flow/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
eventflow-common/src/main/java/com/eventflow/common/event/
├── BaseEvent.java                    # ADD event_type subtypes for new events
├── OrderPlacedEvent.java             # ADD customerEmail + customerName fields
├── PaymentProcessedEvent.java        # NEW
├── PaymentFailedEvent.java           # unchanged
├── InventoryReservedEvent.java       # NEW
├── InventoryReservationFailedEvent.java # NEW (triggers "Insufficient Stock" email)
├── InventoryReleasedEvent.java       # unchanged
├── ShipmentCreatedEvent.java         # unchanged
├── ShipmentDeliveredEvent.java       # unchanged
└── OrderCancelledEvent.java          # unchanged

order-service/src/main/java/com/eventflow/orderservice/
├── service/OrderService.java         # publish OrderPlaced/OrderCancelled → "orders" topic; set customerEmail/customerName on event

payment-service/src/main/java/com/eventflow/paymentservice/
├── consumer/OrderEventConsumer.java  # NEW @KafkaListener("orders") → auto-process payment on OrderPlaced
├── service/PaymentService.java       # publish PaymentProcessed/PaymentFailed → "payments" topic; idempotency check

inventory-service/src/main/java/com/eventflow/inventoryservice/
├── consumer/PaymentEventConsumer.java # NEW @KafkaListener("payments") → reserve stock on PaymentProcessed
├── service/InventoryService.java      # publish InventoryReserved/InventoryReleased → "inventory" topic; idempotency check

shipping-service/src/main/java/com/eventflow/shippingservice/
├── consumer/InventoryEventConsumer.java # NEW @KafkaListener("inventory") → create shipment on InventoryReserved
├── service/ShippingService.java         # publish ShipmentCreated/ShipmentDelivered → "shipments" topic; idempotency check

notification-service/                 # NEW Maven module (port 8085)
├── pom.xml
├── Dockerfile
├── src/main/java/com/eventflow/notificationservice/
│   ├── NotificationServiceApplication.java
│   ├── entity/NotificationEntity.java
│   ├── repository/NotificationRepository.java
│   ├── dto/request/  dto/response/
│   ├── mapper/NotificationMapper.java
│   ├── service/NotificationService.java   # send email + idempotency + orderId→email map
│   ├── consumer/BusinessEventConsumer.java # @KafkaListener on all 4 topics
│   └── controller/NotificationController.java # GET /api/v1/notifications (history)
└── src/main/resources/
    ├── application.yml
    └── application-docker.yml

incident-detector/src/main/java/com/eventflow/incidentdetector/
├── consumer/BusinessEventConsumer.java  # UPDATE topics → all 4 topics

docker/
├── compose.yml                # ADD notification-service (core profile, port 8085)
└── certs/aiven-ca.pem         # reused by notification-service Dockerfile

dashboard/
├── nginx.conf                 # ADD /api/v1/notifications → notification-service:8085
└── src/
    ├── pages/DashboardPage.tsx  # ADD Notification Service to health grid
    ├── pages/EmailPage.tsx      # wire to real /api/v1/notifications API (replace mock)
    └── services/api.ts          # ADD notificationApi
```

**Structure Decision**: Follows the existing multi-module Maven layout — one module per microservice, all depending on `eventflow-common`. The notification-service mirrors the exact package structure of the existing services (entity/repository/dto/mapper/service/controller/consumer). No new top-level directories; the feature is additive to the existing tree.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 5th core service (notification-service) | Email delivery is a distinct cross-cutting concern consumed by all 4 topics; the user explicitly requested a dedicated notification service | Embedding mail in each of the 4 services would duplicate SMTP config, idempotency, and email history 4×; a single consumer is the lazy, DRY option |
| 4 Kafka topics instead of 1 | Real-world per-domain topics decouple producers/consumers and match the user's explicit request ("like actual real world") | Keeping 1 topic would require every consumer to filter by event type and couples all domains; 4 topics is the standard choreography pattern |
| `OrderPlacedEvent` gains 2 fields | Notification-service needs the customer email/name to address emails; it is the only event that carries customer identity | Looking up email via REST from order-service on every event would add synchronous coupling and latency to the async chain |
