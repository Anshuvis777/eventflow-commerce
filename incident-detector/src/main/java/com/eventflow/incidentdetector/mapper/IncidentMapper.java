package com.eventflow.incidentdetector.mapper;

import com.eventflow.incidentdetector.dto.response.IncidentResponse;
import com.eventflow.incidentdetector.entity.IncidentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IncidentMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "correlationId", source = "correlationId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "severity", source = "severity")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "affectedServices", source = "affectedServices")
    @Mapping(target = "durationSeconds", source = "durationSeconds")
    @Mapping(target = "firstEventAt", source = "firstEventAt")
    @Mapping(target = "lastEventAt", source = "lastEventAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    IncidentResponse toResponse(IncidentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "events", ignore = true)
    IncidentEntity toEntity(IncidentResponse response);
}
