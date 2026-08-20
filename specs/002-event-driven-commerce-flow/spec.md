# Feature Specification: Event-Driven Commerce Flow

**Feature Branch**: `002-event-driven-commerce-flow`

**Created**: 2026-08-18

**Status**: Draft

**Input**: User description: "Event-driven commerce flow: split Kafka into per-domain topics (orders, payments, inventory, shipments), add event choreography so payment-service auto-processes on OrderPlaced, inventory-service reserves on PaymentProcessed, shipping-service creates shipment on InventoryReserved, add new notification-service that consumes all topics and sends email notifications (order confirmed, payment received, shipped, delivered, payment failed) via SMTP, and wire notification-service into docker-compose and nginx dashboard proxy."

## Clarifications

### Session 2026-08-18

- Q: Should customers receive an email when stock cannot be reserved for their order? → A: Yes — send an "Insufficient Stock" email when reservation fails
- Q: Should customers receive an "Order Cancelled" email when their order is cancelled mid-chain? → A: Yes — send an "Order Cancelled" email when an order is cancelled
- Q: How many times should a failed email be retried before giving up? → A: Retry 3 times with backoff, then mark FAILED

## User Scenarios & Testing

### User Story 1 - Automatic Payment Processing (Priority: P1)

When a customer places an order, the payment is automatically processed without any manual action from an operator. The customer does not need to click a separate "process payment" button.

**Why this priority**: This is the first link in the event chain. Without automatic payment processing, the rest of the flow (inventory, shipping, notifications) cannot proceed. It delivers immediate value by removing a manual step.

**Independent Test**: Can be fully tested by placing an order and verifying a payment record is created and marked successful without any manual intervention.

**Acceptance Scenarios**:

1. **Given** an order is placed, **When** the order event is published, **Then** a payment is automatically processed for the order amount and marked successful
2. **Given** an order with insufficient funds, **When** payment is attempted, **Then** a payment failure is recorded and the order chain stops
3. **Given** a payment that was already processed for an order, **When** the same order event is received again, **Then** no duplicate payment is created

---

### User Story 2 - Automatic Inventory Reservation (Priority: P1)

When a payment succeeds, the ordered items' stock is automatically reserved so the items cannot be sold to someone else.

**Why this priority**: Stock reservation is the second link in the chain and protects inventory integrity. Without it, overselling occurs.

**Independent Test**: Can be fully tested by processing a successful payment and verifying stock levels decrease by the ordered quantities.

**Acceptance Scenarios**:

1. **Given** a successful payment event, **When** inventory processes it, **Then** stock for each ordered product is reserved and reduced by the ordered quantity
2. **Given** insufficient stock for an item, **When** reservation is attempted, **Then** a reservation failure is recorded and the chain stops for that order
3. **Given** a payment failure event, **When** inventory receives it, **Then** no stock is reserved

---

### User Story 3 - Automatic Shipment Creation (Priority: P1)

When stock is successfully reserved, a shipment is automatically created for the order so fulfillment begins without manual entry.

**Why this priority**: This is the third link in the chain and completes the core fulfillment flow. It removes manual shipment creation.

**Independent Test**: Can be fully tested by completing a successful reservation and verifying a shipment record is created with the order's shipping address.

**Acceptance Scenarios**:

1. **Given** a successful inventory reservation event, **When** shipping processes it, **Then** a shipment is created for the order with the correct shipping address
2. **Given** a reservation failure event, **When** shipping receives it, **Then** no shipment is created
3. **Given** a shipment already created for an order, **When** the same reservation event is received again, **Then** no duplicate shipment is created

---

### User Story 4 - Email Notifications (Priority: P1)

Customers receive email notifications at each stage of their order journey: order confirmed, payment received, order shipped, order delivered, payment failed, insufficient stock, and order cancelled.

**Why this priority**: This is the user-facing value of the whole feature — customers are kept informed automatically. It is the reason the notification service exists.

**Independent Test**: Can be fully tested by driving an order through the full chain and verifying an email is sent at each stage to the customer's email address.

**Acceptance Scenarios**:

1. **Given** an order is placed, **When** the order event is published, **Then** an "Order Confirmed" email is sent to the customer's email address
2. **Given** a payment succeeds, **When** the payment event is published, **Then** a "Payment Received" email is sent
3. **Given** a shipment is created, **When** the shipment event is published, **Then** a "Your Order Shipped" email is sent
4. **Given** a shipment is delivered, **When** the delivery event is published, **Then** a "Your Order Delivered" email is sent
5. **Given** a payment fails, **When** the payment failure event is published, **Then** a "Payment Failed" email is sent
6. **Given** stock is insufficient for an order, **When** reservation fails, **Then** an "Insufficient Stock" email is sent to the customer
7. **Given** an order is cancelled, **When** the cancellation event is published, **Then** an "Order Cancelled" email is sent to the customer
8. **Given** the same event is received twice, **When** notifications process it, **Then** only one email is sent (no duplicates)

---

### User Story 5 - Failure Isolation (Priority: P2)

If one service in the chain fails (e.g., email provider is down), the rest of the order flow continues unaffected. A notification failure does not block payment, inventory, or shipping.

**Why this priority**: Ensures the core commerce flow is resilient. Email is important but must not block order fulfillment.

**Independent Test**: Can be fully tested by simulating an email provider outage and verifying orders still flow through payment, inventory, and shipping.

**Acceptance Scenarios**:

1. **Given** the email provider is unavailable, **When** an order is placed, **Then** the order still flows through payment, inventory, and shipping, and the failed email is retried later
2. **Given** a service in the chain is down, **When** events are published, **Then** events are not lost and are processed when the service recovers

---

### User Story 6 - Dashboard Visibility (Priority: P2)

Operators can see the notification service in the dashboard health grid and view email history, so they can confirm notifications are working.

**Why this priority**: Provides operational confidence that the new service is running and emails are being sent.

**Independent Test**: Can be fully tested by starting the notification service and verifying it appears as UP in the dashboard and email history is viewable.

**Acceptance Scenarios**:

1. **Given** the notification service is running, **When** the dashboard loads, **Then** the notification service appears in the health grid as UP
2. **Given** emails have been sent, **When** viewing email history, **Then** sent emails are listed with recipient, subject, and status

---

### Edge Cases

- What happens when Kafka is temporarily unavailable? Events are buffered and processed when Kafka recovers (at-least-once delivery)
- What happens when a payment fails? The chain stops — no inventory reservation, no shipment, and a "Payment Failed" email is sent
- What happens when stock is insufficient? Reservation fails, the chain stops, and an "Insufficient Stock" email is sent to the customer
- What happens when the email provider is down? Emails are retried up to 3 times with backoff; the core order flow is not blocked
- What happens when the same event is delivered twice? Idempotent processing prevents duplicate payments, reservations, shipments, and emails
- What happens when an order is cancelled mid-chain? Cancellation events stop further processing, release any reserved stock, and an "Order Cancelled" email is sent to the customer
- What happens when a service is down during the chain? Events persist in Kafka and are processed on recovery

## Requirements

### Functional Requirements

- **FR-001**: System MUST publish an order event to the orders topic when an order is placed
- **FR-002**: System MUST automatically process a payment when an order event is received, without manual intervention
- **FR-003**: System MUST publish a payment success event to the payments topic when payment succeeds
- **FR-004**: System MUST publish a payment failure event to the payments topic when payment fails
- **FR-005**: System MUST automatically reserve stock when a payment success event is received
- **FR-006**: System MUST publish an inventory reservation event to the inventory topic when stock is reserved
- **FR-007**: System MUST publish an inventory release event when stock is released (e.g., order cancelled)
- **FR-008**: System MUST automatically create a shipment when an inventory reservation event is received
- **FR-009**: System MUST publish a shipment created event to the shipments topic when a shipment is created
- **FR-010**: System MUST publish a shipment delivered event when a shipment is delivered
- **FR-011**: System MUST consume events from all four topics (orders, payments, inventory, shipments) for notification purposes
- **FR-012**: System MUST send an "Order Confirmed" email when an order is placed
- **FR-013**: System MUST send a "Payment Received" email when a payment succeeds
- **FR-014**: System MUST send a "Payment Failed" email when a payment fails
- **FR-015**: System MUST send a "Your Order Shipped" email when a shipment is created
- **FR-016**: System MUST send a "Your Order Delivered" email when a shipment is delivered
- **FR-025**: System MUST send an "Insufficient Stock" email when inventory reservation fails for an order
- **FR-026**: System MUST send an "Order Cancelled" email when an order is cancelled
- **FR-017**: System MUST send emails to the customer's email address associated with the order
- **FR-018**: System MUST process each event exactly once for side effects (no duplicate payments, reservations, shipments, or emails)
- **FR-019**: System MUST continue the order flow when email delivery fails (notification failure must not block payment, inventory, or shipping)
- **FR-027**: System MUST retry a failed email up to 3 times with backoff before marking it FAILED
- **FR-020**: System MUST propagate a correlation ID across the entire event chain so all events for one order are traceable
- **FR-021**: System MUST deploy the notification service as part of the same orchestration as the other services
- **FR-022**: System MUST expose the notification service through the dashboard proxy so the dashboard can reach it
- **FR-023**: System MUST show the notification service in the dashboard health grid
- **FR-024**: System MUST record email history (recipient, subject, status, timestamp) for operator review

### Key Entities

- **Order**: A customer purchase; attributes: id, order_number, customer_id, customer_email, status, total_amount, currency, shipping_address, items
- **Payment**: A payment attempt for an order; attributes: id, order_id, amount, status (SUCCESS, FAILED), failure_reason, processed_at
- **InventoryReservation**: A stock reservation for an order; attributes: id, order_id, product_id, quantity, status (RESERVED, RELEASED), reserved_at
- **Shipment**: A fulfillment shipment for an order; attributes: id, order_id, shipping_address, status (CREATED, DELIVERED), created_at, delivered_at
- **Notification**: An email sent to a customer; attributes: id, correlation_id, recipient, subject, body, status (SENT, FAILED, RETRYING), sent_at
- **BusinessEvent**: A domain event on a Kafka topic; attributes: event_id, event_type, correlation_id, service_name, timestamp, severity, payload

## Success Criteria

### Measurable Outcomes

- **SC-001**: A payment is automatically processed within 5 seconds of an order being placed
- **SC-002**: The full chain (order → payment → inventory → shipment) completes within 30 seconds of order placement
- **SC-003**: Customers receive each stage email within 30 seconds of the triggering event
- **SC-004**: 99.9% of published events are processed without loss during normal operation
- **SC-005**: Zero duplicate emails, payments, reservations, or shipments during normal operation
- **SC-006**: An email provider outage does not delay order fulfillment by more than 1 minute
- **SC-007**: Operators can confirm the notification service is running and view email history from the dashboard
- **SC-008**: A single service failure does not lose events — all events are processed once the service recovers

## Assumptions

- SMTP provider is configurable (e.g., Gmail SMTP, SendGrid, or Mailtrap for development) — provider choice is a deployment detail
- Email recipient is the customer email address captured on the order
- Existing manual REST endpoints (process payment, reserve stock, create shipment) remain available for backward compatibility and operator override
- Kafka uses at-least-once delivery, so idempotent event processing is required
- Events without a correlation ID are logged but not chained
- Order cancellation mid-chain releases reserved stock and stops further processing
- Reuses **eventflow-common** for BaseEntity, ApiResponse, CorrelationIdFilter, and domain event POJOs
- **Java 21** with **Spring Boot 3.4.4** for all backend services
- **Maven** multi-module build, each service depends on eventflow-common
- **Docker Compose** for local development orchestration; notification service joins the existing compose file
- **Single shared PostgreSQL** database (consistent with existing services)
- Dashboard is the existing React dashboard at port 3000; notification service is added to its nginx proxy
- Email sending uses a standard mail library with SMTP; no third-party email SaaS SDK required