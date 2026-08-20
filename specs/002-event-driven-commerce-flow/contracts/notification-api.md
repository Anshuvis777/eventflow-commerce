# REST Contract: Notification Service

**Date**: 2026-08-18
**Feature**: [spec.md](../spec.md)

Base path: `/api/v1/notifications` (proxied by nginx → `notification-service:8085`)

All responses use the shared `ApiResponse<T>` envelope from `eventflow-common`:

```json
{ "success": true, "message": "...", "data": { } }
```

## GET /api/v1/notifications

List email history (FR-024). Optional query filters.

**Query params** (all optional):
- `correlationId` — filter by orderId
- `status` — SENT | FAILED | RETRYING
- `eventType` — OrderPlaced, PaymentProcessed, etc.

**Response 200**:

```json
{
  "success": true,
  "message": "Notifications retrieved",
  "data": [
    {
      "id": "uuid",
      "eventId": "uuid",
      "correlationId": "order-uuid",
      "eventType": "OrderPlaced",
      "recipient": "customer@example.com",
      "subject": "Order Confirmed",
      "status": "SENT",
      "sentAt": "2026-08-18T10:00:00Z"
    }
  ]
}
```

## GET /api/v1/notifications/{id}

Get a single notification by id.

**Response 200**: single notification object (as above, plus `body`).

**Response 404**: `{ "success": false, "message": "Notification not found", "data": null }`

## GET /api/v1/notifications/health

Health check used by the dashboard grid (FR-023).

**Response 200**: `{ "success": true, "message": "UP", "data": { "status": "UP" } }`

## Error Responses

Standard `GlobalExceptionHandler` from `eventflow-common`:
- 400 — invalid query param (e.g. bad status value)
- 404 — notification not found
- 500 — unexpected error

## Email Templates (subject lines)

| Event | Subject |
|-------|---------|
| OrderPlaced | "Order Confirmed — {orderNumber}" |
| PaymentProcessed | "Payment Received — {orderNumber}" |
| PaymentFailed | "Payment Failed — {orderNumber}" |
| InventoryReservationFailed | "Insufficient Stock — {orderNumber}" |
| ShipmentCreated | "Your Order Has Shipped — {orderNumber}" |
| ShipmentDelivered | "Your Order Has Been Delivered — {orderNumber}" |
| OrderCancelled | "Order Cancelled — {orderNumber}" |