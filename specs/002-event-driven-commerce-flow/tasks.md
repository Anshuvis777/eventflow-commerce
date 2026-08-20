---

description: "Task list template for feature implementation"
---

# Tasks: Event-Driven Commerce Flow

**Input**: Design documents from `/specs/002-event-driven-commerce-flow/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Test tasks ARE included — the project constitution mandates Test-First (NON-NEGOTIABLE) with Testcontainers integration tests for real Kafka/PostgreSQL and RestAssured contract tests.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Maven multi-module: each service is a module at repo root (`order-service/`, `notification-service/`, etc.)
- Shared code lives in `eventflow-common/src/main/java/com/eventflow/common/`
- Dashboard lives in `dashboard/src/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Add `notification-service` module to the `<modules>` list in `pom.xml`
- [X] T002 [P] Add `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` to `.env.example` with Mailtrap-style dev defaults
- [X] T003 Create `notification-service/pom.xml` with spring-boot-starter-web, spring-boot-starter-data-jpa, spring-kafka, spring-boot-starter-mail, postgresql, flyway-core, flyway-database-postgresql, eventflow-common, mapstruct, lombok
- [X] T004 Create `notification-service/src/main/java/com/eventflow/notificationservice/NotificationServiceApplication.java`
- [X] T005 Create `notification-service/src/main/resources/application.yml` (port 8085, datasource NEON_*, kafka consumer config, spring.mail.* from MAIL_* env vars) and `notification-service/src/main/resources/application-docker.yml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T006 Create `PaymentProcessedEvent.java` in `eventflow-common/src/main/java/com/eventflow/common/event/` (fields: orderId, paymentId, amount, currency, paymentMethod, transactionId)
- [X] T007 [P] Create `InventoryReservedEvent.java` in `eventflow-common/src/main/java/com/eventflow/common/event/` (fields: orderId, items[productId, productName, quantity, warehouseId], reservedBy)
- [X] T008 [P] Create `InventoryReservationFailedEvent.java` in `eventflow-common/src/main/java/com/eventflow/common/event/` (fields: orderId, items, failureReason, reservedBy)
- [X] T009 Register all 3 new event classes in `@JsonSubTypes` in `eventflow-common/src/main/java/com/eventflow/common/event/BaseEvent.java` (names: PaymentProcessed, InventoryReserved, InventoryReservationFailed)
- [X] T010 Add `customerEmail` and `customerName` fields to `eventflow-common/src/main/java/com/eventflow/common/event/OrderPlacedEvent.java`
- [X] T011 Update `order-service/src/main/java/com/eventflow/orderservice/service/OrderService.java` to publish `OrderPlaced` to the `orders` topic and set customerEmail, customerName, correlationId, serviceName, timestamp, severity on the event
- [X] T012 Update `payment-service/src/main/java/com/eventflow/paymentservice/service/PaymentService.java` to publish `PaymentProcessed` (success) and `PaymentFailed` (failure) to the `payments` topic
- [X] T013 Update `inventory-service/src/main/java/com/eventflow/inventoryservice/service/InventoryService.java` to publish `InventoryReserved`, `InventoryReservationFailed`, and `InventoryReleased` to the `inventory` topic
- [X] T014 Update `shipping-service/src/main/java/com/eventflow/shippingservice/service/ShippingService.java` to publish `ShipmentCreated` and `ShipmentDelivered` to the `shipments` topic
- [X] T015 Update `incident-detector/src/main/java/com/eventflow/incidentdetector/consumer/BusinessEventConsumer.java` `@KafkaListener` to consume all 4 topics: `orders`, `payments`, `inventory`, `shipments`
- [X] T016 Create `NotificationEntity.java` and `NotificationRecipientEntity.java` in `notification-service/src/main/java/com/eventflow/notificationservice/entity/` per `data-model.md` (Notification: eventId unique, correlationId, eventType, recipient, subject, body, status, retryCount, sentAt)
- [X] T017 Create `NotificationRepository.java` and `NotificationRecipientRepository.java` in `notification-service/src/main/java/com/eventflow/notificationservice/repository/` (findByEventId, findByCorrelationId, findByOrderId)
- [X] T018 Create Flyway migration `notification-service/src/main/resources/db/migration/V1__create_notifications.sql` creating `notification` and `notification_recipient` tables (event_id unique, retry_count default 0)
- [X] T019 Add `notification-service` to `docker/compose.yml` (profiles: ["core", "all"], port 8085, mem_limit 350m, env_file ../.env, network eventflow-net) and create `notification-service/Dockerfile` with the Aiven CA bake step (same as existing service Dockerfiles)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Automatic Payment Processing (Priority: P1) 🎯 MVP

**Goal**: When an order is placed, payment-service auto-processes the payment by consuming the `orders` topic — no manual action.

**Independent Test**: Place an order via the REST API and verify a payment record is auto-created (COMPLETED or FAILED) without calling POST /payments/process. Duplicate order events create no duplicate payment.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T020 [P] [US1] Integration test `payment-service/src/test/java/com/eventflow/paymentservice/integration/OrderEventConsumerIntegrationTest.java` (Testcontainers Kafka: publish OrderPlaced → payment created; republish same event → no duplicate)
- [X] T021 [P] [US1] Contract test asserting `OrderPlaced` on `orders` topic produces `PaymentProcessed`/`PaymentFailed` on `payments` topic per `contracts/kafka-topics.md`

### Implementation for User Story 1

- [X] T022 [US1] Create `payment-service/src/main/java/com/eventflow/paymentservice/consumer/OrderEventConsumer.java` with `@KafkaListener(topics = "orders", groupId = "payment-service-group")` handling OrderPlaced
- [X] T023 [US1] Add `processPaymentForOrder(OrderPlacedEvent)` to `payment-service/src/main/java/com/eventflow/paymentservice/service/PaymentService.java` with idempotency (skip if payment already exists for orderId)
- [X] T024 [US1] Add consumer config (key/value deserializer, `spring.json.trusted.packages: com.eventflow.common.event`, group-id, auto-offset-reset earliest) to `payment-service/src/main/resources/application.yml`

**Checkpoint**: User Story 1 fully functional and testable independently

---

## Phase 4: User Story 2 - Automatic Inventory Reservation (Priority: P1)

**Goal**: When a payment succeeds, inventory-service auto-reserves stock by consuming the `payments` topic.

**Independent Test**: Process a successful payment and verify stock `reserved` increases for ordered products. Payment failure → no reservation. Insufficient stock → `InventoryReservationFailed` event published.

### Tests for User Story 2

- [X] T025 [P] [US2] Integration test `inventory-service/src/test/java/com/eventflow/inventoryservice/integration/PaymentEventConsumerIntegrationTest.java` (Testcontainers Kafka: PaymentProcessed → stock reserved; PaymentFailed → no reservation; insufficient stock → failure event)

### Implementation for User Story 2

- [X] T026 [US2] Create `inventory-service/src/main/java/com/eventflow/inventoryservice/consumer/PaymentEventConsumer.java` with `@KafkaListener(topics = "payments", groupId = "inventory-service-group")` handling PaymentProcessed
- [X] T027 [US2] Add `reserveStockForOrder(PaymentProcessedEvent)` to `inventory-service/src/main/java/com/eventflow/inventoryservice/service/InventoryService.java` with idempotency (skip if already reserved for orderId+productId) and publish `InventoryReserved` or `InventoryReservationFailed`
- [X] T028 [US2] Add consumer config to `inventory-service/src/main/resources/application.yml` (same pattern as T024)

**Checkpoint**: User Stories 1 AND 2 both work independently

---

## Phase 5: User Story 3 - Automatic Shipment Creation (Priority: P1)

**Goal**: When stock is reserved, shipping-service auto-creates a shipment by consuming the `inventory` topic.

**Independent Test**: Complete a successful reservation and verify a shipment record is auto-created with the order's shipping address. Reservation failure → no shipment. Duplicate event → no duplicate shipment.

### Tests for User Story 3

- [X] T029 [P] [US3] Integration test `shipping-service/src/test/java/com/eventflow/shippingservice/integration/InventoryEventConsumerIntegrationTest.java` (Testcontainers Kafka: InventoryReserved → shipment created; InventoryReservationFailed → no shipment; duplicate → no duplicate)

### Implementation for User Story 3

- [X] T030 [US3] Create `shipping-service/src/main/java/com/eventflow/shippingservice/consumer/InventoryEventConsumer.java` with `@KafkaListener(topics = "inventory", groupId = "shipping-service-group")` handling InventoryReserved
- [X] T031 [US3] Add `createShipmentForOrder(InventoryReservedEvent)` to `shipping-service/src/main/java/com/eventflow/shippingservice/service/ShippingService.java` with idempotency (skip if shipment exists for orderId)
- [X] T032 [US3] Add consumer config to `shipping-service/src/main/resources/application.yml` (same pattern as T024)

**Checkpoint**: User Stories 1, 2, AND 3 all work independently

---

## Phase 6: User Story 4 - Email Notifications (Priority: P1)

**Goal**: notification-service consumes all 4 topics and sends 7 email types (Order Confirmed, Payment Received, Payment Failed, Insufficient Stock, Order Shipped, Order Delivered, Order Cancelled) via SMTP, with idempotency and email history.

**Independent Test**: Drive an order through the chain and verify an email notification is recorded for each stage with the correct subject and SENT status; duplicate events produce no duplicate notifications.

### Tests for User Story 4

- [X] T033 [P] [US4] Integration test `notification-service/src/test/java/com/eventflow/notificationservice/integration/BusinessEventConsumerIntegrationTest.java` (Testcontainers Kafka + mocked JavaMailSender: each of the 7 event types produces one notification; duplicate event_id → no duplicate; unknown order email → skipped without crash)

### Implementation for User Story 4

- [X] T034 [US4] Create `notification-service/src/main/java/com/eventflow/notificationservice/consumer/BusinessEventConsumer.java` with `@KafkaListener(topics = {"orders","payments","inventory","shipments"}, groupId = "notification-service-group")` handling all event types
- [X] T035 [US4] Create `notification-service/src/main/java/com/eventflow/notificationservice/service/NotificationService.java` — orderId→email lookup from NotificationRecipient, 7 subject templates per `contracts/notification-api.md`, save NotificationEntity, send via JavaMailSender
- [X] T036 [US4] Create `notification-service/src/main/java/com/eventflow/notificationservice/controller/NotificationController.java` — GET `/api/v1/notifications` (list with correlationId/status/eventType filters), GET `/api/v1/notifications/{id}`, GET `/api/v1/notifications/health` per `contracts/notification-api.md`
- [X] T037 [US4] Add consumer config to `notification-service/src/main/resources/application.yml` (group-id, earliest, JsonDeserializer, trusted packages com.eventflow.common.event)

**Checkpoint**: User Story 4 fully functional — all stage emails sent

---

## Phase 7: User Story 5 - Failure Isolation (Priority: P2)

**Goal**: A notification failure never blocks the core order flow; failed emails retry up to 3 times with backoff then mark FAILED.

**Independent Test**: Simulate SMTP outage — orders still flow through payment/inventory/shipping; notification records go RETRYING → FAILED after 3 attempts.

### Tests for User Story 5

- [X] T038 [P] [US5] Integration test in `notification-service/src/test/java/com/eventflow/notificationservice/integration/` — mail-sender throws → consumer still commits offset, notification status RETRYING then FAILED after 3 attempts (retryCount = 3)

### Implementation for User Story 5

- [X] T039 [US5] Add retry-with-backoff logic to `notification-service/src/main/java/com/eventflow/notificationservice/service/NotificationService.java` (retryCount ≤ 3, backoff between attempts, status RETRYING → FAILED; never rethrow into Kafka consumer)

**Checkpoint**: Core chain resilient to notification failures

---

## Phase 8: User Story 6 - Dashboard Visibility (Priority: P2)

**Goal**: Operators see the notification service in the dashboard health grid and view real email history.

**Independent Test**: Start notification-service — dashboard shows it UP (6/6 core services); Email & Alerts page lists real notifications from the API.

### Implementation for User Story 6

- [X] T040 [US6] Add `location /api/v1/notifications { proxy_pass http://notification-service:8085/api/v1/notifications; ... }` to `dashboard/nginx.conf`
- [X] T041 [P] [US6] Add `Notification` type to `dashboard/src/types/index.ts` and `notificationApi` (list/get/health) to `dashboard/src/services/api.ts`
- [X] T042 [US6] Add `Notification Service` entry to the `SERVICES` health-check array in `dashboard/src/pages/DashboardPage.tsx` (URL `/api/v1/notifications/health`)
- [X] T043 [US6] Replace mock data in `dashboard/src/pages/EmailPage.tsx` with `notificationApi` queries (email history list)

**Checkpoint**: Dashboard reflects the full core fleet

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T044 [P] Update `README.md` and `.env.example` with notification-service + `MAIL_*` configuration docs
- [X] T045 Run `mvn clean package` — full 9-module build succeeds (now including notification-service)
- [X] T046 Run `mvn test` — all contract + integration tests green (Testcontainers)
- [X] T047 Deploy `docker compose --env-file ../.env --profile core up -d --build` and run the full `quickstart.md` end-to-end validation (place order → payment → inventory → shipment → 7 email types in Mailtrap, dashboard 6/6 UP)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phases 3-8)**: All depend on Foundational phase completion
  - US1 (Phase 3) → US2 (Phase 4) → US3 (Phase 5) run sequentially because each consumes the previous stage's event
  - US4 (Phase 6) can start in parallel with US1-3 (only needs Phase 2 topics + entities)
  - US5 (Phase 7) depends on US4
  - US6 (Phase 8) depends on Phase 2 deploy wiring
- **Polish (Phase 9)**: Depends on all user stories

### User Story Dependencies

- **US1 (P1)**: After Phase 2 — no deps on other stories
- **US2 (P1)**: After Phase 2 + US1 (needs PaymentProcessed published)
- **US3 (P1)**: After Phase 2 + US2 (needs InventoryReserved published)
- **US4 (P1)**: After Phase 2 only (can run in parallel with US1-3)
- **US5 (P2)**: After US4
- **US6 (P2)**: After Phase 2 (health grid + api) — email history wiring after US4

### Within Each User Story

- Tests MUST be written and FAIL before implementation (constitution Test-First)
- Models before services, services before consumers/endpoints, implementation before integration

### Parallel Opportunities

- T002, T003-T005 (notification-service skeleton) parallel within Phase 1
- T006-T019 foundational tasks: event classes (T006-T010) parallel; publisher updates (T011-T014) parallel; T015-T018 parallel
- US1/US4 consumers can be built in parallel (different modules)
- All tests marked [P] within a story can run in parallel
- Dashboard tasks T041 parallel with T040/T042/T043

---

## Parallel Example: Phase 2 (Foundational)

| Task | Owner |
|------|-------|
| T006-T010 (event classes + BaseEvent) | A |
| T011-T014 (4 publisher updates) | B |
| T015 (incident-detector) | B |
| T016-T018 (notification entities + migration) | C |
| T019 (compose + Dockerfile) | D |

## Parallel Example: User Story 1 + User Story 4

| Task | Owner |
|------|-------|
| T020-T021 (US1 tests) | A |
| T022-T024 (US1 consumer + service) | A |
| T033 (US4 test) | B |
| T034-T037 (US4 consumer + service + controller) | B |

---

## Implementation Strategy

**MVP = User Story 1** (auto-payment on OrderPlaced). It delivers the first link of the chain and is independently testable. Then US2, US3 build the chain. US4 (emails) is the user-facing payoff and can be developed in parallel. US5 hardens retries, US6 surfaces it in the dashboard. Deploy incrementally via docker-compose `core` profile; validate each phase with its Independent Test before moving on.
