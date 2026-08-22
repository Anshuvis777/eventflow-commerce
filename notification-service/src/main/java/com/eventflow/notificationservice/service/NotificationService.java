package com.eventflow.notificationservice.service;

import com.eventflow.common.event.*;
import com.eventflow.notificationservice.entity.NotificationEntity;
import com.eventflow.notificationservice.entity.NotificationRecipientEntity;
import com.eventflow.notificationservice.repository.NotificationRecipientRepository;
import com.eventflow.notificationservice.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Transactional
    public void processEvent(BaseEvent event) {
        // Idempotency check
        if (notificationRepository.findByEventId(event.getEventId()).isPresent()) {
            log.info("Notification already sent for event: {} — skipping", event.getEventId());
            return;
        }

        String eventType = event.getEventType();
        String orderId = extractOrderId(event);

        // Register the recipient from the OrderPlaced event FIRST, so the
        // lookup below succeeds and downstream events can notify this order.
        if (event instanceof OrderPlacedEvent orderEvent) {
            saveOrUpdateRecipient(orderEvent);
        }

        // Look up recipient email or fall back to system admin
        String recipientEmail = "system-alerts@eventflow-commerce.com";
        String customerName = "System Operations Admin";
        
        Optional<NotificationRecipientEntity> recipientOpt = notificationRecipientRepository.findByOrderId(orderId);
        if (recipientOpt.isPresent()) {
            recipientEmail = recipientOpt.get().getCustomerEmail();
            customerName = recipientOpt.get().getCustomerName();
        } else {
            log.info("No customer recipient found for orderId: {} — using fallback admin recipient {}", orderId, recipientEmail);
        }

        // Generate email content
        String subject = buildSubject(eventType, orderId);
        String body = buildBody(eventType, orderId, customerName);

        // Send email with fallback handling
        String status = "SENT";
        int retryCount = 0;

        try {
            sendEmail(recipientEmail, subject, body);
            log.info("Email sent to {} for event: {}", recipientEmail, eventType);
        } catch (Exception e) {
            log.warn("SMTP email send failed for event {} ({}), recording as FAILED / Mock: {}", event.getEventId(), eventType, e.getMessage());
            status = "FAILED";
        }

        // Save notification record
        NotificationEntity notification = NotificationEntity.builder()
                .eventId(event.getEventId())
                .correlationId(orderId)
                .eventType(eventType)
                .recipient(recipientEmail)
                .subject(subject)
                .body(body)
                .status(status)
                .retryCount(retryCount)
                .sentAt(OffsetDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    private void saveOrUpdateRecipient(OrderPlacedEvent event) {
        Optional<NotificationRecipientEntity> existing = notificationRecipientRepository.findByOrderId(event.getOrderId());
        if (existing.isPresent()) {
            NotificationRecipientEntity recipient = existing.get();
            recipient.setCustomerEmail(event.getCustomerEmail());
            recipient.setCustomerName(event.getCustomerName());
            notificationRecipientRepository.save(recipient);
        } else {
            notificationRecipientRepository.save(NotificationRecipientEntity.builder()
                    .orderId(event.getOrderId())
                    .customerEmail(event.getCustomerEmail())
                    .customerName(event.getCustomerName())
                    .firstSeenAt(OffsetDateTime.now())
                    .build());
        }
    }

    private String extractOrderId(BaseEvent event) {
        if (event instanceof OrderPlacedEvent e) return e.getOrderId();
        if (event instanceof PaymentProcessedEvent e) return e.getOrderId();
        if (event instanceof PaymentFailedEvent e) return e.getOrderId();
        if (event instanceof InventoryReservedEvent e) return e.getOrderId();
        if (event instanceof InventoryReservationFailedEvent e) return e.getOrderId();
        if (event instanceof InventoryReleasedEvent e) return e.getOrderId();
        if (event instanceof OrderCancelledEvent e) return e.getOrderId();
        if (event instanceof ShipmentCreatedEvent e) return e.getOrderId();
        if (event instanceof ShipmentDeliveredEvent e) return e.getOrderId();
        return event.getCorrelationId();
    }

    private String buildSubject(String eventType, String orderId) {
        return switch (eventType) {
            case "OrderPlaced" -> "Order Confirmed — " + orderId;
            case "PaymentProcessed" -> "Payment Received — " + orderId;
            case "PaymentFailed" -> "Payment Failed — " + orderId;
            case "InventoryReservationFailed" -> "Insufficient Stock — " + orderId;
            case "ShipmentCreated" -> "Your Order Has Shipped — " + orderId;
            case "ShipmentDelivered" -> "Your Order Has Been Delivered — " + orderId;
            case "OrderCancelled" -> "Order Cancelled — " + orderId;
            default -> "Event Notification — " + eventType;
        };
    }

    private String buildBody(String eventType, String orderId, String customerName) {
        String name = customerName != null ? customerName : "Customer";
        return switch (eventType) {
            case "OrderPlaced" -> "Hi " + name + ",\n\nYour order " + orderId + " has been confirmed.\n\nThank you for your purchase!";
            case "PaymentProcessed" -> "Hi " + name + ",\n\nYour payment for order " + orderId + " has been processed successfully.";
            case "PaymentFailed" -> "Hi " + name + ",\n\nUnfortunately, your payment for order " + orderId + " could not be processed. Please try again.";
            case "InventoryReservationFailed" -> "Hi " + name + ",\n\nWe're sorry, but we couldn't reserve stock for your order " + orderId + " due to insufficient inventory.";
            case "ShipmentCreated" -> "Hi " + name + ",\n\nYour order " + orderId + " has been shipped! You will receive tracking details shortly.";
            case "ShipmentDelivered" -> "Hi " + name + ",\n\nYour order " + orderId + " has been delivered. We hope you enjoy your purchase!";
            case "OrderCancelled" -> "Hi " + name + ",\n\nYour order " + orderId + " has been cancelled.";
            default -> "Hi " + name + ",\n\nThere has been an update to your order " + orderId + ".";
        };
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            String from = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : "alerts@eventflow-commerce.com";
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
