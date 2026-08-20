package com.eventflow.shippingservice.controller;

import com.eventflow.common.dto.ApiResponse;
import com.eventflow.shippingservice.entity.ShipmentEntity;
import com.eventflow.shippingservice.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentEntity>> createShipment(@RequestBody Map<String, Object> request) {
        String orderIdStr = (String) request.get("orderId");
        String orderNumber = request.get("orderNumber") != null ? (String) request.get("orderNumber") : "ORD-" + orderIdStr.substring(0, Math.min(8, orderIdStr.length()));
        String customerIdStr = request.get("customerId") != null ? (String) request.get("customerId") : orderIdStr;
        String shippingAddress = request.get("shippingAddress") != null ? (String) request.get("shippingAddress") : 
                                 request.get("recipientAddress") != null ? (String) request.get("recipientAddress") : "";
        
        ShipmentEntity shipment = shippingService.createShipment(
                UUID.fromString(orderIdStr),
                orderNumber,
                UUID.fromString(customerIdStr),
                shippingAddress
        );
        // Set additional fields from frontend
        if (request.get("trackingNumber") != null) shipment.setTrackingNumber((String) request.get("trackingNumber"));
        if (request.get("carrier") != null) shipment.setCarrier((String) request.get("carrier"));
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipment created", shipment));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipmentEntity>>> getAllShipments() {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getAllShipments()));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<ShipmentEntity>>> getShipmentsByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getShipmentsByOrder(orderId)));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<String>> markDelivered(@PathVariable UUID id) {
        shippingService.markDelivered(id);
        return ResponseEntity.ok(ApiResponse.success("Shipment marked as delivered"));
    }
}
