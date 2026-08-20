package com.eventflow.incidentquery.integration;

import com.eventflow.incidentquery.entity.SimilarIncidentEntity;
import com.eventflow.incidentquery.repository.SimilarIncidentRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class SimilarIncidentIntegrationTest {

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
    private SimilarIncidentRepository similarIncidentRepository;

    @BeforeEach
    void setUp() {
        similarIncidentRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveSimilarIncidents() {
        // Given
        UUID incidentId = UUID.randomUUID();
        UUID similarIncidentId = UUID.randomUUID();

        SimilarIncidentEntity similar = SimilarIncidentEntity.builder()
                .incidentId(incidentId)
                .similarIncidentId(similarIncidentId)
                .similarityScore(0.85f)
                .matchedOn("embedding")
                .build();

        similar = similarIncidentRepository.save(similar);

        // When
        List<SimilarIncidentEntity> results = similarIncidentRepository
                .findByIncidentIdOrderBySimilarityScoreDesc(incidentId);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSimilarityScore()).isEqualTo(0.85f);
    }

    @Test
    void shouldReturnEmptyForNoMatches() {
        // When
        List<SimilarIncidentEntity> results = similarIncidentRepository
                .findByIncidentIdOrderBySimilarityScoreDesc(UUID.randomUUID());

        // Then
        assertThat(results).isEmpty();
    }
}
