package com.eventflow.incidentanalyzer.integration;

import com.eventflow.incidentanalyzer.dto.response.TimelineResponse;
import com.eventflow.incidentanalyzer.entity.EventEntity;
import com.eventflow.incidentanalyzer.entity.IncidentEntity;
import com.eventflow.incidentanalyzer.repository.EventRepository;
import com.eventflow.incidentanalyzer.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TimelineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("incident_analytics")
            .withUsername("eventflow")
            .withPassword("eventflow_secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        incidentRepository.deleteAll();
    }

    @Test
    void shouldRetrieveTimelineForIncident() {
        // Given
        IncidentEntity incident = IncidentEntity.builder()
                .correlationId("timeline-test-001")
                .status(com.eventflow.incidentanalyzer.domain.IncidentStatus.OPEN)
                .severity(com.eventflow.incidentanalyzer.domain.Severity.HIGH)
                .title("Test incident")
                .firstEventAt(OffsetDateTime.now().minusHours(1))
                .build();
        incident = incidentRepository.save(incident);

        EventEntity event1 = EventEntity.builder()
                .incident(incident)
                .eventId("evt-001")
                .correlationId("timeline-test-001")
                .eventType("OrderPlaced")
                .serviceName("order-service")
                .timestamp(OffsetDateTime.now().minusMinutes(30))
                .payload("{}")
                .severity(com.eventflow.incidentanalyzer.domain.Severity.LOW)
                .build();

        EventEntity event2 = EventEntity.builder()
                .incident(incident)
                .eventId("evt-002")
                .correlationId("timeline-test-001")
                .eventType("PaymentFailed")
                .serviceName("payment-service")
                .timestamp(OffsetDateTime.now().minusMinutes(25))
                .payload("{}")
                .severity(com.eventflow.incidentanalyzer.domain.Severity.HIGH)
                .build();

        eventRepository.saveAll(List.of(event1, event2));

        // When
        List<EventEntity> timeline = eventRepository.findByIncidentIdOrderByTimestampAsc(incident.getId());

        // Then
        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).getEventType()).isEqualTo("OrderPlaced");
        assertThat(timeline.get(1).getEventType()).isEqualTo("PaymentFailed");
    }
}
