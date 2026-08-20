package com.eventflow.incidentquery.domain;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class LogQueryParams {
    private String correlationId;
    private String serviceName;
    private String level;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private int limit;
    private int offset;
}
