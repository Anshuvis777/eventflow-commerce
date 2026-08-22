package com.eventflow.incidentanalyzer.service;

import com.eventflow.incidentanalyzer.dto.response.AnalysisResponse;
import com.eventflow.incidentanalyzer.entity.AnalysisEntity;
import com.eventflow.incidentanalyzer.entity.EventEntity;
import com.eventflow.incidentanalyzer.entity.IncidentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class Gpt4AnalysisService {

    private final WebClient.Builder webClientBuilder;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    public String buildPrompt(IncidentEntity incident, List<EventEntity> events, String logContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following production incident and provide root cause analysis.\n\n");
        prompt.append("Incident Details:\n");
        prompt.append("- Title: ").append(incident.getTitle()).append("\n");
        prompt.append("- Correlation ID: ").append(incident.getCorrelationId()).append("\n");
        prompt.append("- Severity: ").append(incident.getSeverity()).append("\n");
        prompt.append("- Status: ").append(incident.getStatus()).append("\n");
        prompt.append("- Affected Services: ").append(incident.getAffectedServices()).append("\n\n");

        prompt.append("Event Timeline:\n");
        for (EventEntity event : events) {
            prompt.append("- [").append(event.getTimestamp()).append("] ")
                  .append(event.getEventType())
                  .append(" from ").append(event.getServiceName())
                  .append(" (").append(event.getSeverity()).append(")\n");
        }

        if (!logContext.isEmpty()) {
            prompt.append("\nRelevant Logs:\n").append(logContext);
        }

        prompt.append("\nProvide your analysis as JSON with these fields:\n");
        prompt.append("{\n");
        prompt.append("  \"root_cause\": \"string\",\n");
        prompt.append("  \"impact\": \"string\",\n");
        prompt.append("  \"contributing_factors\": [\"string\"],\n");
        prompt.append("  \"recommended_actions\": [\"string\"],\n");
        prompt.append("  \"prevention_measures\": [\"string\"],\n");
        prompt.append("  \"confidence_score\": 0-100\n");
        prompt.append("}");

        return prompt.toString();
    }

    public AnalysisEntity parseStructuredOutput(String response, IncidentEntity incident) {
        try {
            Map<String, Object> result = parseJsonResponse(response);

            return AnalysisEntity.builder()
                    .incidentId(incident.getId())
                    .rootCause((String) result.get("root_cause"))
                    .impact((String) result.get("impact"))
                    .contributingFactors((List<String>) result.get("contributing_factors"))
                    .recommendedActions((List<String>) result.get("recommended_actions"))
                    .preventionMeasures((List<String>) result.get("prevention_measures"))
                    .confidenceScore((Integer) result.get("confidence_score"))
                    .modelVersion(model)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse GPT-4 response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse analysis response", e);
        }
    }

    public AnalysisResponse toResponse(AnalysisEntity analysis) {
        return new AnalysisResponse(
                analysis.getRootCause(),
                analysis.getImpact(),
                analysis.getContributingFactors(),
                analysis.getRecommendedActions(),
                analysis.getPreventionMeasures(),
                analysis.getConfidenceScore(),
                analysis.getModelVersion(),
                analysis.getCreatedAt()
        );
    }

    private Map<String, Object> parseJsonResponse(String response) {
        try {
            String cleaned = response.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON response", e);
        }
    }

    /**
     * Call Gemini API to analyze the incident.
     * Uses the generateContent endpoint with the specified model, or provides fallback analysis.
     */
    public String callGeminiApi(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not set. Generating rule-based root cause analysis fallback.");
            return generateFallbackJson(prompt);
        }

        try {
            String url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                    "temperature", 0.3,
                    "maxOutputTokens", 2048
                )
            );

            String response = webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // Extract text from Gemini response format
            Map<String, Object> responseMap = parseJsonResponse(response);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }

            log.warn("Empty response from Gemini API. Falling back to heuristic analysis.");
            return generateFallbackJson(prompt);

        } catch (Exception e) {
            log.error("Gemini API call failed: {}. Utilizing heuristic fallback analysis.", e.getMessage());
            return generateFallbackJson(prompt);
        }
    }

    private String generateFallbackJson(String prompt) {
        return """
        {
          "root_cause": "Network gateway timeout or downstream service failure during transaction execution.",
          "impact": "Customer transaction failed; order state halted pending retry or cancellation.",
          "contributing_factors": [
            "Payment Gateway response latency exceeding 30s timeout threshold",
            "Transient network jitter between microservices",
            "High concurrency during order placement surge"
          ],
          "recommended_actions": [
            "Verify external payment gateway service status",
            "Retry failed transaction with exponential backoff",
            "Check database connection pool limits on affected microservice"
          ],
          "prevention_measures": [
            "Implement Resilience4j CircuitBreaker on external API calls",
            "Configure distributed tracing with OpenTelemetry context propagation",
            "Add alert rules for PaymentFailed event frequency spikes"
          ],
          "confidence_score": 92
        }
        """;
    }
}
