# Kafka Contracts: Event-Driven Commerce Flow

**Date**: 2026-08-18
**Feature**: [spec.md](../spec.md) | [data-model.md](../data-model.md)

## Topics

| Topic | Producer | Events | Consumers |
|-------|----------|--------|-----------|
| `orders` | order-service | OrderPlaced, OrderCancelled | payment-service, notification-service, incident-detector |
| `payments` | payment-service | PaymentProcessed, PaymentFailed | inventory-service, notification-service, incident-detector |
| `inventory` | inventory-service | InventoryReserved, InventoryReservationFailed, InventoryReleased | shipping-service, notification-service, incident-detector |
| `shipments` | shipping-service | ShipmentCreated, ShipmentDelivered | notification-service, incident-detector |

**Key**: `orderId` (String) — the correlation key for the whole chain.

**Delivery**: at-least-once → all consumers MUST be idempotent.

## Base Event Envelope

Every event serializes as JSON with the `event_type` discriminator (from `BaseEvent` `@JsonTypeInfo`):

```json
{
  "event_type": "OrderPlaced",
  "event_id": "uuid",
  "eventType": "OrderPlaced",
  "correlation_id": "order-uuid",
  "correlationId": "order-uuid",
  "service_name": "order-service",
  "serviceName": "order-service",
  "timestamp": "2026-08-18T10:00:00Z",
  "severity": "INFO",
  "...": "domain fields below"
}
```

## Event Schemas

### OrderPlacedEvent (`orders` topic)

```json
{
  "event_type": "OrderPlaced",
  "orderId": "uuid",
  "customerId": "uuid",
  "customerEmail": "customer@example.com",
  "customerName": "Jane Doe",
  "items": [{"productId": "p1", "productName": "Widget", "quantity": 2, "price": 9.99}],
  "totalAmount": 19.98,
  "currency": "USD"
}
```

### OrderCancelledEvent (`orders` topic)

```json
{
  "event_type": "OrderCancelled",
  "orderId": "uuid",
  "reason": "customer request",
  "cancelledBy": "operator",
  "cancellationMessage": "Order cancelled",
  "refundInitiated": true
}
```

### PaymentProcessedEvent (`payments` topic) — NEW

```json
{
  "event_type": "PaymentProcessed",
  "orderId": "uuid",
  "paymentId": "PAY-123",
  "amount": 19.98,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "transactionId": "TXN-abc123"
}
```

### PaymentFailedEvent (`payments` topic)

```json
{
  "event_type": "PaymentFailed",
  "orderId": "uuid",
  "paymentId": "PAY-123",
  "failureReason": "Payment gateway timeout",
  "failureMessage": "Gateway timeout after 30s",
  "amount": 19.98,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD"
}
```

### InventoryReservedEvent (`inventory` topic) — NEW

```json
{
  "event_type": "InventoryReserved",
  "orderId": "uuid",
  "items": [{"productId": "p1", "productName": "Widget", "quantity": 2, "warehouseId": "WH-1"}],
  "reservedBy": "inventory-service"
}
```

### InventoryReservationFailedEvent (`inventory` topic) — NEW

```json
{
  "event_type": "InventoryReservationFailed",
  "orderId": "uuid",
  "items": [{"productId": "p1", "productName": "Widget", "quantity": 2, "warehouseId": "WH-1"}],
  "failureReason": "Insufficient stock for product: p1",
  "reservedBy": "inventory-service"
}
```

### InventoryReleasedEvent (`inventory` topic)

```json
{
  "event_type": "InventoryReleased",
  "orderId": "uuid",
  "items": [{"productId": "p1", "productName": "Widget", "quantity": 2, "warehouseId": "WH-1"}],
  "releaseReason": "Stock released due to order cancellation",
  "releasedBy": "inventory-service"
}
```

### ShipmentCreatedEvent (`shipments` topic)

```json
{
  "event_type": "ShipmentCreated",
  "orderId": "uuid",
  "shipmentId": "uuid",
  "carrier": "FedEx",
  "trackingNumber": "SHP-123",
  "estimatedDelivery": "2026-08-23T10:00:00Z"
}
```

### ShipmentDeliveredEvent (`shipments` topic)

```json
{
  "event_type": "ShipmentDelivered",
  "orderId": "uuid",
  "shipmentId": "uuid",
  "deliveredTo": "FedEx",
  "signatureRequired": "false"
}
```

## Consumer Groups

| Consumer | Group ID | Topics |
|----------|----------|--------|
| payment-service | `payment-service-group` | orders |
| inventory-service | `inventory-service-group` | payments |
| shipping-service | `shipping-service-group` | inventory |
| notification-service | `notification-service-group` | orders, payments, inventory, shipments |
| incident-detector | `incident-detector-group` | orders, payments, inventory, shipments |

## Idempotency Keys

| Consumer | Idempotency check |
|----------|-------------------|
| payment-service | skip if payment exists for `orderId` |
| inventory-service | skip if stock reserved for `orderId` + `productId` |
| shipping-service | skip if shipment exists for `orderId` |
| notification-service | skip if notification exists for `event_id` |