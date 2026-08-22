package com.eventflow.incidentanalyzer.entity;

import com.eventflow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analyses")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AnalysisEntity extends BaseEntity {

    @Column(name = "incident_id", nullable = false, unique = true)
    private UUID incidentId;

    @Column(name = "root_cause", nullable = false, columnDefinition = "TEXT")
    private String rootCause;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String impact;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "contributing_factors", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> contributingFactors = List.of();

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "recommended_actions", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> recommendedActions = List.of();

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "prevention_measures", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> preventionMeasures = List.of();

    @Column(name = "confidence_score", nullable = false)
    private Integer confidenceScore;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;
}
