package com.eventflow.orderservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String orderNumber,
    UUID customerId,
    String customerName,
    String status,
    BigDecimal totalAmount,
    String currency,
    String shippingAddress,
    OffsetDateTime createdAt
) {}
