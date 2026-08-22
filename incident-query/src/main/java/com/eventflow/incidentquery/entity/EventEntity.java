package com.eventflow.incidentquery.entity;

import com.eventflow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventEntity extends BaseEntity {

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "severity", nullable = false)
    private String severity;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;
}
