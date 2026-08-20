package com.eventflow.incidentanalyzer.repository;

import com.eventflow.incidentanalyzer.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

    Optional<IncidentEntity> findByCorrelationId(String correlationId);
}
