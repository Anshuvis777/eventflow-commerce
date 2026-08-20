# Quickstart: Event-Driven Commerce Flow

**Date**: 2026-08-18
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

This guide validates the feature end-to-end. Contracts: [kafka-topics.md](contracts/kafka-topics.md), [notification-api.md](contracts/notification-api.md). Data model: [data-model.md](data-model.md).

## Prerequisites

- Docker + docker-compose
- `.env` populated (Neon, Aiven Kafka, Redis) — see `.env.example`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` added to `.env` (Mailtrap for dev)
- Maven 3.9.9 + Java 21 (for local build)

## 1. Build

```powershell
mvn clean package -DskipTests
```

Expected: all modules compile, including new `notification-service` and updated `eventflow-common`.

## 2. Run tests

```powershell
mvn test
```

Expected: Testcontainers integration tests pass for each consumer (payment auto-process, inventory reserve, shipment create, notification email) and contract tests pass.

## 3. Deploy

```powershell
cd docker
docker compose --env-file ../.env --profile core up -d --build
```

Expected: 6 containers UP — order-service (8081), payment-service (8082), inventory-service (8083), shipping-service (8084), notification-service (8085), dashboard (3000).

## 4. Verify topics exist

```powershell
docker exec notification-service sh -c "echo 'topics check via logs'"
docker logs notification-service | Select-String "partitions assigned"
```

Expected: notification-service consumer assigned partitions on `orders`, `payments`, `inventory`, `shipments`.

## 5. End-to-end chain validation

### 5a. Place an order (triggers OrderPlaced → orders topic)

```powershell
curl.exe -s -X POST http://localhost:3000/api/v1/orders `
  -H "Content-Type: application/json" `
  --data-binary '{"customerId":"11111111-1111-1111-1111-111111111111","customerName":"Jane Doe","customerEmail":"jane@example.com","items":[{"productId":"p1","productName":"Widget","quantity":2,"price":9.99}],"shippingAddress":"123 Main St"}'
```

Expected: 201 with order data.

### 5b. Verify payment auto-processed (no manual call)

```powershell
curl.exe -s http://localhost:3000/api/v1/payments/order/<ORDER_ID>
```

Expected: a payment record exists with status COMPLETED or FAILED — created automatically by payment-service consuming `orders`.

### 5c. Verify inventory reserved

```powershell
curl.exe -s http://localhost:3000/api/v1/inventory
```

Expected: `reserved` increased for the ordered product.

### 5d. Verify shipment created

```powershell
curl.exe -s http://localhost:3000/api/v1/shipments
```

Expected: a shipment exists for the order with status SHIPPED.

### 5e. Verify email history

```powershell
curl.exe -s "http://localhost:3000/api/v1/notifications?correlationId=<ORDER_ID>"
```

Expected: notifications for OrderPlaced, PaymentProcessed (or PaymentFailed), ShipmentCreated — each with status SENT. Check the Mailtrap inbox for the actual emails.

## 6. Verify dashboard

Open `http://localhost:3000`:
- Health grid shows **Notification Service: UP** (6/6 core services)
- **Email & Alerts** page lists real notifications from `/api/v1/notifications` (no longer mock data)

## 7. Failure-path validation

### Payment failure (30% simulated failure rate — retry until FAILED)

```powershell
curl.exe -s "http://localhost:3000/api/v1/notifications?eventType=PaymentFailed"
```

Expected: a PaymentFailed notification exists; NO inventory reservation and NO shipment for that order.

### Idempotency (redelivery)

Restart notification-service while an order is in flight, then re-check:

```powershell
curl.exe -s "http://localhost:3000/api/v1/notifications?correlationId=<ORDER_ID>"
```

Expected: no duplicate notifications (each `event_id` processed once).

## Expected Outcomes (Success Criteria)

- Payment auto-processed < 5s after order placed (SC-001)
- Full chain completes < 30s (SC-002)
- Each stage email sent < 30s after its event (SC-003)
- Zero duplicate emails/payments/reservations/shipments (SC-005)
- Email outage does not block order flow (SC-006)
- Dashboard shows notification service UP + email history (SC-007)