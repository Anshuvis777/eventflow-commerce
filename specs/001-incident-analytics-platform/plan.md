# Implementation Plan: Incident Analytics Platform

**Branch**: `001-incident-analytics-platform` | **Date**: 2026-08-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-incident-analytics-platform/spec.md`

## Summary

Build an AI-powered incident analytics platform that automatically detects, correlates, and analyzes production incidents across microservices. The system monitors Kafka business events and auto-creates incidents on failures, reconstructs timelines by correlation ID, performs GPT-4 root cause analysis with structured output, finds similar incidents via ChromaDB vector embeddings, provides centralized log query via REST API, and includes a React/TypeScript dashboard.

**Architecture style follows EventFlow Commerce**: Same Clean Architecture package layout, reuses `eventflow-common` (BaseEntity, ApiResponse, ErrorResponse, CorrelationIdFilter, LoggingAspect), MapStruct + Lombok, same structured logging. But lighter: **single PostgreSQL database**, REST-only (no CLI), 3 services + 1 dashboard.

Technical approach: Three independent Spring Boot microservices (incident-detector, incident-analyzer, incident-query) sharing a single PostgreSQL database, each running in its own Docker container. Reuses `eventflow-common` library for shared DTOs, exceptions, and cross-cutting concerns. Contract-first development with OpenAPI 3.1 specs and Kafka JSON schemas. Test-first TDD with JUnit 5 + Testcontainers for real Kafka/PostgreSQL/ChromaDB integration tests. Event-driven architecture with correlation ID propagation.

## Technical Context

**Language/Version**: Java 21, TypeScript 5+ (frontend)

**Primary Dependencies**: 
- Backend: Spring Boot 3.4.4, Spring Data JPA, Spring Kafka, Spring Web, Spring Validation
- Mapping: MapStruct 1.6.3, Lombok
- AI/ML: OpenAI Java SDK (or Spring WebClient) for GPT-4, ChromaDB REST API for embeddings
- Database: PostgreSQL 16 Alpine (single shared DB), ChromaDB (vector search)
- Migrations: Flyway
- Testing: JUnit 5, Mockito, Testcontainers, RestAssured, Spring Boot Test
- Frontend: React 18, TypeScript, Vite, TanStack Query, Tailwind CSS

**Shared Library**: Reuses `eventflow-common` module for:
- `BaseEntity` (id, createdAt, updatedAt, version, active)
- `ApiResponse<T>` / `ErrorResponse` — consistent API response format
- `CorrelationIdFilter` — MDC correlation ID propagation
- `LoggingAspect` — structured logging with execution time
- `JacksonConfig` — JSON serialization config
- `GlobalExceptionHandler` — `@ControllerAdvice` error handling
- `ResourceNotFoundException`, `BusinessRuleViolationException` — domain exceptions
- Domain event POJOs (OrderCreatedEvent, PaymentFailedEvent, etc.)

**Storage**: 
- PostgreSQL 16 (single shared database for all incident tables)
- ChromaDB (vector embeddings for similarity search)
- Kafka (event streaming — consumes business events from EventFlow services)

**Testing**: JUnit 5 (unit), Testcontainers (integration with real Kafka/PostgreSQL/ChromaDB), RestAssured (contract)

**Target Platform**: Linux server — **each service containerized with Docker**

**Project Type**: 3 Spring Boot microservices (incident-detector, incident-analyzer, incident-query) + 1 React frontend, Maven multi-module with shared `eventflow-common`

**Performance Goals**: 
- Incident detection latency < 5 seconds from Kafka event
- AI analysis completion < 30 seconds
- Similar incident search < 2 seconds (10k incidents)
- Log query API < 1 second (24h range)
- 10,000 events/second peak throughput

**Constraints**: 
- ≤3 backend services (incident-detector, incident-analyzer, incident-query)
- Each service runs as independent Docker container
- Reuses `eventflow-common` — no duplicate BaseEntity/ApiResponse/exceptions
- Single PostgreSQL database shared by all 3 services (lighter than DB-per-service)
- Dashboard as separate frontend project in its own container
- No authentication in v1 (open access)
- Single tenant (no multi-tenancy)
- 90-day retention in ChromaDB, then archive
- Correlation ID mandatory across all services

**Scale/Scope**: 
- 3 Spring Boot services + 1 frontend
- ~15 API endpoints
- 6 Kafka event types
- 5 database tables + 1 ChromaDB collection
- Dashboard with 4 main views

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Reuse eventflow-common | ✅ PASS | All 3 services depend on eventflow-common for BaseEntity, ApiResponse, CorrelationIdFilter, etc. |
| II. Clean Architecture Layout | ✅ PASS | Each service follows domain/entity/repository/dto/mapper/service/controller package structure |
| III. Test-First (NON-NEGOTIABLE) | ✅ PASS | Contract tests first (RestAssured), then unit/integration with Testcontainers |
| IV. Integration Testing with Real Infrastructure | ✅ PASS | Testcontainers for Kafka, PostgreSQL, ChromaDB in Docker |
| V. Observability, Simplicity & Framework Trust | ✅ PASS | ≤3 services, direct Spring Boot starters, SLF4J structured JSON logging |

**Pre-Implementation Gates** (must pass before /speckit-implement):
- Simplicity Gate: ✅ 3 Spring Boot services + 1 React frontend = 4 containers, all independent
- Anti-Abstraction Gate: ✅ Using Spring Boot, Spring Data JPA, ChromaDB REST API directly
- Integration-First Gate: ✅ Contracts defined in Phase 1, contract tests before implementation

## Project Structure

### Documentation (this feature)

```text
specs/001-incident-analytics-platform/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── openapi.yaml     # REST API specification
│   └── events/          # Kafka event schemas
│       ├── order-placed.json
│       ├── payment-failed.json
│       ├── inventory-released.json
│       ├── order-cancelled.json
│       ├── shipment-created.json
│       └── shipment-delivered.json
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
# Parent POM (multi-module Maven project — extends EventFlow parent)
pom.xml                              # Parent POM with shared dependency management

# Backend Services (each is a Spring Boot microservice with own Dockerfile)
incident-detector/
├── pom.xml                          # Maven build file (depends on eventflow-common)
├── Dockerfile                       # Multi-stage: Maven build + JRE 21 runtime
├── src/main/java/com/eventflow/incidentdetector/
│   ├── IncidentDetectorApplication.java    # @SpringBootApplication entry point
│   ├── domain/                        # Enums, value objects (no Spring deps)
│   │   ├── IncidentStatus.java        # OPEN, ANALYZING, ANALYZED, RESOLVED
│   │   └── Severity.java             # LOW, MEDIUM, HIGH, CRITICAL
│   ├── entity/                        # JPA entities (extends BaseEntity from common)
│   │   ├── IncidentEntity.java        # @Entity — incident metadata
│   │   └── EventEntity.java           # @Entity — Kafka events for timeline
│   ├── repository/                    # Spring Data JPA repositories
│   │   ├── IncidentRepository.java
│   │   └── EventRepository.java
│   ├── dto/request/                   # Request DTOs with @Valid
│   │   └── EventIngestRequest.java
│   ├── dto/response/                  # Response DTOs
│   │   ├── IncidentResponse.java
│   │   └── EventResponse.java
│   ├── mapper/                        # MapStruct mappers
│   │   └── IncidentMapper.java
│   ├── service/                       # Business logic (@Transactional)
│   │   └── IncidentDetectionService.java
│   ├── consumer/                      # Kafka listeners
│   │   └── BusinessEventConsumer.java # @KafkaListener
│   └── controller/                    # REST endpoints (@RestController)
│       └── IncidentController.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-docker.yml         # Docker profile (container hostnames)
│   └── db/migration/                  # Flyway SQL migrations
│       ├── V1__create_incidents_table.sql
│       └── V2__create_events_table.sql
├── src/test/java/com/eventflow/incidentdetector/
│   ├── unit/                          # Unit tests (Mockito)
│   ├── integration/                   # Testcontainers (Kafka, PostgreSQL)
│   └── contract/                      # REST API contract tests
└── pom.xml

incident-analyzer/
├── pom.xml
├── Dockerfile
├── src/main/java/com/eventflow/incidentanalyzer/
│   ├── IncidentAnalyzerApplication.java
│   ├── domain/
│   │   └── AnalysisConfidence.java    # Value object for confidence score
│   ├── entity/
│   │   ├── AnalysisEntity.java        # @Entity — GPT-4 analysis results
│   │   ├── IncidentEntity.java        # Read-only reference to incidents
│   │   └── EventEntity.java           # Read-only reference to events
│   ├── repository/
│   │   └── AnalysisRepository.java
│   ├── dto/request/
│   │   └── AnalysisTriggerRequest.java
│   ├── dto/response/
│   │   ├── AnalysisResponse.java
│   │   └── TimelineResponse.java
│   ├── mapper/
│   │   └── AnalysisMapper.java
│   ├── service/
│   │   ├── TimelineService.java       # Timeline reconstruction
│   │   ├── Gpt4AnalysisService.java   # OpenAI GPT-4 integration
│   │   └── AnalysisOrchestrationService.java
│   └── controller/
│       └── AnalysisController.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-docker.yml
│   └── db/migration/
│       └── V3__create_analyses_table.sql
├── src/test/java/com/eventflow/incidentanalyzer/
│   ├── unit/
│   ├── integration/
│   └── contract/
└── pom.xml

incident-query/
├── pom.xml
├── Dockerfile
├── src/main/java/com/eventflow/incidentquery/
│   ├── IncidentQueryApplication.java
│   ├── domain/
│   │   └── LogQueryParams.java        # Query parameter value object
│   ├── entity/
│   │   ├── LogEntryEntity.java        # @Entity — centralized log storage
│   │   ├── SimilarIncidentEntity.java # @Entity — pre-computed similarity
│   │   ├── IncidentEntity.java        # Read-only reference
│   │   └── EventEntity.java           # Read-only reference
│   ├── repository/
│   │   ├── LogEntryRepository.java
│   │   └── SimilarIncidentRepository.java
│   ├── dto/request/
│   │   └── LogIngestRequest.java
│   ├── dto/response/
│   │   ├── IncidentResponse.java
│   │   ├── TimelineResponse.java
│   │   ├── AnalysisResponse.java
│   │   ├── LogEntryResponse.java
│   │   ├── LogStatsResponse.java
│   │   └── SimilarIncidentResponse.java
│   ├── mapper/
│   │   └── LogMapper.java
│   ├── service/
│   │   ├── IncidentQueryService.java  # Incident CRUD + filters
│   │   ├── LogQueryService.java       # Centralized log queries
│   │   └── VectorService.java         # ChromaDB embedding & search
│   └── controller/                    # REST API (@RestController)
│       ├── IncidentController.java    # GET/PATCH /incidents
│       ├── TimelineController.java    # GET /incidents/{id}/timeline
│       ├── AnalysisController.java    # GET/POST /incidents/{id}/analysis
│       ├── SimilarController.java     # GET /incidents/{id}/similar
│       ├── LogController.java         # GET /logs, /logs/errors/stats
│       └── HealthController.java      # GET /health
├── src/main/resources/
│   ├── application.yml
│   ├── application-docker.yml
│   └── db/migration/
│       ├── V4__create_log_entries_table.sql
│       └── V5__create_similar_incidents_table.sql
├── src/test/java/com/eventflow/incidentquery/
│   ├── unit/
│   ├── integration/
│   └── contract/
└── pom.xml

# Frontend Dashboard (separate project, own Dockerfile)
dashboard/
├── src/
│   ├── components/                    # Reusable UI components
│   │   ├── IncidentList.tsx
│   │   ├── IncidentDetail.tsx
│   │   ├── Timeline.tsx
│   │   ├── Analysis.tsx
│   │   ├── SimilarIncidents.tsx
│   │   └── LogViewer.tsx
│   ├── pages/                         # Route-level components
│   │   ├── IncidentListPage.tsx
│   │   └── IncidentDetailPage.tsx
│   ├── services/                      # API client
│   │   └── api.ts
│   ├── hooks/                         # Custom React hooks
│   │   └── useIncidents.ts
│   ├── types/                         # TypeScript interfaces
│   │   └── index.ts
│   ├── App.tsx
│   └── main.tsx
├── tests/
├── package.json
├── vite.config.ts
├── Dockerfile                         # Multi-stage: Node build + Nginx
├── nginx.conf                         # Production static file serving
└── tsconfig.json

# Docker Infrastructure
docker/
├── compose.yml                        # Full stack: infra + services + dashboard
└── compose-infra.yml                  # Infrastructure only (Kafka, PostgreSQL, ChromaDB)
```

**Structure Decision**: Follows EventFlow Commerce's Clean Architecture package layout exactly:
- `domain/` — Enums, value objects (no Spring dependencies)
- `entity/` — JPA entities extending `BaseEntity` from `eventflow-common`
- `repository/` — Spring Data JPA repositories
- `dto/request/` — Request DTOs with Bean Validation
- `dto/response/` — Response DTOs
- `mapper/` — MapStruct mapper interfaces (not manual conversion)
- `service/` — Business logic with `@Transactional`
- `controller/` — REST endpoints with `@RestController`

**Key difference from EventFlow Commerce**: All 3 incident services share a **single PostgreSQL database** (lighter), but each service still has its own Docker container for independent deployment. Reuses `eventflow-common` for BaseEntity, ApiResponse, ErrorResponse, CorrelationIdFilter, LoggingAspect, and domain events.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 4th project (dashboard) | Dashboard is a web application, not a service; constitution allows separate frontend | Dashboard cannot be a microservice — it's a user-facing React app requiring Vite/Nginx build pipeline |
| Single shared PostgreSQL | Lighter than DB-per-service for 3 small services that share the same incident data model | DB-per-service would require cross-service joins and duplicate schemas for no benefit at this scale |

## Patterns Reused from EventFlow Commerce

| Pattern | How Applied |
|---------|-------------|
| **BaseEntity** | All JPA entities extend `BaseEntity` from `eventflow-common` (id, createdAt, updatedAt, version, active) |
| **ApiResponse / ErrorResponse** | All REST endpoints return `ApiResponse<T>` for success, `ErrorResponse` for errors |
| **CorrelationIdFilter** | Servlet filter extracts/generates correlation ID, stores in MDC for all log lines |
| **LoggingAspect** | AOP aspect logs method entry/exit with execution time |
| **GlobalExceptionHandler** | `@ControllerAdvice` handles validation, not-found, business rule violations |
| **MapStruct mappers** | Entity ↔ DTO mapping via `@Mapper(componentModel = "spring")` interfaces |
| **Lombok** | `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` on entities/DTOs |
| **Transactional Outbox** | Not needed — incident-detector reads from Kafka (doesn't publish business events) |
| **application-docker.yml** | Docker profile with container hostnames (e.g., `postgres` instead of `localhost`) |
| **Flyway migrations** | Versioned SQL in `db/migration/` — V1 through V5 across services |
| **Testcontainers** | Real PostgreSQL, Kafka, ChromaDB in integration tests |
| **Structured logging** | SLF4J + Logback with JSON encoder, MDC correlation ID |
