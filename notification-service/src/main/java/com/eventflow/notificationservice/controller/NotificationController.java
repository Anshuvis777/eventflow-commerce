package com.eventflow.notificationservice.controller;

import com.eventflow.notificationservice.entity.NotificationEntity;
import com.eventflow.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType) {

        List<NotificationEntity> notifications;

        if (correlationId != null) {
            notifications = notificationRepository.findByCorrelationId(correlationId);
        } else if (status != null) {
            notifications = notificationRepository.findByStatus(status);
        } else if (eventType != null) {
            notifications = notificationRepository.findByEventType(eventType);
        } else {
            notifications = notificationRepository.findAll();
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notifications retrieved",
                "data", notifications.stream().map(n -> Map.<String, Object>of(
                        "id", n.getId(),
                        "eventId", n.getEventId(),
                        "correlationId", n.getCorrelationId(),
                        "eventType", n.getEventType(),
                        "recipient", n.getRecipient(),
                        "subject", n.getSubject(),
                        "status", n.getStatus(),
                        "sentAt", n.getSentAt() != null ? n.getSentAt().toString() : null
                )).toList()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        return notificationRepository.findById(id)
                .map(n -> ResponseEntity.ok(Map.<String, Object>of(
                        "success", true,
                        "message", "Notification retrieved",
                        "data", Map.of(
                                "id", n.getId(),
                                "eventId", n.getEventId(),
                                "correlationId", n.getCorrelationId(),
                                "eventType", n.getEventType(),
                                "recipient", n.getRecipient(),
                                "subject", n.getSubject(),
                                "body", n.getBody(),
                                "status", n.getStatus(),
                                "retryCount", n.getRetryCount(),
                                "sentAt", n.getSentAt() != null ? n.getSentAt().toString() : null
                        )
                )))
                .orElse(ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Notification not found",
                        "data", (Object) null
                )));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "UP",
                "data", Map.of("status", "UP")
        ));
    }
}
