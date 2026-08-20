package com.eventflow.inventoryservice.integration;

import com.eventflow.common.event.PaymentProcessedEvent;
import com.eventflow.inventoryservice.entity.InventoryEntity;
import com.eventflow.inventoryservice.repository.InventoryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PaymentEventConsumerIntegrationTest {

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
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        InventoryEntity inv = InventoryEntity.builder()
                .productId("prod-1")
                .productName("Widget")
                .quantity(100)
                .reserved(0)
                .warehouseLocation("WH-1")
                .build();
        inventoryRepository.save(inv);
    }

    @Test
    void shouldReserveStockOnPaymentProcessed() throws Exception {
        String orderId = UUID.randomUUID().toString();

        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentProcessed")
                .orderId(orderId)
                .paymentId("PAY-" + System.currentTimeMillis())
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .paymentMethod("CREDIT_CARD")
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8))
                .correlationId(orderId)
                .serviceName("payment-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();

        kafkaTemplate.send("payments", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(5000);

        Optional<InventoryEntity> inv = inventoryRepository.findByProductId("prod-1");
        assertTrue(inv.isPresent());
        assertEquals(1, inv.get().getReserved(), "Stock should be reserved");
    }

    @Test
    void shouldNotReserveOnPaymentFailed() throws Exception {
        String orderId = UUID.randomUUID().toString();

        var event = com.eventflow.common.event.PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentFailed")
                .orderId(orderId)
                .paymentId("PAY-" + System.currentTimeMillis())
                .failureReason("Gateway timeout")
                .failureMessage("Timeout")
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .correlationId(orderId)
                .serviceName("payment-service")
                .timestamp(OffsetDateTime.now())
                .severity("WARN")
                .build();

        kafkaTemplate.send("payments", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(3000);

        Optional<InventoryEntity> inv = inventoryRepository.findByProductId("prod-1");
        assertTrue(inv.isPresent());
        assertEquals(0, inv.get().getReserved(), "Stock should NOT be reserved on payment failure");
    }
}
