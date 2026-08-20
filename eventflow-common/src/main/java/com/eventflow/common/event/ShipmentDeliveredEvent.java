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
public class ShipmentDeliveredEvent extends BaseEvent {

    private String orderId;
    private String shipmentId;
    private String deliveredTo;
    private String signatureRequired;
}
