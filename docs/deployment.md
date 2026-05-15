# Deployment Guide — URL Shortener

---

## Architecture Overview

```
Cloudflare CDN (free tier)
       ↓
NGINX (reverse proxy on VPS)
    ┌────┴────┐
api_service  redirect_service  ←-- Port 8080 / 8081
    |             |
PostgreSQL     Redis
(Supabase)    (self-hosted on VPS)
    |
Kafka (Aiven free trial)
    |
analytics_worker  ←-- Port 8082
```

---

## Prerequisites

- Docker & Docker Compose
- Java 25+ (for local development)
- Accounts: Supabase (free), Aiven (free trial), Cloudflare (free), Docker Hub (free)

---

## Local Development

### 1. Start Infrastructure

Each component starts independently:

```bash
# From docker/ directory
docker compose -f docker-compose.postgres.yml --env-file .env.dev up -d
docker compose -f docker-compose.redis.yml up -d
docker compose -f docker-compose.kafka.yml up -d
```

### 2. Run Services

Each service runs independently from its directory:

```bash
# services/api_service
./mvnw spring-boot:run

# services/redirect_service (separate terminal)
./mvnw spring-boot:run

# services/analytics_worker (separate terminal)
./mvnw spring-boot:run
```

Alternatively, use `TestApiServiceApplication.main()` in `api_service` for Testcontainers-managed infrastructure.

---

## Docker Build

Each service has its own `Dockerfile`. Build context must be `services/`:

```bash
# From services/ directory
docker build -f api_service/Dockerfile -t ssurl-api .
docker build -f redirect_service/Dockerfile -t ssurl-redirect .
docker build -f analytics_worker/Dockerfile -t ssurl-analytics .
```

Dockerfiles use multi-stage builds:
1. `eclipse-temurin:25-jre-noble` — extract Spring Boot layered JAR
2. `gcr.io/distroless/java25-debian13` — runtime image (non-root user `nonroot`)

---

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_URL` | Yes | — | R2DBC PostgreSQL URL |
| `DB_USERNAME` | Yes | — | Database user |
| `DB_PASSWORD` | Yes | — | Database password |
| `REDIS_HOST` | Yes | `localhost` | Redis host |
| `REDIS_PORT` | No | `6379` | Redis port |
| `REDIS_PASSWORD` | No | — | Redis password |
| `KAFKA_BOOTSTRAP_SERVERS` | Yes | — | Kafka broker(s) |
| `SCHEMA_REGISTRY_URL` | Yes | — | Confluent Schema Registry URL |
| `JWT_SECRET` | Yes | — | Base64-encoded HMAC-SHA key |
| `JWT_ACCESS_EXPIRY` | No | `900` | Access token TTL (seconds) |
| `JWT_REFRESH_EXPIRY` | No | `604800` | Refresh token TTL (seconds) |
| `SHORT_URL_BASE` | Yes | — | Base URL for short links |

---

## Database Setup

Schema files in `database/schemas/`:

1. `users.sql`
2. `urls.sql`
3. `refresh_tokens.sql`
4. `analytics.sql` (partitioned table)
5. `domain_blacklist.sql`

Seed data in `database/init-db/`:
- `domain_blacklist_001.sql` — 54 disposable email domains
- `domain_blacklist_002.sql` — 3600+ additional blacklisted domains

**Important:** Database schema is initialized by Docker Compose init scripts only — never by Spring Boot (no Flyway, Liquibase, or `spring.sql.init`).

---

## Infrastructure Services

### PostgreSQL (Supabase)

Managed PostgreSQL with free tier:
- 500 MB storage
- 5 users
- Auto-backups
- Connection pooling via PgBouncer

### Redis (Self-hosted on VPS)

Single-node Redis 7 with AOF persistence:
- Port 6379
- Password: set via `REDIS_PASSWORD`
- AOF enabled for durability

### Kafka (Aiven Free Trial)

1 topic (`analytics-events`), 3 partitions, replication factor 1 (single broker).
Schema Registry required for Avro serialization.

---

## CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/`):

```
Push to main → Run tests → Build Docker images → Push to Docker Hub → SSH deploy to VPS
```

### Secrets Required

| Secret | Description |
|---|---|
| `DOCKER_USERNAME` | Docker Hub username |
| `DOCKER_PASSWORD` | Docker Hub password/token |
| `VPS_HOST` | VPS IP address |
| `VPS_USER` | SSH username |
| `VPS_SSH_KEY` | SSH private key |

---

## VPS Setup

### 1. Install Docker

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

### 2. Create Directory

```bash
mkdir -p /opt/url-shortener
```

### 3. Copy docker-compose.yml and .env

### 4. Deploy

```bash
docker compose pull
docker compose up -d
```

---

## Monitoring

- **Health:** `/actuator/health` on each service
- **Metrics:** `/actuator/metrics` (Micrometer)
- **Logs:** Docker logs per service
- **Kafka:** Confluent Control Center (port 9021, local dev only)

---

## Performance Targets

| Metric | Target |
|---|---|
| Redirect (Cloudflare cache hit) | < 5 ms |
| Redirect (Redis cache hit) | < 10 ms |
| Redirect (DB fallback) | < 100 ms |
| URL creation throughput | > 500 req/s |
| Read throughput (single VPS) | > 1,000 req/s |

---

## Scaling

| Component | Strategy |
|---|---|
| Redirect Service | Horizontal replicas behind NGINX |
| API Service | Horizontal replicas behind NGINX |
| Redis | Single node → Sentinel → Cluster |
| PostgreSQL | Supabase upgrade (read replicas) |
| Kafka | Aiven paid plan (more partitions, RF > 1) |

---

## Known Issues

1. **Cache TTL vs expireAt mismatch** — Redis cache uses fixed 24h TTL regardless of URL `expireAt`. May serve expired URLs from cache. Fix: compute TTL as `min(24h, expireAt - now)`.
2. **Rate limiting not implemented** — `RateLimitExceededException` exists but no rate limiter code has been integrated.
3. **Refresh token expiry calculation** — `AuthServiceImpl.login()` uses `accessExpiryMs / 1000 * 800` instead of proper refresh expiry.
4. **Bloom filter blocks event loop** — `RBloomFilter.contains()` is synchronous, offloaded via `Schedulers.boundedElastic()`.
