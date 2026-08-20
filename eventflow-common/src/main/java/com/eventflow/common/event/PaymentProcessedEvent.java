package com.eventflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PaymentProcessedEvent extends BaseEvent {

    private String orderId;
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String transactionId;
}
