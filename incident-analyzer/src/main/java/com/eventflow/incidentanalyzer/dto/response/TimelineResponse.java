package com.eventflow.incidentanalyzer.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TimelineResponse(
        UUID incidentId,
        List<EventResponse> events,
        long totalDurationSeconds,
        int eventCount,
        List<String> affectedServices,
        OffsetDateTime firstEventAt,
        OffsetDateTime lastEventAt
) {}
