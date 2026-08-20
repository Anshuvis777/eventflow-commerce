package com.eventflow.incidentquery.service;

import com.eventflow.incidentquery.dto.request.LogIngestRequest;
import com.eventflow.incidentquery.dto.response.LogEntryResponse;
import com.eventflow.incidentquery.dto.response.LogStatsResponse;
import com.eventflow.incidentquery.entity.LogEntryEntity;
import com.eventflow.incidentquery.mapper.LogMapper;
import com.eventflow.incidentquery.repository.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogQueryService {

    private final LogEntryRepository logEntryRepository;
    private final LogMapper logMapper;

    public List<LogEntryResponse> queryLogs(String correlationId, String serviceName,
                                           String level, OffsetDateTime startTime,
                                           OffsetDateTime endTime) {
        List<LogEntryEntity> logs = logEntryRepository.findByFilters(
                correlationId, serviceName, level, startTime, endTime);

        return logs.stream()
                .map(logMapper::toResponse)
                .toList();
    }

    public LogStatsResponse getErrorStats(OffsetDateTime startTime, OffsetDateTime endTime) {
        List<Object[]> stats = logEntryRepository.getErrorStatsByService(startTime, endTime);

        List<LogStatsResponse.ServiceErrorCount> serviceCounts = new ArrayList<>();
        for (Object[] row : stats) {
            String serviceName = (String) row[0];
            Long errorCount = (Long) row[1];
            serviceCounts.add(new LogStatsResponse.ServiceErrorCount(serviceName, errorCount));
        }

        return new LogStatsResponse(startTime, endTime, serviceCounts);
    }

    @Transactional
    public LogEntryEntity ingestLog(LogIngestRequest request) {
        LogEntryEntity entity = LogEntryEntity.builder()
                .correlationId(request.correlationId())
                .serviceName(request.serviceName())
                .level(request.level())
                .message(request.message())
                .timestamp(request.timestamp())
                .traceId(request.traceId())
                .spanId(request.spanId())
                .build();

        return logEntryRepository.save(entity);
    }
}
