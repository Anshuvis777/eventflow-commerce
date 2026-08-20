# Feature Specification: Container Grouping

**Feature Branch**: `003-container-grouping`

**Created**: 2026-08-19

**Status**: Draft

**Input**: User description: "Group the 8 backend services into 3 Docker containers to reduce RAM usage on a dev laptop: core-group-1 runs order-service and payment-service, core-group-2 runs inventory-service, shipping-service and notification-service, ai-group runs incident-detector, incident-analyzer and incident-query. Each grouped container uses one shared multi-jar Docker image with an entrypoint script that starts multiple Spring Boot processes, keeping all service ports (8081-8085, 8091-8093) and the nginx dashboard proxy unchanged. Also lower JVM memory settings (-Xmx128m, smaller MaxMetaspaceSize) and reduce compose mem_limit to cut total memory."

## Clarifications

### Session 2026-08-19

- Q: When one service inside a group container crashes, should the whole group container restart, or should the surviving services in that group keep running? → A: Restart the whole group container — the launcher exits when any service fails, and the container restart policy brings all services in the group back together
- Q: Should the 3 grouped containers replace the current 8 individual service containers, or should the grouped setup be added alongside the existing per-service layout? → A: Add alongside as a separate file — a new docker/compose-grouped.yml, leaving the existing compose file untouched

## User Scenarios & Testing

### User Story 1 - Run All Services in 3 Containers (Priority: P1)

The developer runs the entire platform (all 8 backend services) using only 3 containers instead of 8, so the laptop uses much less RAM while every feature still works.

**Why this priority**: This is the core request. Without the consolidation, RAM usage stays high and the developer cannot comfortably run the full platform locally.

**Independent Test**: Can be fully tested by starting the stack and verifying exactly 3 backend containers are running and healthy, and that all 8 services respond on their expected ports.

**Acceptance Scenarios**:

1. **Given** the consolidated deployment is configured, **When** the developer starts the stack, **Then** exactly 3 backend containers start and all 8 services become healthy
2. **Given** the consolidated deployment is running, **When** the developer inspects the running processes, **Then** each group container runs all of its assigned services simultaneously
3. **Given** the consolidated deployment, **When** compared to the previous 8-container setup, **Then** total memory usage is measurably lower

---

### User Story 2 - All Services Remain Reachable on Unchanged Ports (Priority: P1)

Every service stays reachable on the exact same port as before (8081–8085 for the commerce core, 8091–8093 for incident services), so the dashboard, the web proxy, and any existing scripts keep working without modification.

**Why this priority**: If ports changed, every client integration would break. Preserving ports makes the consolidation invisible to consumers.

**Independent Test**: Can be fully tested by calling each service's endpoint on its original port and confirming a healthy response.

**Acceptance Scenarios**:

1. **Given** the consolidated deployment is running, **When** each service is called on its original port, **Then** it responds successfully
2. **Given** the dashboard is connected through the web proxy, **When** the developer uses each dashboard page, **Then** data loads from all services as before
3. **Given** the consolidated deployment, **When** two services in the same container are called at the same time, **Then** both respond correctly (no port or process conflicts)

---

### User Story 3 - E-Commerce Flow Still Works End-to-End (Priority: P1)

The full order journey — order placed → payment → inventory reservation → shipment → notification emails — completes exactly as it did before the consolidation.

**Why this priority**: The consolidation must not regress the core business value. Even though services now share containers, the event chain between them must remain intact.

**Independent Test**: Can be fully tested by placing an order and verifying payment, inventory, shipping, and email notifications all fire in sequence.

**Acceptance Scenarios**:

1. **Given** an order is placed through the dashboard, **When** the flow runs, **Then** payment is processed, inventory is reserved, a shipment is created, and the expected emails are sent
2. **Given** a failure scenario (e.g., payment failure), **When** it occurs, **Then** the chain stops correctly and the expected failure email is sent
3. **Given** the consolidated deployment, **When** compared to the prior setup, **Then** the end-to-end flow completes within the same time target

---

### User Story 4 - Incident Analytics Still Works (Priority: P2)

Automatic incident detection, AI root cause analysis, and similar-incident search continue to function from the single AI group container.

**Why this priority**: The AI platform is a separate capability; consolidating it into one container must not break detection, analysis, or querying.

**Independent Test**: Can be fully tested by triggering a failure event and confirming an incident is created, analyzed, and queryable.

**Acceptance Scenarios**:

1. **Given** a failure event is published, **When** the AI group processes it, **Then** an incident is created automatically
2. **Given** an incident exists, **When** analysis is triggered, **Then** structured root cause output is returned
3. **Given** incidents exist, **When** a similar-incident search runs, **Then** ranked results are returned

---

### User Story 5 - Lower Total Memory Footprint (Priority: P1)

The consolidated deployment uses noticeably less RAM than the current 8-container setup, making it comfortable to run the whole platform on a development laptop.

**Why this priority**: This is the measurable outcome the developer wants — less memory pressure without losing functionality.

**Independent Test**: Can be fully tested by measuring container memory usage before and after consolidation and comparing totals under the same workload.

**Acceptance Scenarios**:

1. **Given** the same workload, **When** memory usage is measured, **Then** the consolidated setup uses at least 30% less RAM than the 8-container setup
2. **Given** the consolidated setup, **When** all 8 services are idle, **Then** total memory stays within the laptop's comfortable budget
3. **Given** a memory-limited laptop, **When** the full stack runs, **Then** no service is killed or restarted due to memory pressure

---

### Edge Cases

- What happens when one service inside a group container fails? → The launcher exits and the whole group container restarts, bringing all services in the group back together (per the container restart policy)
- What happens if two grouped services try to bind the same port? → Port assignment is verified so every service keeps its unique port
- What happens when a group container is stopped or restarted? → All services in that group stop and start together
- What happens to the dashboard during consolidation? → It continues to reach every service through the proxy on unchanged ports
- What happens on a machine with very low memory? → The reduced footprint must still fit without service restarts
- What happens during a clean build? → The build produces the images required by all 3 group containers

## Requirements

### Functional Requirements

- **FR-001**: System MUST run all 8 backend services using no more than 3 containers
- **FR-002**: System MUST run order-service and payment-service within the same group container
- **FR-003**: System MUST run inventory-service, shipping-service, and notification-service within the same group container
- **FR-004**: System MUST run incident-detector, incident-analyzer, and incident-query within the same group container
- **FR-005**: Every service MUST remain individually reachable on its existing port (8081–8085, 8091–8093)
- **FR-006**: The dashboard MUST continue to reach all services exactly as it does today, with no client-side changes required
- **FR-007**: The e-commerce event chain (order → payment → inventory → shipment → notification) MUST complete without regression
- **FR-008**: Incident detection, AI analysis, and similar-incident search MUST function without regression
- **FR-009**: Total memory usage of the 8 services MUST be lower than the current 8-container deployment
- **FR-010**: A single start command MUST bring up all containers in a group together
- **FR-011**: The restart policy MUST apply to each group container so services recover automatically
- **FR-012**: The build process MUST produce the images required by all 3 group containers
- **FR-013**: System MUST provide the grouped deployment as a separate configuration file that leaves the existing per-service deployment configuration unchanged and available

## Success Criteria

### Measurable Outcomes

- **SC-001**: Total RAM used by the 8 backend services drops by at least 30% compared to the 8-container setup
- **SC-002**: The full platform (all 8 services) starts and becomes healthy within 3 minutes of a single start command
- **SC-003**: 100% of existing service ports (8081–8085, 8091–8093) remain reachable after consolidation
- **SC-004**: The end-to-end order-to-email flow completes within the pre-existing time target (no regression)
- **SC-005**: Incident detection and AI analysis complete within the pre-existing time targets (no regression)
- **SC-006**: The dashboard renders data for all pages without errors

## Assumptions

- Target users are developers running the full platform on a development laptop with limited RAM
- Services communicate asynchronously, so co-locating them in shared containers does not break the event chain
- Service ports and the dashboard proxy configuration remain unchanged (backward compatibility)
- The grouped deployment ships as a separate configuration file (`docker/compose-grouped.yml`); the existing per-service `docker/compose.yml` remains unchanged and available for other environments
- This is a deliberate deviation from the project convention of one service per container, justified by the RAM savings on dev hardware; the trade-off (a failing service shares restart fate with its group) is accepted for local use
- Existing secrets and connection settings are reused as-is
- No new features, endpoints, or data models are added by this change — only deployment topology and memory settings change
