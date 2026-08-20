package com.eventflow.incidentanalyzer.service;

import com.eventflow.common.exception.BusinessRuleViolationException;
import com.eventflow.common.exception.ResourceNotFoundException;
import com.eventflow.incidentanalyzer.domain.IncidentStatus;
import com.eventflow.incidentanalyzer.dto.request.AnalysisTriggerRequest;
import com.eventflow.incidentanalyzer.dto.response.AnalysisResponse;
import com.eventflow.incidentanalyzer.entity.AnalysisEntity;
import com.eventflow.incidentanalyzer.entity.EventEntity;
import com.eventflow.incidentanalyzer.entity.IncidentEntity;
import com.eventflow.incidentanalyzer.repository.AnalysisRepository;
import com.eventflow.incidentanalyzer.repository.EventRepository;
import com.eventflow.incidentanalyzer.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisOrchestrationService {

    private final IncidentRepository incidentRepository;
    private final EventRepository eventRepository;
    private final AnalysisRepository analysisRepository;
    private final Gpt4AnalysisService gpt4AnalysisService;
    private final TimelineService timelineService;

    @Transactional
    public AnalysisResponse triggerAnalysis(UUID incidentId, AnalysisTriggerRequest request) {
        log.info("Triggering analysis for incident: {}", incidentId);

        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));

        if (incident.getStatus() == IncidentStatus.ANALYZING && !request.force()) {
            throw new BusinessRuleViolationException("Analysis already in progress for this incident");
        }

        incident.setStatus(IncidentStatus.ANALYZING);
        incidentRepository.save(incident);

        try {
            List<EventEntity> events = eventRepository.findByIncidentIdOrderByTimestampAsc(incidentId);

            String prompt = gpt4AnalysisService.buildPrompt(incident, events, "");

            String analysisResult = gpt4AnalysisService.callGeminiApi(prompt);

            AnalysisEntity analysis = gpt4AnalysisService.parseStructuredOutput(analysisResult, incident);
            analysis = analysisRepository.save(analysis);

            incident.setStatus(IncidentStatus.ANALYZED);
            incidentRepository.save(incident);

            log.info("Analysis completed for incident: {} with confidence: {}",
                    incidentId, analysis.getConfidenceScore());

            return gpt4AnalysisService.toResponse(analysis);

        } catch (Exception e) {
            incident.setStatus(IncidentStatus.OPEN);
            incidentRepository.save(incident);
            throw new RuntimeException("Analysis failed: " + e.getMessage(), e);
        }
    }

    public AnalysisResponse getAnalysis(UUID incidentId) {
        AnalysisEntity analysis = analysisRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis", "incidentId", incidentId));
        return gpt4AnalysisService.toResponse(analysis);
    }
}
