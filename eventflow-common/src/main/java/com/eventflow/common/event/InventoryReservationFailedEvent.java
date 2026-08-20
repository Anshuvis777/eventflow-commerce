package com.eventflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InventoryReservationFailedEvent extends BaseEvent {

    private String orderId;
    private List<InventoryReservedEvent.ReservedItem> items;
    private String failureReason;
    private String reservedBy;
}
