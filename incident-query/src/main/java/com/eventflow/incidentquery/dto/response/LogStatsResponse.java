package com.eventflow.incidentquery.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record LogStatsResponse(
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        List<ServiceErrorCount> services
) {
    public record ServiceErrorCount(
            String serviceName,
            long errorCount
    ) {}
}
