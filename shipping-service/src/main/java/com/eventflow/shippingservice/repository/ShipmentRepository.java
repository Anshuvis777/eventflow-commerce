package com.eventflow.shippingservice.repository;

import com.eventflow.shippingservice.entity.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<ShipmentEntity, UUID> {
    List<ShipmentEntity> findByOrderId(UUID orderId);
    List<ShipmentEntity> findByStatus(String status);
    List<ShipmentEntity> findByCustomerId(UUID customerId);
}
