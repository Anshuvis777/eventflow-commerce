package com.eventflow.incidentdetector.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record EventIngestRequest(
        @NotBlank(message = "Event ID is required")
        String eventId,

        @NotBlank(message = "Event type is required")
        String eventType,

        @NotBlank(message = "Correlation ID is required")
        String correlationId,

        @NotBlank(message = "Service name is required")
        String serviceName,

        @NotNull(message = "Timestamp is required")
        OffsetDateTime timestamp,

        @NotBlank(message = "Severity is required")
        String severity,

        @NotBlank(message = "Payload is required")
        String payload
) {}
