package com.eventflow.incidentanalyzer.repository;

import com.eventflow.incidentanalyzer.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    List<EventEntity> findByIncidentIdOrderByTimestampAsc(UUID incidentId);
}
