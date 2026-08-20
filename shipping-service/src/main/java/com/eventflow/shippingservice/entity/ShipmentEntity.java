package com.eventflow.shippingservice.entity;

import com.eventflow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ShipmentEntity extends BaseEntity {

    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;

    @Column(name = "estimated_delivery")
    private OffsetDateTime estimatedDelivery;

    @Column(name = "actual_delivery")
    private OffsetDateTime actualDelivery;
}
