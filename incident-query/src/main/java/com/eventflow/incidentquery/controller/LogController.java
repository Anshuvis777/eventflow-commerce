package com.eventflow.incidentquery.controller;

import com.eventflow.common.dto.ApiResponse;
import com.eventflow.incidentquery.dto.request.LogIngestRequest;
import com.eventflow.incidentquery.dto.response.LogEntryResponse;
import com.eventflow.incidentquery.dto.response.LogStatsResponse;
import com.eventflow.incidentquery.service.LogQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogQueryService logQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LogEntryResponse>>> getLogs(
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime) {

        List<LogEntryResponse> logs = logQueryService.queryLogs(
                correlationId, serviceName, level, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/errors/stats")
    public ResponseEntity<ApiResponse<LogStatsResponse>> getErrorStats(
            @RequestParam OffsetDateTime startTime,
            @RequestParam OffsetDateTime endTime) {

        LogStatsResponse stats = logQueryService.getErrorStats(startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> ingestLog(
            @Valid @RequestBody LogIngestRequest request) {
        logQueryService.ingestLog(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Log ingested successfully"));
    }
}
