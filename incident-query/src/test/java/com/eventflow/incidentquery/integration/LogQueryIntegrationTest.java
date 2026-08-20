package com.eventflow.incidentquery.integration;

import com.eventflow.incidentquery.entity.LogEntryEntity;
import com.eventflow.incidentquery.repository.LogEntryRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class LogQueryIntegrationTest {

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
    private LogEntryRepository logEntryRepository;

    @BeforeEach
    void setUp() {
        logEntryRepository.deleteAll();
    }

    @Test
    void shouldFilterLogsByCorrelationId() {
        // Given
        String correlationId = "corr-test-001";
        logEntryRepository.save(createLogEntry(correlationId, "payment-service", "ERROR"));
        logEntryRepository.save(createLogEntry(correlationId, "order-service", "INFO"));
        logEntryRepository.save(createLogEntry("corr-other", "payment-service", "ERROR"));

        // When
        List<LogEntryEntity> results = logEntryRepository.findByCorrelationId(correlationId);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFilterLogsByLevel() {
        // Given
        logEntryRepository.save(createLogEntry("corr-1", "payment-service", "ERROR"));
        logEntryRepository.save(createLogEntry("corr-2", "order-service", "INFO"));
        logEntryRepository.save(createLogEntry("corr-3", "inventory-service", "CRITICAL"));

        // When
        List<LogEntryEntity> results = logEntryRepository.findByLevel("ERROR");

        // Then
        assertThat(results).hasSize(1);
    }

    private LogEntryEntity createLogEntry(String correlationId, String serviceName, String level) {
        return LogEntryEntity.builder()
                .correlationId(correlationId)
                .serviceName(serviceName)
                .level(level)
                .message("Test log message")
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
