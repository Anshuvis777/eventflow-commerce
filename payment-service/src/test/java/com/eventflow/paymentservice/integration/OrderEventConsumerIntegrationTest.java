package com.eventflow.paymentservice.integration;

import com.eventflow.common.event.OrderPlacedEvent;
import com.eventflow.paymentservice.entity.PaymentEntity;
import com.eventflow.paymentservice.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderEventConsumerIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Test
    void shouldCreatePaymentOnOrderPlaced() throws Exception {
        String orderId = UUID.randomUUID().toString();

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

        // Verify payment was created via direct DB check
        var payments = paymentRepository.findByOrderId(UUID.fromString(orderId));
        assertFalse(payments.isEmpty(), "Payment should have been created for order");
        assertEquals("COMPLETED", payments.get(0).getStatus());
    }

    @Test
    void shouldNotCreateDuplicatePaymentOnRepublish() throws Exception {
        String orderId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();

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

        // Send same event twice
        kafkaTemplate.send("orders", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(3000);
        kafkaTemplate.send("orders", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(3000);

        var payments = paymentRepository.findByOrderId(UUID.fromString(orderId));
        assertEquals(1, payments.size(), "Should have exactly one payment (idempotent)");
    }
}
