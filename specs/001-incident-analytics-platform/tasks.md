# Tasks: Incident Analytics Platform

**Input**: Design documents from `/specs/001-incident-analytics-platform/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Test tasks included per constitution mandate (Test-First, NON-NEGOTIABLE).

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup — Project Infrastructure

> Goal: Bootstrap Maven multi-module project, parent POM, shared Docker infrastructure.
> Independent Test: `./mvnw clean install` succeeds, `docker compose up -d` starts Kafka + PostgreSQL + ChromaDB.

- [X] T001 Create parent POM with shared dependency management in `pom.xml` (Java 21, Spring Boot 3.4.4, MapStruct 1.6.3, Lombok, Testcontainers 1.21.4)
- [X] T002 [P] Create `incident-detector/pom.xml` with dependencies on `eventflow-common`, Spring Boot Web, Spring Data JPA, Spring Kafka, Flyway, PostgreSQL, Lombok, MapStruct
- [X] T003 [P] Create `incident-analyzer/pom.xml` with dependencies on `eventflow-common`, Spring Boot Web, Spring Data JPA, Spring WebFlux (for OpenAI), Flyway, PostgreSQL, Lombok, MapStruct
- [X] T004 [P] Create `incident-query/pom.xml` with dependencies on `eventflow-common`, Spring Boot Web, Spring Data JPA, Spring WebFlux (for ChromaDB), SpringDoc OpenAPI, Flyway, PostgreSQL, Lombok, MapStruct
- [X] T005 [P] Create `dashboard/package.json` with React 18, TypeScript, Vite, TanStack Query, Tailwind CSS, Axios
- [X] T006 Create `docker/compose.yml` with infrastructure services: Kafka (port 9092), PostgreSQL 16 (port 5432, shared DB `incident_analytics`), ChromaDB (port 8000), all on `eventflow-net` bridge network
- [X] T007 [P] Create `docker/compose-infra.yml` for infrastructure-only startup (Kafka, PostgreSQL, ChromaDB)
- [X] T008 [P] Create `incident-detector/Dockerfile` (multi-stage: Maven build + JRE 21 runtime, `--spring.profiles.active=docker`)
- [X] T009 [P] Create `incident-analyzer/Dockerfile` (multi-stage: Maven build + JRE 21 runtime)
- [X] T010 [P] Create `incident-query/Dockerfile` (multi-stage: Maven build + JRE 21 runtime)
- [X] T011 [P] Create `dashboard/Dockerfile` (multi-stage: Node build + Nginx)
- [X] T012 [P] Create `dashboard/nginx.conf` for production static file serving with API proxy to incident-query

---

## Phase 2: Foundational — Shared Module & Database

> Goal: Build eventflow-common integration, create Flyway migrations, establish shared patterns.
> Independent Test: All 5 Flyway migrations run successfully on shared PostgreSQL, BaseEntity/ApiResponse work in all services.

- [X] T013 Create shared database migrations in `incident-detector/src/main/resources/db/migration/V1__create_incidents_table.sql` (incidents table with all columns, indexes per data-model.md)
- [X] T014 [P] Create `incident-detector/src/main/resources/db/migration/V2__create_events_table.sql` (events table with foreign key to incidents, indexes)
- [X] T015 [P] Create `incident-analyzer/src/main/resources/db/migration/V3__create_analyses_table.sql` (analyses table with foreign key to incidents, CHECK constraint on confidence_score)
- [X] T016 [P] Create `incident-query/src/main/resources/db/migration/V4__create_log_entries_table.sql` (log_entries table with indexes)
- [X] T017 [P] Create `incident-query/src/main/resources/db/migration/V5__create_similar_incidents_table.sql` (similar_incidents table with foreign keys)
- [X] T018 Create `incident-detector/src/main/java/com/eventflow/incidentdetector/domain/IncidentStatus.java` enum (OPEN, ANALYZING, ANALYZED, RESOLVED)
- [X] T019 [P] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/domain/Severity.java` enum (LOW, MEDIUM, HIGH, CRITICAL)
- [X] T020 [P] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/domain/LogLevel.java` enum (DEBUG, INFO, WARN, ERROR, CRITICAL)
- [X] T021 Create `incident-detector/src/main/resources/application.yml` with Spring Data JPA, Kafka, Flyway, and logging config (single shared PostgreSQL)
- [X] T022 [P] Create `incident-detector/src/main/resources/application-docker.yml` with Docker profile (container hostnames: `postgres`, `kafka`, `chromadb`)
- [X] T023 [P] Create `incident-analyzer/src/main/resources/application.yml` and `application-docker.yml`
- [X] T024 [P] Create `incident-query/src/main/resources/application.yml` and `application-docker.yml`
- [X] T025 Create `incident-detector/src/main/java/com/eventflow/incidentdetector/IncidentDetectorApplication.java` with `@SpringBootApplication`
- [X] T109 [P] Create `incident-detector/src/test/java/com/eventflow/incidentdetector/contract/IncidentControllerContractTest.java` (RestAssured: validate against OpenAPI spec)
- [X] T110 [P] Create `incident-query/src/test/java/com/eventflow/incidentquery/contract/LogControllerContractTest.java` (RestAssured: validate log query endpoints)
- [X] T111 [P] Create `incident-query/src/test/java/com/eventflow/incidentquery/contract/HealthControllerContractTest.java` (RestAssured: validate health endpoint)

---

## Phase 3: User Story 1 — Automatic Incident Detection [US1]

> Goal: Kafka consumer detects failure events and auto-creates incidents with correct metadata.
> Independent Test: Publish PaymentFailed event to Kafka → incident created with status OPEN, severity HIGH, correct correlation_id.

### US1 — Tests (write first)

- [X] T026 [US1] Create `incident-detector/src/test/java/com/eventflow/incidentdetector/unit/IncidentDetectionServiceTest.java` (Mockito unit tests for incident creation logic)
- [X] T027 [US1] Create `incident-detector/src/test/java/com/eventflow/incidentdetector/integration/BusinessEventConsumerIntegrationTest.java` (Testcontainers: real Kafka + PostgreSQL, publish PaymentFailed → verify incident created)

### US1 — Entities & Repositories

- [X] T028 [P] [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/entity/IncidentEntity.java` (extends BaseEntity, Lombok @Data/@Builder, all fields per data-model.md)
- [X] T029 [P] [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/entity/EventEntity.java` (extends BaseEntity, @ManyToOne to IncidentEntity)
- [X] T030 [P] [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/repository/IncidentRepository.java` (Spring Data JPA, findByCorrelationId)
- [X] T031 [P] [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/repository/EventRepository.java` (Spring Data JPA, findByCorrelationId)

### US1 — DTOs & Mappers

- [X] T032 [P] [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/dto/response/IncidentResponse.java` (Java record)
- [X] T033 [P] [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/dto/response/EventResponse.java` (Java record)
- [X] T034 [P] [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/dto/request/EventIngestRequest.java` (Java record with @Valid)
- [X] T035 [P] [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/mapper/IncidentMapper.java` (MapStruct @Mapper(componentModel="spring"))

### US1 — Service & Consumer

- [X] T036 [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/service/IncidentDetectionService.java` with `@Transactional` methods: `processEvent(EventIngestRequest)`, `findOrCreateIncident(correlationId, severity)`, `groupByCorrelationId(events)`
- [X] T037 [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/consumer/BusinessEventConsumer.java` with `@KafkaListener(topics="business-events")`, deserializes JSON, calls IncidentDetectionService.processEvent()
- [X] T038 [US1] Create `incident-detector/src/main/java/com/eventflow/incidentdetector/controller/IncidentController.java` with GET /incidents, GET /incidents/{id}, POST /incidents, PATCH /incidents/{id} — all returning `ApiResponse<T>`

---

## Phase 4: User Story 2 — Timeline Reconstruction [US2]

> Goal: Engineers can view chronological event timeline for any incident by correlation_id.
> Independent Test: Publish OrderPlaced → PaymentFailed → InventoryReleased with same correlation_id → timeline shows 3 events in order with duration.

### US2 — Tests (write first)

- [X] T039 [US2] Create `incident-analyzer/src/test/java/com/eventflow/incidentanalyzer/unit/TimelineServiceTest.java` (Mockito unit tests)
- [X] T040 [US2] Create `incident-analyzer/src/test/java/com/eventflow/incidentanalyzer/integration/TimelineIntegrationTest.java` (Testcontainers: real PostgreSQL, verify timeline query)

### US2 — Entities & Repositories

- [X] T041 [P] [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/entity/IncidentEntity.java` (read-only reference, extends BaseEntity)
- [X] T042 [P] [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/entity/EventEntity.java` (read-only reference, extends BaseEntity)
- [X] T043 [P] [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/repository/IncidentRepository.java` (findById, findByCorrelationId)
- [X] T044 [P] [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/repository/EventRepository.java` (findByIncidentIdOrderByTimestampAsc)

### US2 — DTOs & Mappers

- [X] T045 [P] [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/dto/response/TimelineResponse.java` (Java record: incidentId, events list, totalDurationSeconds, eventCount, affectedServices)
- [X] T046 [P] [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/dto/response/EventResponse.java` (Java record)
- [X] T047 [P] [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/mapper/AnalysisMapper.java` (MapStruct, maps EventEntity → EventResponse)

### US2 — Service & Controller

- [X] T048 [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/service/TimelineService.java` with methods: `getTimeline(incidentId)`, `calculateDuration(events)`, `extractAffectedServices(events)`
- [X] T049 [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/controller/AnalysisController.java` with GET /incidents/{id}/timeline returning `ApiResponse<TimelineResponse>`
- [X] T050 [US2] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/IncidentAnalyzerApplication.java`

---

## Phase 5: User Story 3 — AI Root Cause Analysis [US3]

> Goal: Trigger GPT-4 analysis returning structured output with root cause, impact, factors, actions, prevention, confidence.
> Independent Test: Create incident with PaymentFailed timeline → trigger analysis → response contains all 6 fields with confidence > 80%.

### US3 — Tests (write first)

- [X] T051 [US3] Create `incident-analyzer/src/test/java/com/eventflow/incidentanalyzer/unit/Gpt4AnalysisServiceTest.java` (Mockito: mock WebClient, verify prompt building, verify structured output parsing)
- [X] T052 [US3] Create `incident-analyzer/src/test/java/com/eventflow/incidentanalyzer/integration/AnalysisIntegrationTest.java` (Testcontainers: real PostgreSQL, verify analysis creation and retrieval)

### US3 — Entities & Repository

- [X] T053 [P] [US3] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/entity/AnalysisEntity.java` (extends BaseEntity, @OneToOne to IncidentEntity, all fields per data-model.md)
- [X] T054 [P] [US3] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/repository/AnalysisRepository.java` (findByIncidentId)
- [X] T055 [P] [US3] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/domain/AnalysisConfidence.java` (value object: score 0-100, with factory methods for high/low confidence)

### US3 — DTOs & Service

- [X] T056 [P] [US3] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/dto/response/AnalysisResponse.java` (Java record: rootCause, impact, contributingFactors, recommendedActions, preventionMeasures, confidenceScore, modelVersion)
- [X] T057 [P] [US3] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/dto/request/AnalysisTriggerRequest.java` (Java record: force boolean)
- [X] T058 [US3] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/service/Gpt4AnalysisService.java` with: `analyzeIncident(incidentId)`, `buildPrompt(incident, timeline, logs)`, `parseStructuredOutput(response)`, `generateEmbedding(analysis)` — uses Spring WebClient to call OpenAI API
- [X] T059 [US3] Create `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/service/AnalysisOrchestrationService.java` with `triggerAnalysis(incidentId)`: sets status → ANALYZING, calls Gpt4AnalysisService, saves AnalysisEntity, stores embedding in ChromaDB, sets status → ANALYZED
- [X] T060 [US3] Add POST /incidents/{id}/analysis and GET /incidents/{id}/analysis endpoints to `incident-analyzer/src/main/java/com/eventflow/incidentanalyzer/controller/AnalysisController.java`

---

## Phase 6: User Story 4 — Similar Incident Detection [US4]

> Goal: ChromaDB vector similarity search returns relevant past incidents ranked by cosine similarity.
> Independent Test: Create 10 incidents with embeddings → query similar → returns matches with similarity > 70%.

### US4 — Tests (write first)

- [X] T061 [US4] Create `incident-query/src/test/java/com/eventflow/incidentquery/unit/VectorServiceTest.java` (Mockito: mock WebClient, verify ChromaDB API calls)
- [X] T062 [US4] Create `incident-query/src/test/java/com/eventflow/incidentquery/integration/SimilarIncidentIntegrationTest.java` (Testcontainers: real PostgreSQL + ChromaDB, verify similarity search)

### US4 — Entities & Repository

- [X] T063 [P] [US4] Create `incident-query/src/main/java/com/eventflow/incidentquery/entity/SimilarIncidentEntity.java` (extends BaseEntity, @ManyToOne to IncidentEntity)
- [X] T064 [P] [US4] Create `incident-query/src/main/java/com/eventflow/incidentquery/repository/SimilarIncidentRepository.java` (findByIncidentIdOrderBySimilarityScoreDesc)

### US4 — DTOs & Service & Controller

- [X] T065 [P] [US4] Create `incident-query/src/main/java/com/eventflow/incidentquery/dto/response/SimilarIncidentResponse.java` (Java record: incidentId, title, severity, status, similarityScore, matchedOn, rootCauseSummary)
- [X] T066 [US4] Create `incident-query/src/main/java/com/eventflow/incidentquery/service/VectorService.java` with: `storeEmbedding(incidentId, vector, metadata)`, `searchSimilar(queryVector, limit, minSimilarity)` — uses Spring WebClient to call ChromaDB REST API
- [X] T067 [US4] Create `incident-query/src/main/java/com/eventflow/incidentquery/controller/SimilarController.java` with GET /incidents/{id}/similar returning `ApiResponse<List<SimilarIncidentResponse>>`

---

## Phase 7: User Story 5 — Centralized Log Query [US5]

> Goal: REST API for querying structured logs with filters (correlation_id, service, time range, level).
> Independent Test: Ingest logs from 3 services → query by correlation_id → only matching logs returned. Query by ERROR level → only ERROR/CRITICAL logs.

### US5 — Tests (write first)

- [X] T068 [US5] Create `incident-query/src/test/java/com/eventflow/incidentquery/unit/LogQueryServiceTest.java` (Mockito unit tests)
- [X] T069 [US5] Create `incident-query/src/test/java/com/eventflow/incidentquery/integration/LogQueryIntegrationTest.java` (Testcontainers: real PostgreSQL, verify log filtering)

### US5 — Entity, Repository & Service

- [X] T070 [P] [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/entity/LogEntryEntity.java` (extends BaseEntity, all fields per data-model.md)
- [X] T071 [P] [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/repository/LogEntryRepository.java` (Spring Data JPA with custom queries for filters)
- [X] T072 [P] [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/dto/response/LogEntryResponse.java` (Java record)
- [X] T073 [P] [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/dto/response/LogStatsResponse.java` (Java record: startTime, endTime, services list with error counts)
- [X] T074 [P] [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/dto/request/LogIngestRequest.java` (Java record with @Valid)
- [X] T075 [P] [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/domain/LogQueryParams.java` (value object for query parameters)
- [X] T076 [P] [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/mapper/LogMapper.java` (MapStruct)
- [X] T077 [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/service/LogQueryService.java` with: `queryLogs(params)`, `getErrorStats(startTime, endTime)`, `ingestLog(request)`
- [X] T078 [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/controller/LogController.java` with GET /logs, GET /logs/errors/stats, POST /logs

### US5 — Additional Controllers for incident-query

- [X] T079 [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/entity/IncidentEntity.java` (read-only reference)
- [X] T080 [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/entity/EventEntity.java` (read-only reference)
- [X] T081 [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/repository/IncidentRepository.java`
- [X] T082 [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/service/IncidentQueryService.java` with: `listIncidents(filters)`, `getIncident(id)`, `updateIncident(id, update)`
- [X] T083 [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/controller/IncidentController.java` with GET /incidents, GET /incidents/{id}, PATCH /incidents/{id}
- [X] T084 [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/controller/HealthController.java` with GET /health (checks Kafka, PostgreSQL, ChromaDB connectivity)
- [X] T085 [US5] Create `incident-query/src/main/java/com/eventflow/incidentquery/IncidentQueryApplication.java`

---

## Phase 8: User Story 6 — Web Dashboard [US6]

> Goal: React dashboard at port 8091 showing incident list, timeline, analysis, similar incidents with one-click analysis trigger.
> Independent Test: Start dashboard → navigate to incident → all 4 tabs render correctly → "Analyze" button triggers analysis.

### US6 — Frontend Setup

- [X] T086 [P] [US6] Create `dashboard/src/types/index.ts` with TypeScript interfaces: Incident, Event, Timeline, Analysis, SimilarIncident, LogEntry, ApiResponse
- [X] T087 [P] [US6] Create `dashboard/src/services/api.ts` with Axios client: listIncidents, getIncident, getTimeline, triggerAnalysis, getAnalysis, getSimilarIncidents, queryLogs
- [X] T088 [P] [US6] Create `dashboard/src/hooks/useIncidents.ts` with TanStack Query hooks: useIncidents, useIncident, useTimeline, useAnalysis, useSimilarIncidents

### US6 — Components & Pages

- [X] T089 [P] [US6] Create `dashboard/src/components/IncidentList.tsx` (table with status badges, severity colors, clickable rows)
- [X] T090 [P] [US6] Create `dashboard/src/components/Timeline.tsx` (chronological event list with timestamps, service names, event types)
- [X] T091 [P] [US6] Create `dashboard/src/components/Analysis.tsx` (root cause, impact, factors, actions, prevention measures, confidence score gauge, "Analyze" button)
- [X] T092 [P] [US6] Create `dashboard/src/components/SimilarIncidents.tsx` (similarity scores, links to detail pages)
- [X] T093 [P] [US6] Create `dashboard/src/components/LogViewer.tsx` (filterable log table with level badges)
- [X] T094 [US6] Create `dashboard/src/components/IncidentDetail.tsx` (tabbed view: Overview, Timeline, Analysis, Similar, Logs)
- [X] T095 [US6] Create `dashboard/src/pages/IncidentListPage.tsx` (route: /)
- [X] T096 [US6] Create `dashboard/src/pages/IncidentDetailPage.tsx` (route: /incidents/:id)
- [X] T097 [US6] Create `dashboard/src/App.tsx` with React Router, TanStack QueryProvider, Tailwind setup
- [X] T098 [US6] Create `dashboard/src/main.tsx` entry point

### US6 — Build & Config

- [X] T099 [P] [US6] Create `dashboard/vite.config.ts` with API proxy to localhost:8091
- [X] T100 [P] [US6] Create `dashboard/tsconfig.json`
- [X] T101 [P] [US6] Create `dashboard/tailwind.config.js`

---

## Phase 9: Polish & Cross-Cutting Concerns

> Goal: Docker Compose orchestration, health checks, structured logging verification, final test run.
> Independent Test: Full stack starts with `docker compose up -d`, all health checks pass, structured JSON logs with correlation IDs.

- [X] T102 Update `docker/compose.yml` to include all 3 services + dashboard with health checks, depends_on, environment variables
- [X] T106 Verify structured JSON logging with correlation ID propagation across all 3 services (manual verification per quickstart.md Scenario G)
- [X] T107 Verify all Flyway migrations run cleanly on fresh PostgreSQL (`docker compose down -v && docker compose up -d`)
- [X] T108 Run full test suite: `./mvnw clean verify` — all unit, integration, and contract tests pass

---

## Dependencies

```
Phase 1 (Setup)
  └── Phase 2 (Foundational)
        ├── Phase 3 (US1: Detection)
        │     ├── Phase 4 (US2: Timeline) — reads incidents/events created by US1
        │     ├── Phase 6 (US4: Similar) — reads incidents for vector search
        │     └── Phase 7 (US5: Logs) — independent but shares incident-query module
        ├── Phase 5 (US3: Analysis) — needs incidents from US1 + timeline from US2
        └── Phase 8 (US6: Dashboard) — needs all APIs from US1-US5
              └── Phase 9 (Polish) — integration verification
```

## Parallel Execution Opportunities

**Within Phase 2**: T014–T017 (migrations), T018–T020 (enums), T022–T024 (config files) can all run in parallel.

**Within Phase 3**: T026–T032 (entities, repos, DTOs, mappers) can all run in parallel before T034 (service).

**Within Phase 4**: T039–T045 (entities, repos, DTOs, mappers) can all run in parallel before T046 (service).

**Across Phases 4, 5, 6, 7**: Once Phase 3 (US1) is complete, Phases 4 (US2) and 7 (US5) can start in parallel. Phase 5 (US3) needs Phase 4 timeline data. Phase 6 (US4) can start after Phase 3.

**Within Phase 8**: T086–T093 (types, API client, hooks, components) can all run in parallel.

## Implementation Strategy

**MVP Scope**: User Story 1 (Automatic Incident Detection) + User Story 2 (Timeline Reconstruction) + User Story 5 (Centralized Log Query)

**Rationale**: US1 is the foundation (creates incidents). US2 gives immediate value (timeline). US5 provides log query without AI dependency. This delivers a working incident detection and investigation platform.

**Incremental Delivery**:
1. **Sprint 1**: Phase 1 (Setup) + Phase 2 (Foundational) + Phase 3 (US1) = Incident detection works end-to-end
2. **Sprint 2**: Phase 4 (US2) + Phase 7 (US5) = Timeline + Log query working
3. **Sprint 3**: Phase 5 (US3) + Phase 6 (US4) = AI analysis + Similar incidents
4. **Sprint 4**: Phase 8 (US6) + Phase 9 (Polish) = Dashboard + Full integration
