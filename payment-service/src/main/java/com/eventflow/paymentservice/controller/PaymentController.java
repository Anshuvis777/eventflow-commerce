package com.eventflow.paymentservice.controller;

import com.eventflow.common.dto.ApiResponse;
import com.eventflow.paymentservice.entity.PaymentEntity;
import com.eventflow.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentEntity>> processPayment(@RequestBody Map<String, Object> request) {
        String orderIdStr = (String) request.get("orderId");
        String orderNumber = request.get("orderNumber") != null ? (String) request.get("orderNumber") : "ORD-" + orderIdStr.substring(0, Math.min(8, orderIdStr.length()));
        String customerIdStr = request.get("customerId") != null ? (String) request.get("customerId") : orderIdStr;
        String paymentMethod = request.get("paymentMethod") != null ? (String) request.get("paymentMethod") : "CREDIT_CARD";
        
        PaymentEntity payment = paymentService.processPayment(
                UUID.fromString(orderIdStr),
                orderNumber,
                UUID.fromString(customerIdStr),
                new BigDecimal(request.get("amount").toString()),
                (String) request.get("currency")
        );
        payment.setPaymentMethod(paymentMethod);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed", payment));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<PaymentEntity>>> getPaymentsByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByOrder(orderId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentEntity>> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentById(id)));
    }
}
