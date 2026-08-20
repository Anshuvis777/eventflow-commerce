package com.eventflow.incidentdetector.integration;

import com.eventflow.common.event.PaymentFailedEvent;
import com.eventflow.incidentdetector.entity.IncidentEntity;
import com.eventflow.incidentdetector.repository.IncidentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(
        partitions = 1,
        topics = {"business-events"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
)
@ActiveProfiles("test")
class BusinessEventConsumerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("incident_analytics")
            .withUsername("eventflow")
            .withPassword("eventflow_secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9093");
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private IncidentRepository incidentRepository;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
    }

    @Test
    void shouldCreateIncidentFromPaymentFailedEvent() throws Exception {
        // Given
        String correlationId = "test-" + UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentFailed")
                .correlationId(correlationId)
                .serviceName("payment-service")
                .timestamp(OffsetDateTime.now())
                .severity("HIGH")
                .orderId("order-001")
                .paymentId("payment-001")
                .failureReason("CARD_DECLINED")
                .failureMessage("Card was declined")
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .paymentMethod("credit_card")
                .build();

        // When
        kafkaTemplate.send("business-events", correlationId, event);

        // Then
        TimeUnit.SECONDS.sleep(5); // Wait for consumer to process

        Optional<IncidentEntity> incident = incidentRepository.findByCorrelationId(correlationId);
        assertThat(incident).isPresent();
        assertThat(incident.get().getCorrelationId()).isEqualTo(correlationId);
        assertThat(incident.get().getStatus()).isEqualTo(com.eventflow.incidentdetector.domain.IncidentStatus.OPEN);
        assertThat(incident.get().getSeverity()).isEqualTo(com.eventflow.incidentdetector.domain.Severity.HIGH);
    }

    @Test
    void shouldAttachEventToExistingIncident() throws Exception {
        // Given
        String correlationId = "test-" + UUID.randomUUID();

        // First event
        PaymentFailedEvent event1 = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentFailed")
                .correlationId(correlationId)
                .serviceName("payment-service")
                .timestamp(OffsetDateTime.now().minusMinutes(5))
                .severity("HIGH")
                .orderId("order-002")
                .paymentId("payment-002")
                .failureReason("CARD_DECLINED")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .build();

        kafkaTemplate.send("business-events", correlationId, event1);
        TimeUnit.SECONDS.sleep(3);

        // Second event
        PaymentFailedEvent event2 = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentFailed")
                .correlationId(correlationId)
                .serviceName("payment-service")
                .timestamp(OffsetDateTime.now())
                .severity("HIGH")
                .orderId("order-003")
                .paymentId("payment-003")
                .failureReason("PROCESSING_ERROR")
                .amount(new BigDecimal("75.00"))
                .currency("USD")
                .build();

        kafkaTemplate.send("business-events", correlationId, event2);
        TimeUnit.SECONDS.sleep(3);

        // Then
        Optional<IncidentEntity> incident = incidentRepository.findByCorrelationId(correlationId);
        assertThat(incident).isPresent();
    }
}
