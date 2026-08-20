package com.eventflow.notificationservice.repository;

import com.eventflow.notificationservice.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    Optional<NotificationEntity> findByEventId(String eventId);

    List<NotificationEntity> findByCorrelationId(String correlationId);

    List<NotificationEntity> findByStatus(String status);

    List<NotificationEntity> findByEventType(String eventType);
}
