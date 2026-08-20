package com.eventflow.incidentquery.unit;

import com.eventflow.incidentquery.dto.response.LogEntryResponse;
import com.eventflow.incidentquery.dto.response.LogStatsResponse;
import com.eventflow.incidentquery.service.LogQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogQueryServiceTest {

    @Mock
    private com.eventflow.incidentquery.repository.LogEntryRepository logEntryRepository;

    @InjectMocks
    private LogQueryService logQueryService;

    private static final String CORRELATION_ID = "corr-123";
    private static final String SERVICE_NAME = "payment-service";
    private static final String LEVEL = "ERROR";
    private static final OffsetDateTime START = OffsetDateTime.now().minusHours(1);
    private static final OffsetDateTime END = OffsetDateTime.now();

    @Test
    void shouldQueryLogsWithFilters() {
        // Given
        when(logEntryRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        // When
        List<LogEntryResponse> results = logQueryService.queryLogs(CORRELATION_ID, SERVICE_NAME, LEVEL, START, END);

        // Then
        assertThat(results).isNotNull();
    }

    @Test
    void shouldReturnEmptyForNoMatches() {
        // Given
        when(logEntryRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        // When
        List<LogEntryResponse> results = logQueryService.queryLogs(CORRELATION_ID, SERVICE_NAME, LEVEL, START, END);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void shouldGetErrorStats() {
        // Given
        OffsetDateTime startTime = OffsetDateTime.now().minusHours(24);
        OffsetDateTime endTime = OffsetDateTime.now();
        when(logEntryRepository.getErrorStatsByService(any(), any()))
                .thenReturn(List.of());

        // When
        LogStatsResponse stats = logQueryService.getErrorStats(startTime, endTime);

        // Then
        assertThat(stats).isNotNull();
    }
}
