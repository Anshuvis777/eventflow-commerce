package com.eventflow.incidentdetector.dto.response;

import com.eventflow.incidentdetector.domain.Severity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String eventId,
        String eventType,
        String serviceName,
        OffsetDateTime timestamp,
        String payload,
        Severity severity,
        OffsetDateTime createdAt
) {}
