package com.eventflow.paymentservice.consumer;

import com.eventflow.common.event.OrderPlacedEvent;
import com.eventflow.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "orders",
            groupId = "payment-service-group",
            properties = {
                "spring.json.value.default.type=com.eventflow.common.event.OrderPlacedEvent"
            }
    )
    public void listen(ConsumerRecord<String, OrderPlacedEvent> record) {
        log.info("Received OrderPlaced event from topic: {}, key: {}", record.topic(), record.key());

        try {
            OrderPlacedEvent event = record.value();
            paymentService.processPaymentForOrder(event);
            log.info("Successfully processed payment for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process OrderPlaced event: {}", e.getMessage(), e);
        }
    }
}
