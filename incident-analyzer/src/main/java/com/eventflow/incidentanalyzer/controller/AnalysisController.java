package com.eventflow.incidentanalyzer.controller;

import com.eventflow.common.dto.ApiResponse;
import com.eventflow.incidentanalyzer.dto.request.AnalysisTriggerRequest;
import com.eventflow.incidentanalyzer.dto.response.AnalysisResponse;
import com.eventflow.incidentanalyzer.dto.response.TimelineResponse;
import com.eventflow.incidentanalyzer.service.AnalysisOrchestrationService;
import com.eventflow.incidentanalyzer.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnalysisController {

    private final TimelineService timelineService;
    private final AnalysisOrchestrationService analysisOrchestrationService;

    @GetMapping("/incidents/{id}/timeline")
    public ResponseEntity<ApiResponse<TimelineResponse>> getIncidentTimeline(
            @PathVariable UUID id) {
        TimelineResponse timeline = timelineService.getTimeline(id);
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }

    @GetMapping("/incidents/{id}/analysis")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysis(
            @PathVariable UUID id) {
        AnalysisResponse analysis = analysisOrchestrationService.getAnalysis(id);
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    @PostMapping("/incidents/{id}/analysis")
    public ResponseEntity<ApiResponse<AnalysisResponse>> triggerAnalysis(
            @PathVariable UUID id,
            @RequestBody(required = false) AnalysisTriggerRequest request) {
        if (request == null) {
            request = new AnalysisTriggerRequest(false);
        }
        AnalysisResponse analysis = analysisOrchestrationService.triggerAnalysis(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Analysis triggered", analysis));
    }
}
