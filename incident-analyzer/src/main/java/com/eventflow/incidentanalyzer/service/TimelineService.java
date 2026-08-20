package com.eventflow.incidentanalyzer.service;

import com.eventflow.incidentanalyzer.dto.response.EventResponse;
import com.eventflow.incidentanalyzer.dto.response.TimelineResponse;
import com.eventflow.incidentanalyzer.entity.EventEntity;
import com.eventflow.incidentanalyzer.entity.IncidentEntity;
import com.eventflow.incidentanalyzer.mapper.AnalysisMapper;
import com.eventflow.incidentanalyzer.repository.EventRepository;
import com.eventflow.incidentanalyzer.repository.IncidentRepository;
import com.eventflow.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineService {

    private final IncidentRepository incidentRepository;
    private final EventRepository eventRepository;
    private final AnalysisMapper analysisMapper;

    public TimelineResponse getTimeline(UUID incidentId) {
        log.info("Building timeline for incident: {}", incidentId);

        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));

        List<EventEntity> events = eventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);

        List<EventResponse> eventResponses = events.stream()
                .map(analysisMapper::toEventResponse)
                .toList();

        long duration = 0;
        if (!events.isEmpty()) {
            OffsetDateTime first = events.get(0).getTimestamp();
            OffsetDateTime last = events.get(events.size() - 1).getTimestamp();
            duration = calculateDuration(first, last);
        }

        List<String> services = extractAffectedServices(eventResponses);

        return new TimelineResponse(
                incidentId,
                eventResponses,
                duration,
                eventResponses.size(),
                services,
                incident.getFirstEventAt(),
                incident.getLastEventAt()
        );
    }

    public long calculateDuration(OffsetDateTime start, OffsetDateTime end) {
        return Duration.between(start, end).getSeconds();
    }

    public List<String> extractAffectedServices(List<EventResponse> events) {
        return events.stream()
                .map(EventResponse::serviceName)
                .distinct()
                .collect(Collectors.toList());
    }
}
