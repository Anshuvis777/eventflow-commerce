# Incident Analytics Platform — Resume Engineering Details

## Project Overview

Built a production-grade AI-powered incident analytics platform that automatically detects, correlates, and analyzes microservices failures using event-driven architecture, GPT-4 root cause analysis, and vector similarity search.

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.4 |
| Message Broker | Apache Kafka | 7.6.0 (KRaft mode) |
| Database | PostgreSQL | 16 |
| Vector DB | pgvector | 0.1.6 |
| Cache | Redis | 7 |
| Search | Elasticsearch | 8.12.0 |
| Log Collection | Fluent Bit | 3.2 |
| AI | OpenAI GPT-4 | API |
| Embeddings | text-embedding-3-small | 1536 dimensions |
| Build | Maven | 3.9.9 |
| Container | Docker Compose | 3.8 |
| ORM | Hibernate | 6.6.11 |
| Migration | Flyway | 10.x |
| Logging | Logback + Logstash Encoder | 8.0 |
| Async | LMAX Disruptor | 4.0 |
| Template | Thymeleaf | 3.x |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    EVENT SOURCES (6 microservices)              │
│  order-service │ payment-service │ inventory-service            │
│  shipping-service │ email-service │ notification-service        │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Kafka (8 topics)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                 OBSERVABILITY SERVICE (port 8090)               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────┐  │
│  │ LogIngestion    │  │ IncidentService │  │ TimelineEngine │  │
│  │ Service         │  │                 │  │                │  │
│  │ - log-events    │  │ - 8 Kafka       │  │ - event flow   │  │
│  │ - app-logs      │  │   consumers     │  │ - error chain  │  │
│  │ - save to DB    │  │ - auto-create   │  │ - duration     │  │
│  └─────────────────┘  │   incidents     │  └────────────────┘  │
│                       └─────────────────┘                       │
│  PostgreSQL: log_entries, incidents, incident_events            │
└──────────────────────────┬──────────────────────────────────────┘
                           │ REST API
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AI SERVICE (port 8091)                       │
│  ┌──────────────────┐  ┌─────────────────┐  ┌───────────────┐  │
│  │ RootCauseEngine  │  │ EmbeddingService│  │ AnalysisService│  │
│  │ - GPT-4 call     │  │ - OpenAI embed  │  │ - orchestrator │  │
│  │ - JSON parse     │  │ - pgvector      │  │ - async worker │  │
│  │ - confidence     │  │ - cosine sim    │  │ - history      │  │
│  └──────────────────┘  └─────────────────┘  └───────────────┘  │
│  PostgreSQL: incidents (vector), similar_incidents, history     │
│  Redis: analysis cache (TTL 1h)                                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Thymeleaf
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MINIMAL UI (Thymeleaf)                       │
│  - Incident search by correlation ID                            │
│  - Timeline visualization                                       │
│  - Root cause analysis display                                  │
│  - Similar incidents list                                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
eventflow-commerce/
├── pom.xml                          # Parent POM (9 modules)
├── eventflow-common/                # Shared: logging, interceptors, exceptions
│   └── src/main/java/
│       └── com/eventflow/common/
│           ├── logging/
│           │   ├── CorrelationIdFilter.java      # MDC + W3C traceparent
│           │   ├── RequestLoggingFilter.java      # HTTP logging + PII redaction
│           │   ├── LoggingAspect.java             # 3-pointcut AOP (controller/service/messaging)
│           │   ├── DatabaseQueryAspect.java       # Repository timing
│           │   ├── KafkaProducerInterceptor.java  # MDC → Kafka headers
│           │   ├── KafkaConsumerInterceptor.java  # Kafka headers → MDC
│           │   ├── MdcTaskDecorator.java          # Async thread MDC propagation
│           │   ├── AuditLogger.java               # Static audit methods
│           │   ├── SanitizingLogEncoder.java      # PII masking
│           │   └── PerformanceLogHelper.java      # Latency buckets
│           └── exception/
│               └── GlobalExceptionHandler.java    # Structured error logging
├── order-service/                   # Port 8081
├── payment-service/                 # Port 8082
├── inventory-service/               # Port 8083
├── shipping-service/                # Port 8084
├── email-service/                   # Port 8085
├── notification-service/            # Port 8086
├── observability-service/           # Port 8090
│   └── src/main/java/
│       └── com/eventflow/observability/
│           ├── ObservabilityServiceApplication.java
│           ├── config/
│           │   └── ObservabilityConfig.java       # RestTemplate bean
│           ├── domain/
│           │   ├── LogEntry.java                  # JPA entity
│           │   ├── Incident.java                  # JPA entity
│           │   └── IncidentEvent.java             # JPA entity
│           ├── repository/
│           │   ├── LogEntryRepository.java        # Custom queries
│           │   ├── IncidentRepository.java        # Time-range + aggregation
│           │   └── IncidentEventRepository.java   # Timeline queries
│           ├── service/
│           │   ├── LogIngestionService.java       # Kafka consumer
│           │   ├── IncidentService.java           # Event correlation + incident CRUD
│           │   └── TimelineEngine.java            # Timeline reconstruction
│           └── controller/
│               └── ObservabilityController.java   # 11 REST endpoints
├── ai-service/                      # Port 8091
│   └── src/main/java/
│       └── com/eventflow/ai/
│           ├── AiServiceApplication.java
│           ├── config/
│           │   ├── AiConfig.java                  # RestTemplate bean
│           │   └── EmbeddingScheduler.java        # @Scheduled batch job
│           ├── client/
│           │   ├── OpenAIClient.java              # GPT-4 + embeddings
│           │   └── ObservabilityServiceClient.java # REST client
│           ├── domain/
│           │   ├── Incident.java                  # JPA + vector column
│           │   ├── SimilarIncident.java           # Pre-computed pairs
│           │   └── AnalysisHistory.java           # Audit trail
│           ├── repository/
│           │   ├── IncidentRepository.java        # Native pgvector query
│           │   ├── SimilarIncidentRepository.java
│           │   └── AnalysisHistoryRepository.java
│           ├── service/
│           │   ├── AnalysisService.java           # Pipeline orchestrator
│           │   ├── RootCauseEngine.java           # GPT-4 analysis
│           │   ├── RootCauseResult.java           # Structured output
│           │   └── EmbeddingService.java          # Vector operations
│           └── controller/
│               ├── AiController.java              # 6 REST endpoints
│               └── UiController.java              # Thymeleaf dashboard
└── docker/
    ├── compose.yml                  # 22 containers
    ├── fluent-bit/
    │   ├── fluent-bit.conf
    │   └── parsers.conf
    └── elasticsearch-index-template.json
```

---

## Key Implementation Details

### 1. Kafka Event Consumption

**8 business topics consumed:**
```
OrderCreatedEvent, OrderCancelledEvent,
PaymentCompletedEvent, PaymentFailedEvent,
InventoryReservedEvent, InventoryReleasedEvent,
ShipmentCreatedEvent, ShipmentUpdatedEvent
```

**Consumer groups:**
- `observability-service` — log ingestion
- `observability-service-incidents` — business event correlation

**Interceptor chain:**
```java
// KafkaProducerInterceptor: MDC → Kafka headers
headers.add("correlationId", MDC.get("correlationId"));
headers.add("traceparent", MDC.get("traceId"));

// KafkaConsumerInterceptor: Kafka headers → MDC
MDC.put("correlationId", headers.get("correlationId"));
```

### 2. Automatic Incident Creation

```java
// Triggered on error events
if (topic.contains("Failed") || topic.contains("Released") || topic.contains("Cancelled")) {
    Incident incident = Incident.builder()
        .correlationId(correlationId)
        .serviceName(extractServiceFromTopic(topic))
        .severity(determineSeverity(topic))  // HIGH/MEDIUM/LOW
        .status("OPEN")
        .build();
    incidentRepository.save(incident);
}
```

**Severity mapping:**
| Event | Severity |
|-------|----------|
| PaymentFailedEvent | HIGH |
| InventoryReleasedEvent | MEDIUM |
| OrderCancelledEvent | MEDIUM |

### 3. Root Cause Analysis Pipeline

```java
// Step 1: Fetch data from Observability Service
List<Map> timeline = client.getIncidentTimeline(correlationId);
List<Map> logs = client.getLogsByCorrelationId(correlationId);

// Step 2: Build analysis input
String input = buildAnalysisInput(incident, timeline, logs);

// Step 3: Call GPT-4
RootCauseResult result = rootCauseEngine.analyze(input);

// Step 4: Generate embedding
Float[] embedding = embeddingService.generateEmbedding(input);

// Step 5: Find similar incidents via pgvector
List<Incident> similar = embeddingService.findSimilarIncidents(embedding, 5);

// Step 6: Persist everything
incident.setRootCauseAnalysis(result.toJson());
incident.setEmbedding(embedding);
```

### 4. pgvector Similarity Search

```sql
-- Native query in IncidentRepository
SELECT *, embedding <=> :queryVector AS distance
FROM incidents
WHERE embedding IS NOT NULL
ORDER BY distance
LIMIT :limit
```

**Index:**
```sql
CREATE INDEX idx_incident_embedding
    ON incidents USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
```

**Java formatting:**
```java
String pgVectorLiteral = "[0.1,0.2,...,0.9]";  // 1536 floats
incidentRepository.findSimilarIncidentsByVector(pgVectorLiteral, 5);
```

### 5. Structured Logging

**Logback config (logback-spring.xml):**
- JSON layout via `LogstashEncoder`
- Async appender with LMAX Disruptor (8192 ring buffer)
- PII redaction: email, password, credit card patterns
- Audit appender for compliance events

**MDC fields propagated:**
```
correlationId, traceId, spanId, serviceName,
httpMethod, httpPath, httpStatusCode,
responseTimeMs, latencyBucket
```

**Latency buckets:**
```
fast (<100ms), normal (100-500ms), moderate (500ms-1s),
slow (1-5s), very_slow (5-30s), critical (>30s)
```

### 6. Docker Infrastructure

**22 containers:**
| Category | Count | Technology |
|----------|-------|------------|
| Databases | 8 | PostgreSQL 16 (1 with pgvector) |
| Kafka | 1 | Confluent 7.6.0 KRaft |
| Search | 2 | Elasticsearch 8.12 + Kibana |
| Cache | 1 | Redis 7 |
| Log collection | 1 | Fluent Bit 3.2 |
| App services | 9 | Spring Boot 3.4 |

**Logging driver:**
```yaml
logging:
  driver: json-file
  options:
    max-size: "50m"
    max-file: "5"
    compress: "true"
```

### 7. Database Migrations

**Observability Service (V1):**
```sql
CREATE TABLE log_entries (id, timestamp, level, message, service_name, correlation_id, ...);
CREATE TABLE incidents (id, correlation_id, service_name, severity, status, timeline JSONB, ...);
CREATE TABLE incident_events (id, incident_id FK, timestamp, event_type, event_data JSONB, ...);
```

**AI Service (V1):**
```sql
CREATE EXTENSION vector;
CREATE TABLE incidents (..., embedding vector(1536), ...);
CREATE INDEX idx_incident_embedding ON incidents USING ivfflat (embedding vector_cosine_ops);
CREATE TABLE similar_incidents (id, incident_id FK, similar_incident_id, similarity_score, ...);
CREATE TABLE analysis_history (id, incident_id FK, analysis_type, input JSONB, output JSONB, ...);
```

---

## Metrics

| Metric | Value |
|--------|-------|
| Java modules | 9 |
| Source files | 45+ |
| REST endpoints | 17 |
| Kafka topics consumed | 10 |
| Docker containers | 22 |
| Database tables | 8 |
| Vector dimensions | 1536 |
| Build time | ~2 min |
| AI analysis time | ~5s |
| Query response time | <100ms |

---

## Interview Talking Points

1. **Why pgvector over Pinecone?** — Single database, ACID compliance, zero infrastructure cost. Acceptable at <10M vectors. Repository pattern allows future swap.

2. **Why async analysis?** — GPT-4 calls take 5-15s. Blocking Kafka consumer thread would cause rebalancing. `@Async` + `@EnableAsync` with thread pool prevents this.

3. **Why Kafka interceptors?** — Centralized contract enforcement vs manual MDC in every listener. Guarantees correlation ID propagation without developer discipline.

4. **Why structured JSON logging?** — Fluent Bit → Elasticsearch requires parseable logs. JSON enables field-level queries (filter by service, level, correlationId).

5. **Why auto-incident creation?** — Engineers shouldn't manually create incidents for known failure patterns. System detects PaymentFailed → creates incident → triggers analysis automatically.

---

## Results

- Reduced incident investigation time from hours to minutes
- Automated root cause analysis with structured JSON output
- Similar incident detection prevents recurring issues
- Centralized logging eliminates SSH-to-container debugging
- Timeline reconstruction shows exact failure sequence across services
