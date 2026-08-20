package com.eventflow.shippingservice.service;

import com.eventflow.common.event.InventoryReservedEvent;
import com.eventflow.common.event.ShipmentCreatedEvent;
import com.eventflow.common.event.ShipmentDeliveredEvent;
import com.eventflow.shippingservice.entity.ShipmentEntity;
import com.eventflow.shippingservice.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ShipmentEntity createShipment(UUID orderId, String orderNumber, UUID customerId,
                                          String shippingAddress) {
        log.info("Creating shipment for order: {}", orderNumber);

        String trackingNumber = "SHP-" + System.currentTimeMillis();

        ShipmentEntity shipment = ShipmentEntity.builder()
                .trackingNumber(trackingNumber)
                .orderId(orderId)
                .orderNumber(orderNumber)
                .customerId(customerId)
                .status("SHIPPED")
                .carrier("FedEx")
                .shippingAddress(shippingAddress)
                .estimatedDelivery(OffsetDateTime.now().plusDays(5))
                .build();

        shipment = shipmentRepository.save(shipment);

        ShipmentCreatedEvent event = ShipmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ShipmentCreated")
                .orderId(orderId.toString())
                .shipmentId(shipment.getId().toString())
                .carrier("FedEx")
                .trackingNumber(trackingNumber)
                .estimatedDelivery(shipment.getEstimatedDelivery() != null ? shipment.getEstimatedDelivery().toString() : "5 days")
                .correlationId(orderId.toString())
                .serviceName("shipping-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();
        kafkaTemplate.send("shipments", orderId.toString(), event);
        log.info("Shipment created: {} — event published", trackingNumber);

        return shipment;
    }

    public void markDelivered(UUID shipmentId) {
        log.info("Marking shipment as delivered: {}", shipmentId);

        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found: " + shipmentId));

        shipment.setStatus("DELIVERED");
        shipment.setActualDelivery(OffsetDateTime.now());
        shipment = shipmentRepository.save(shipment);

        ShipmentDeliveredEvent event = ShipmentDeliveredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ShipmentDelivered")
                .orderId(shipment.getOrderId().toString())
                .shipmentId(shipment.getId().toString())
                .deliveredTo(shipment.getCarrier())
                .correlationId(shipment.getOrderId().toString())
                .serviceName("shipping-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();
        kafkaTemplate.send("shipments", shipment.getOrderId().toString(), event);
        log.info("Shipment delivered: {} — event published", shipment.getTrackingNumber());
    }

    public List<ShipmentEntity> getShipmentsByOrder(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }

    public List<ShipmentEntity> getAllShipments() {
        return shipmentRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public void createShipmentForOrder(InventoryReservedEvent event) {
        UUID orderId = UUID.fromString(event.getOrderId());

        // Idempotency check — skip if shipment already exists for this order
        if (!shipmentRepository.findByOrderId(orderId).isEmpty()) {
            log.info("Shipment already exists for order: {} — skipping", event.getOrderId());
            return;
        }

        createShipment(orderId, event.getOrderId(), UUID.randomUUID(), "Default Address");
    }
}
