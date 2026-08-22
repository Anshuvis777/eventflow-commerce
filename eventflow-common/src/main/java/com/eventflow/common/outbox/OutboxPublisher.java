package com.eventflow.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEntity> pending = outboxRepository.findByStatus("PENDING");
        if (pending.isEmpty()) return;

        log.info("Publishing {} pending outbox events", pending.size());

        for (OutboxEntity entity : pending) {
            try {
                kafkaTemplate.send(entity.getTopic(), entity.getAggregateId(), entity.getPayload());
                entity.setStatus("PUBLISHED");
                outboxRepository.save(entity);
                log.debug("Published outbox event: {} -> {}", entity.getEventType(), entity.getTopic());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", entity.getEventId(), e.getMessage());
            }
        }
    }
}
