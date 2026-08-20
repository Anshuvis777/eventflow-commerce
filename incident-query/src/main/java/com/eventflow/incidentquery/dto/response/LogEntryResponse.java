package com.eventflow.incidentquery.dto.response;

import java.time.OffsetDateTime;

public record LogEntryResponse(
        String correlationId,
        String serviceName,
        String level,
        String message,
        OffsetDateTime timestamp,
        String traceId
) {}
