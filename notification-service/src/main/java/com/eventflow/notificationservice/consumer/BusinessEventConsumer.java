package com.eventflow.notificationservice.consumer;

import com.eventflow.common.event.*;
import com.eventflow.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = {"orders", "payments", "inventory", "shipments"},
            groupId = "notification-service-group",
            properties = {
                "spring.json.value.default.type=com.eventflow.common.event.BaseEvent"
            }
    )
    public void listen(ConsumerRecord<String, Object> record) {
        log.info("Received event from topic: {}, key: {}", record.topic(), record.key());

        try {
            Object value = record.value();
            if (value instanceof BaseEvent event) {
                notificationService.processEvent(event);
            } else {
                log.debug("Ignoring non-BaseEvent: {}", value != null ? value.getClass().getSimpleName() : "null");
            }
        } catch (Exception e) {
            log.error("Failed to process event: {}", e.getMessage(), e);
        }
    }
}
