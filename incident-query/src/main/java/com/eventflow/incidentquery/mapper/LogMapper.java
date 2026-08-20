package com.eventflow.incidentquery.mapper;

import com.eventflow.incidentquery.dto.response.LogEntryResponse;
import com.eventflow.incidentquery.entity.LogEntryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LogMapper {

    LogEntryResponse toResponse(LogEntryEntity entity);
}
