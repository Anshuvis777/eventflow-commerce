package com.eventflow.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderRequest(
    @NotNull UUID customerId,
    String customerName,
    String customerEmail,
    BigDecimal totalAmount,
    String currency,
    @NotBlank String shippingAddress,
    @NotEmpty List<OrderItemRequest> items
) {
    public record OrderItemRequest(
        @NotBlank String productId,
        String productName,
        @NotNull @Min(1) Integer quantity,
        @JsonAlias("unitPrice") @DecimalMin("0.01") BigDecimal price
    ) {}
}
