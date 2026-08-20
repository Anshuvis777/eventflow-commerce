package com.eventflow.incidentquery.unit;

import com.eventflow.incidentquery.service.VectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorServiceTest {

    @Mock
    private WebClient webClient;

    @InjectMocks
    private VectorService vectorService;

    private UUID testIncidentId;

    @BeforeEach
    void setUp() {
        testIncidentId = UUID.randomUUID();
    }

    @Test
    void shouldStoreEmbedding() {
        // Given
        double[] embedding = new double[1536];
        Map<String, String> metadata = Map.of(
                "incident_id", testIncidentId.toString(),
                "correlation_id", "test-correlation-001",
                "severity", "HIGH"
        );

        // When
        vectorService.storeEmbedding(testIncidentId, embedding, metadata);

        // Then
        verify(webClient).post();
    }

    @Test
    void shouldSearchSimilarIncidents() {
        // Given
        double[] queryVector = new double[1536];
        int limit = 10;
        double minSimilarity = 0.7;

        // When
        List<Map<String, Object>> results = vectorService.searchSimilar(queryVector, limit, minSimilarity);

        // Then
        assertThat(results).isNotNull();
        verify(webClient).post();
    }

    @Test
    void shouldBuildChromaDBRequest() {
        // Given
        double[] embedding = new double[1536];
        Map<String, String> metadata = Map.of("incident_id", testIncidentId.toString());

        // When
        Map<String, Object> request = vectorService.buildAddRequest(testIncidentId.toString(), embedding, metadata);

        // Then
        assertThat(request).containsKey("ids");
        assertThat(request).containsKey("embeddings");
        assertThat(request).containsKey("metadatas");
    }
}
