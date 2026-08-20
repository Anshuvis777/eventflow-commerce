package com.eventflow.incidentdetector.consumer;

import com.eventflow.incidentdetector.dto.request.EventIngestRequest;
import com.eventflow.incidentdetector.entity.IncidentEntity;
import com.eventflow.incidentdetector.service.IncidentDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessEventConsumer {

    private final IncidentDetectionService incidentDetectionService;

    @KafkaListener(
            topics = {"orders", "payments", "inventory", "shipments"},
            groupId = "incident-detector-group",
            properties = {
                "spring.json.value.default.type=com.fasterxml.jackson.databind.JsonNode"
            }
    )
    public void listen(ConsumerRecord<String, Object> record) {
        log.info("Received event from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        try {
            Object rawValue = record.value();
            if (rawValue == null) {
                log.warn("Received empty/null event record value");
                return;
            }

            EventIngestRequest request;
            if (rawValue instanceof com.eventflow.common.event.BaseEvent baseEvent) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                String payloadJson = mapper.writeValueAsString(baseEvent);

                request = EventIngestRequest.builder()
                        .eventId(baseEvent.getEventId())
                        .eventType(baseEvent.getEventType())
                        .correlationId(baseEvent.getCorrelationId())
                        .serviceName(baseEvent.getServiceName())
                        .timestamp(baseEvent.getTimestamp())
                        .severity(baseEvent.getSeverity())
                        .payload(payloadJson)
                        .build();
            } else if (rawValue instanceof Map) {
                Map<String, Object> event = (Map<String, Object>) rawValue;
                request = EventIngestRequest.builder()
                        .eventId((String) event.get("event_id"))
                        .eventType((String) event.get("event_type"))
                        .correlationId((String) event.get("correlation_id"))
                        .serviceName((String) event.get("service_name"))
                        .timestamp(OffsetDateTime.parse((String) event.get("timestamp")))
                        .severity((String) event.get("severity"))
                        .payload(event.get("payload") != null ? event.get("payload").toString() : "{}")
                        .build();
            } else {
                log.error("Unknown event payload class: {}", rawValue.getClass().getName());
                return;
            }

            IncidentEntity incident = incidentDetectionService.processEvent(request);
            log.info("Successfully processed event for incident: {}", incident.getId());

        } catch (Exception e) {
            log.error("Failed to process Kafka event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
