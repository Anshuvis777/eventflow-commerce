package com.eventflow.incidentanalyzer.entity;

import com.eventflow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "events")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentEntity incident;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.eventflow.incidentanalyzer.domain.Severity severity;
}
