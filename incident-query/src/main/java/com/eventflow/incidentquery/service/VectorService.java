package com.eventflow.incidentquery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.http.MediaType;

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

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.embedding-model:text-embedding-004}")
    private String embeddingModel;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String geminiBaseUrl;

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
     * Generate embedding via Gemini text-embedding-004 when GEMINI_API_KEY is set,
     * otherwise falls back to deterministic TF-hash (keeps dev/offline working).
     */
    public double[] embedText(String text, int dimensions) {
        if (text == null || text.isBlank()) text = "empty incident";
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                return embedWithGemini(text, dimensions);
            } catch (Exception e) {
                log.warn("Gemini embedding failed ({}), using hash fallback", e.getMessage());
            }
        }
        return hashEmbed(text, dimensions);
    }

    private double[] embedWithGemini(String text, int requestedDimensions) {
        String url = geminiBaseUrl + "/v1beta/models/" + embeddingModel + ":embedContent?key=" + geminiApiKey;
        Map<String, Object> body = Map.of("content", Map.of("parts", List.of(Map.of("text", text))));
        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClientBuilder.build()
                .post().uri(url).contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                .retrieve().bodyToMono(Map.class).block();
        @SuppressWarnings("unchecked")
        Map<String, Object> embedding = (Map<String, Object>) response.get("embedding");
        @SuppressWarnings("unchecked")
        List<Number> values = (List<Number>) embedding.get("values");
        double[] vec = new double[values.size()];
        for (int i = 0; i < values.size(); i++) vec[i] = values.get(i).doubleValue();
        // adapt to requested dimensions (Gemini 004 = 768)
        if (vec.length == requestedDimensions) return vec;
        double[] out = new double[requestedDimensions];
        System.arraycopy(vec, 0, out, 0, Math.min(vec.length, requestedDimensions));
        // L2 normalize adapted vector
        double norm = 0; for (double v : out) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < out.length; i++) out[i] /= norm;
        return out;
    }

    private double[] hashEmbed(String text, int dimensions) {
        double[] vec = new double[dimensions];
        String[] tokens = text.toLowerCase().split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.length() < 2) continue;
            int hash = Math.abs(token.hashCode());
            int idx = hash % dimensions;
            vec[idx] += 1.0;
            int idx2 = (hash * 31) % dimensions;
            vec[Math.abs(idx2) % dimensions] += 0.3;
        }
        double norm = 0; for (double v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < vec.length; i++) vec[i] /= norm;
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
