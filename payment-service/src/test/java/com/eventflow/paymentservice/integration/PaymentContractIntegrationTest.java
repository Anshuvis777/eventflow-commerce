package com.eventflow.paymentservice.integration;

import com.eventflow.common.event.OrderPlacedEvent;
import com.eventflow.common.event.PaymentProcessedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PaymentContractIntegrationTest {

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

    @Test
    void orderPlacedShouldProducePaymentProcessedOnPaymentsTopic() throws Exception {
        String orderId = UUID.randomUUID().toString();

        OrderPlacedEvent orderEvent = OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("OrderPlaced")
                .orderId(orderId)
                .customerId(UUID.randomUUID().toString())
                .customerEmail("contract@example.com")
                .customerName("Contract Test")
                .totalAmount(new BigDecimal("99.99"))
                .currency("USD")
                .correlationId(orderId)
                .serviceName("order-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();

        kafkaTemplate.send("orders", orderId, orderEvent).get(10, TimeUnit.SECONDS);

        // Consume from payments topic with a dedicated consumer
        var consumerProps = Map.<String, Object>of(
                "bootstrap.servers", kafka.getBootstrapServers(),
                "group.id", "contract-test-" + UUID.randomUUID(),
                "auto.offset.reset", "earliest",
                "key.deserializer", StringDeserializer.class,
                "value.deserializer", JsonDeserializer.class,
                "spring.json.trusted.packages", "com.eventflow.common.event"
        );

        try (var consumer = new KafkaConsumer<String, PaymentProcessedEvent>(consumerProps,
                new StringDeserializer(),
                new org.springframework.kafka.support.serializer.JsonDeserializer<PaymentProcessedEvent>()
                        .trustedPackages("com.eventflow.common.event"))) {
            consumer.subscribe(Collections.singletonList("payments"));

            ConsumerRecords<String, PaymentProcessedEvent> records = consumer.poll(Duration.ofSeconds(15));

            boolean found = false;
            for (ConsumerRecord<String, PaymentProcessedEvent> record : records) {
                if (orderId.equals(record.value().getOrderId())) {
                    found = true;
                    assertNotNull(record.value().getPaymentId());
                    assertNotNull(record.value().getAmount());
                    assertEquals("USD", record.value().getCurrency());
                    break;
                }
            }
            assertTrue(found, "PaymentProcessed event should appear on payments topic");
        }
    }
}
