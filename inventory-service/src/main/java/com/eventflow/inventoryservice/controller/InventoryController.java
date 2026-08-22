package com.eventflow.inventoryservice.controller;

import com.eventflow.common.dto.ApiResponse;
import com.eventflow.inventoryservice.entity.InventoryEntity;
import com.eventflow.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryEntity>>> getAllInventory() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllInventory()));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryEntity>> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventory(productId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryEntity>> addProduct(@RequestBody Map<String, Object> request) {
        String productId = (String) request.get("productId");
        String productName = request.get("productName") != null ? (String) request.get("productName") : "New Product";
        Integer quantity = request.get("quantity") != null ? ((Number) request.get("quantity")).intValue() : 10;
        String warehouseLocation = (String) request.get("warehouseLocation");

        InventoryEntity inventory = inventoryService.addProduct(productId, productName, quantity, warehouseLocation);
        return ResponseEntity.ok(ApiResponse.success("Product added/replenished successfully", inventory));
    }

    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<InventoryEntity>> reserveStock(@RequestBody Map<String, Object> request) {
        // Support both flat {productId, quantity, orderId} and nested {orderId, items: [{productId, quantity}]}
        if (request.containsKey("items")) {
            // Nested format from frontend: reserve first item
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            String orderId = (String) request.get("orderId");
            if (items != null && !items.isEmpty()) {
                Map<String, Object> firstItem = items.get(0);
                InventoryEntity inventory = inventoryService.reserveStock(
                        (String) firstItem.get("productId"),
                        firstItem.get("quantity") != null ? ((Number) firstItem.get("quantity")).intValue() : 1,
                        UUID.fromString(orderId)
                );
                return ResponseEntity.ok(ApiResponse.success("Stock reserved", inventory));
            }
            throw new RuntimeException("No items provided for reservation");
        }
        // Flat format
        InventoryEntity inventory = inventoryService.reserveStock(
                (String) request.get("productId"),
                request.get("quantity") != null ? ((Number) request.get("quantity")).intValue() : 1,
                UUID.fromString((String) request.get("orderId"))
        );
        return ResponseEntity.ok(ApiResponse.success("Stock reserved", inventory));
    }

    @PostMapping("/release")
    public ResponseEntity<ApiResponse<String>> releaseStock(@RequestBody Map<String, Object> request) {
        String productId = request.get("productId") != null ? (String) request.get("productId") : "PROD-001";
        Integer quantity = request.get("quantity") != null ? ((Number) request.get("quantity")).intValue() : 1;
        String orderId = (String) request.get("orderId");
        String orderNumber = request.get("orderNumber") != null ? (String) request.get("orderNumber") : "ORD-" + orderId.substring(0, Math.min(8, orderId.length()));
        
        inventoryService.releaseStock(productId, quantity, UUID.fromString(orderId), orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Stock released"));
    }
}
