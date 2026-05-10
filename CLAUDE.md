# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A URL shortener built as a modular monolith — three independently deployable Spring Boot services sharing a common Maven module. The system uses a **write/read/async** service split:

- `api_service` (port 8080) — URL creation, auth, user management
- `redirect_service` (port 8081) — short-code resolution and 302 redirect (hot path)
- `analytics_worker` (port 8082) — Kafka consumer for async click analytics

## Build Commands

Each service is built independently from its own directory (no root aggregator POM):

```bash
# From within a service directory (e.g., services/api_service)
./mvnw clean package           # build JAR
./mvnw test                    # run all tests
./mvnw test -Dgroups=unit      # run unit tests only
./mvnw test -Dgroups=integration  # run integration tests only
./mvnw test jacoco:report      # run tests with coverage report
```

For local development with live infrastructure, use `TestApiServiceApplication.main()` — it starts Postgres and Redis via Testcontainers automatically.

## Infrastructure (Local Dev)

Start each infrastructure component separately:

```bash
# From the docker/ directory
docker compose -f docker-compose.postgres.yml --env-file .env.dev up -d
docker compose -f docker-compose.redis.yml up -d
docker compose -f docker-compose.kafka.yml up -d
```

Ports: Postgres `5432`, pgAdmin `5050`, Redis `6379`, Kafka `9092`, Confluent Control Center `9021`.

Redis password (dev only): `12345678` (set in `docker/redis.conf`).

## Tech Stack

- **Java 25**, **Spring Boot 4.0.6**
- **Spring WebFlux** (reactive) — all services use `Mono`/`Flux` throughout
- **R2DBC + r2dbc-postgresql** — reactive DB access (no JPA/Hibernate)
- **Spring Data Redis Reactive** — caching and rate limiting
- **Apache Kafka** — async analytics event pipeline
- **Testcontainers** — Postgres + Redis spun up per test run

## Architecture & Data Flow

```
Client → Cloudflare CDN → NGINX
  → redirect_service: Bloom filter → Redis cache → Postgres → 302
  → api_service: REST API → Postgres + Redis → Kafka (analytics events)
                                                    ↓
                                           analytics_worker (batch insert)
```

**Redis cache keys:**
- `short:{code}` — original URL (24h TTL)
- `rate_limit:ip:{ip}`, `rate_limit:user:{id}` — sliding window counters
- `bloom:urls` — Redisson `RBloomFilter` (10M capacity, 1% false positive)
- `blacklist:domain:{domain}` — 10m TTL

**Short code generation:** BIGSERIAL → Base62 encode (0-9a-zA-Z). Implement `Base62Encoder` in `common` first — both `api_service` and `redirect_service` need it.

**Redirect strategy:** Always 302 (browser must recheck, enabling analytics). 301 only at Cloudflare edge.

**Analytics pipeline:** `api_service` produces to Kafka topic `analytics-events` (3 partitions). `analytics_worker` batch-consumes up to 500 events per poll.

## Module Map

```
services/
├── api_service/        # Write path — auth, URL CRUD, user mgmt
├── redirect_service/   # Read path — high-throughput redirect
├── analytics_worker/   # Async Kafka consumer
├── common/             # Shared DTOs, utilities (scaffold — nothing implemented yet)
└── starters/
    ├── kafka_starter/    # Kafka auto-config (scaffold)
    ├── postgres_starter/ # R2DBC auto-config (scaffold)
    └── redis_starter/    # Redis Reactive auto-config (scaffold)
```

The starters and `common` are **scaffolds only** — no shared code exists yet. They are also structured as `@SpringBootApplication` apps rather than library JARs, and are not yet listed as Maven dependencies in the main services.

## Database Schema (database/*.sql)

Five tables: `users`, `urls`, `refresh_tokens`, `analytics`, `domain_blacklist`.

Notable design choices:
- `analytics` is partitioned by `RANGE (created_at)` monthly — no FK to `urls` (intentional, performance)
- `urls.user_id` cascades to `NULL` on user delete (URLs survive)
- `refresh_tokens.user_id` cascades `DELETE` (tokens purged with user)

**Known SQL bug:** `database/urls.sql` is missing commas before `created_by` and `updated_by` column definitions — fix before running migrations.

## Environment Variables

All runtime config is injected via env vars (no hardcoded values in `application.properties`):

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Postgres connection |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis connection |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers |
| `JWT_SECRET`, `JWT_ACCESS_EXPIRY` (900s), `JWT_REFRESH_EXPIRY` (604800s) | Auth tokens |
| `SHORT_URL_BASE` | Base URL for generated short links |

JWT refresh tokens are stored in HTTP-only cookies (XSS protection); access tokens in-memory only.

## Layered Package Structure

Follow this layout inside each service:

```
io.lvoxx.ssurl.<service>/
├── controller/     # @RestController or RouterFunction
├── service/        # Business logic (interfaces + impl)
├── repository/     # ReactiveCrudRepository extensions
├── config/         # @Configuration beans
├── domain/         # Entity / aggregate classes
└── dto/
    ├── request/
    └── response/
```

## Known Issues

1. **Invalid test dependency IDs** in `services/api_service/pom.xml` — `spring-boot-starter-data-redis-reactive-test`, `-validation-test`, `-webflux-test` don't exist. Replace with `spring-boot-starter-test`.
2. **Starters need restructuring** — remove `spring-boot-maven-plugin` executable JAR packaging; add as `<dependency>` entries in consuming services before they can be shared.
3. **SQL syntax error** in `database/urls.sql` — missing commas before `created_by`/`updated_by`.

## Reference

Full API spec, flow diagrams, rate-limiting details, and deployment configuration are in [`url-shortener-technical-docs.md`](url-shortener-technical-docs.md).
