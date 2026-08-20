package com.eventflow.incidentquery.dto.response;

import com.eventflow.incidentquery.domain.Severity;

import java.util.UUID;

public record SimilarIncidentResponse(
        UUID incidentId,
        String title,
        Severity severity,
        String status,
        Float similarityScore,
        String matchedOn,
        String rootCauseSummary
) {}
