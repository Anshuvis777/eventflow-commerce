package com.eventflow.incidentdetector.entity;

import com.eventflow.common.entity.BaseEntity;
import com.eventflow.incidentdetector.domain.IncidentStatus;
import com.eventflow.incidentdetector.domain.Severity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "incidents")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IncidentEntity extends BaseEntity {

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "affected_services", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> affectedServices = List.of();

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "first_event_at", nullable = false)
    private OffsetDateTime firstEventAt;

    @Column(name = "last_event_at")
    private OffsetDateTime lastEventAt;

    @Column(name = "chroma_collection_id")
    private String chromaCollectionId;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EventEntity> events = List.of();
}
