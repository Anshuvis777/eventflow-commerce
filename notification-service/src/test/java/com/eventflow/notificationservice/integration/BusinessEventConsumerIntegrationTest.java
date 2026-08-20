package com.eventflow.notificationservice.integration;

import com.eventflow.common.event.*;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
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
class BusinessEventConsumerIntegrationTest {

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
    }

    private void seedRecipient(String orderId, String email, String name) {
        notificationRecipientRepository.save(NotificationRecipientEntity.builder()
                .orderId(orderId)
                .customerEmail(email)
                .customerName(name)
                .firstSeenAt(OffsetDateTime.now())
                .build());
    }

    @Test
    void shouldCreateNotificationOnOrderPlaced() throws Exception {
        String orderId = UUID.randomUUID().toString();
        seedRecipient(orderId, "test@example.com", "Test User");

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("OrderPlaced")
                .orderId(orderId)
                .customerId(UUID.randomUUID().toString())
                .customerEmail("test@example.com")
                .customerName("Test User")
                .totalAmount(new BigDecimal("49.99"))
                .currency("USD")
                .correlationId(orderId)
                .serviceName("order-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();

        kafkaTemplate.send("orders", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(5000);

        var notifications = notificationRepository.findByCorrelationId(orderId);
        assertFalse(notifications.isEmpty(), "Should have created notification");
        assertEquals("SENT", notifications.get(0).getStatus());
        assertTrue(notifications.get(0).getSubject().contains("Order Confirmed"));
    }

    @Test
    void shouldNotDuplicateOnRepublish() throws Exception {
        String orderId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        seedRecipient(orderId, "test@example.com", "Test User");

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventId(eventId)
                .eventType("OrderPlaced")
                .orderId(orderId)
                .customerId(UUID.randomUUID().toString())
                .customerEmail("test@example.com")
                .customerName("Test User")
                .totalAmount(new BigDecimal("49.99"))
                .currency("USD")
                .correlationId(orderId)
                .serviceName("order-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();

        kafkaTemplate.send("orders", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(3000);
        kafkaTemplate.send("orders", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(3000);

        var notifications = notificationRepository.findByCorrelationId(orderId);
        assertEquals(1, notifications.size(), "Should have exactly one notification (idempotent)");
    }

    @Test
    void shouldSkipWhenNoRecipient() throws Exception {
        String orderId = UUID.randomUUID().toString();
        // No recipient seeded — should skip without crash

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("OrderPlaced")
                .orderId(orderId)
                .customerId(UUID.randomUUID().toString())
                .customerEmail("nobody@example.com")
                .customerName("Ghost")
                .totalAmount(new BigDecimal("10.00"))
                .currency("USD")
                .correlationId(orderId)
                .serviceName("order-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();

        kafkaTemplate.send("orders", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(3000);

        var notifications = notificationRepository.findByCorrelationId(orderId);
        assertTrue(notifications.isEmpty(), "Should have no notifications when no recipient");
    }
}
