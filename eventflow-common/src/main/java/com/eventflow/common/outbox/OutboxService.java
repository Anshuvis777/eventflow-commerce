package com.eventflow.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public void saveEvent(String eventId, String aggregateId, String aggregateType,
                          String eventType, String topic, Object eventPayload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String payload = mapper.writeValueAsString(eventPayload);

            OutboxEntity entity = OutboxEntity.builder()
                    .eventId(eventId)
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .topic(topic)
                    .payload(payload)
                    .status("PENDING")
                    .build();

            outboxRepository.save(entity);
            log.debug("Outbox event saved: {} -> topic {}", eventType, topic);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }
}
