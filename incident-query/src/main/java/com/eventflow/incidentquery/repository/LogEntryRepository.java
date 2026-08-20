package com.eventflow.incidentquery.repository;

import com.eventflow.incidentquery.entity.LogEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntryEntity, String> {

    List<LogEntryEntity> findByCorrelationId(String correlationId);

    List<LogEntryEntity> findByLevel(String level);

    List<LogEntryEntity> findByServiceName(String serviceName);

    @Query("SELECT l FROM LogEntryEntity l WHERE " +
           "(:correlationId IS NULL OR l.correlationId = :correlationId) AND " +
           "(:serviceName IS NULL OR l.serviceName = :serviceName) AND " +
           "(:level IS NULL OR l.level = :level) AND " +
           "(:startTime IS NULL OR l.timestamp >= :startTime) AND " +
           "(:endTime IS NULL OR l.timestamp <= :endTime) " +
           "ORDER BY l.timestamp DESC")
    List<LogEntryEntity> findByFilters(
            @Param("correlationId") String correlationId,
            @Param("serviceName") String serviceName,
            @Param("level") String level,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    @Query("SELECT l.serviceName, COUNT(l) FROM LogEntryEntity l WHERE " +
           "l.timestamp >= :startTime AND l.timestamp <= :endTime AND " +
           "l.level IN ('ERROR', 'CRITICAL') " +
           "GROUP BY l.serviceName")
    List<Object[]> getErrorStatsByService(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );
}
