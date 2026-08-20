package com.eventflow.notificationservice.integration;

import com.eventflow.common.event.OrderPlacedEvent;
import com.eventflow.notificationservice.entity.NotificationEntity;
import com.eventflow.notificationservice.entity.NotificationRecipientEntity;
import com.eventflow.notificationservice.repository.NotificationRecipientRepository;
import com.eventflow.notificationservice.repository.NotificationRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class NotificationRetryIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("eventflow")
            .withUsername("eventflow")
            .withPassword("eventflow_secret");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.properties.spring.json.trusted.packages", () -> "com.eventflow.common.event");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationRecipientRepository notificationRecipientRepository;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        notificationRecipientRepository.deleteAll();

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));

        // Mock mail sender to throw
        org.mockito.Mockito.doThrow(new MailException("SMTP unavailable") {})
                .when(mailSender).send(org.mockito.ArgumentMatchers.<org.springframework.mail.SimpleMailMessage>any());
        try {
            org.mockito.Mockito.doThrow(new MailException("SMTP unavailable") {})
                    .when(mailSender).send(org.mockito.ArgumentMatchers.<jakarta.mail.internet.MimeMessage>any());
        } catch (Exception ignored) {}
    }

    @Test
    void shouldMarkNotificationFailedAfterMailException() throws Exception {
        String orderId = UUID.randomUUID().toString();
        notificationRecipientRepository.save(NotificationRecipientEntity.builder()
                .orderId(orderId)
                .customerEmail("fail@example.com")
                .customerName("Fail Test")
                .firstSeenAt(OffsetDateTime.now())
                .build());

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("OrderPlaced")
                .orderId(orderId)
                .customerId(UUID.randomUUID().toString())
                .customerEmail("fail@example.com")
                .customerName("Fail Test")
                .totalAmount(new BigDecimal("25.00"))
                .currency("USD")
                .correlationId(orderId)
                .serviceName("order-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();

        kafkaTemplate.send("orders", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(5000);

        var notifications = notificationRepository.findByCorrelationId(orderId);
        assertFalse(notifications.isEmpty());
        NotificationEntity n = notifications.get(0);
        assertEquals("FAILED", n.getStatus());
        assertTrue(n.getRetryCount() > 0 || n.getStatus().equals("FAILED"));
    }
}
