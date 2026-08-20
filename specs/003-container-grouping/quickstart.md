# Quickstart: Container Grouping

**Feature**: 003-container-grouping | **Date**: 2026-08-19

Runnable validation guide proving the grouped deployment works end-to-end. Implementation details (entrypoint, Dockerfile, compose contents) belong to `tasks.md` / the implementation phase — this is the run/validate guide. See [contracts/deployment.md](contracts/deployment.md) for the full contract and [data-model.md](data-model.md) for the topology.

## Prerequisites

- Docker Desktop **running** (Linux containers) — the daemon must be up
- `.env` present at repo root with all secrets (Neon, Aiven Kafka, Redis, SMTP, Gemini, ChromaDB)
- Infrastructure running:
  ```powershell
  docker compose -f docker/compose-infra.yml up -d
  ```

## 1. Build the shared grouped image

```powershell
docker build -f docker/Dockerfile.grouped -t eventflow-grouped .
```

**Expected**: build succeeds; image contains all 8 jars (`/app/<service>.jar`) and the entrypoint.

## 2. Start the grouped deployment

```powershell
# Full platform (core + AI groups + dashboard)
docker compose -f docker/compose-grouped.yml --profile all up -d

# Or just the commerce core
docker compose -f docker/compose-grouped.yml --profile core up -d
```

**Expected**: containers `core-group-1`, `core-group-2`, `ai-group`, `incident-dashboard` created and running.

## 3. Validate container count & health

```powershell
docker compose -f docker/compose-grouped.yml --profile all ps
```

**Expected**: exactly **3 backend containers** + dashboard, all `Up`. Then verify all 8 services respond on their ports:

```powershell
@(8081,8082,8083,8084,8085,8091,8092,8093) | ForEach-Object { "port $_ -> " + (curl -s -o NUL -w "%{http_code}" http://localhost:$_) }
```

**Expected**: every port returns HTTP 2xx (SC-002: healthy within 3 minutes; SC-003: 100% ports reachable).

## 4. Validate memory reduction (SC-001)

```powershell
docker stats --no-stream
```

**Expected**: sum of the 3 group containers' memory is at or below ~1.9 GB total cap (550 + 700 + 700), and measured RSS is ≥30% lower than the previous 8-container baseline (2800m cap).

## 5. Validate e-commerce flow (SC-004)

Place an order through the dashboard (`http://localhost:3000` → Orders), or via the existing order API on 8081.

**Expected**: payment processes automatically (payment-service 8082), inventory reserves (8083), shipment created (8084), and notification emails sent (8085) — the full chain completes within the pre-existing 30s target with no regression.

## 6. Validate incident analytics (SC-005)

Publish a failure event (e.g., payment failure) or trigger detection via the incident API on 8092.

**Expected**: an incident is created automatically; triggering analysis returns structured root-cause output; similar-incident search returns ranked results — all within the pre-existing targets.

## 7. Validate dashboard (SC-006)

Open `http://localhost:3000` and visit each page (Orders, Payments, Inventory, Shipping, Email & Alerts, AI Analysis).

**Expected**: every page loads data from the grouped services through the nginx proxy with no errors.

## 8. Validate failure behavior (Q1)

Kill one JVM inside a group (e.g., `docker exec core-group-2 sh -c "kill \$(pgrep -f inventory-service)"`).

**Expected**: the `core-group-2` container exits and automatically restarts; **all three** of its services come back together (whole-group restart).

## 9. Confirm per-service layout still works (FR-013)

The existing `docker/compose.yml` is untouched — the original one-container-per-service workflow remains available and unchanged.

## Success criteria map

| Criterion | Validated in |
|---|---|
| SC-001 ≥30% RAM reduction | §4 |
| SC-002 healthy within 3 min | §3 |
| SC-003 100% ports reachable | §3 |
| SC-004 e-commerce flow no regression | §5 |
| SC-005 incident analytics no regression | §6 |
| SC-006 dashboard no errors | §7 |
