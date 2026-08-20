package com.eventflow.incidentanalyzer.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record AnalysisResponse(
        String rootCause,
        String impact,
        List<String> contributingFactors,
        List<String> recommendedActions,
        List<String> preventionMeasures,
        int confidenceScore,
        String modelVersion,
        OffsetDateTime createdAt
) {}
