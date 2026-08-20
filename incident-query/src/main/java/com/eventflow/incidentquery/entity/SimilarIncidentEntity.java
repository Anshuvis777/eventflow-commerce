package com.eventflow.incidentquery.entity;

import com.eventflow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "similar_incidents")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SimilarIncidentEntity extends BaseEntity {

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "similar_incident_id", nullable = false)
    private UUID similarIncidentId;

    @Column(name = "similarity_score", nullable = false)
    private Float similarityScore;

    @Column(name = "matched_on", nullable = false)
    private String matchedOn;
}
