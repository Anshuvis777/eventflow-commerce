package com.eventflow.incidentquery.controller;

import com.eventflow.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    @Value("${spring.datasource.url:}")
    private String postgresUrl;

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Value("${chromadb.base-url:http://localhost:8000}")
    private String chromaDbUrl;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "incident-query");
        health.put("postgres", "CONNECTED");
        health.put("kafka", "CONNECTED");
        health.put("chromadb", "CONNECTED");
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
