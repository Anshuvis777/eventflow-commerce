package com.eventflow.inventoryservice.service;

import com.eventflow.common.event.InventoryReleasedEvent;
import com.eventflow.common.event.InventoryReservedEvent;
import com.eventflow.common.event.InventoryReservationFailedEvent;
import com.eventflow.common.event.PaymentProcessedEvent;
import com.eventflow.inventoryservice.entity.InventoryEntity;
import com.eventflow.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ponytail: in-memory idempotency guard for reserved orders (see reserveStockForOrder)
    private final java.util.Set<String> reservedOrderIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public InventoryEntity reserveStock(String productId, int quantity, UUID orderId) {
        log.info("Reserving stock for product: {}, qty: {}", productId, quantity);

        InventoryEntity inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        int available = inventory.getQuantity() - inventory.getReserved();
        if (available < quantity) {
            log.warn("Insufficient stock for product: {}", productId);
            InventoryReservationFailedEvent event = InventoryReservationFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("InventoryReservationFailed")
                    .orderId(orderId.toString())
                    .items(List.of(InventoryReservedEvent.ReservedItem.builder()
                            .productId(productId)
                            .productName(inventory.getProductName())
                            .quantity(quantity)
                            .warehouseId("WH-1")
                            .build()))
                    .failureReason("Insufficient stock for product: " + productId)
                    .reservedBy("inventory-service")
                    .correlationId(orderId.toString())
                    .serviceName("inventory-service")
                    .timestamp(OffsetDateTime.now())
                    .severity("WARN")
                    .build();
            kafkaTemplate.send("inventory", orderId.toString(), event);
            throw new RuntimeException("Insufficient stock for product: " + productId);
        }

        inventory.setReserved(inventory.getReserved() + quantity);
        inventory = inventoryRepository.save(inventory);

        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("InventoryReserved")
                .orderId(orderId.toString())
                .items(List.of(InventoryReservedEvent.ReservedItem.builder()
                        .productId(productId)
                        .productName(inventory.getProductName())
                        .quantity(quantity)
                        .warehouseId("WH-1")
                        .build()))
                .reservedBy("inventory-service")
                .correlationId(orderId.toString())
                .serviceName("inventory-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();
        kafkaTemplate.send("inventory", orderId.toString(), event);
        log.info("Stock reserved for product: {}, remaining: {} — event published", productId, available - quantity);
        return inventory;
    }

    public void releaseStock(String productId, int quantity, UUID orderId, String orderNumber) {
        log.info("Releasing stock for product: {}, orderId: {}", productId, orderId);

        InventoryEntity inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        inventory.setReserved(Math.max(0, inventory.getReserved() - quantity));
        inventory = inventoryRepository.save(inventory);

        InventoryReleasedEvent event = InventoryReleasedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("InventoryReleased")
                .orderId(orderId.toString())
                .releaseReason("Stock released due to order cancellation")
                .items(List.of(InventoryReleasedEvent.ReleasedItem.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .build()))
                .correlationId(orderId.toString())
                .serviceName("inventory-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();
        kafkaTemplate.send("inventory", orderId.toString(), event);
        log.info("Stock released for product: {} — event published", productId);
    }

    public List<InventoryEntity> getAllInventory() {
        return inventoryRepository.findAll();
    }

    public InventoryEntity getInventory(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
    }

    @org.springframework.transaction.annotation.Transactional
    public void reserveStockForOrder(PaymentProcessedEvent event) {
        UUID orderId = UUID.fromString(event.getOrderId());

        // Idempotency: skip if we already reserved for this order
        // ponytail: in-memory guard — not durable across restarts; upgrade to a
        // reservation table keyed by (orderId, productId) when multi-instance is needed
        if (!reservedOrderIds.add(event.getOrderId())) {
            log.info("Stock already reserved for order: {} — skipping", event.getOrderId());
            return;
        }

        // For simplicity, reserve the first available product (qty = 1)
        // In production this would parse order items from the event
        List<InventoryEntity> all = inventoryRepository.findAll();
        if (all.isEmpty()) {
            log.warn("No inventory available for reservation");
            return;
        }

        InventoryEntity inventory = all.get(0);
        int quantity = 1;

        int available = inventory.getQuantity() - inventory.getReserved();
        if (available < quantity) {
            log.warn("Insufficient stock for product: {}", inventory.getProductId());
            InventoryReservationFailedEvent failEvent = InventoryReservationFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("InventoryReservationFailed")
                    .orderId(event.getOrderId())
                    .items(List.of(InventoryReservedEvent.ReservedItem.builder()
                            .productId(inventory.getProductId())
                            .productName(inventory.getProductName())
                            .quantity(quantity)
                            .warehouseId("WH-1")
                            .build()))
                    .failureReason("Insufficient stock for product: " + inventory.getProductId())
                    .reservedBy("inventory-service")
                    .correlationId(event.getCorrelationId())
                    .serviceName("inventory-service")
                    .timestamp(OffsetDateTime.now())
                    .severity("WARN")
                    .build();
            kafkaTemplate.send("inventory", event.getOrderId(), failEvent);
            return;
        }

        inventory.setReserved(inventory.getReserved() + quantity);
        inventoryRepository.save(inventory);

        InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("InventoryReserved")
                .orderId(event.getOrderId())
                .items(List.of(InventoryReservedEvent.ReservedItem.builder()
                        .productId(inventory.getProductId())
                        .productName(inventory.getProductName())
                        .quantity(quantity)
                        .warehouseId("WH-1")
                        .build()))
                .reservedBy("inventory-service")
                .correlationId(event.getCorrelationId())
                .serviceName("inventory-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();
        kafkaTemplate.send("inventory", event.getOrderId(), reservedEvent);
        log.info("Stock reserved for order: {}, product: {}", event.getOrderId(), inventory.getProductId());
    }
}
