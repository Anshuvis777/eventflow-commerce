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
public class InventoryReleasedEvent extends BaseEvent {

    private String orderId;
    private List<ReleasedItem> items;
    private String releaseReason;
    private String releasedBy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public static class ReleasedItem {
        private String productId;
        private String productName;
        private Integer quantity;
        private String warehouseId;
    }
}
