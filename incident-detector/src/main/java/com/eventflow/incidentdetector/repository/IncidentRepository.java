package com.eventflow.incidentdetector.repository;

import com.eventflow.incidentdetector.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

    Optional<IncidentEntity> findByCorrelationId(String correlationId);

    List<IncidentEntity> findByStatus(com.eventflow.incidentdetector.domain.IncidentStatus status);

    List<IncidentEntity> findBySeverity(com.eventflow.incidentdetector.domain.Severity severity);
}
