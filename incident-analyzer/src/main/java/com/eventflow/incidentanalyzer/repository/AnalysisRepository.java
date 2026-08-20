package com.eventflow.incidentanalyzer.repository;

import com.eventflow.incidentanalyzer.entity.AnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisRepository extends JpaRepository<AnalysisEntity, UUID> {

    Optional<AnalysisEntity> findByIncidentId(UUID incidentId);
}
