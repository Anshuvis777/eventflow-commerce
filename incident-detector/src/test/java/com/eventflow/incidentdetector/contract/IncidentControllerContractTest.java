package com.eventflow.incidentdetector.contract;

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
class IncidentControllerContractTest {

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
    void shouldReturnIncidentsList() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/incidents")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data", is(notNullValue()));
    }

    @Test
    void shouldReturnIncidentById() {
        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "00000000-0000-0000-0000-000000000001")
        .when()
            .get("/api/v1/incidents/{id}")
        .then()
            .statusCode(404)
            .body("success", is(false));
    }

    @Test
    void shouldCreateIncident() {
        String requestBody = """
                {
                    "correlationId": "test-correlation-001",
                    "severity": "HIGH",
                    "title": "Payment processing failure",
                    "description": "Test incident creation"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/incidents")
        .then()
            .statusCode(201)
            .body("success", is(true))
            .body("data.correlationId", is("test-correlation-001"))
            .body("data.severity", is("HIGH"))
            .body("data.status", is("OPEN"));
    }

    @Test
    void shouldReturnBadRequestForMissingFields() {
        String requestBody = """
                {
                    "severity": "HIGH"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/v1/incidents")
        .then()
            .statusCode(400)
            .body("success", is(false))
            .body("error", is("VALIDATION_ERROR"));
    }

    @Test
    void shouldUpdateIncident() {
        String requestBody = """
                {
                    "status": "ANALYZING"
                }
                """;

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", "00000000-0000-0000-0000-000000000001")
            .body(requestBody)
        .when()
            .patch("/api/v1/incidents/{id}")
        .then()
            .statusCode(404)
            .body("success", is(false));
    }
}
