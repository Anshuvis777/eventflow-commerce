package com.eventflow.incidentquery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorService {

    private final WebClient.Builder webClientBuilder;

    @Value("${chromadb.base-url:http://localhost:8000}")
    private String chromaDbBaseUrl;

    @Value("${chromadb.collection-name:incident_embeddings}")
    private String collectionName;

    public void storeEmbedding(UUID incidentId, double[] vector, Map<String, String> metadata) {
        log.info("Storing embedding for incident: {}", incidentId);

        Map<String, Object> request = buildAddRequest(
                incidentId.toString(),
                vector,
                metadata
        );

        webClientBuilder.baseUrl(chromaDbBaseUrl).build()
                .post()
                .uri("/api/v1/collections/{collectionName}/add", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public List<Map<String, Object>> searchSimilar(double[] queryVector, int limit, double minSimilarity) {
        log.debug("Searching for similar incidents with limit: {}, minSimilarity: {}", limit, minSimilarity);

        Map<String, Object> request = Map.of(
                "query_embeddings", List.of(queryVector),
                "n_results", limit,
                "where", Map.of("similarity_score", Map.of("$gte", minSimilarity))
        );

        return webClientBuilder.baseUrl(chromaDbBaseUrl).build()
                .post()
                .uri("/api/v1/collections/{collectionName}/query", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .map(list -> (List<Map<String, Object>>) (List<?>) list)
                .block();
    }

    public Map<String, Object> buildAddRequest(String id, double[] embedding, Map<String, String> metadata) {
        return Map.of(
                "ids", List.of(id),
                "embeddings", List.of(embedding),
                "metadatas", List.of(metadata)
        );
    }
}
