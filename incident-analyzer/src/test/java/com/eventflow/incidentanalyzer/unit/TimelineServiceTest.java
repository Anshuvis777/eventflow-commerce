package com.eventflow.incidentanalyzer.unit;

import com.eventflow.incidentanalyzer.domain.Severity;
import com.eventflow.incidentanalyzer.dto.response.EventResponse;
import com.eventflow.incidentanalyzer.dto.response.TimelineResponse;
import com.eventflow.incidentanalyzer.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TimelineServiceTest {

    @InjectMocks
    private TimelineService timelineService;

    private List<EventResponse> testEvents;

    private EventResponse event(String type, String service, String severity, String ts) {
        return new EventResponse(UUID.randomUUID(), null, type, service,
                OffsetDateTime.parse(ts), null, Severity.valueOf(severity), null);
    }

    @BeforeEach
    void setUp() {
        testEvents = List.of(
                event("OrderPlaced", "order-service", "LOW", "2026-08-15T10:00:00Z"),
                event("PaymentFailed", "payment-service", "HIGH", "2026-08-15T10:05:00Z"),
                event("InventoryReleased", "inventory-service", "MEDIUM", "2026-08-15T10:10:00Z")
        );
    }

    @Test
    void shouldCalculateDurationInSeconds() {
        // Given
        OffsetDateTime start = OffsetDateTime.parse("2026-08-15T10:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-08-15T10:10:00Z");

        // When
        long duration = timelineService.calculateDuration(start, end);

        // Then
        assertThat(duration).isEqualTo(600); // 10 minutes = 600 seconds
    }

    @Test
    void shouldExtractAffectedServices() {
        // When
        List<String> services = timelineService.extractAffectedServices(testEvents);

        // Then
        assertThat(services).containsExactlyInAnyOrder(
                "order-service", "payment-service", "inventory-service"
        );
    }

    @Test
    void shouldReturnUniqueServices() {
        // Given
        List<EventResponse> eventsWithDuplicates = List.of(
                event("OrderPlaced", "order-service", "LOW", "2026-08-15T10:00:00Z"),
                event("PaymentFailed", "payment-service", "HIGH", "2026-08-15T10:05:00Z"),
                event("OrderCancelled", "order-service", "LOW", "2026-08-15T10:06:00Z")
        );

        // When
        List<String> services = timelineService.extractAffectedServices(eventsWithDuplicates);

        // Then
        assertThat(services).hasSize(2);
        assertThat(services).containsExactlyInAnyOrder("order-service", "payment-service");
    }

    @Test
    void shouldHandleEmptyEvents() {
        // When
        List<String> services = timelineService.extractAffectedServices(List.of());

        // Then
        assertThat(services).isEmpty();
    }
}
