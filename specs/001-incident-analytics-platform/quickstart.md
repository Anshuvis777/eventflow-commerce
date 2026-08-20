# Quickstart Validation Guide: Incident Analytics Platform

**Feature**: 001-incident-analytics-platform
**Date**: 2026-08-15
**Stack**: Java 21 / Spring Boot 3.4.4 / Docker

## Prerequisites

- Docker & Docker Compose running
- JDK 21+ installed (Temurin recommended)
- Maven Wrapper included (no separate Maven install needed)
- Node.js 18+ installed (for dashboard)
- OpenAI API key set in environment: `OPENAI_API_KEY`

## 1. Build All Services

```bash
# Build parent POM and all service JARs (uses Maven Wrapper)
./mvnw clean package -DskipTests

# Build Docker images
docker compose -f docker/compose.yml build
```

## 2. Start Full Stack (Infrastructure + Services + Dashboard)

```bash
# Start everything: Kafka, PostgreSQL, ChromaDB, 3 Spring Boot services, dashboard
docker compose -f docker/compose.yml up -d

# Verify all containers are running
docker compose -f docker/compose.yml ps

# Expected containers:
# - eventflow-kafka (port 9092)
# - eventflow-postgresql (port 5432) — single shared DB for all 3 services
# - eventflow-chromadb (port 8000)
# - incident-detector (port 8081)
# - incident-analyzer (port 8082)
# - incident-query (port 8091)
# - dashboard (port 80/443)
```

### Verify Container Health

```bash
# PostgreSQL ready
docker compose exec postgresql pg_isready -U postgres

# Kafka ready
docker compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# ChromaDB ready
curl http://localhost:8000/api/v1/heartbeat

# incident-detector ready (Spring Boot Actuator)
curl http://localhost:8081/actuator/health

# incident-analyzer ready
curl http://localhost:8082/actuator/health

# incident-query ready
curl http://localhost:8091/actuator/health
```

### Database Migrations (Flyway)

Flyway runs automatically on Spring Boot startup (each service runs its own migration version):
- V1–V2: incident-detector (incidents, events tables)
- V3: incident-analyzer (analyses table)
- V4–V5: incident-query (log_entries, similar_incidents tables)

Verify:
```bash
docker compose exec postgresql psql -U postgres -d incident_analytics -c \
  "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

## 3. Validation Scenarios

### Scenario A: Automatic Incident Detection

**Goal**: Verify incidents are auto-created when failure events are published to Kafka.

```bash
# Send a PaymentFailed event to Kafka via CLI
docker compose exec incident-detector java -jar /app/incident-detector.jar \
  detect --event '{
    "event_id": "test-001",
    "event_type": "PaymentFailed",
    "correlation_id": "corr-test-001",
    "service_name": "payment-service",
    "timestamp": "2026-08-15T10:00:00Z",
    "severity": "HIGH",
    "payload": {
      "order_id": "ord-001",
      "payment_id": "pay-001",
      "failure_reason": "INSUFFICIENT_FUNDS",
      "amount": 99.99,
      "currency": "USD"
    }
  }'
```

**Verify via API**:
```bash
curl http://localhost:8091/api/v1/incidents | jq '.items[] | select(.correlationId == "corr-test-001")'

# Expected output:
# {
#   "id": "uuid",
#   "correlationId": "corr-test-001",
#   "status": "OPEN",
#   "severity": "HIGH",
#   "title": "PaymentFailed detected"
# }
```

**Verify in database**:
```bash
docker compose exec postgresql psql -U postgres -d incident_analytics -c \
  "SELECT id, correlation_id, status, severity FROM incidents WHERE correlation_id = 'corr-test-001';"
```

### Scenario B: Timeline Reconstruction

**Goal**: Verify events are grouped by correlation_id into a timeline.

```bash
# Send additional events for same correlation
docker compose exec incident-detector java -jar /app/incident-detector.jar \
  detect --event '{
    "event_id": "test-002",
    "event_type": "OrderPlaced",
    "correlation_id": "corr-test-001",
    "service_name": "order-service",
    "timestamp": "2026-08-15T09:58:00Z",
    "severity": "LOW",
    "payload": {
      "order_id": "ord-001",
      "customer_id": "cust-001",
      "items": [{"product_id": "p1", "quantity": 1, "price": 99.99}],
      "total_amount": 99.99
    }
  }'

# Query timeline via incident-query API
curl http://localhost:8091/api/v1/incidents/corr-test-001/timeline | jq .

# Expected: 2 events in chronological order
# {
#   "incidentId": "uuid",
#   "events": [
#     {"eventType": "OrderPlaced", "timestamp": "2026-08-15T09:58:00Z", "serviceName": "order-service"},
#     {"eventType": "PaymentFailed", "timestamp": "2026-08-15T10:00:00Z", "serviceName": "payment-service"}
#   ],
#   "totalDurationSeconds": 120,
#   "affectedServices": ["order-service", "payment-service"]
# }
```

### Scenario C: AI Root Cause Analysis

**Goal**: Verify GPT-4 analysis returns structured output with confidence score.

```bash
# Trigger analysis
curl -X POST http://localhost:8091/api/v1/incidents/{INCIDENT_ID}/analysis

# Wait for async processing, then retrieve
curl http://localhost:8091/api/v1/incidents/{INCIDENT_ID}/analysis | jq .

# Expected output contains all required fields:
# {
#   "rootCause": "Payment was declined due to insufficient funds...",
#   "impact": "Customer order could not be fulfilled...",
#   "contributingFactors": ["Customer account balance too low"],
#   "recommendedActions": ["Notify customer of payment failure"],
#   "preventionMeasures": ["Implement pre-authorization check"],
#   "confidenceScore": 85,
#   "modelVersion": "gpt-4-turbo-preview"
# }
```

### Scenario D: Similar Incident Search

**Goal**: Verify ChromaDB vector similarity search returns relevant past incidents.

```bash
curl "http://localhost:8091/api/v1/incidents/{INCIDENT_ID}/similar?limit=5&minSimilarity=0.7" | jq .

# Expected: Ranked list with similarity scores
# {
#   "incidentId": "uuid",
#   "similarIncidents": [
#     {
#       "incidentId": "uuid",
#       "similarityScore": 0.87,
#       "rootCauseSummary": "Previous payment failure...",
#       "status": "RESOLVED"
#     }
#   ],
#   "queryTimeMs": 42
# }
```

### Scenario E: Centralized Log Query

```bash
# Query logs by correlation_id
curl "http://localhost:8091/api/v1/logs?correlationId=corr-test-001&limit=10" | jq .

# Query logs by service and level
curl "http://localhost:8091/api/v1/logs?serviceName=payment-service&level=ERROR&limit=5" | jq .

# Query error stats
curl "http://localhost:8091/api/v1/logs/errors/stats?startTime=2026-08-15T00:00:00Z&endTime=2026-08-15T23:59:59Z" | jq .
```

### Scenario F: Web Dashboard

**Goal**: Verify dashboard loads and displays incident data.

```
Open http://localhost:8091 in browser (or http://localhost if using Nginx proxy)
```

**Manual verification**:
1. ✅ Dashboard loads without errors
2. ✅ Incident list page shows incidents
3. ✅ Clicking an incident opens detail page
4. ✅ Timeline tab shows ordered events
5. ✅ Analysis tab shows AI analysis (or "pending" state)
6. ✅ Similar Incidents tab shows matches
7. ✅ "Analyze" button triggers analysis and results appear

### Scenario G: View Service Logs

**Goal**: Verify structured JSON logs with correlation IDs.

```bash
# View incident-detector logs
docker compose logs -f incident-detector

# Filter by correlation ID
docker compose logs incident-query | grep "corr-test-001"

# Filter by log level
docker compose logs incident-query | grep '"level":"ERROR"'
```

**Expected**: All log lines are structured JSON with correlation_id, service_name, and execution time.

### Scenario H: Health Check

```bash
curl http://localhost:8091/api/v1/health | jq .

# Expected:
# {
#   "status": "healthy",
#   "checks": {
#     "kafka": "connected",
#     "postgresql": "connected",
#     "chromadb": "connected",
#     "openai": "connected"
#   }
# }
```

## 4. Run Tests

```bash
# Unit tests (no infrastructure needed)
./mvnw test -pl incident-detector
./mvnw test -pl incident-analyzer
./mvnw test -pl incident-query

# Integration tests (Testcontainers auto-starts Docker)
./mvnw verify -pl incident-detector -Dgroups="integration"
./mvnw verify -pl incident-analyzer -Dgroups="integration"
./mvnw verify -pl incident-query -Dgroups="integration"

# Contract tests (validates OpenAPI spec compliance)
./mvnw verify -pl incident-query -Dgroups="contract"

# Dashboard tests
cd dashboard && npm test && cd ..
```

## 5. View Service Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f incident-detector
docker compose logs -f incident-analyzer
docker compose logs -f incident-query

# Filter by log level
docker compose logs incident-query | grep '"level":"ERROR"'
```

## 6. Teardown

```bash
docker compose -f docker/compose.yml down          # Stop containers
docker compose -f docker/compose.yml down -v       # Stop and remove volumes
docker compose -f docker/compose.yml down -v --rmi all  # Stop, remove volumes and images
```

## Success Criteria Checklist

| Scenario | Criteria | Pass/Fail |
|----------|----------|-----------|
| A - Detection | Incident auto-created from PaymentFailed event | |
| B - Timeline | Events ordered chronologically with duration | |
| C - Analysis | GPT-4 returns all 6 structured fields | |
| D - Similarity | ChromaDB cosine similarity scores returned | |
| E - Log Query | Filters by correlation_id, service, level work | |
| F - Dashboard | All 4 views render correctly | |
| G - Health | All systems report "connected" via Actuator | |
| H - Tests | JUnit 5 + Testcontainers tests pass | |
| I - Docker | Each service in own container, shared PostgreSQL | |
| J - Common | Reuses eventflow-common (BaseEntity, ApiResponse, etc.) | |

## Troubleshooting

| Issue | Resolution |
|-------|------------|
| Kafka connection refused | Wait 30s after `docker compose up`; check `docker compose logs kafka` |
| PostgreSQL connection refused | Check port 5432; verify `docker compose ps`; check Flyway migration logs |
| ChromaDB connection refused | Check port 8000; verify `docker compose logs chromadb` |
| OpenAI 429 rate limit | Add retry delay in application.yml; use mock for contract tests |
| Dashboard 404 on API | Ensure incident-query container is healthy on port 8091 |
| Maven build fails | Run `./mvnw clean install -DskipTests` first to resolve dependencies |
| Docker image build slow | Multi-stage Dockerfile with Maven cache layers; use `--mount=type=cache` |
| Flyway migration error | Check `flyway_schema_history` table; ensure DB is empty or consistent |
| BaseEntity not found | Ensure `eventflow-common` is built first: `./mvnw install -pl eventflow-common` |
