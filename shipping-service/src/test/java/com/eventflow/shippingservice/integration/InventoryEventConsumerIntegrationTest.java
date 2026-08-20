package com.eventflow.shippingservice.integration;

import com.eventflow.common.event.InventoryReservedEvent;
import com.eventflow.shippingservice.entity.ShipmentEntity;
import com.eventflow.shippingservice.repository.ShipmentRepository;
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

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InventoryEventConsumerIntegrationTest {

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
    private ShipmentRepository shipmentRepository;

    @BeforeEach
    void setUp() {
        shipmentRepository.deleteAll();
    }

    @Test
    void shouldCreateShipmentOnInventoryReserved() throws Exception {
        String orderId = UUID.randomUUID().toString();

        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("InventoryReserved")
                .orderId(orderId)
                .items(java.util.List.of(InventoryReservedEvent.ReservedItem.builder()
                        .productId("prod-1")
                        .productName("Widget")
                        .quantity(1)
                        .warehouseId("WH-1")
                        .build()))
                .reservedBy("inventory-service")
                .correlationId(orderId)
                .serviceName("inventory-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();

        kafkaTemplate.send("inventory", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(5000);

        var shipments = shipmentRepository.findByOrderId(UUID.fromString(orderId));
        assertFalse(shipments.isEmpty(), "Shipment should have been created");
        assertEquals("SHIPPED", shipments.get(0).getStatus());
    }

    @Test
    void shouldNotCreateDuplicateShipment() throws Exception {
        String orderId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();

        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(eventId)
                .eventType("InventoryReserved")
                .orderId(orderId)
                .items(java.util.List.of(InventoryReservedEvent.ReservedItem.builder()
                        .productId("prod-1")
                        .productName("Widget")
                        .quantity(1)
                        .warehouseId("WH-1")
                        .build()))
                .reservedBy("inventory-service")
                .correlationId(orderId)
                .serviceName("inventory-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();

        kafkaTemplate.send("inventory", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(3000);
        kafkaTemplate.send("inventory", orderId, event).get(10, TimeUnit.SECONDS);
        Thread.sleep(3000);

        var shipments = shipmentRepository.findByOrderId(UUID.fromString(orderId));
        assertEquals(1, shipments.size(), "Should have exactly one shipment (idempotent)");
    }
}
