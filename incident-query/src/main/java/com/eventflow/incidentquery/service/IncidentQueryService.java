package com.eventflow.incidentquery.service;

import com.eventflow.common.exception.ResourceNotFoundException;
import com.eventflow.incidentquery.entity.IncidentEntity;
import com.eventflow.incidentquery.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentQueryService {

    private final IncidentRepository incidentRepository;

    public List<IncidentEntity> listIncidents(String status, String severity) {
        if (status != null) {
            return incidentRepository.findByStatus(status);
        }
        return incidentRepository.findAll();
    }

    public IncidentEntity getIncident(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
    }

    public IncidentEntity updateIncident(UUID id, String status, String title) {
        IncidentEntity incident = getIncident(id);

        if (status != null) {
            incident.setStatus(status);
        }
        if (title != null) {
            incident.setTitle(title);
        }

        return incidentRepository.save(incident);
    }
}
