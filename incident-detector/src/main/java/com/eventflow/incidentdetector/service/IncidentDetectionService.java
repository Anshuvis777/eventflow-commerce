package com.eventflow.incidentdetector.service;

import com.eventflow.incidentdetector.domain.IncidentStatus;
import com.eventflow.incidentdetector.domain.Severity;
import com.eventflow.incidentdetector.dto.request.EventIngestRequest;
import com.eventflow.incidentdetector.entity.EventEntity;
import com.eventflow.incidentdetector.entity.IncidentEntity;
import com.eventflow.incidentdetector.repository.EventRepository;
import com.eventflow.incidentdetector.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentDetectionService {

    private final IncidentRepository incidentRepository;
    private final EventRepository eventRepository;

    @Transactional
    public IncidentEntity processEvent(EventIngestRequest request) {
        log.info("Processing event {} for correlation_id: {}", request.eventType(), request.correlationId());

        Optional<IncidentEntity> existingIncident =
                incidentRepository.findByCorrelationId(request.correlationId());

        IncidentEntity incident;
        if (existingIncident.isPresent()) {
            incident = existingIncident.get();
            log.debug("Attaching event to existing incident: {}", incident.getId());
        } else {
            incident = findOrCreateIncident(request.correlationId(), request.severity(), request);
            incident = incidentRepository.save(incident);
            log.debug("Created new incident: {}", incident.getId());
        }

        EventEntity event = createEventEntity(request, incident);
        eventRepository.save(event);

        updateIncidentMetadata(incident, request);
        incident = incidentRepository.save(incident);

        return incident;
    }

    private IncidentEntity findOrCreateIncident(String correlationId, String severity,
                                                 EventIngestRequest request) {
        Severity incidentSeverity = parseSeverity(severity);

        return IncidentEntity.builder()
                .correlationId(correlationId)
                .status(IncidentStatus.OPEN)
                .severity(incidentSeverity)
                .title(generateTitle(request))
                .description("Auto-created from " + request.eventType() + " event")
                .affectedServices(List.of(request.serviceName()))
                .firstEventAt(request.timestamp())
                .lastEventAt(request.timestamp())
                .build();
    }

    private EventEntity createEventEntity(EventIngestRequest request, IncidentEntity incident) {
        return EventEntity.builder()
                .incident(incident)
                .eventId(request.eventId())
                .correlationId(request.correlationId())
                .eventType(request.eventType())
                .serviceName(request.serviceName())
                .timestamp(request.timestamp())
                .payload(request.payload())
                .severity(parseSeverity(request.severity()))
                .build();
    }

    private Severity parseSeverity(String severityStr) {
        if (severityStr == null) return Severity.MEDIUM;
        String upper = severityStr.toUpperCase();
        return switch (upper) {
            case "LOW", "INFO" -> Severity.LOW;
            case "MEDIUM", "WARN" -> Severity.MEDIUM;
            case "HIGH", "ERROR" -> Severity.HIGH;
            case "CRITICAL", "FATAL" -> Severity.CRITICAL;
            default -> Severity.MEDIUM;
        };
    }

    private void updateIncidentMetadata(IncidentEntity incident, EventIngestRequest request) {
        incident.setLastEventAt(request.timestamp());

        List<String> services = incident.getAffectedServices();
        if (!services.contains(request.serviceName())) {
            services.add(request.serviceName());
            incident.setAffectedServices(services);
        }

        incidentRepository.save(incident);
    }

    private String generateTitle(EventIngestRequest request) {
        return String.format("%s from %s", request.eventType(), request.serviceName());
    }
}
