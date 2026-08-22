package com.eventflow.common.outbox;

import com.eventflow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "outbox_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class OutboxEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(nullable = false)
    private String aggregateType;
}
