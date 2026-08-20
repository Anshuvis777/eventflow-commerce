package com.eventflow.incidentdetector.controller;

import com.eventflow.common.dto.ApiResponse;
import com.eventflow.incidentdetector.dto.request.EventIngestRequest;
import com.eventflow.incidentdetector.dto.response.IncidentResponse;
import com.eventflow.incidentdetector.entity.IncidentEntity;
import com.eventflow.incidentdetector.mapper.IncidentMapper;
import com.eventflow.incidentdetector.repository.IncidentRepository;
import com.eventflow.incidentdetector.service.IncidentDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentDetectionService incidentDetectionService;
    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;

    @GetMapping("/incidents")
    public ResponseEntity<ApiResponse<List<IncidentResponse>>> listIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        List<IncidentEntity> incidents = incidentRepository.findAll();
        List<IncidentResponse> response = incidents.stream()
                .map(incidentMapper::toResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncident(@PathVariable UUID id) {
        IncidentEntity incident = incidentRepository.findById(id)
                .orElseThrow(() -> new com.eventflow.common.exception.ResourceNotFoundException(
                        "Incident", "id", id));

        return ResponseEntity.ok(ApiResponse.success(incidentMapper.toResponse(incident)));
    }

    @PostMapping("/incidents")
    public ResponseEntity<ApiResponse<IncidentResponse>> createIncident(
            @Valid @RequestBody EventIngestRequest request) {

        IncidentEntity incident = incidentDetectionService.processEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(incidentMapper.toResponse(incident)));
    }

    @PatchMapping("/incidents/{id}")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateIncident(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates) {

        IncidentEntity incident = incidentRepository.findById(id)
                .orElseThrow(() -> new com.eventflow.common.exception.ResourceNotFoundException(
                        "Incident", "id", id));

        if (updates.containsKey("status")) {
            incident.setStatus(com.eventflow.incidentdetector.domain.IncidentStatus.valueOf(
                    (String) updates.get("status")));
        }

        IncidentEntity updated = incidentRepository.save(incident);
        return ResponseEntity.ok(ApiResponse.success(incidentMapper.toResponse(updated)));
    }
}
