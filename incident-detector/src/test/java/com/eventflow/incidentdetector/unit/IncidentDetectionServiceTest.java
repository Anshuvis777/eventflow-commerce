package com.eventflow.incidentdetector.unit;

import com.eventflow.incidentdetector.dto.request.EventIngestRequest;
import com.eventflow.incidentdetector.entity.EventEntity;
import com.eventflow.incidentdetector.entity.IncidentEntity;
import com.eventflow.incidentdetector.repository.EventRepository;
import com.eventflow.incidentdetector.repository.IncidentRepository;
import com.eventflow.incidentdetector.service.IncidentDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentDetectionServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private IncidentDetectionService incidentDetectionService;

    private EventIngestRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = EventIngestRequest.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PaymentFailed")
                .correlationId("test-correlation-001")
                .serviceName("payment-service")
                .timestamp(OffsetDateTime.now())
                .severity("HIGH")
                .payload("{\"orderId\": \"order-001\", \"failureReason\": \"CARD_DECLINED\"}")
                .build();
    }

    @Test
    void shouldCreateNewIncidentForNewCorrelationId() {
        // Given
        when(incidentRepository.findByCorrelationId("test-correlation-001"))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(IncidentEntity.class)))
                .thenAnswer(invocation -> {
                    IncidentEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });
        when(eventRepository.save(any(EventEntity.class)))
                .thenAnswer(invocation -> {
                    EventEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        // When
        IncidentEntity result = incidentDetectionService.processEvent(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCorrelationId()).isEqualTo("test-correlation-001");
        assertThat(result.getStatus()).isEqualTo(com.eventflow.incidentdetector.domain.IncidentStatus.OPEN);
        assertThat(result.getSeverity()).isEqualTo(com.eventflow.incidentdetector.domain.Severity.HIGH);

        verify(incidentRepository).findByCorrelationId("test-correlation-001");
        verify(incidentRepository).save(any(IncidentEntity.class));
        verify(eventRepository).save(any(EventEntity.class));
    }

    @Test
    void shouldReuseExistingIncidentForKnownCorrelationId() {
        // Given
        IncidentEntity existingIncident = IncidentEntity.builder()
                .id(UUID.randomUUID())
                .correlationId("test-correlation-001")
                .status(com.eventflow.incidentdetector.domain.IncidentStatus.OPEN)
                .severity(com.eventflow.incidentdetector.domain.Severity.HIGH)
                .title("Existing incident")
                .build();

        when(incidentRepository.findByCorrelationId("test-correlation-001"))
                .thenReturn(Optional.of(existingIncident));
        when(eventRepository.save(any(EventEntity.class)))
                .thenAnswer(invocation -> {
                    EventEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        // When
        IncidentEntity result = incidentDetectionService.processEvent(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingIncident.getId());
        verify(incidentRepository, never()).save(any(IncidentEntity.class));
        verify(eventRepository).save(any(EventEntity.class));
    }

    @Test
    void shouldExtractSeverityFromPayload() {
        // Given
        when(incidentRepository.findByCorrelationId(anyString()))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(IncidentEntity.class)))
                .thenAnswer(invocation -> {
                    IncidentEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });
        when(eventRepository.save(any(EventEntity.class)))
                .thenAnswer(invocation -> {
                    EventEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        // When
        IncidentEntity result = incidentDetectionService.processEvent(testRequest);

        // Then
        assertThat(result.getSeverity()).isEqualTo(com.eventflow.incidentdetector.domain.Severity.HIGH);
    }
}
