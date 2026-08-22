package com.eventflow.incidentquery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    /**
     * Generate a deterministic embedding from incident text without requiring an external model.
     * Uses TF-hash style hashing into a fixed dimension. For production, replace with a call to
     * an embedding provider (OpenAI / Gemini / sentence-transformers).
     *
     * @param text incident text (title + rootCause + event types)
     * @param dimensions embedding size (e.g. 384 or 768)
     */
    public double[] embedText(String text, int dimensions) {
        if (text == null || text.isBlank()) text = "empty incident";
        double[] vec = new double[dimensions];
        String[] tokens = text.toLowerCase().split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.length() < 2) continue;
            int hash = Math.abs(token.hashCode());
            int idx = hash % dimensions;
            vec[idx] += 1.0;
            // bigram spread for slightly richer signal
            int idx2 = (hash * 31) % dimensions;
            vec[Math.abs(idx2) % dimensions] += 0.3;
        }
        // L2 normalize
        double norm = 0;
        for (double v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        }
        return vec;
    }

    public double[] embedText(String text) {
        return embedText(text, 384);
    }

    /**
     * Cosine similarity between two vectors.
     */
    public double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Dimension mismatch");
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom == 0 ? 0 : dot / denom;
    }
}
