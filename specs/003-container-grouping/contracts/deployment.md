# Deployment Contract: Container Grouping

**Feature**: 003-container-grouping | **Date**: 2026-08-19

This is the external interface contract for the grouped deployment. No new REST/API endpoints are introduced; the contract below defines the **deployment interface** — what operators start, what they can expect, and how to validate it.

## 1. Artifacts

| Artifact | Path | Purpose |
|---|---|---|
| Grouped image | `docker/Dockerfile.grouped` | Builds all 8 service jars into one shared image (`eventflow-grouped`) |
| Launcher | `docker/entrypoint-grouped.sh` | Starts the jars listed in `STARTUP_SERVICES` |
| Grouped compose | `docker/compose-grouped.yml` | Deploys `core-group-1`, `core-group-2`, `ai-group`, `dashboard` |

## 2. Container / Service / Port matrix (fixed contract)

| Container | Service | Port |
|---|---|---|
| core-group-1 | order-service | 8081 |
| core-group-1 | payment-service | 8082 |
| core-group-2 | inventory-service | 8083 |
| core-group-2 | shipping-service | 8084 |
| core-group-2 | notification-service | 8085 |
| ai-group | incident-query | 8091 |
| ai-group | incident-detector | 8092 |
| ai-group | incident-analyzer | 8093 |
| dashboard | — (nginx) | 3000 |

These ports **must not change** (SC-003, FR-005); the nginx proxy targets `order-service:8081`, `payment-service:8082`, etc., which resolve via Docker DNS to the group containers hosting them.

## 3. Environment contract

`STARTUP_SERVICES` (space-separated `service:port` list):

```
core-group-1:  order-service:8081 payment-service:8082
core-group-2:  inventory-service:8083 shipping-service:8084 notification-service:8085
ai-group:      incident-query:8091 incident-detector:8092 incident-analyzer:8093
```

Required env (from `../.env`): `NEON_HOST/NEON_USER/NEON_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS/KAFKA_USERNAME/KAFKA_PASSWORD`, `REDIS_HOST/REDIS_PORT/REDIS_USERNAME/REDIS_PASSWORD`, `MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD`, `GEMINI_API_KEY`, `CHROMADB_HOST/CHROMADB_PORT`.

## 4. Lifecycle & failure behavior

- Start: `docker compose -f docker/compose-grouped.yml --profile all up -d --build`
- Stop: `docker compose -f docker/compose-grouped.yml --profile all down`
- In-group failure: launcher exits → container restarts → all member services recover together (Q1).
- Network: all groups join the existing `eventflow-net` bridge; infra containers (Postgres, Kafka, Zookeeper, ChromaDB) are started via `docker compose -f docker/compose-infra.yml up -d`.

## 5. Validation checks

| Check | Command | Expected |
|---|---|---|
| 3 backend containers up | `docker compose -f docker/compose-grouped.yml --profile all ps` | `core-group-1`, `core-group-2`, `ai-group` running |
| 8 services respond | `curl -sf http://localhost:8081/...` … `curl -sf http://localhost:8093/...` | HTTP 2xx on every port |
| Memory reduction (SC-001) | `docker stats --no-stream` | Sum of group memory ≤ 1950m cap; measured RSS < pre-change baseline by ≥30% |
| Dashboard proxy | open `http://localhost:3000` | All pages load data from grouped services |

## 6. Out of scope (unchanged contract)

- Application REST API contracts (existing OpenAPI/contracts in `specs/001`/`specs/002`) — untouched.
- Kafka event schemas — untouched (same topics, same event types, idempotent consumers).
- The per-service deployment (`docker/compose.yml`) — unchanged and still available (FR-013).
