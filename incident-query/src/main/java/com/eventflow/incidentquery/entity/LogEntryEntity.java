package com.eventflow.incidentquery.entity;

import com.eventflow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "log_entries")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LogEntryEntity extends BaseEntity {

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "level", nullable = false)
    private String level;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "span_id")
    private String spanId;
}
