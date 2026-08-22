package com.eventflow.common.health;

import com.eventflow.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared lightweight health endpoint that hits the DB (SELECT 1).
 * Mounted at GET /health (Render healthCheckPath + cron ping).
 * incident-query keeps its richer GET /api/v1/health; this controller
 * avoids that path to prevent duplicate mapping.
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class DbHealthController {

    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", Instant.now().toString());
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            conn.createStatement().execute("SELECT 1");
            body.put("database", valid ? "UP" : "DOWN");
            body.put("databaseUrl", dataSource.getConnection().getMetaData().getURL().replaceAll("password=[^&;]+", "password=***"));
        } catch (Exception e) {
            body.put("database", "DOWN");
            body.put("error", e.getMessage());
            return ResponseEntity.status(503).body(ApiResponse.success(body));
        }
        return ResponseEntity.ok(ApiResponse.success(body));
    }
}
