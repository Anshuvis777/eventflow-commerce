# Incident Analytics System — Analysis

## What It Is

An AI-powered incident analysis platform that automatically detects, correlates, and analyzes production incidents across microservices. It sits on top of your existing event-driven architecture and uses GPT-4 + vector embeddings to find root causes and similar past incidents.

---

## What It Does

### 1. Automatic Incident Detection
- Monitors all Kafka business events (orders, payments, inventory, shipments)
- When something fails (PaymentFailed, InventoryReleased, OrderCancelled), it auto-creates an incident
- No manual setup required — incidents appear as events flow through the system

### 2. Timeline Reconstruction
- Groups all events by correlation ID into a single timeline
- Shows the exact sequence: order placed → payment failed → inventory released
- Calculates affected services and total duration

### 3. Root Cause Analysis
- Sends incident data + timeline + logs to GPT-4
- Returns structured analysis: root cause, impact, contributing factors, recommended actions, prevention measures
- Assigns confidence score based on evidence quality

### 4. Similar Incident Detection
- Converts each incident into a 1536-dimensional vector embedding
- Uses pgvector to find cosine-similar past incidents
- Engineers can see "this happened before in INC-001 with 87% similarity"

### 5. Centralized Log Query
- All structured logs from all services are queryable via REST API
- Filter by correlation ID, service name, time range, log level
- Error statistics by service for dashboards

### 6. Minimal Dashboard
- Web UI at port 8091
- Enter correlation ID → see overview, timeline, analysis, similar incidents
- One-click analysis trigger

---

## What It Can Do (Future Enhancements)

| Capability | Impact |
|------------|--------|
| **Slack/PagerDuty alerts** | Auto-notify on-call when HIGH severity incident detected |
| **Auto-remediation** | Execute runbooks from recommended actions (restart service, rollback) |
| **Trend analysis** | "payment-service has 3x more incidents this week" |
| **Cost tracking** | Estimate incident cost based on affected services and duration |
| **Multi-tenant** | Isolate incidents by team/environment/region |
| **Custom models** | Fine-tune on your codebase for better root cause accuracy |
| **OpenTelemetry** | Distributed tracing integration for deeper correlation |
| **Chat interface** | Ask follow-up questions about an incident |
| **Bulk analysis** | Analyze all open incidents overnight |
| **SLA tracking** | Auto-resolve incidents, track MTTR |

---

## One-Line Summary

It watches your events, catches failures, explains why they happened, finds similar past incidents, and shows everything in one place — so engineers spend minutes instead of hours on incident analysis.
