# Implementation Plan: Container Grouping

**Branch**: `003-container-grouping` | **Date**: 2026-08-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-container-grouping/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Consolidate the 8 EventFlow backend services from 8 Docker containers into 3 grouped containers (`core-group-1`, `core-group-2`, `ai-group`) to cut RAM usage on a dev laptop. One shared multi-jar image (`docker/Dockerfile.grouped`) builds all 8 Spring Boot jars; a single POSIX-sh entrypoint (`docker/entrypoint-grouped.sh`) starts a per-container subset of jars selected by the `STARTUP_SERVICES` env var (format `service:port ...`). All service ports (8081–8085, 8091–8093) and the nginx dashboard proxy targets are preserved. JVM settings drop to `-Xmx128m -XX:MaxMetaspaceSize=96m` and per-group `mem_limit` is sized so total memory falls roughly 32%. A new `docker/compose-grouped.yml` (profiles `core`, `ai`, `all`) deploys the groups **alongside** the untouched per-service `docker/compose.yml` (clarification Q2). Any in-group service failure restarts the whole group container (clarification Q1).

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.4.4 — unchanged; existing service jars are reused as-is, no source code changes

**Primary Dependencies**: Docker, Docker Compose, POSIX sh; existing jars produced by Maven 3.9.9 multi-module build (`eventflow-common` + 8 services)

**Storage**: Unchanged — Neon PostgreSQL shared `eventflow` DB; ChromaDB for incident vectors; **no schema or entity changes**

**Testing**: Smoke/validation via `docker compose -f docker/compose-grouped.yml up` + port health checks (curl on 8081–8085, 8091–8093); `docker stats` comparison for SC-001; existing per-service contract/integration suites remain the regression net

**Target Platform**: Windows dev laptop running Docker Desktop (Linux containers); grouped compose file is for local development

**Project Type**: Infrastructure / deployment-topology change (Dockerfile + entrypoint script + Docker Compose)

**Performance Goals**: SC-001 ≥30% total RAM reduction for the 8 services; SC-002 all 8 services healthy within 3 minutes of a single start command; no regression in the e-commerce chain (<30s) or incident analysis (<30s)

**Constraints**: Aiven Kafka SASL_SSL with CA cert baked into the image (as today); `mem_limit` sized per group for multiple JVMs; at-least-once Kafka semantics unchanged (idempotent consumers already in place); ports 8081–8085/8091–8093 and dashboard proxy unchanged; secrets via existing `.env`

**Scale/Scope**: 3 grouped containers + dashboard; separate `compose-grouped.yml`; local-dev only; per-service layout preserved for other environments

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| I. Reuse `eventflow-common` (MANDATORY) | ✅ PASS | No source changes; the 8 service jars already depend on `eventflow-common` |
| II. Clean Architecture Package Layout (MANDATORY) | ✅ PASS | No package/layout changes; grouping is purely at the Docker layer |
| III. Test-First (NON-NEGOTIABLE) | ⚠️ DEVIATION | Deployment-only change, no new business logic to TDD. Mitigation: runnable smoke-validations in `quickstart.md` (all 8 services healthy on their ports, memory reduction measured); existing contract/integration suites remain the regression net |
| IV. Integration Testing with Real Infrastructure | ✅ PASS | Quickstart validates against real Docker/Kafka/PostgreSQL; no mocks |
| V. Simplicity & Single Shared Database | ✅ PASS | Single shared DB retained; one shared image + one entrypoint = simplest launcher |
| Deployment rule: "each microservice runs as an independent Docker container" | ❌ DELIBERATE VIOLATION | Justified in Complexity Tracking below (dev-laptop RAM; per-service layout preserved for other environments) |

**GATE RESULT**: Pass — the only violation is the deliberate container-grouping, explicitly requested by the user and justified in Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/003-container-grouping/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── deployment.md    # Grouped deployment contract (containers, ports, env, STARTUP_SERVICES)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
docker/
├── Dockerfile.grouped        # NEW — multi-stage build of all 8 service jars into ONE shared image
├── entrypoint-grouped.sh     # NEW — starts jars listed in $STARTUP_SERVICES ("service:port ...")
├── compose-grouped.yml       # NEW — 3 grouped containers + dashboard (profiles: core, ai, all)
├── compose.yml               # UNCHANGED — per-service layout preserved (clarification Q2)
├── compose-infra.yml         # UNCHANGED — Postgres/Kafka/Zookeeper/ChromaDB infra
└── certs/aiven-ca.pem        # REUSED — baked into grouped image for Kafka TLS

order-service/  payment-service/  inventory-service/  shipping-service/
notification-service/  incident-detector/  incident-analyzer/  incident-query/
eventflow-common/  dashboard/
├── ...                      # ALL UNCHANGED — existing source, Dockerfiles, and jar output reused as-is
```

**Structure Decision**: Additive, infra-only change. No application source code is modified. A new `docker/Dockerfile.grouped`, `docker/entrypoint-grouped.sh`, and `docker/compose-grouped.yml` are added beside the existing per-service layout — matching the clarified decision to keep the existing `docker/compose.yml` untouched.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Grouped containers (deviates from constitution's "each service in its own container") | Dev laptop has limited RAM; 8 JVMs in 8 containers exceed a comfortable budget. Grouping to 3 containers + lower JVM heap cuts total RAM ~32% while keeping all functionality and ports. Explicitly requested by the user. | Tuning memory per-service only saves ~15–20% (still 8 base JVMs); a single-JVM monolith saves the most but is a high-risk refactor (Flyway migration version conflicts, port/consumer-group merging) and the user chose grouping, not a monolith. |
| One shared multi-jar image instead of per-group images | User explicitly requested "one shared multi-jar Docker image"; builds once, each container selects its services via `STARTUP_SERVICES`. | Three separate images would duplicate the jar-copy + CA-cert steps and triple build time with zero RAM benefit. |
| Entrypoint launcher (multiple processes per container) | Simplest way to run N jars in one container with whole-group restart (Q1): the launcher exits on any child death; Docker's restart policy recovers the whole group. | A supervisor (supervisord/s6) adds a dependency and per-process restart complexity not needed for local dev; single-JVM rejected above. |
