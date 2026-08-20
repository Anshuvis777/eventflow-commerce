package com.eventflow.notificationservice.repository;

import com.eventflow.notificationservice.entity.NotificationRecipientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipientEntity, UUID> {

    Optional<NotificationRecipientEntity> findByOrderId(String orderId);
}
