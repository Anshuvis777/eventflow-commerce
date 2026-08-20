# Data Model: Event-Driven Commerce Flow

**Date**: 2026-08-18
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

## Overview

This feature adds **two new tables** (notification-service) and **two new event schemas** (eventflow-common). Existing core entities (Order, Payment, Inventory, Shipment) are unchanged. All tables live in the shared `eventflow` PostgreSQL database.

## New Entities

### NotificationEntity

Represents an email sent to a customer. Stored by notification-service.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | BaseEntity |
| eventId | String | unique, not null | Kafka event_id — idempotency key |
| correlationId | String | not null | orderId of the chain |
| eventType | String | not null | OrderPlaced, PaymentProcessed, PaymentFailed, InventoryReserved, ShipmentCreated, ShipmentDelivered |
| recipient | String | not null | customer email |
| subject | String | not null | email subject |
| body | String | not null | email body |
| status | String | not null, default SENT | SENT, FAILED, RETRYING |
| retryCount | Integer | not null, default 0 | attempts made (max 3) |
| sentAt | OffsetDateTime | nullable | when sent |

**Validation rules** (from FR-012..FR-017, FR-024, FR-027):
- `eventId` must be unique → duplicate event delivery is skipped (FR-018)
- `recipient` must be a non-empty email
- `status` transitions: `SENT` → (no further transitions); `FAILED` → `RETRYING` (up to 3 attempts with backoff) → `SENT` or `FAILED`

### NotificationRecipientEntity

Maps `orderId → customerEmail`, learned from `OrderPlaced` events so downstream events can be addressed.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK | BaseEntity |
| orderId | String | unique, not null | correlation key |
| customerEmail | String | not null | email to send to |
| customerName | String | nullable | for personalization |
| firstSeenAt | OffsetDateTime | not null | when OrderPlaced was consumed |

**Validation rules**:
- `orderId` must be unique → upsert on repeated OrderPlaced delivery
- If no recipient is known for an order, notification is logged and skipped (no crash)

## New Event Schemas (eventflow-common)

### PaymentProcessedEvent (extends BaseEvent)

| Field | Type | Notes |
|-------|------|-------|
| orderId | String | correlation key |
| paymentId | String | payment number |
| amount | BigDecimal | paid amount |
| currency | String | e.g. USD |
| paymentMethod | String | e.g. CREDIT_CARD |
| transactionId | String | gateway txn ref |

### InventoryReservedEvent (extends BaseEvent)

| Field | Type | Notes |
|-------|------|-------|
| orderId | String | correlation key |
| items | List<ReservedItem> | productId, productName, quantity, warehouseId |
| reservedBy | String | service name |

### InventoryReservationFailedEvent (extends BaseEvent) — NEW

| Field | Type | Notes |
|-------|------|-------|
| orderId | String | correlation key |
| items | List<ReservedItem> | productId, productName, quantity, warehouseId |
| failureReason | String | e.g. "Insufficient stock for product: p1" |
| reservedBy | String | service name |

### OrderPlacedEvent (modified)

Adds two fields (existing fields unchanged):

| Field | Type | Notes |
|-------|------|-------|
| customerEmail | String | NEW — from OrderEntity |
| customerName | String | NEW — from OrderEntity |

## Existing Entities (unchanged, referenced)

- **Order** (`orders` table, order-service): id, order_number, customer_id, customer_name, customer_email, status, total_amount, currency, shipping_address, items_json
- **Payment** (`payments` table, payment-service): id, payment_number, order_id, order_number, customer_id, amount, currency, status (COMPLETED/FAILED), payment_method, transaction_id
- **Inventory** (`inventory` table, inventory-service): id, product_id, product_name, quantity, reserved, price
- **Shipment** (`shipments` table, shipping-service): id, tracking_number, order_id, order_number, customer_id, status (SHIPPED/DELIVERED), carrier, shipping_address, estimated_delivery, actual_delivery

## State Transitions

### Payment (per order)
```
OrderPlaced ──► Payment COMPLETED ──► InventoryReserved ──► Shipment SHIPPED ──► Shipment DELIVERED
        └──► Payment FAILED (chain stops, PaymentFailed email)
```

### Notification status
```
SENT (success)
FAILED ──► RETRYING (up to 3 attempts, backoff) ──► SENT | FAILED
```

## Relationships

```
Order 1 ──► N Payment
Order 1 ──► N Shipment
Order 1 ──► 1 NotificationRecipient (orderId → email)
Order 1 ──► N Notification (one per stage event)
```

All entities extend `BaseEntity` from `eventflow-common` (id, createdAt, updatedAt).