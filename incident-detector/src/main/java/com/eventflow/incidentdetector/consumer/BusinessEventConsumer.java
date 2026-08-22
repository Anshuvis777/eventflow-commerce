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
                        .severity(baseEvent.getSeverity() != null ? baseEvent.getSeverity() : "INFO")
                        .payload(payloadJson)
                        .build();
            } else {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                com.fasterxml.jackson.databind.JsonNode node = mapper.valueToTree(rawValue);

                String eventId = node.hasNonNull("eventId") ? node.get("eventId").asText() : (node.hasNonNull("event_id") ? node.get("event_id").asText() : UUID.randomUUID().toString());
                String eventType = node.hasNonNull("eventType") ? node.get("eventType").asText() : (node.hasNonNull("event_type") ? node.get("event_type").asText() : "UnknownEvent");
                String correlationId = node.hasNonNull("correlationId") ? node.get("correlationId").asText() : (node.hasNonNull("correlation_id") ? node.get("correlation_id").asText() : (node.hasNonNull("orderId") ? node.get("orderId").asText() : UUID.randomUUID().toString()));
                String serviceName = node.hasNonNull("serviceName") ? node.get("serviceName").asText() : (node.hasNonNull("service_name") ? node.get("service_name").asText() : "eventflow-service");
                String severityStr = node.hasNonNull("severity") ? node.get("severity").asText() : "INFO";
                
                OffsetDateTime ts = OffsetDateTime.now();
                if (node.hasNonNull("timestamp")) {
                    try {
                        ts = OffsetDateTime.parse(node.get("timestamp").asText());
                    } catch (Exception ignored) {}
                }

                request = EventIngestRequest.builder()
                        .eventId(eventId)
                        .eventType(eventType)
                        .correlationId(correlationId)
                        .serviceName(serviceName)
                        .timestamp(ts)
                        .severity(severityStr)
                        .payload(node.toString())
                        .build();
            }

            IncidentEntity incident = incidentDetectionService.processEvent(request);
            log.info("Successfully processed event for incident: {}", incident.getId());

        } catch (Exception e) {
            log.error("Failed to process Kafka event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process Kafka event", e);
        }
    }
}
