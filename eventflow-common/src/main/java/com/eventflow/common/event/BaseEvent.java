package com.eventflow.common.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "event_type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderPlacedEvent.class, name = "OrderPlaced"),
        @JsonSubTypes.Type(value = PaymentProcessedEvent.class, name = "PaymentProcessed"),
        @JsonSubTypes.Type(value = PaymentFailedEvent.class, name = "PaymentFailed"),
        @JsonSubTypes.Type(value = InventoryReservedEvent.class, name = "InventoryReserved"),
        @JsonSubTypes.Type(value = InventoryReservationFailedEvent.class, name = "InventoryReservationFailed"),
        @JsonSubTypes.Type(value = InventoryReleasedEvent.class, name = "InventoryReleased"),
        @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "OrderCancelled"),
        @JsonSubTypes.Type(value = ShipmentCreatedEvent.class, name = "ShipmentCreated"),
        @JsonSubTypes.Type(value = ShipmentDeliveredEvent.class, name = "ShipmentDelivered")
})
public abstract class BaseEvent {

    private String eventId;
    private String eventType;
    private String correlationId;
    private String serviceName;
    private OffsetDateTime timestamp;
    private String severity;
}
