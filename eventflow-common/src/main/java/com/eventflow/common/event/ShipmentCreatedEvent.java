package com.eventflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShipmentCreatedEvent extends BaseEvent {

    private String orderId;
    private String shipmentId;
    private String carrier;
    private String trackingNumber;
    private String estimatedDelivery;
}
