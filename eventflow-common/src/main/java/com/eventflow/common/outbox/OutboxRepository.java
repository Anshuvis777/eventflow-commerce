package com.eventflow.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    List<OutboxEntity> findByStatus(String status);

    boolean existsByEventId(String eventId);
}
