package com.eventflow.inventoryservice.consumer;

import com.eventflow.common.event.PaymentProcessedEvent;
import com.eventflow.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "payments",
            groupId = "inventory-service-group",
            properties = {
                "spring.json.value.default.type=com.eventflow.common.event.BaseEvent"
            }
    )
    public void listen(ConsumerRecord<String, Object> record) {
        log.info("Received event from topic: {}, key: {}", record.topic(), record.key());

        try {
            Object value = record.value();
            if (value instanceof PaymentProcessedEvent event) {
                log.info("Processing PaymentProcessed for order: {}", event.getOrderId());
                inventoryService.reserveStockForOrder(event);
            } else {
                log.debug("Ignoring non-PaymentProcessed event: {}", value != null ? value.getClass().getSimpleName() : "null");
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
        }
    }
}
