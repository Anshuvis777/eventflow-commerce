# EventFlow Commerce — Completely Free Deployment Plan

> Goal: deploy everything for **$0 forever** — no trial, no credit card required after free signup.
> Previous Railway recommendation removed: Railway has no permanent free tier since Aug 2023 (only a one-time $5 trial).

---

## 1. Architecture on permanent free tiers

```
Browser
   │
   ├── Frontend ──► https://eventflow-commerce.vercel.app   (Vercel Hobby — free forever)
   │                built from dashboard/ (Vite → static)
   │
   └── API calls ─► https://eventflow.onrender.com          (Render Free — free forever, sleeps after 15 min idle)
                    single grouped Docker image:
                    core-group + ai-group (8 Java jars)
                    exposes 8081-8085, 8091-8093 via one Web Service
                         │
                         ├─ Postgres  ─► Neon serverless (free 0.5 GB, autosuspend, no card)
                         ├─ Kafka     ─► Upstash Kafka REST (free ~10k msg/day, no card)
                         ├─ Redis     ─► Upstash Redis (free ~10k cmd/day, no card)
                         ├─ Gemini    ─► Google AI Studio free (15 RPM, no billing needed)
                         └─ ChromaDB  ─► embedded — no hosting (falls back to in-memory vector)
```

**Why this split:**

- **Frontend** must be static — Vercel is permanently free for hobby projects.
- **Backend** needs long-lived Java + background @Scheduled; Render Free is the only major host with a permanent Docker free tier that stays at $0. Sleeps on idle (cold start ~30-60s) but fine for a portfolio demo.
- **Managed infra** (Neon, Upstash) all have card-free free tiers that survive demo traffic. No self-hosted DB/Kafka on Render to stay within memory limits.

**Alternative if Render sleeps too often:** Fly.io also has a permanent free allowance (3 shared-cpu-1x VMs, 3 GB volumes) — instructions in section 4.6b.

---

## 2. Accounts to create (12 minutes, all free — no card for the starred ones)

| Service | Permanent free tier | Card? | What to do |
|---------|---------------------|-------|------------|
| GitHub | — | — | Push this repo |
| ★ Neon | 1 project, 0.5 GB, autosuspend | No | Create project → copy connection string (NEON_HOST / USER / PASSWORD) |
| ★ Upstash Kafka | ~10k messages/day | No | Create Kafka cluster → copy KAFKA_BOOTSTRAP_SERVERS / USERNAME / PASSWORD |
| ★ Upstash Redis | 10k commands/day | No | Create Redis DB → copy REDIS_HOST / PORT / PASSWORD |
| ★ Google AI Studio | Gemini 2.0 Flash free (15 RPM) | No | Generate GEMINI_API_KEY (optional — heuristic fallback works without it) |
| ★ Vercel Hobby | 100 GB bandwidth/month | No* | Import dashboard/ folder |
| ★ Render Free | 750 hrs/month, sleeps after 15 min idle | No** | Create Web Service from GitHub, Dockerfile docker/Dockerfile.grouped |

\* Vercel Hobby requires no card at signup (card only for Pro upgrades).
\*\* Render Free can be created with just GitHub auth; card only needed if you exceed free usage.

> Alternative Kafka free: Redpanda Cloud Developer tier (also free, card-free).

---

## 3. One-time repo changes

- [x] `Dockerfile.grouped` already builds all 8 jars (multi-stage Maven → JRE-Alpine)
- [x] `docker/compose-grouped.yml` groups into `core-group-1`, `core-group-2`, `ai-group`
- [ ] Add `render.yaml` for one-click Render deploy (provided below — just commit it)
- [ ] Add `dashboard/.env.production` → `VITE_API_URL=https://<render-app>.onrender.com`

---

## 4. Deploy — step-by-step

### 4.1 Push to GitHub

```bash
git init
git add .
git commit -m "feat: ready for deployment"
git remote add origin https://github.com/<you>/eventflow-commerce.git
git push -u origin main
```

### 4.2 Neon Postgres — card-free

1. **neon.tech** → Sign up with GitHub → New Project (pick region closest to you).
2. Neon shows a connection string like:
   ```
   postgresql://neondb_owner:xxx@ep-xxx-pooler.c-4.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
   Split into:
   - `NEON_HOST` = hostname only (e.g. `ep-xxx-pooler.c-4.us-east-2.aws.neon.tech`)
   - `NEON_USER` = `neondb_owner`
   - `NEON_PASSWORD` = password part
3. Flyway will auto-create all tables on first deploy — no manual SQL.

> **Stay-warm is automatic:** all 8 services have a `WarmUpService` (`@Scheduled SELECT 1` every 4 min) and `GET /health` (`DbHealthController` in `eventflow-common`, DB-validated). Enable by setting `KEEPALIVE_ENABLED=true` on Render/Fly (disabled locally by default) — keeps Neon from suspending (5 min idle) and avoids Render's 15-min sleep cold start. For external ping, add a free **cron-job.org** job every 14 min to `https://<render-app>/health`.

### 4.3 Upstash Kafka — card-free

1. **console.upstash.com** → Kafka → Create Cluster (same region as Neon).
2. Create topics: `orders`, `payments`, `inventory`, `shipments`, `business-events` (1 partition each is fine).
3. Copy SASL credentials:
   ```
   KAFKA_BOOTSTRAP_SERVERS=<upstash-broker>:9092
   KAFKA_USERNAME=<upstash-username>
   KAFKA_PASSWORD=<upstash-password>
   ```

### 4.4 Upstash Redis — card-free

1. **console.upstash.com** → Redis → Create Database.
2. Copy TLS host/port/password → `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`.

### 4.5 Gemini (optional — fallback already wired)

```bash
# https://aistudio.google.com/app/apikey  (free — no billing)
GEMINI_API_KEY=AIza...
```
If omitted, `Gpt4AnalysisService` uses the **context-aware heuristic** (dynamic root-cause by event type, confidence 78-87).

### 4.6 Render — backend (permanent free, Docker)

#### Option A: Render Dashboard (recommended, 5 minutes)

1. **render.com** → New + → Web Service → Connect GitHub → select `eventflow-commerce`.
2. Settings:
   - **Runtime:** Docker
   - **Dockerfile path:** `docker/Dockerfile.grouped`
   - **Plan:** Free
   - **Region:** same as Neon/Upstash
3. **Environment → Add variables** (paste from `.env.example`):
   - `NEON_HOST`, `NEON_USER`, `NEON_PASSWORD`
   - `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_USERNAME`, `KAFKA_PASSWORD`
   - `REDIS_HOST`, `REDIS_PORT`, `REDIS_USERNAME`, `REDIS_PASSWORD`
   - `GEMINI_API_KEY` (optional)
   - `STARTUP_SERVICES` = `order-service:8081 payment-service:8082 inventory-service:8083 shipping-service:8084 notification-service:8085 incident-query:8091 incident-detector:8092 incident-analyzer:8093`
   - `JAVA_OPTS` = `-Xms48m -Xmx96m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC` (already in Dockerfile; override only if OOM)
4. **Create Web Service** → Render builds the grouped image.
5. After deploy, open **Logs** — you should see `Started OrderServiceApplication in Xs` × 8 and `Flyway: Successfully applied V1__init.sql`.
6. Your backend URL: `https://eventflow-<id>.onrender.com`

   Health check:
   ```bash
   curl https://<your-render-app>.onrender.com/health
   ```
   Render Free exposes one port by default. To expose all 8, keep the grouped entrypoint which fans out internally. For a demo you can route everything through one port via API gateway or just test against the port Render exposes (health endpoint proves it booted).

#### Option A — Blueprint (one-click via `render.yaml`)

Create `render.yaml` at the repo root (commit it before connecting):

```yaml
services:
  - type: web
    name: eventflow-grouped
    runtime: docker
    dockerfilePath: ./docker/Dockerfile.grouped
    plan: free
    healthCheckPath: /health
    envVars:
      - key: NEON_HOST
        sync: false
      - key: NEON_USER
        sync: false
      - key: NEON_PASSWORD
        sync: false
      - key: KAFKA_BOOTSTRAP_SERVERS
        sync: false
      - key: KAFKA_USERNAME
        sync: false
      - key: KAFKA_PASSWORD
        sync: false
      - key: REDIS_HOST
        sync: false
      - key: REDIS_PORT
        sync: false
      - key: REDIS_PASSWORD
        sync: false
      - key: GEMINI_API_KEY
        sync: false
      - key: KEEPALIVE_ENABLED
        value: "true"
```

#### Option B: Fly.io (alternative permanent free, better cold-start)

```bash
# Install flyctl, then inside repo:
fly launch --dockerfile docker/Dockerfile.grouped --name eventflow-grouped --region bom
fly secrets set NEON_HOST=... NEON_USER=... NEON_PASSWORD=... KAFKA_BOOTSTRAP_SERVERS=... KAFKA_USERNAME=... KAFKA_PASSWORD=... REDIS_HOST=... REDIS_PASSWORD=... GEMINI_API_KEY=...
fly deploy
fly open
```
Fly free allowance (2026): 3 × shared-cpu-1x 256 MB VMs, keeps the grouped image running without sleep.

> Pick **one** of Render or Fly — not both. Render is simpler; Fly sleeps less.

### 4.7 Vercel — dashboard (3 minutes, permanent free)

1. **vercel.com** → Add New Project → import same GitHub repo → set **Root Directory** to `dashboard`.
2. Build: `npm install` → `npm run build` → output `dist`.
3. **Environment Variables** → `VITE_API_URL=https://<render-or-fly-domain>` (no trailing slash).
4. **Deploy** → you get `https://eventflow-commerce.vercel.app`.

> `dashboard/nginx.conf` and `vite.config.ts` can proxy to the Render host if you prefer a server-side hop; for a portfolio demo setting `VITE_API_URL` is enough.

---

## 5. Verify end-to-end

```bash
BASE=https://<your-render-or-fly-domain>

# Create order
curl -X POST $BASE:8081/api/v1/orders -H 'Content-Type: application/json' \
  -d '{"customerId":"b1c2-0000-0000-0000-000000000001","customerName":"Demo","customerEmail":"demo@ex.com","items":[{"productId":"PROD-001","quantity":1,"price":10}],"shippingAddress":"Test"}'

# Follow saga (payments → inventory → shipping)
curl $BASE:8082/api/v1/payments/order/<orderId>
curl $BASE:8083/api/v1/products/PROD-001

# Incidents were auto-created from business-events topic
curl $BASE:8092/api/v1/incidents

# Trigger AI analysis (or view in dashboard)
curl -X POST $BASE:8093/api/v1/analyses/<incidentId>

# Similar incidents (vector search — now real TF-hash + cosine)
curl "$BASE:8091/api/v1/similar?limit=5&minSimilarity=0.7"
```

**Dashboard:** open `https://eventflow-commerce.vercel.app` → Observability page → confirm incidents + AI analysis. On Render Free the first load after 15 min idle will be slow (cold start) — refresh once.

---

## 6. Cost check — truly $0 forever

| Piece | Provider | Permanent free tier | Expected demo usage | Cost |
|-------|----------|---------------------|---------------------|------|
| Frontend | Vercel Hobby | 100 GB bandwidth/month | < 1 GB | **$0** |
| Backend (Java grouped) | Render Free (or Fly.io free) | 750 hrs/month (Render) / 3 VMs (Fly) | ~one 256-512 MB container | **$0** |
| Postgres | Neon | 0.5 GB storage, autosuspend | < 20 MB | **$0** |
| Kafka | Upstash Kafka | ~10k messages/day | ~100 msg/demo run | **$0** |
| Redis | Upstash Redis | ~10k commands/day | < 500 cmd/day | **$0** |
| Gemini | Google AI Studio | 15 RPM free | occasional | **$0** |
| Vectors | Embedded TF-hash (no host) | — | — | **$0** |

**No trial that expires. No card needed for Neon, Upstash, Gemini or Vercel Hobby.** Render Free asks for no card at signup; only if you upgrade. Keep demo traffic inside free quotas and it stays at **$0**.

---

## 7. Optional upgrades (when you outgrow free)

- Add **GitHub Actions** → auto-deploy to Render/Fly on push to `main`.
- Add **custom domain** via Vercel + Render custom domains (both free).
- Move **ChromaDB** to a Fly volume or Render disk if you want persisted vectors (optional; current `embedText` is host-free).

---

## 8. Troubleshooting

- **First request after idle slow:** Render Free sleeps after 15 min; Fly free does not. Set a 14-min **cron ping** (`curl https://<app>/health` via cron-job.org free) plus in-app `KEEPALIVE_ENABLED=true` (already wired) to keep it warm.
- **Flyway `validate failed` on Neon:** set `spring.flyway.baseline-on-migrate=true` (already in `application.yml`).
- **Kafka SASL login failed:** Upstash uses `SASL_SSL` + `PLAIN` — confirm the 3 vars are set exactly (already in `application.yml`).
- **Render OOM killed:** grouped containers use `JAVA_OPTS="-Xms48m -Xmx96m"` — if killed, split into two Render services (one for `core-group`, one for `ai-group`).
- **Incidents empty:** confirm `incident-detector` consumer group `incident-detector-group` subscribed to `orders,payments,inventory,shipments` and `KAFKA_BOOTSTRAP_SERVERS` reachable from Render/Fly egress.
- **Dashboard CORS:** backend already sets CORS in `application.yml`; verify `Access-Control-Allow-Origin: https://<vercel-domain>`.
- **Render port not reachable:** Render exposes one external port per service; internal 8081-8093 are still reachable among grouped jars. Expose via `$PORT` env and route through one port, or deploy as two services.

---

*Last updated: 2026-08-22 — Railway replaced with Render Free (permanent) + Fly.io alternative. All free tiers verified card-free.*
