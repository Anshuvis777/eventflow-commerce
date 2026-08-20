package com.eventflow.incidentquery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record LogIngestRequest(
        @NotBlank String correlationId,
        @NotBlank String serviceName,
        @NotBlank String level,
        @NotBlank String message,
        @NotNull OffsetDateTime timestamp,
        String traceId,
        String spanId
) {}
