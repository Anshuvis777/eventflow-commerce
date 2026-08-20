# Research: Incident Analytics Platform

**Feature**: 001-incident-analytics-platform
**Date**: 2026-08-15

## Technical Decisions

### 1. Vector Database: ChromaDB vs pgvector

**Decision**: ChromaDB for vector embeddings, PostgreSQL for relational data

**Rationale**: 
- User explicitly requested ChromaDB for RAG database with 90-day retention
- ChromaDB is purpose-built for vector similarity search with better developer experience
- Separation of concerns: PostgreSQL for ACID transactions (incidents, events, analysis), ChromaDB for vector search
- ChromaDB supports metadata filtering alongside vector search via REST API
- Java services interact with ChromaDB via its REST API (ChromaDB Java client or Spring WebClient)

**Alternatives Considered**:
- pgvector in PostgreSQL: Single database, but less optimized for pure vector workloads; user specifically asked for ChromaDB
- Pinecone/Weaviate: Managed services, adds external dependency and cost
- FAISS: Requires custom service wrapper, more operational overhead

### 2. Backend Framework: Spring Boot 3.3+ vs Quarkus vs Micronaut

**Decision**: Spring Boot 3.3+

**Rationale**:
- Most mature Java microservices framework with largest ecosystem
- Spring Kafka provides native Kafka integration with listener containers
- Spring Data JPA simplifies PostgreSQL persistence
- Spring Boot Actuator provides health checks and metrics out of the box
- Native OpenAPI generation via springdoc-openapi
- Excellent Docker containerization support (Spring Boot layers for optimized images)
- Built-in dependency injection, configuration management, and profile support

**Alternatives Considered**:
- Quarkus: Faster startup, but smaller ecosystem and less Spring compatibility
- Micronaut: Good performance, but less mature Spring Data JPA integration

### 3. Kafka Integration: Spring Kafka vs Kafka Java Client (org.apache.kafka)

**Decision**: Spring Kafka

**Rationale**:
- Declarative listener containers with `@KafkaListener` annotation
- Built-in error handling, retry, and dead-letter topic support
- Seamless integration with Spring Boot transaction management
- Automatic JSON serialization/deserialization with Jackson
- Consumer group management and offset tracking built-in

**Alternatives Considered**:
- Raw Kafka Java Client: More control, but requires manual consumer management
- Faust (Python): Not applicable for Java stack

### 4. GPT-4 Integration: Spring AI vs OpenAI Java SDK vs Direct HTTP

**Decision**: OpenAI Java SDK (com.openai:openai-java) or Spring WebClient for HTTP

**Rationale**:
- Official SDK with type-safe request/response models
- Supports structured output (JSON mode) for analysis results
- Built-in retry with exponential backoff
- Spring WebClient as fallback for direct HTTP with better Spring integration

**Alternatives Considered**:
- Spring AI: Newer, may not be production-ready yet; adds abstraction layer
- LangChain4j: Overkill for single GPT-4 call; adds heavy dependency

### 5. ORM: Spring Data JPA vs jOOQ vs JDBC Template

**Decision**: Spring Data JPA (Hibernate 6)

**Rationale**:
- Repository pattern with automatic query derivation
- Entity mapping with JPA annotations
- Flyway integration for schema migrations
- Transaction management via `@Transactional`
- Flexible Specification API for complex log filters

**Alternatives Considered**:
- jOOQ: Type-safe SQL, but requires code generation step
- JDBC Template: Too low-level for complex entity relationships

### 6. Frontend: React/TypeScript/Vite vs Next.js vs Vue

**Decision**: React 18 + TypeScript + Vite

**Rationale**:
- Constitution specifies "React 18+ with TypeScript, Vite build tool"
- Vite provides fast HMR for development
- TanStack Query for server state management
- Tailwind CSS for rapid UI development
- Nginx for production static file serving in Docker

### 7. Testing: JUnit 5 + Testcontainers vs Mockito-only

**Decision**: JUnit 5 + Testcontainers + RestAssured

**Rationale**:
- Constitution mandates "Integration tests MUST use realistic environments: prefer real databases over mocks"
- Testcontainers provides real Kafka, PostgreSQL, ChromaDB in Docker for integration tests
- RestAssured for REST API contract validation
- Mockito for unit tests with external service mocking (OpenAI API)
- `@SpringBootTest` for full application context tests

**Alternatives Considered**:
- Mockito-only: Violates constitution principle IV
- ArchUnit: Useful for architecture tests but not a replacement for integration tests

### 8. Configuration: Spring Boot application.yml vs other

**Decision**: Spring Boot application.yml + profiles

**Rationale**:
- Type-safe configuration with `@ConfigurationProperties`
- Environment-specific profiles (dev, test, prod)
- Native support for Docker environment variables
- Health check endpoints via Actuator
- Supports `.env` files via spring-boot-docker-compose

### 9. Logging: SLF4J + Logback vs Log4j2

**Decision**: SLF4J + Logback (Spring Boot default)

**Rationale**:
- Constitution mandates "Structured JSON logging"
- Logback with `LogstashEncoder` for JSON output
- MDC (Mapped Diagnostic Context) for correlation_id propagation
- Native Spring Boot integration, no extra dependencies
- Structured fields for log aggregation (ELK, Datadog, etc.)

### 10. Container Orchestration: Docker Compose vs Kubernetes

**Decision**: Each service has its own Dockerfile; Docker Compose for local dev, Kubernetes-ready manifests

**Rationale**:
- Constitution specifies "Docker Compose for local dev, Kubernetes-ready manifests"
- Each Spring Boot service builds as independent Docker image
- Docker Compose orchestrates all services locally with health checks
- Multi-stage Dockerfiles for optimized production images
- Kubernetes manifests for production deployment

## Architecture Patterns

### Event-Driven Communication
- incident-detector consumes Kafka business events from EventFlow Commerce services
- Correlation ID propagated in Kafka headers and event payloads (via CorrelationIdFilter from eventflow-common)
- Consumer groups for horizontal scaling of incident-detector

### Reuse eventflow-common (MANDATORY)
All incident services reuse the shared `eventflow-common` library:
- `BaseEntity` — all JPA entities extend this (id, createdAt, updatedAt, version, active)
- `ApiResponse<T>` / `ErrorResponse` — consistent API response format
- `CorrelationIdFilter` — MDC correlation ID propagation
- `LoggingAspect` — structured logging with execution time
- `JacksonConfig` — JSON serialization config
- `GlobalExceptionHandler` — `@ControllerAdvice` error handling
- `ResourceNotFoundException`, `BusinessRuleViolationException` — domain exceptions
- Domain event POJOs (OrderCreatedEvent, PaymentFailedEvent, etc.)

### Clean Architecture Package Layout
Every service follows EventFlow Commerce's package structure:
- `domain/` — Enums, value objects (no Spring dependencies)
- `entity/` — JPA entities extending BaseEntity
- `repository/` — Spring Data JPA repositories
- `dto/request/` — Request DTOs with Bean Validation
- `dto/response/` — Response DTOs (Java records)
- `mapper/` — MapStruct mapper interfaces
- `service/` — Business logic with @Transactional
- `controller/` — REST endpoints with @RestController

### Single Shared PostgreSQL Database
All 3 incident services share one PostgreSQL instance (lighter than DB-per-service):
- incident-detector owns V1–V2 migrations (incidents, events)
- incident-analyzer owns V3 migration (analyses)
- incident-query owns V4–V5 migrations (log_entries, similar_incidents)
- Flyway prevents conflicts via versioned migrations

### Contract-First Development
1. Define OpenAPI 3.1 spec in `contracts/openapi.yaml`
2. Define Kafka event schemas in `contracts/events/*.json`
3. Generate contract tests with RestAssured
4. Implement to satisfy contracts

### Test-First TDD
1. Write contract tests (Red)
2. Write unit tests (Red)
3. Write integration tests with Testcontainers (Red)
4. Implement to make tests pass (Green)
5. Refactor (Refactor)

## Open Questions Resolved

| Question | Resolution |
|----------|------------|
| Vector DB | ChromaDB (user specified) |
| Retention | 90 days active in ChromaDB, then archive |
| Auth | None for v1 (open access) |
| Multi-tenancy | Single tenant |
| Dashboard port | 8091 (specified in requirements) |
| Embedding model | text-embedding-3-small (1536 dim) |
| GPT-4 model | gpt-4-turbo-preview |

## Dependencies Summary

### Parent POM (extends eventflow-commerce parent, shared dependency management)
```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.4.4</spring-boot.version>
    <spring-kafka.version>3.3.0</spring-kafka.version>
    <springdoc.version>2.6.0</springdoc.version>
    <mapstruct.version>1.6.3</mapstruct.version>
    <flyway.version>10.16.0</flyway.version>
    <testcontainers.version>1.21.4</testcontainers.version>
    <chromadb.version>0.5.0</chromadb.version>
</properties>
```

### incident-detector (extends parent, depends on eventflow-common)
```xml
<dependencies>
    <dependency>com.eventflow:eventflow-common</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-web</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-data-jpa</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-validation</dependency>
    <dependency>org.springframework.kafka:spring-kafka</dependency>
    <dependency>org.flywaydb:flyway-core</dependency>
    <dependency>org.postgresql:postgresql</dependency>
    <dependency>org.mapstruct:mapstruct</dependency>
    <dependency>org.projectlombok:lombok</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-test</dependency>
    <dependency>org.testcontainers:junit-jupiter</dependency>
    <dependency>org.testcontainers:kafka</dependency>
    <dependency>org.testcontainers:postgresql</dependency>
</dependencies>
```

### incident-analyzer (extends parent, depends on eventflow-common)
```xml
<dependencies>
    <dependency>com.eventflow:eventflow-common</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-web</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-data-jpa</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-validation</dependency>
    <dependency>org.postgresql:postgresql</dependency>
    <dependency>org.flywaydb:flyway-core</dependency>
    <dependency>org.mapstruct:mapstruct</dependency>
    <dependency>org.projectlombok:lombok</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-test</dependency>
    <dependency>org.testcontainers:junit-jupiter</dependency>
    <dependency>org.testcontainers:postgresql</dependency>
    <!-- OpenAI via Spring WebClient (no SDK dependency needed) -->
    <dependency>org.springframework.boot:spring-boot-starter-webflux</dependency>
</dependencies>
```

### incident-query (extends parent, depends on eventflow-common)
```xml
<dependencies>
    <dependency>com.eventflow:eventflow-common</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-web</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-data-jpa</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-validation</dependency>
    <dependency>org.postgresql:postgresql</dependency>
    <dependency>org.flywaydb:flyway-core</dependency>
    <dependency>org.mapstruct:mapstruct</dependency>
    <dependency>org.projectlombok:lombok</dependency>
    <dependency>org.springdoc:springdoc-openapi-starter-webmvc-ui</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-test</dependency>
    <dependency>org.testcontainers:junit-jupiter</dependency>
    <dependency>org.testcontainers:postgresql</dependency>
    <!-- ChromaDB via Spring WebClient (REST API) -->
    <dependency>org.springframework.boot:spring-boot-starter-webflux</dependency>
</dependencies>
```

### dashboard
```json
{
  "dependencies": {
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "typescript": "^5.5.0",
    "vite": "^5.4.0",
    "@tanstack/react-query": "^5.17.0",
    "axios": "^1.7.0",
    "tailwindcss": "^3.4.0"
  }
}
```

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| OpenAI API rate limits | Medium | High | Implement retry with exponential backoff; queue analysis requests |
| ChromaDB scaling | Low | Medium | Monitor query latency; plan for sharding if needed |
| Kafka consumer lag | Medium | High | Horizontal scaling via consumer groups; monitoring alerts |
| GPT-4 hallucination | Medium | Medium | Structured output with confidence scoring; human review workflow |
| Schema evolution | Medium | Medium | Contract tests; backward-compatible schema changes only |