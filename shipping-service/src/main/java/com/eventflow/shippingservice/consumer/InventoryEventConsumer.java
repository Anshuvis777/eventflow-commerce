package com.eventflow.shippingservice.consumer;

import com.eventflow.common.event.InventoryReservedEvent;
import com.eventflow.shippingservice.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final ShippingService shippingService;

    @KafkaListener(
            topics = "inventory",
            groupId = "shipping-service-group",
            properties = {
                "spring.json.value.default.type=com.eventflow.common.event.BaseEvent"
            }
    )
    public void listen(ConsumerRecord<String, Object> record) {
        log.info("Received event from topic: {}, key: {}", record.topic(), record.key());

        try {
            Object value = record.value();
            if (value instanceof InventoryReservedEvent event) {
                log.info("Processing InventoryReserved for order: {}", event.getOrderId());
                shippingService.createShipmentForOrder(event);
            } else {
                log.debug("Ignoring non-InventoryReserved event: {}", value != null ? value.getClass().getSimpleName() : "null");
            }
        } catch (Exception e) {
            log.error("Failed to process inventory event: {}", e.getMessage(), e);
        }
    }
}
