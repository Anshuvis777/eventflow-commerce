package com.eventflow.incidentquery.controller;

import com.eventflow.common.dto.ApiResponse;
import com.eventflow.incidentquery.dto.response.SimilarIncidentResponse;
import com.eventflow.incidentquery.entity.SimilarIncidentEntity;
import com.eventflow.incidentquery.repository.SimilarIncidentRepository;
import com.eventflow.incidentquery.service.VectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SimilarController {

    private final SimilarIncidentRepository similarIncidentRepository;
    private final VectorService vectorService;

    @GetMapping("/incidents/{id}/similar")
    public ResponseEntity<ApiResponse<List<SimilarIncidentResponse>>> getSimilarIncidents(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0.7") double minSimilarity) {

        List<SimilarIncidentEntity> similarEntities = similarIncidentRepository
                .findByIncidentIdOrderBySimilarityScoreDesc(id);

        List<SimilarIncidentResponse> response = similarEntities.stream()
                .map(entity -> new SimilarIncidentResponse(
                        entity.getSimilarIncidentId(),
                        "Similar incident",
                        com.eventflow.incidentquery.domain.Severity.HIGH,
                        "OPEN",
                        entity.getSimilarityScore(),
                        entity.getMatchedOn(),
                        "Root cause analysis pending"
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
