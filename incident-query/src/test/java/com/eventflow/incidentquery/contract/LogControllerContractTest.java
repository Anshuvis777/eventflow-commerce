package com.eventflow.incidentquery.contract;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class LogControllerContractTest {

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

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void shouldQueryLogs() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("limit", 10)
        .when()
            .get("/api/v1/logs")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data", is(notNullValue()));
    }

    @Test
    void shouldQueryLogsByCorrelationId() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("correlationId", "test-correlation-001")
        .when()
            .get("/api/v1/logs")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }

    @Test
    void shouldQueryLogsByLevel() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("level", "ERROR")
        .when()
            .get("/api/v1/logs")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }

    @Test
    void shouldIngestLog() {
        String requestBody = """
                {
                    "correlationId": "test-correlation-001",
                    "serviceName": "order-service",
                    "level": "ERROR",
                    "message": "Payment processing failed",
                    "timestamp": "2026-08-15T10:30:00Z"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/logs")
        .then()
            .statusCode(201)
            .body("success", is(true));
    }

    @Test
    void shouldGetErrorStats() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("startTime", "2026-08-01T00:00:00Z")
            .queryParam("endTime", "2026-08-16T23:59:59Z")
        .when()
            .get("/api/v1/logs/errors/stats")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
}
