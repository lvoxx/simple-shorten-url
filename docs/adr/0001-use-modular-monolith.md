# ADR 0001: Modular Monolith Architecture

**Status:** Accepted  
**Date:** 2025-01-01  
**Deciders:** Project Lead

## Context

The URL shortener needs three distinct concerns: a write-heavy API (auth, URL creation), a read-heavy redirect hot-path, and an async analytics consumer. Each has different scaling characteristics and latency requirements.

## Decision

Adopt a **modular monolith** — three independently deployable services sharing a common codebase and module:

- `api_service` (port 8080) — write path
- `redirect_service` (port 8081) — read/redirect path
- `analytics_worker` (port 8082) — async consumer

All services share the `common` module for models, DTOs, mappers, and exceptions. Infrastructure concerns are encapsulated in starters (`redis_starter`, `kafka_starter`, `postgres_starter`, `swagger_starter`, `message_starter`).

## Consequences

**Positive:**

- No microservice orchestration overhead (service discovery, inter-service HTTP calls)
- Shared domain models prevent drift
- Single deployment pipeline
- Each service can scale independently if needed

**Negative:**

- Cannot independently upgrade shared dependencies across services
- A shared model change requires rebuilding all services
- Not suitable for teams that need independent deploy cadences

**Rejected alternatives:**

- True microservices: too much operational overhead for a solo project
- Single monolithic JAR: cannot scale read/write paths independently
