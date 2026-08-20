package com.eventflow.orderservice.service;

import com.eventflow.common.event.OrderPlacedEvent;
import com.eventflow.orderservice.dto.OrderRequest;
import com.eventflow.orderservice.dto.OrderResponse;
import com.eventflow.orderservice.entity.OrderEntity;
import com.eventflow.orderservice.mapper.OrderMapper;
import com.eventflow.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.customerId());

        String orderNumber = "ORD-" + System.currentTimeMillis();

        // Calculate total from items if not provided
        java.math.BigDecimal total = request.totalAmount();
        if (total == null && request.items() != null) {
            total = request.items().stream()
                .map(i -> i.price().multiply(new java.math.BigDecimal(i.quantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        }

        OrderEntity order = OrderEntity.builder()
                .orderNumber(orderNumber)
                .customerId(request.customerId())
                .customerName(request.customerName() != null ? request.customerName() : "Customer")
                .customerEmail(request.customerEmail() != null ? request.customerEmail() : "unknown@email.com")
                .status("PLACED")
                .totalAmount(total)
                .currency(request.currency() != null ? request.currency() : "USD")
                .shippingAddress(request.shippingAddress())
                .itemsJson(request.items().toString())
                .build();

        order = orderRepository.save(order);

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("OrderPlaced")
                .orderId(order.getId().toString())
                .customerId(order.getCustomerId().toString())
                .customerEmail(order.getCustomerEmail())
                .customerName(order.getCustomerName())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .correlationId(order.getId().toString())
                .serviceName("order-service")
                .timestamp(OffsetDateTime.now())
                .severity("INFO")
                .build();

        kafkaTemplate.send("orders", order.getId().toString(), event);
        log.info("Order placed: {} — event published to Kafka", orderNumber);

        return orderMapper.toResponse(order);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public OrderResponse getOrderById(UUID id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        return orderMapper.toResponse(order);
    }

    public OrderResponse updateOrderStatus(UUID id, String status) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        order.setStatus(status);
        order = orderRepository.save(order);
        return orderMapper.toResponse(order);
    }
}
