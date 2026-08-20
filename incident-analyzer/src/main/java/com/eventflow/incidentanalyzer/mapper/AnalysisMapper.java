package com.eventflow.incidentanalyzer.mapper;

import com.eventflow.incidentanalyzer.dto.response.EventResponse;
import com.eventflow.incidentanalyzer.entity.EventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AnalysisMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "serviceName", source = "serviceName")
    @Mapping(target = "timestamp", source = "timestamp")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "severity", source = "severity")
    @Mapping(target = "createdAt", source = "createdAt")
    EventResponse toEventResponse(EventEntity entity);
}
