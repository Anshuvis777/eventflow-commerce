package com.eventflow.orderservice.repository;

import com.eventflow.orderservice.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    List<OrderEntity> findByStatus(String status);
    List<OrderEntity> findByCustomerId(UUID customerId);
}
