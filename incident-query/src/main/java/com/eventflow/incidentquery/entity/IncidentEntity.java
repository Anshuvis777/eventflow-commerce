package com.eventflow.incidentquery.entity;

import com.eventflow.common.entity.BaseEntity;
import com.eventflow.incidentquery.domain.Severity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

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

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "affected_services", columnDefinition = "jsonb")
    private java.util.List<String> affectedServices;
}
