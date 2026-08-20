package com.eventflow.incidentquery.repository;

import com.eventflow.incidentquery.entity.SimilarIncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SimilarIncidentRepository extends JpaRepository<SimilarIncidentEntity, UUID> {

    List<SimilarIncidentEntity> findByIncidentIdOrderBySimilarityScoreDesc(UUID incidentId);
}
