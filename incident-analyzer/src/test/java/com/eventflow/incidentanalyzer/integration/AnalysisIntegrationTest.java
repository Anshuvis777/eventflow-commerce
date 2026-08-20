package com.eventflow.incidentanalyzer.integration;

import com.eventflow.incidentanalyzer.entity.AnalysisEntity;
import com.eventflow.incidentanalyzer.entity.IncidentEntity;
import com.eventflow.incidentanalyzer.repository.AnalysisRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AnalysisIntegrationTest {

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
    private AnalysisRepository analysisRepository;

    @BeforeEach
    void setUp() {
        analysisRepository.deleteAll();
        incidentRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveAnalysis() {
        // Given
        IncidentEntity incident = IncidentEntity.builder()
                .correlationId("analysis-test-001")
                .status(com.eventflow.incidentanalyzer.domain.IncidentStatus.ANALYZED)
                .severity(com.eventflow.incidentanalyzer.domain.Severity.HIGH)
                .title("Test incident")
                .firstEventAt(OffsetDateTime.now().minusHours(1))
                .build();
        incident = incidentRepository.save(incident);

        AnalysisEntity analysis = AnalysisEntity.builder()
                .incidentId(incident.getId())
                .rootCause("Payment gateway timeout")
                .impact("100 orders affected")
                .contributingFactors(List.of("High traffic"))
                .recommendedActions(List.of("Add retry logic"))
                .preventionMeasures(List.of("Monitor gateway health"))
                .confidenceScore(85)
                .modelVersion("gpt-4-turbo-preview")
                .build();

        analysis = analysisRepository.save(analysis);

        // When
        Optional<AnalysisEntity> retrieved = analysisRepository.findByIncidentId(incident.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getRootCause()).isEqualTo("Payment gateway timeout");
        assertThat(retrieved.get().getConfidenceScore()).isEqualTo(85);
    }
}
