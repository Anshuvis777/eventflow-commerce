# Feature Specification: Incident Analytics Platform

**Feature Branch**: `001-incident-analytics-platform`

**Created**: 2026-08-15

**Status**: Draft

**Input**: User description: "Build an AI-powered incident analytics platform that automatically detects, correlates, and analyzes production incidents across microservices. The system monitors Kafka business events (orders, payments, inventory, shipments) and auto-creates incidents when failures occur (PaymentFailed, InventoryReleased, OrderCancelled). It reconstructs timelines by grouping events by correlation ID, performs root cause analysis using GPT-4 with structured output (root cause, impact, contributing factors, recommended actions, prevention measures, confidence score), finds similar past incidents using 1536-dimensional ChromaDB vector embeddings with cosine similarity, provides centralized log query via REST API (filter by correlation ID, service, time range, log level), and includes a minimal web dashboard at port 8091 for viewing incident overview, timeline, analysis, and similar incidents with one-click analysis trigger."

## User Scenarios & Testing

### User Story 1 - Automatic Incident Detection (Priority: P1)

When a business event indicates a failure (PaymentFailed, InventoryReleased, OrderCancelled), the system automatically creates an incident record without manual intervention.

**Why this priority**: This is the foundation of the platform - without automatic detection, engineers must manually create incidents, defeating the purpose of reducing analysis time from hours to minutes.

**Independent Test**: Can be fully tested by publishing failure events to Kafka and verifying incident records are created with correct metadata (correlation ID, affected services, timestamp, severity).

**Acceptance Scenarios**:

1. **Given** Kafka is running with business event topics, **When** a PaymentFailed event is published with correlation ID "corr-123", **Then** an incident is created with status "OPEN", severity "HIGH", and correlation ID "corr-123"
2. **Given** multiple failure events for the same correlation ID, **When** events are published within a 5-minute window, **Then** they are grouped into a single incident
3. **Given** a successful event (OrderCompleted), **When** published, **Then** no incident is created

---

### User Story 2 - Timeline Reconstruction (Priority: P1)

Engineers can view a chronological timeline of all events related to an incident, grouped by correlation ID, showing the exact sequence of what happened.

**Why this priority**: Understanding the sequence of events is critical for root cause analysis. Without timeline reconstruction, engineers must manually piece together logs from multiple services.

**Independent Test**: Can be fully tested by publishing a sequence of events with the same correlation ID and verifying the timeline shows them in correct order with timestamps, service names, and event types.

**Acceptance Scenarios**:

1. **Given** an incident with correlation ID "corr-123", **When** events OrderPlaced → PaymentFailed → InventoryReleased are published, **Then** the timeline shows all three events in chronological order with service names (order-service, payment-service, inventory-service)
2. **Given** events with different correlation IDs, **When** viewing incident "corr-123", **Then** only events with correlation ID "corr-123" appear in the timeline
3. **Given** an incident, **When** viewing timeline, **Then** total duration from first to last event is calculated and displayed

---

### User Story 3 - AI Root Cause Analysis (Priority: P1)

Engineers can trigger an AI-powered root cause analysis that returns structured findings: root cause, impact assessment, contributing factors, recommended actions, prevention measures, and a confidence score.

**Why this priority**: This is the core value proposition - replacing hours of manual investigation with AI-generated analysis that engineers can act on immediately.

**Independent Test**: Can be fully tested by creating an incident with a known failure pattern, triggering analysis, and verifying the output contains all required structured fields with reasonable content.

**Acceptance Scenarios**:

1. **Given** an incident with timeline and logs, **When** analysis is triggered, **Then** response includes: root_cause (string), impact (string), contributing_factors (array), recommended_actions (array), prevention_measures (array), confidence_score (0-100)
2. **Given** a PaymentFailed incident due to insufficient funds, **When** analysis runs, **Then** root_cause identifies "payment declined due to insufficient funds" with confidence > 80%
3. **Given** an incident with insufficient context, **When** analysis runs, **Then** confidence_score reflects uncertainty (< 50%) and recommended_actions include "gather more logs"

---

### User Story 4 - Similar Incident Detection (Priority: P2)

Engineers can see similar past incidents ranked by cosine similarity of vector embeddings, helping them leverage previous solutions.

**Why this priority**: Reduces repeat investigation effort. If a similar incident was resolved before, engineers can apply the same fix.

**Independent Test**: Can be fully tested by creating multiple incidents with similar patterns, generating embeddings, and verifying similarity search returns relevant matches with similarity scores.

**Acceptance Scenarios**:

1. **Given** 10 historical incidents in the database, **When** a new incident with similar failure pattern is created, **Then** similar incidents query returns at least 1 match with similarity > 70%
2. **Given** a new incident, **When** viewing similar incidents, **Then** results show incident ID, similarity percentage, root cause summary, and resolution status
3. **Given** no similar incidents exist, **When** querying, **Then** empty result is returned (not an error)

---

### User Story 5 - Centralized Log Query (Priority: P2)

Engineers can query structured logs from all services via REST API, filtering by correlation ID, service name, time range, and log level.

**Why this priority**: Eliminates the need to SSH into multiple services or use separate logging tools. Single query interface for all logs.

**Independent Test**: Can be fully tested by ingesting logs from multiple services and verifying API returns correct filtered results.

**Acceptance Scenarios**:

1. **Given** logs from order-service, payment-service, inventory-service, **When** querying with correlation ID "corr-123", **Then** only logs with that correlation ID are returned
2. **Given** logs spanning 24 hours, **When** querying with time range "last 1 hour", **Then** only logs within that window are returned
3. **Given** logs at various levels, **When** querying with level "ERROR", **Then** only ERROR and CRITICAL logs are returned
4. **Given** logs from multiple services, **When** querying with service "payment-service", **Then** only payment-service logs are returned

---

### User Story 6 - Web Dashboard (Priority: P2)

Engineers can access a web dashboard at port 8091 to view incident overview, timeline, AI analysis, and similar incidents with one-click analysis trigger.

**Why this priority**: Provides a unified visual interface for incident investigation without requiring CLI or API knowledge.

**Independent Test**: Can be fully tested by starting the dashboard, navigating to an incident, and verifying all views render correctly with data.

**Acceptance Scenarios**:

1. **Given** dashboard is running on port 8091, **When** accessing http://localhost:8091, **Then** incident list page loads with recent incidents
2. **Given** an incident ID, **When** navigating to incident detail page, **Then** overview, timeline, analysis, and similar incidents tabs are all accessible
3. **Given** an incident without analysis, **When** clicking "Analyze" button, **Then** analysis is triggered and results appear when complete
4. **Given** similar incidents exist, **When** viewing similar incidents tab, **Then** they are displayed with similarity scores and links to details

---

### Edge Cases

- What happens when Kafka is temporarily unavailable? System should buffer events and process when restored
- How does system handle events with missing correlation ID? Events without correlation ID are logged but not associated with incidents
- What happens when GPT-4 API is unavailable? Analysis returns error with retry guidance, incident remains in "ANALYSIS_PENDING" state
- How does system handle very high event volumes? Consumer groups with horizontal scaling, backpressure handling
- What happens when ChromaDB is unavailable? System fails fast at startup with clear error message
- How are incidents cleaned up? Retention policy: incidents older than 90 days archived, not deleted
- What happens with concurrent analysis requests for same incident? Idempotency key prevents duplicate analysis

## Requirements

### Functional Requirements

- **FR-001**: System MUST automatically create incident records when failure events (PaymentFailed, InventoryReleased, OrderCancelled) are detected on Kafka topics
- **FR-002**: System MUST group all events by correlation ID into a single incident timeline
- **FR-003**: System MUST calculate incident duration from first to last event in the timeline
- **FR-004**: System MUST identify affected services from event metadata in the timeline
- **FR-005**: System MUST trigger AI root cause analysis on demand (manual or automatic)
- **FR-006**: System MUST return structured analysis output: root_cause, impact, contributing_factors, recommended_actions, prevention_measures, confidence_score
- **FR-007**: System MUST generate 1536-dimensional vector embeddings for each incident using text-embedding-3-small
- **FR-008**: System MUST store embeddings in ChromaDB for vector similarity search
- **FR-009**: System MUST find similar incidents using cosine similarity search on embeddings
- **FR-010**: System MUST return similar incidents ranked by similarity score with incident metadata
- **FR-011**: System MUST provide REST API for log queries with filters: correlation_id, service_name, time_range, log_level
- **FR-012**: System MUST provide REST API for incident CRUD operations (create, read, list, update status)
- **FR-013**: System MUST provide REST API for triggering and retrieving analysis results
- **FR-014**: System MUST provide REST API for similar incident search
- **FR-015**: System MUST serve a web dashboard at port 8091 with incident list, detail views, and analysis trigger
- **FR-016**: System MUST propagate correlation IDs across all service boundaries
- **FR-017**: System MUST support configurable severity levels (LOW, MEDIUM, HIGH, CRITICAL) based on event type
- **FR-018**: System MUST implement structured JSON logging for all services with correlation ID propagation
- **FR-019**: System MUST support horizontal scaling of Kafka consumers via consumer groups

*Clarifications resolved:*

- **FR-021**: System MUST authenticate dashboard access via **none (open access for v1, can be added later)**
- **FR-022**: System MUST retain incidents for **90 days active storage in ChromaDB, then archived**
- **FR-023**: System MUST support multi-tenancy for **single tenant (no isolation)**

### Key Entities

- **Incident**: Represents a production failure; attributes: id, correlation_id, status (OPEN, ANALYZING, ANALYZED, RESOLVED), severity, created_at, updated_at, affected_services, duration_seconds, embedding_vector
- **Event**: A business event from Kafka; attributes: event_id, correlation_id, event_type, service_name, timestamp, payload, severity
- **Timeline**: Chronological sequence of events for an incident; attributes: incident_id, events (ordered), total_duration
- **Analysis**: AI-generated root cause analysis; attributes: incident_id, root_cause, impact, contributing_factors, recommended_actions, prevention_measures, confidence_score, created_at, model_version
- **SimilarIncident**: Reference to a similar past incident; attributes: incident_id, similar_incident_id, similarity_score, matched_on
- **LogEntry**: Structured log from any service; attributes: log_id, correlation_id, service_name, timestamp, level, message, metadata

## Success Criteria

### Measurable Outcomes

- **SC-001**: Engineers can view incident timeline within 5 seconds of failure event occurring
- **SC-002**: AI root cause analysis completes within 30 seconds of trigger
- **SC-003**: Similar incident search returns results within 2 seconds for databases up to 10,000 incidents
- **SC-004**: Log query API returns filtered results within 1 second for time ranges up to 24 hours
- **SC-005**: Dashboard loads incident list within 2 seconds
- **SC-006**: System detects and creates incidents for 99.9% of failure events published to Kafka
- **SC-007**: AI analysis confidence score correlates with actual resolution accuracy (>80% confidence = >90% accurate root cause)
- **SC-008**: Engineers reduce mean time to root cause identification from hours to under 10 minutes
- **SC-009**: Zero data loss for events during normal operation (at-least-once delivery)
- **SC-010**: System handles 10,000 events/second peak throughput without data loss

## Assumptions

- Target users are platform engineers and SREs familiar with Kafka, microservices, and incident response
- Existing Kafka infrastructure with business event topics (orders, payments, inventory, shipments) is available
- Events follow a consistent schema with correlation_id, event_type, service_name, timestamp fields
- OpenAI API access is available for GPT-4 and text-embedding-3-small
- **ChromaDB** is available for vector embedding storage and similarity search (RAG database)
- **Java 21** with **Spring Boot 3.4.4** for all backend services
- Reuses **eventflow-common** for BaseEntity, ApiResponse, CorrelationIdFilter, and domain events
- **Maven** multi-module as build tool, each service depends on eventflow-common
- **Docker** for containerization — each service runs as its own container
- **Single shared PostgreSQL** database (lighter than DB-per-service)
- **Docker Compose** for local development orchestration
- Node.js 18+ for frontend dashboard build
- Single-tenant deployment (no multi-tenancy isolation)
- No authentication required for dashboard in v1 (open access, can be added later)
- Incident retention: 90 days active in ChromaDB, then archived (configurable)
- Events without correlation_id are logged but not correlated (edge case handling)
- GPT-4 model version: gpt-4-turbo-preview (configurable)
- Embedding model: text-embedding-3-small (1536 dimensions, fixed)
- Dashboard port 8091 is available and not conflicting with other services
- PostgreSQL used for incident metadata and relational data; ChromaDB for vector embeddings