package com.eventflow.incidentquery.controller;

import com.eventflow.common.dto.ApiResponse;
import com.eventflow.incidentquery.entity.IncidentEntity;
import com.eventflow.incidentquery.service.IncidentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentQueryService incidentQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IncidentEntity>>> getIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {
        List<IncidentEntity> incidents = incidentQueryService.listIncidents(status, severity);
        return ResponseEntity.ok(ApiResponse.success(incidents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentEntity>> getIncident(@PathVariable UUID id) {
        IncidentEntity incident = incidentQueryService.getIncident(id);
        return ResponseEntity.ok(ApiResponse.success(incident));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentEntity>> updateIncident(
            @PathVariable UUID id,
            @RequestBody Map<String, String> updates) {
        IncidentEntity incident = incidentQueryService.updateIncident(
                id, updates.get("status"), updates.get("title"));
        return ResponseEntity.ok(ApiResponse.success(incident));
    }
}
