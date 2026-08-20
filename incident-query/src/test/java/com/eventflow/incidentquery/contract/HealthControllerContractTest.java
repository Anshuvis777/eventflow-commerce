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
class HealthControllerContractTest {

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
    void shouldReturnHealthStatus() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/health")
        .then()
            .statusCode(200)
            .body("status", is("UP"))
            .body("components", is(notNullValue()));
    }

    @Test
    void shouldCheckPostgresHealth() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/health")
        .then()
            .statusCode(200)
            .body("components.postgres.status", is("UP"));
    }

    @Test
    void shouldCheckChromaDBHealth() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/health")
        .then()
            .statusCode(200)
            .body("components.chromadb.status", is("UP"));
    }
}
