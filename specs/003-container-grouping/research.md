# Research: Container Grouping

**Feature**: 003-container-grouping | **Date**: 2026-08-19

Phase 0 output resolving all unknowns in the plan's Technical Context. Each topic documents the decision, rationale, and alternatives considered.

## 1. Running multiple Spring Boot jars in one container

**Decision**: A single POSIX-sh entrypoint script (`docker/entrypoint-grouped.sh`) that starts each jar in the background with `java $JAVA_OPTS -jar /app/<name>.jar --server.port=<port> &`, then `wait`. The script exits when any child dies, which triggers Docker's `restart: unless-stopped` to recover the whole group (matches clarification Q1: whole-group restart).

**Rationale**: Zero extra dependencies (plain `sh` is present in the `eclipse-temurin:21-jre-alpine` base). Because the failure policy is "restart everything", no per-process supervision is needed — the launcher simply fails fast. `wait` keeps PID 1 alive and forwards the container lifecycle to all children.

**Alternatives considered**:
- *supervisord / s6-overlay*: adds a package and per-process restart logic we do not need for local dev; rejected for simplicity.
- *Single JVM running all services*: would save the most RAM but requires merging Spring contexts, ports, Flyway migrations, and Kafka consumer groups — a large, risky refactor explicitly out of scope.

## 2. JVM memory tuning on a constrained heap

**Decision**: Per-service JVM flags change from `-Xms128m -Xmx256m -XX:MaxMetaspaceSize=128m` to `-Xms64m -Xmx128m -XX:MaxMetaspaceSize=96m -XX:+UseG1GC -XX:MaxGCPauseMillis=50`. Keep `-XX:+UseContainerSupport` (default in Java 21).

**Rationale**: The platform is a small demo workload; each Spring Boot service runs comfortably on a 128 MB heap. Cutting `-Xmx` in half and trimming metaspace reduces both committed heap and class-metadata overhead. Actual RSS per JVM drops from roughly 250–300 MB to ~150–200 MB.

**Alternatives considered**:
- *Keep 256 MB heap*: no meaningful RAM saving; rejected.
- *-Xmx96m*: risk of OOM under transient spikes (e.g., Jackson deserialization of large event batches); 128 MB is the safe floor for this stack.

## 3. Group memory sizing (`mem_limit`)

**Decision**: Size `mem_limit` per group to cover its JVM count plus headroom:

| Container | Services (JVMs) | mem_limit |
|---|---|---|
| core-group-1 | order, payment (2) | `550m` |
| core-group-2 | inventory, shipping, notification (3) | `700m` |
| ai-group | incident-detector, analyzer, query (3) | `700m` |

**Rationale**: Memory caps are per-container, so the cap must scale with the number of co-located JVMs. Estimated caps: before = 8 × 350m = 2800m; after = 550 + 700 + 700 = 1950m → **~30% cap reduction**, and actual RSS (at 128 MB heap) drops further, satisfying SC-001 (≥30%).

**Alternatives considered**:
- *Uniform 350m per group*: would OOM core-group-2 and ai-group (3 JVMs each); rejected.
- *No mem_limit*: loses the hard guarantee and risks the host swapping; rejected.

## 4. Port management for co-located services

**Decision**: Each jar binds its own port inside the shared container; the entrypoint passes `--server.port=<port>` explicitly, and `compose-grouped.yml` publishes all group ports (`ports: "8081:8081", "8082:8082"`, etc.).

**Rationale**: Command-line `--server.port` overrides both `application.yml` and `SERVER_PORT` env, guaranteeing each service binds a distinct, stable port. Ports 8081–8085 (core) and 8091–8093 (AI) are preserved exactly, so the nginx dashboard proxy and any client keep working unchanged (spec US-2, SC-003).

**Alternatives considered**:
- *Single shared port with path routing*: would break every existing endpoint and the nginx proxy; rejected.
- *Rely on each jar's own application.yml*: works today, but an explicit `--server.port` removes any ambiguity inside a shared container; chosen.

## 5. Docker Compose grouped deployment + env handling

**Decision**: New `docker/compose-grouped.yml` defines `core-group-1`, `core-group-2`, `ai-group`, and `dashboard` on the shared `eventflow-net` bridge. Each group service:
- `build: context: .. dockerfile: docker/Dockerfile.grouped`
- `env_file: ../.env`
- receives the union of env vars its member services need (NEON_*, KAFKA_*, REDIS_*, MAIL_*, GEMINI_API_KEY, CHROMADB_HOST/PORT)
- `STARTUP_SERVICES` env selects the jars to launch (e.g., `"order-service:8081 payment-service:8082"`)
- profiles `core`, `ai`, `all` mirror the existing compose profiles

**Rationale**: Mirroring the existing compose.yml env wiring keeps behavior identical while switching the deployment unit from service → group. Keeping the existing `docker/compose.yml` untouched satisfies clarification Q2 (separate file, `compose-grouped.yml`).

**Alternatives considered**:
- *Modify compose.yml with a `grouped` profile*: works but bloats the canonical file and risks the existing workflow; rejected in favor of a separate file.
- *Per-group images*: rejected in research item on the shared image.

## 6. Health-checking multiple services in one container

**Decision**: No single container-level `HEALTHCHECK` (it can only probe one endpoint). Instead, `quickstart.md` validates health by curling each published port (`curl -sf http://localhost:<port>/actuator/health` or service base path) for all 8 services, plus `docker compose ps` for container state.

**Rationale**: A Docker HEALTHCHECK is a single binary check and cannot represent 8 Spring Boot apps. Port-level validation is simple, exact, and matches the acceptance scenarios (each service reachable on its port).

**Alternatives considered**:
- *HEALTHCHECK hitting one representative service per group*: misleading (siblings may be down); rejected.
- *Supervisor-managed health aggregation*: over-engineered for dev; rejected.

## 7. Logging for co-located services

**Decision**: Keep default behavior — all services log to the container's stdout, interleaved in `docker compose logs`. No per-service log files or routing in v1.

**Rationale**: Spring Boot's default console output already prefixes each line with the service's application name, so `docker compose logs` remains greppable per service. This is acceptable for local dev and avoids adding a log-routing layer. (Flagged as deferred during clarify; handled here as a documented decision.)

**Alternatives considered**:
- *Per-service log files + `tail -F`*: extra complexity, no real benefit for local dev; rejected.

## 8. CA cert + non-root user in the grouped image

**Decision**: The grouped Dockerfile copies `docker/certs/aiven-ca.pem` and imports it into the JVM truststore (same `keytool` step as today), and runs as a non-root user.

**Rationale**: Kafka uses SASL_SSL against Aiven; without the CA, consumers fail TLS handshakes. Non-root execution matches the existing hardening and the `addgroup`/`adduser` pattern in every current Dockerfile.

**Alternatives considered**: *Skip CA import*: would break Kafka TLS for the AI group and notification consumers; rejected.

## 9. Build strategy

**Decision**: `mvn clean package -DskipTests` builds all modules (eventflow-common + 8 services) in one build stage; the runtime stage copies all 8 jars (`order-service.jar`, `payment-service.jar`, etc.) and the entrypoint. One image is used by all 3 group containers.

**Rationale**: Matches the user's explicit "one shared multi-jar Docker image". Building all modules is a single Maven invocation; Docker layer caching means the shared image is stored once regardless of how many containers use it.

**Alternatives considered**: *Three images, each building only its group's modules*: more build invocations, duplicated CA-cert and base-image layers, no RAM benefit; rejected.
