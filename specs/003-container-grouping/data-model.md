# Data Model: Container Grouping

**Feature**: 003-container-grouping | **Date**: 2026-08-19

> **Explicit scope note**: This feature introduces **no database schema, entity, or relationship changes**. The application data model (orders, payments, inventory, shipments, notifications, incidents, logs) is untouched — the 8 services reuse their existing jars and schemas as-is. This document therefore models the **deployment topology** introduced by the feature, which is the only "structure" this change defines.

## Deployment Topology

### Groups (containers)

| Container | Services (jars) | JVM count | Published ports | mem_limit | Compose profile |
|---|---|---|---|---|---|
| `core-group-1` | order-service, payment-service | 2 | 8081, 8082 | `550m` | core, all |
| `core-group-2` | inventory-service, shipping-service, notification-service | 3 | 8083, 8084, 8085 | `700m` | core, all |
| `ai-group` | incident-detector, incident-analyzer, incident-query | 3 | 8092, 8093, 8091 | `700m` | ai, all |
| `dashboard` | (nginx React app) | 0 | 3000 | `50m` | core, ai, all |

### Service → Group mapping (canonical)

- **core-group-1**: order-service (8081), payment-service (8082)
- **core-group-2**: inventory-service (8083), shipping-service (8084), notification-service (8085)
- **ai-group**: incident-query (8091), incident-detector (8092), incident-analyzer (8093)

### Launcher contract (`STARTUP_SERVICES`)

The entrypoint reads `STARTUP_SERVICES`, a space-separated list of `service:port` pairs, and starts one JVM per pair:

```
STARTUP_SERVICES="order-service:8081 payment-service:8082"
```

- `service` is the jar name in `/app/<service>.jar` (e.g., `order-service.jar`).
- `port` is the explicit `--server.port` bound inside the container (overrides `application.yml` / `SERVER_PORT`).
- Every `service:port` pair must be unique across the whole deployment (port uniqueness rule).

### Environment requirements per group (union of member services)

| Group | Env vars required |
|---|---|
| core-group-1 | NEON_*, KAFKA_*, REDIS_* |
| core-group-2 | NEON_*, KAFKA_*, REDIS_*, MAIL_* |
| ai-group | NEON_*, KAFKA_*, REDIS_*, GEMINI_API_KEY, CHROMADB_HOST, CHROMADB_PORT |

All values come from the existing `../.env` via `env_file`.

## State Transitions

- **Container lifecycle** (per group): `created → running → (any service exits) → exited → restarted` via Docker `restart: unless-stopped`.
- **In-group failure policy** (clarification Q1): if any member JVM exits non-zero, the entrypoint exits and the **whole group container restarts** — all member services come back together. There is no per-process recovery.

## Validation Rules (from spec)

- Port uniqueness: each of 8081–8085, 8091–8093 is bound by exactly one service (FR-005).
- Group membership: order+payment only in core-group-1; inventory+shipping+notification only in core-group-2; detector+analyzer+query only in ai-group (FR-002/003/004).
- Deployment isolation: grouped layout must not modify the existing per-service compose configuration (FR-013).
- Memory: total mem_limit of the 8 services ≤ 1950m (from 2800m) to meet SC-001.
