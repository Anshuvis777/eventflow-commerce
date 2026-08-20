package com.eventflow.paymentservice.service;

import com.eventflow.common.event.PaymentFailedEvent;
import com.eventflow.common.event.PaymentProcessedEvent;
import com.eventflow.common.event.OrderPlacedEvent;
import com.eventflow.paymentservice.entity.PaymentEntity;
import com.eventflow.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEntity processPayment(UUID orderId, String orderNumber, UUID customerId,
                                         BigDecimal amount, String currency) {
        log.info("Processing payment for order: {}", orderNumber);

        String paymentNumber = "PAY-" + System.currentTimeMillis();
        boolean success = simulatePaymentGateway(amount);

        PaymentEntity payment = PaymentEntity.builder()
                .paymentNumber(paymentNumber)
                .orderId(orderId)
                .orderNumber(orderNumber)
                .customerId(customerId)
                .amount(amount)
                .currency(currency)
                .status(success ? "COMPLETED" : "FAILED")
                .paymentMethod("CREDIT_CARD")
                .transactionId(success ? "TXN-" + UUID.randomUUID().toString().substring(0, 8) : null)
                .build();

        payment = paymentRepository.save(payment);

        if (success) {
            PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PaymentProcessed")
                    .orderId(orderId.toString())
                    .paymentId(payment.getPaymentNumber())
                    .amount(amount)
                    .currency(currency)
                    .paymentMethod("CREDIT_CARD")
                    .transactionId(payment.getTransactionId())
                    .correlationId(orderId.toString())
                    .serviceName("payment-service")
                    .timestamp(OffsetDateTime.now())
                    .severity("INFO")
                    .build();
            kafkaTemplate.send("payments", orderId.toString(), event);
            log.info("Payment COMPLETED for order: {} — event published", orderNumber);
        } else {
            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PaymentFailed")
                    .orderId(orderId.toString())
                    .paymentId(payment.getPaymentNumber())
                    .failureReason("Payment gateway timeout")
                    .failureMessage("Gateway timeout after 30s")
                    .amount(amount)
                    .currency(currency)
                    .correlationId(orderId.toString())
                    .serviceName("payment-service")
                    .timestamp(OffsetDateTime.now())
                    .severity("WARN")
                    .build();
            kafkaTemplate.send("payments", orderId.toString(), event);
            log.warn("Payment FAILED for order: {} — event published", orderNumber);
        }

        return payment;
    }

    public List<PaymentEntity> getPaymentsByOrder(UUID orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional
    public void processPaymentForOrder(OrderPlacedEvent event) {
        UUID orderId = UUID.fromString(event.getOrderId());

        // Idempotency check — skip if payment already exists for this order
        if (!paymentRepository.findByOrderId(orderId).isEmpty()) {
            log.info("Payment already exists for order: {} — skipping", event.getOrderId());
            return;
        }

        processPayment(orderId, event.getOrderId(),
                UUID.fromString(event.getCustomerId()),
                event.getTotalAmount(), event.getCurrency());
    }

    public PaymentEntity getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    private boolean simulatePaymentGateway(BigDecimal amount) {
        // Simulate random failures (30% chance of failure)
        return Math.random() > 0.3;
    }
}
