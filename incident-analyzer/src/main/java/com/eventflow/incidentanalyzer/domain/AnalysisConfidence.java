package com.eventflow.incidentanalyzer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisConfidence {

    private int score;

    public static AnalysisConfidence high(int score) {
        if (score < 70) {
            throw new IllegalArgumentException("High confidence must be >= 70");
        }
        return new AnalysisConfidence(score);
    }

    public static AnalysisConfidence low(int score) {
        if (score >= 70) {
            throw new IllegalArgumentException("Low confidence must be < 70");
        }
        return new AnalysisConfidence(score);
    }

    public boolean isHighConfidence() {
        return score >= 70;
    }
}
