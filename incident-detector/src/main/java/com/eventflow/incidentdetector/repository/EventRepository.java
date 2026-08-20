package com.eventflow.incidentdetector.repository;

import com.eventflow.incidentdetector.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    List<EventEntity> findByCorrelationId(String correlationId);

    List<EventEntity> findByIncidentId(UUID incidentId);

    List<EventEntity> findByIncidentIdOrderByTimestampAsc(UUID incidentId);
}
