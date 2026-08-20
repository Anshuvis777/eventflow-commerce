---

description: "Task list for the Container Grouping feature"
---

# Tasks: Container Grouping

**Input**: Design documents from `/specs/003-container-grouping/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Deployment-only feature — no new business logic, so no unit/contract test tasks. Acceptance is verified by runnable validation tasks (mapped to `quickstart.md`), which replace TDD tests per the documented Test-First deviation in `plan.md` (Constitution Check, gate III).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Grouped artifacts live in `docker/` (repository root)
- No application source files are modified (see `plan.md` — additive, infra-only change)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Author the 3 grouped-deployment artifacts

- [x] T001 [P] Create launcher script `docker/entrypoint-grouped.sh`: read `$STARTUP_SERVICES` (space-separated `service:port` list), start each as `java $JAVA_OPTS -jar /app/<service>.jar --server.port=<port> &`, trap TERM/INT to kill children, then `wait` so the container exits when any child dies (whole-group restart per clarification Q1)
- [x] T002 [P] Create shared multi-jar image `docker/Dockerfile.grouped`: multi-stage build (Maven stage `mvn clean package -DskipTests` for all modules; runtime stage copies the 8 jars to `/app/<service>.jar`, copies `docker/entrypoint-grouped.sh`, imports `docker/certs/aiven-ca.pem` into the JVM truststore, non-root user via `addgroup`/`adduser`, `ENV JAVA_OPTS="-Xms64m -Xmx128m -XX:MaxMetaspaceSize=96m -XX:+UseG1GC -XX:MaxGCPauseMillis=50"`, `ENTRYPOINT ["sh","/app/entrypoint-grouped.sh"]`)
- [x] T003 Create grouped compose `docker/compose-grouped.yml`: define `core-group-1` (STARTUP_SERVICES `order-service:8081 payment-service:8082`, mem_limit `550m`), `core-group-2` (STARTUP_SERVICES `inventory-service:8083 shipping-service:8084 notification-service:8085`, mem_limit `700m`), `ai-group` (STARTUP_SERVICES `incident-query:8091 incident-detector:8092 incident-analyzer:8093`, mem_limit `700m`), and `dashboard` (port 3000); publish all ports; `env_file: ../.env` with the union env per group (NEON_*/KAFKA_*/REDIS_* for core-group-1; +MAIL_* for core-group-2; +GEMINI_API_KEY/CHROMADB_HOST/CHROMADB_PORT for ai-group); profiles `core`/`ai`/`all`; shared `eventflow-net`; `restart: unless-stopped` — validate against `specs/003-container-grouping/contracts/deployment.md`

**Checkpoint**: All 3 artifacts authored; cross-check ports, env, `STARTUP_SERVICES`, and mem_limit against `contracts/deployment.md`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the shared grouped image — blocks every user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 [P] Build the grouped image: `docker build -f docker/Dockerfile.grouped -t eventflow-grouped .`
- [x] T005 [P] Start infrastructure dependencies: `docker compose -f docker/compose-infra.yml up -d`
- [x] T006 Verify the image contains all 8 jars and the entrypoint: `docker run --rm eventflow-grouped ls /app`

**Checkpoint**: Image builds and `/app` contains `order-service.jar`, `payment-service.jar`, `inventory-service.jar`, `shipping-service.jar`, `notification-service.jar`, `incident-detector.jar`, `incident-analyzer.jar`, `incident-query.jar`, and `entrypoint-grouped.sh`.

---

## Phase 3: User Story 1 - Run All Services in 3 Containers (Priority: P1) 🎯 MVP

**Goal**: Start the entire platform as exactly 3 backend containers hosting all 8 services (FR-001..004).

**Independent Test**: `docker compose -f docker/compose-grouped.yml --profile all ps` shows exactly 3 backend containers (`core-group-1`, `core-group-2`, `ai-group`) + dashboard, all `Up`, and all 8 services healthy within 3 minutes (SC-002).

### Implementation for User Story 1

- [x] T007 [US1] Start the grouped deployment: `docker compose -f docker/compose-grouped.yml --profile all up -d --build`
- [x] T008 [US1] Verify exactly 3 backend containers + dashboard are running: `docker compose -f docker/compose-grouped.yml --profile all ps`
- [x] T009 [US1] Verify each group runs all of its assigned services as separate JVMs: `docker exec core-group-1 ps aux | grep java` (repeat for `core-group-2` and `ai-group`)

**Checkpoint**: At this point, User Story 1 is fully functional and independently testable (MVP deliverable).

---

## Phase 4: User Story 2 - All Services Remain Reachable on Unchanged Ports (Priority: P1)

**Goal**: Every service responds on its original port (8081–8085, 8091–8093) and the dashboard proxy works unchanged (FR-005, FR-006, SC-003).

**Independent Test**: Curl each of the 8 ports and get HTTP 2xx; open dashboard pages via `http://localhost:3000`.

### Implementation for User Story 2

- [ ] T010 [P] [US2] Verify all 8 ports respond with HTTP 2xx: `@(8081,8082,8083,8084,8085,8091,8092,8093) | ForEach-Object { "port $_ -> " + (curl -s -o NUL -w "%{http_code}" http://localhost:$_) }`
- [ ] T011 [US2] Verify the nginx dashboard proxy reaches grouped services: open `http://localhost:3000` and confirm each page (Orders, Payments, Inventory, Shipping, Email & Alerts, AI Analysis) loads data
- [ ] T012 [P] [US2] Verify no port/process conflict when two services in the same group are called concurrently (e.g., call 8083 and 8084 simultaneously)

**Checkpoint**: At this point, User Stories 1 AND 2 both work independently.

---

## Phase 5: User Story 5 - Lower Total Memory Footprint (Priority: P1)

**Goal**: The grouped deployment uses at least 30% less RAM than the 8-container baseline (FR-009, SC-001).

**Independent Test**: `docker stats --no-stream` shows group memory within the 550/700/700 caps, with total ≥30% below the 2800m baseline.

### Implementation for User Story 5

- [ ] T013 [P] [US5] Confirm per-group memory caps in `docker/compose-grouped.yml` (core-group-1 `550m`, core-group-2 `700m`, ai-group `700m`; total 1950m vs 2800m baseline)
- [ ] T014 [US5] Measure actual memory: `docker stats --no-stream` and sum the 3 group containers
- [ ] T015 [US5] Compute the reduction vs. the 8-container baseline (2800m cap) and confirm ≥30% (SC-001); confirm no service is OOM-killed on a memory-limited host

**Checkpoint**: User Stories 1, 2, and 5 pass.

---

## Phase 6: User Story 3 - E-Commerce Flow Still Works End-to-End (Priority: P1)

**Goal**: The full order journey (order → payment → inventory → shipment → emails) completes with no regression (FR-007, SC-004).

**Independent Test**: Place an order via dashboard/API and verify payment, inventory, shipment, and notification emails all fire in sequence within 30s.

### Implementation for User Story 3

- [ ] T016 [US3] Place an order through the dashboard (`http://localhost:3000` → Orders) or `POST /api/v1/orders` on 8081
- [ ] T017 [US3] Verify payment auto-processes (payment-service 8082), inventory reserves (8083), shipment created (8084), and emails sent (8085) — chain completes within 30s
- [ ] T018 [US3] Verify a failure path: trigger a payment failure and confirm the chain stops and the failure email is sent

**Checkpoint**: User Stories 1–3 and 5 pass.

---

## Phase 7: User Story 4 - Incident Analytics Still Works (Priority: P2)

**Goal**: Incident detection, AI analysis, and similar-incident search function from the single ai-group container (FR-008, SC-005).

**Independent Test**: Publish a failure event; confirm an incident is created, analysis returns structured output, and similar search returns ranked results.

### Implementation for User Story 4

- [ ] T019 [US4] Publish a failure event (e.g., payment failure) to Kafka, or trigger detection via the incident API on 8092
- [ ] T020 [US4] Verify an incident is created automatically (incident-detector)
- [ ] T021 [US4] Trigger analysis and verify structured root-cause output (incident-analyzer 8093)
- [ ] T022 [US4] Run a similar-incident search and verify ranked results (incident-query 8091)

**Checkpoint**: All user stories pass.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Docs, guarantees, and final validation

- [ ] T023 [P] Update `README.md` with grouped-deployment instructions (`docker build -f docker/Dockerfile.grouped -t eventflow-grouped .` then `docker compose -f docker/compose-grouped.yml --profile all up -d`)
- [ ] T024 Verify `docker/compose.yml` is untouched and the per-service layout still works (FR-013)
- [ ] T025 Verify whole-group restart (Q1): kill one JVM in a group (`docker exec core-group-2 sh -c "kill \$(pgrep -f inventory-service)"`) and confirm the container restarts with all 3 services back
- [ ] T026 [P] Run the full `specs/003-container-grouping/quickstart.md` end-to-end as the final acceptance run
- [ ] T027 [P] Clean up temporary test containers/images created during validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phases 3–7)**: All depend on Foundational phase completion
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: MVP — builds and starts the deployment; no dependencies on other stories
- **User Story 2 (P1)**: Depends on US1 (deployment must be running to check ports)
- **User Story 5 (P1)**: Depends on US1 (needs running containers to measure memory)
- **User Story 3 (P1)**: Depends on US1 + US2 (needs working services and reachable ports)
- **User Story 4 (P2)**: Depends on US1 (ai-group must be running); independent of US2/US3

### Within Each User Story

- Validation tasks run in order; tasks marked [P] can run in parallel
- Core implementation (Phase 1/2 artifacts) before any story validation

### Parallel Opportunities

- All Phase 1 tasks (T001, T002, T003) can run in parallel — different files
- Phase 2: T004 (build image) and T005 (start infra) can run in parallel
- US2: T010 and T012 can run in parallel (independent port checks)
- US5: T013 and T014 can run in parallel
- Polish: T023, T026, T027 can run in parallel

---

## Parallel Example: User Story 2

| Task | Run |
|------|-----|
| T010 [P] Verify all 8 ports respond | Engineer A |
| T011 Verify dashboard proxy | Engineer B |
| T012 [P] Verify no concurrent port conflict | Engineer A (after T010) |

---

## Implementation Strategy

- **MVP first**: Deliver Phase 1 + Phase 2 + Phase 3 (User Story 1) as the minimal viable increment — a working 3-container deployment of all 8 services.
- **Incremental delivery**: Add US2 (ports) and US5 (memory) validation next, then US3 (e-commerce flow), then US4 (incident analytics).
- **Safest path**: Because this is a deployment change to an existing working platform, run the full `quickstart.md` (T026) and confirm the original `docker/compose.yml` still works (T024) before considering the feature complete.
