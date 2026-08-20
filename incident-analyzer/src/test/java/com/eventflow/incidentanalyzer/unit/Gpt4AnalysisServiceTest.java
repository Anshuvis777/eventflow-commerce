package com.eventflow.incidentanalyzer.unit;

import com.eventflow.incidentanalyzer.dto.response.AnalysisResponse;
import com.eventflow.incidentanalyzer.entity.AnalysisEntity;
import com.eventflow.incidentanalyzer.entity.IncidentEntity;
import com.eventflow.incidentanalyzer.repository.AnalysisRepository;
import com.eventflow.incidentanalyzer.service.Gpt4AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Gpt4AnalysisServiceTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private WebClient webClient;

    @InjectMocks
    private Gpt4AnalysisService gpt4AnalysisService;

    private IncidentEntity testIncident;

    @BeforeEach
    void setUp() {
        testIncident = IncidentEntity.builder()
                .id(UUID.randomUUID())
                .correlationId("test-correlation-001")
                .status(com.eventflow.incidentanalyzer.domain.IncidentStatus.ANALYZING)
                .severity(com.eventflow.incidentanalyzer.domain.Severity.HIGH)
                .title("Payment processing failure")
                .description("Auto-created from PaymentFailed event")
                .build();
    }

    @Test
    void shouldBuildPromptForIncident() {
        // When
        String prompt = gpt4AnalysisService.buildPrompt(testIncident, List.of(), "");

        // Then
        assertThat(prompt).contains("Payment processing failure");
        assertThat(prompt).contains("test-correlation-001");
        assertThat(prompt).contains("HIGH");
    }

    @Test
    void shouldParseStructuredOutput() {
        // Given
        String jsonResponse = """
                {
                    "root_cause": "Credit card gateway timeout",
                    "impact": "100 orders affected",
                    "contributing_factors": ["High traffic", "Gateway latency"],
                    "recommended_actions": ["Add retry logic", "Implement circuit breaker"],
                    "prevention_measures": ["Monitor gateway health"],
                    "confidence_score": 85
                }
                """;

        // When
        AnalysisEntity result = gpt4AnalysisService.parseStructuredOutput(jsonResponse, testIncident);

        // Then
        assertThat(result.getRootCause()).isEqualTo("Credit card gateway timeout");
        assertThat(result.getImpact()).isEqualTo("100 orders affected");
        assertThat(result.getContributingFactors()).containsExactly("High traffic", "Gateway latency");
        assertThat(result.getRecommendedActions()).containsExactly("Add retry logic", "Implement circuit breaker");
        assertThat(result.getPreventionMeasures()).containsExactly("Monitor gateway health");
        assertThat(result.getConfidenceScore()).isEqualTo(85);
    }

    @Test
    void shouldReturnAnalysisResponse() {
        // Given
        AnalysisEntity analysis = AnalysisEntity.builder()
                .id(UUID.randomUUID())
                .incidentId(testIncident.getId())
                .rootCause("Payment gateway timeout")
                .impact("Order processing delayed")
                .contributingFactors(List.of("High traffic"))
                .recommendedActions(List.of("Add retry"))
                .preventionMeasures(List.of("Monitor health"))
                .confidenceScore(85)
                .modelVersion("gpt-4-turbo-preview")
                .createdAt(OffsetDateTime.now())
                .build();

        // When
        AnalysisResponse response = gpt4AnalysisService.toResponse(analysis);

        // Then
        assertThat(response.rootCause()).isEqualTo("Payment gateway timeout");
        assertThat(response.confidenceScore()).isEqualTo(85);
        assertThat(response.modelVersion()).isEqualTo("gpt-4-turbo-preview");
    }
}
