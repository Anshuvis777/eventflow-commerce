package com.eventflow.incidentdetector.dto.response;

import com.eventflow.incidentdetector.domain.IncidentStatus;
import com.eventflow.incidentdetector.domain.Severity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String correlationId,
        IncidentStatus status,
        Severity severity,
        String title,
        String description,
        List<String> affectedServices,
        Integer durationSeconds,
        OffsetDateTime firstEventAt,
        OffsetDateTime lastEventAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
