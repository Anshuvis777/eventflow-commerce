# Changelog

All notable changes to the EventFlow Commerce platform.

---

## [Unreleased] - Observability & AI Services

### Added

#### Observability Service (port 8090)
New centralized observability service for log ingestion, incident management, and metrics.

**Module**: `observability-service/`

| File | Purpose |
|------|---------|
| `ObservabilityServiceApplication.java` | Spring Boot main class with `@EnableKafka`, `@EnableScheduling` |
| `config/ObservabilityConfig.java` | Spring configuration (RestTemplate bean) |
| `domain/LogEntry.java` | JPA entity for structured log storage |
| `domain/Incident.java` | JPA entity for incident tracking |
| `domain/IncidentEvent.java` | JPA entity for event-level audit trail |
| `repository/LogEntryRepository.java` | Spring Data JPA repository with custom queries |
| `repository/IncidentRepository.java` | Spring Data JPA repository with time-range and aggregation queries |
| `repository/IncidentEventRepository.java` | Spring Data JPA repository for timeline queries |
| `service/LogIngestionService.java` | Kafka consumer for log-events/application-logs topics |
| `service/IncidentService.java` | Kafka consumer for all 8 business event topics; incident creation/correlation |
| `controller/ObservabilityController.java` | REST API for logs, incidents, timeline, statistics |
| `resources/application.yml` | Local development config (port 8090, localhost:5432, localhost:9200) |
| `resources/application-docker.yml` | Docker profile config (Docker hostnames) |
| `resources/db/migration/V1__create_observability_tables.sql` | Flyway migration: log_entries, incidents, incident_events tables |
| `Dockerfile` | eclipse-temurin:21-jre-alpine |

**API Endpoints**:
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/observability/logs/correlation/{id}` | Logs by correlation ID |
| GET | `/api/v1/observability/logs/service/{name}` | Logs by service + time range |
| GET | `/api/v1/observability/logs/errors/{name}` | Error logs by service |
| GET | `/api/v1/observability/logs/errors/stats` | Error counts by service |
| POST | `/api/v1/observability/incidents` | Create incident |
| GET | `/api/v1/observability/incidents/correlation/{id}` | Get incident |
| PUT | `/api/v1/observability/incidents/{id}/status` | Update status |
| GET | `/api/v1/observability/incidents/timeline/{id}` | Event timeline |
| GET | `/api/v1/observability/incidents/stats/severity` | Stats by severity |
| GET | `/api/v1/observability/incidents/stats/service` | Stats by service |
| GET | `/api/v1/observability/health` | Health check |

**Kafka Topics Consumed**:
- `log-events`, `application-logs` (group: `observability-service`)
- `OrderCreatedEvent`, `OrderCancelledEvent`, `PaymentCompletedEvent`, `PaymentFailedEvent`, `InventoryReservedEvent`, `InventoryReleasedEvent`, `ShipmentCreatedEvent`, `ShipmentUpdatedEvent` (group: `observability-service-incidents`)

---

#### AI Service (port 8091)
AI-powered incident analysis service with GPT-4 root cause analysis and pgvector similarity search.

**Module**: `ai-service/`

| File | Purpose |
|------|---------|
| `AiServiceApplication.java` | Spring Boot main class with `@EnableCaching`, `@EnableScheduling` |
| `config/AiConfig.java` | Spring configuration (RestTemplate bean) |
| `config/EmbeddingScheduler.java` | Scheduled task for batch embedding generation (every 60s) |
| `client/OpenAIClient.java` | OpenAI GPT-4 chat completion + text-embedding-3-small |
| `client/ObservabilityServiceClient.java` | REST client for Observability Service (timeline, logs, incidents) |
| `domain/Incident.java` | JPA entity with `vector(1536)` embedding column |
| `domain/SimilarIncident.java` | JPA entity for pre-computed similar incident pairs |
| `domain/AnalysisHistory.java` | JPA entity for audit trail of all analyses |
| `repository/IncidentRepository.java` | JPA repository with pgvector embedding queries |
| `repository/SimilarIncidentRepository.java` | JPA repository for similarity lookups |
| `repository/AnalysisHistoryRepository.java` | JPA repository for analysis audit trail |
| `service/AnalysisService.java` | Core analysis pipeline: fetch data -> GPT-4 -> store -> find similar |
| `service/EmbeddingService.java` | Embedding generation + cosine similarity search |
| `controller/AiController.java` | REST API for incident registration, analysis trigger, results |
| `resources/application.yml` | Local config (port 8091, Redis, OpenAI settings) |
| `resources/application-docker.yml` | Docker profile config |
| `resources/db/migration/V1__create_ai_tables.sql` | Flyway migration: incidents (with vector), similar_incidents, analysis_history |
| `Dockerfile` | eclipse-temurin:21-jre-alpine |

**API Endpoints**:
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/ai/incidents` | Register incident for analysis |
| POST | `/api/v1/ai/incidents/{correlationId}/analyze` | Trigger analysis (async) |
| GET | `/api/v1/ai/incidents/{correlationId}` | Get analysis result |
| GET | `/api/v1/ai/incidents/{correlationId}/similar` | Get similar incidents |
| GET | `/api/v1/ai/incidents/{correlationId}/history` | Analysis history |
| GET | `/api/v1/ai/health` | Health check |

**External Integrations**:
- OpenAI GPT-4 (root cause analysis)
- OpenAI text-embedding-3-small (1536-dim vectors)
- Observability Service REST API (data source)
- Redis (caching, TTL 3600000ms)

---

#### Infrastructure Additions

| Container | Image | Port | Purpose |
|-----------|-------|------|---------|
| observability-db | postgres:16-alpine | 5438 | Observability Service database |
| ai-db | pgvector/pgvector:pg16 | 5439 | AI Service database with vector extension |
| elasticsearch | docker.elastic.co/elasticsearch/elasticsearch:8.12.0 | 9200 | Log search and analytics |
| kibana | docker.elastic.co/kibana/kibana:8.12.0 | 5601 | Log visualization dashboards |
| redis | redis:7-alpine | 6379 | AI Service caching layer |

**New Docker Volumes**: `observability-db-data`, `ai-db-data`, `es-data`, `redis-data`

---

#### OpenSpec Documentation

| File | Description |
|------|-------------|
| `openspec/specs/observability/spec.md` | Full BDD specification for Observability Service |
| `openspec/specs/ai/spec.md` | Full BDD specification for AI Service |
| `openspec/changes/observability-service-addition.md` | Change proposal for Observability Service |
| `openspec/changes/ai-service-addition.md` | Change proposal for AI Service |

**Updated files**:
- `openspec/config.yaml` - Updated context to reflect 9 modules, new infrastructure
- `openspec/specs/infrastructure/spec.md` - Updated with all 22 containers, observability stack

---

### Build Verification

```
mvn compile -Dmaven.compiler.fork=true -Dmaven.compiler.executable=$(which javac)
```

Result: **CLEAN** across all 9 modules (0 errors).

---

### Project Structure (Post-Change)

```
eventflow-commerce/
├── pom.xml                          # Parent POM (9 modules)
├── eventflow-common/                # Shared logging, interceptors, exceptions
├── order-service/                   # Port 8081
├── payment-service/                 # Port 8082
├── inventory-service/               # Port 8083
├── shipping-service/                # Port 8084
├── email-service/                   # Port 8085
├── notification-service/            # Port 8086
├── observability-service/           # Port 8090 (NEW)
├── ai-service/                      # Port 8091 (NEW)
├── docker/
│   ├── compose.yml                  # 22 containers
│   ├── fluent-bit/
│   │   ├── fluent-bit.conf
│   │   └── parsers.conf
│   └── elasticsearch-index-template.json
└── openspec/
    ├── config.yaml
    ├── specs/
    │   ├── order/spec.md
    │   ├── payment/spec.md
    │   ├── inventory/spec.md
    │   ├── shipping/spec.md
    │   ├── email/spec.md
    │   ├── notification/spec.md
    │   ├── events/spec.md
    │   ├── infrastructure/spec.md
    │   ├── observability/spec.md    # NEW
    │   └── ai/spec.md               # NEW
    └── changes/
        ├── observability-service-addition.md  # NEW
        └── ai-service-addition.md             # NEW
```

---

### Dependencies Added

**observability-service/pom.xml**:
- `spring-boot-starter-data-elasticsearch`
- `spring-kafka`
- `flyway-core` + `flyway-database-postgresql`
- `mapstruct`

**ai-service/pom.xml**:
- `spring-boot-starter-data-redis`
- `spring-boot-starter-cache`
- `flyway-core` + `flyway-database-postgresql`
- `jackson-databind`
- `mapstruct`

---

### Database Migrations

**observability-service** (`V1__create_observability_tables.sql`):
- `log_entries` - Structured log storage with indexes on correlationId, serviceName, level, timestamp, traceId
- `incidents` - Incident tracking with severity, status, timeline (JSONB), root cause analysis (JSONB)
- `incident_events` - Event-level audit trail linked to incidents

**ai-service** (`V1__create_ai_tables.sql`):
- `incidents` - Incident data with `vector(1536)` column, IVFFlat index for cosine similarity
- `similar_incidents` - Pre-computed similar incident pairs with similarity scores
- `analysis_history` - Audit trail of all AI analyses with input/output, confidence, processing time

---

### Configuration Changes

**Parent POM** (`pom.xml`):
- Added `<module>observability-service</module>`
- Added `<module>ai-service</module>`

**Docker Compose** (`docker/compose.yml`):
- Added observability-db, ai-db, elasticsearch, kibana, redis, observability-service, ai-service
- Added volumes: observability-db-data, ai-db-data, es-data, redis-data
- Updated dependency chains for new services
