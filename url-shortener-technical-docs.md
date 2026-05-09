# URL Shortener System — Full Technical Documentation

**Version:** 1.0.0  
**Status:** Pre-Implementation  
**Target:** Mid-Level Backend Portfolio Project  
**Stack:** Spring Boot · Redis · PostgreSQL (Supabase) · Kafka (Aiven) · NGINX · Cloudflare  

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture Overview](#2-architecture-overview)
3. [Module Breakdown](#3-module-breakdown)
4. [Database Design](#4-database-design)
5. [API Specification](#5-api-specification)
6. [Core Flows (Sequence Diagrams)](#6-core-flows)
7. [Caching Strategy](#7-caching-strategy)
8. [Rate Limiting](#8-rate-limiting)
9. [Bloom Filter](#9-bloom-filter)
10. [Authentication & Authorization](#10-authentication--authorization)
11. [Analytics Pipeline](#11-analytics-pipeline)
12. [Anti-Spam & Abuse Detection](#12-anti-spam--abuse-detection)
13. [CDN & Edge Caching](#13-cdn--edge-caching)
14. [Infrastructure & Deployment](#14-infrastructure--deployment)
15. [CI/CD Pipeline](#15-cicd-pipeline)
16. [Error Handling & Resilience](#16-error-handling--resilience)
17. [Configuration Reference](#17-configuration-reference)
18. [Scaling Strategy](#18-scaling-strategy)
19. [Trade-offs & Decisions](#19-trade-offs--decisions)
20. [Development Roadmap](#20-development-roadmap)
21. [Glossary](#21-glossary)

---

## 1. Project Overview

### 1.1 Purpose

A production-oriented URL shortener service designed to handle high read throughput with low-latency redirects. Built as a solo mid-level backend project demonstrating real-world system design skills under budget constraints.

### 1.2 Functional Requirements

| ID   | Requirement                                           | Priority |
|------|-------------------------------------------------------|----------|
| F-01 | Shorten a long URL to a unique short code             | Must     |
| F-02 | Redirect short code to original URL                   | Must     |
| F-03 | User registration and login                           | Must     |
| F-04 | User can manage (view/delete) their own short URLs    | Must     |
| F-05 | URL expiration (optional TTL per link)                | Should   |
| F-06 | Click analytics (timestamp, IP, user-agent)           | Should   |
| F-07 | Domain blacklist / spam detection                     | Should   |
| F-08 | Rate limiting per IP and per user                     | Must     |
| F-09 | Custom short code alias (vanity URL)                  | Could    |
| F-10 | QR code generation for short URLs                     | Could    |

### 1.3 Non-Functional Requirements

| ID    | Requirement                                          | Target                |
|-------|------------------------------------------------------|-----------------------|
| NF-01 | Redirect latency (cache hit)                         | < 10 ms               |
| NF-02 | Redirect latency (cache miss)                        | < 100 ms              |
| NF-03 | API availability                                     | 99.5% uptime          |
| NF-04 | Read throughput                                      | ≥ 1,000 req/s (VPS)   |
| NF-05 | Infrastructure cost                                  | Near-zero / free tier |
| NF-06 | Horizontal stateless scaling                         | Required              |
| NF-07 | Async analytics (no impact on redirect latency)      | Required              |

---

## 2. Architecture Overview

### 2.1 High-Level Diagram

```
                          ┌─────────────────────────┐
                          │      Cloudflare CDN      │
                          │  (Edge caching 301/302)  │
                          └────────────┬────────────┘
                                       │
                          ┌────────────▼────────────┐
                          │          NGINX           │
                          │   Reverse Proxy / LB     │
                          └────┬──────────────┬──────┘
                               │              │
              ┌────────────────▼──┐      ┌────▼──────────────────┐
              │    API Service    │      │   Redirect Service     │
              │   (Spring Boot)   │      │    (Spring Boot)       │
              │  - Auth           │      │  - High-perf redirect  │
              │  - URL creation   │      │  - Bloom filter check  │
              │  - User mgmt      │      │  - Cache lookup        │
              └────────┬──────────┘      └──────────┬────────────┘
                       │                            │
              ┌────────▼────────────────────────────▼────────┐
              │                   Redis                       │
              │  Cache │ Rate Limiter │ Bloom Filter          │
              └────────┬─────────────────────────────────────┘
                       │
              ┌────────▼────────────────┐
              │  PostgreSQL (Supabase)  │
              │  Primary data store     │
              └────────┬────────────────┘
                       │
              ┌────────▼────────────────┐
              │   Apache Kafka (Aiven)  │
              │   Analytics events      │
              └────────┬────────────────┘
                       │
              ┌────────▼────────────────┐
              │   Analytics Worker      │
              │   (Spring Boot)         │
              └─────────────────────────┘
```

### 2.2 Architecture Style: Modular Monolith

The system is a **modular monolith** deployed as separate logical services (API, Redirect, Analytics Worker) but sharing the same codebase and deployment pipeline. This avoids microservice overhead while keeping concerns separated.

```
url-shortener/
├── api-service/         ← Write path (auth, URL creation)
├── redirect-service/    ← Read path (hot path, redirect only)
├── analytics-worker/    ← Async consumer (Kafka)
└── shared/              ← DTOs, utilities, domain models
```

### 2.3 Service Responsibilities

| Service            | Role                                        | Port   |
|--------------------|---------------------------------------------|--------|
| API Service        | URL creation, user auth, management         | 8080   |
| Redirect Service   | Short-code resolution and HTTP redirect     | 8081   |
| Analytics Worker   | Kafka consumer, batch analytics insert      | 8082   |
| Redis              | Cache, rate limiter, bloom filter           | 6379   |
| PostgreSQL         | Persistent storage (via Supabase)           | 5432   |
| Kafka (Aiven)      | Analytics event stream                      | 9092   |
| NGINX              | Reverse proxy, routing, SSL termination     | 80/443 |

---

## 3. Module Breakdown

### 3.1 API Service (`api-service`)

```
api-service/
├── controller/
│   ├── AuthController.java       ← /api/v1/auth/**
│   ├── UrlController.java        ← /api/v1/urls/**
│   └── UserController.java       ← /api/v1/users/**
├── service/
│   ├── AuthService.java
│   ├── UrlService.java
│   ├── UserService.java
│   └── SpamDetectionService.java
├── repository/
│   ├── UrlRepository.java
│   └── UserRepository.java
├── config/
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   └── KafkaProducerConfig.java
├── domain/
│   ├── Url.java
│   └── User.java
└── dto/
    ├── request/
    └── response/
```

### 3.2 Redirect Service (`redirect-service`)

```
redirect-service/
├── controller/
│   └── RedirectController.java   ← /{shortCode}
├── service/
│   ├── RedirectService.java
│   ├── CacheService.java
│   └── BloomFilterService.java
├── publisher/
│   └── AnalyticsEventPublisher.java
└── config/
    ├── RedisConfig.java
    └── KafkaProducerConfig.java
```

### 3.3 Analytics Worker (`analytics-worker`)

```
analytics-worker/
├── consumer/
│   └── AnalyticsEventConsumer.java
├── service/
│   └── AnalyticsPersistenceService.java
├── repository/
│   └── AnalyticsRepository.java
└── config/
    └── KafkaConsumerConfig.java
```

### 3.4 Shared Module (`shared`)

```
shared/
├── dto/
│   ├── AnalyticsEvent.java
│   ├── UrlCreateRequest.java
│   └── UrlResponse.java
├── util/
│   ├── Base62Encoder.java
│   └── ShortCodeGenerator.java
└── exception/
    ├── ShortCodeNotFoundException.java
    ├── UrlExpiredException.java
    └── RateLimitExceededException.java
```

---

## 4. Database Design

### 4.1 PostgreSQL (Supabase)

#### Table: `users`

```sql
CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) UNIQUE NOT NULL,
  username      VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(20) NOT NULL DEFAULT 'USER',  -- USER | ADMIN
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
```

#### Table: `urls`

```sql
CREATE TABLE urls (
  id            BIGSERIAL PRIMARY KEY,
  short_code    VARCHAR(10) UNIQUE NOT NULL,
  original_url  TEXT NOT NULL,
  user_id       BIGINT REFERENCES users(id) ON DELETE SET NULL,
  title         VARCHAR(255),
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  click_count   BIGINT NOT NULL DEFAULT 0,
  expire_at     TIMESTAMP,
  created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by     VARCHAR(255)  -- optional, for tracking who created the short URL
  updated_by     VARCHAR(255)  -- optional, for tracking who last updated the short URL
);

CREATE UNIQUE INDEX idx_urls_short_code ON urls(short_code);
CREATE INDEX idx_urls_user_id ON urls(user_id);
CREATE INDEX idx_urls_expire_at ON urls(expire_at) WHERE expire_at IS NOT NULL;
```

#### Table: `refresh_tokens`

```sql
CREATE TABLE refresh_tokens (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token       VARCHAR(512) UNIQUE NOT NULL,
  expires_at  TIMESTAMP NOT NULL,
  is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

#### Table: `analytics`

```sql
CREATE TABLE analytics (
  id          BIGSERIAL,
  short_code  VARCHAR(10) NOT NULL,
  ip          INET,
  user_agent  TEXT,
  referer     TEXT,
  country     VARCHAR(10),  -- from IP geo-lookup (optional)
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- Monthly partitions (example)
CREATE TABLE analytics_2024_01 PARTITION OF analytics
  FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE INDEX idx_analytics_short_code ON analytics(short_code);
CREATE INDEX idx_analytics_created_at ON analytics(created_at);
```

> **Note:** Analytics table is partitioned by month to manage row growth and enable efficient pruning of old data.

#### Table: `domain_blacklist`

```sql
CREATE TABLE domain_blacklist (
  id          BIGSERIAL PRIMARY KEY,
  domain      VARCHAR(255) UNIQUE NOT NULL,
  reason      VARCHAR(255),
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_domain_blacklist_domain ON domain_blacklist(domain);
```

### 4.2 Entity Relationships

```
users (1) ──── (N) urls
users (1) ──── (N) refresh_tokens
urls   ──────────── analytics  (by short_code, no FK for performance)
```

### 4.3 ID Generation Strategy

```
Auto-increment BIGSERIAL ID → Base62 Encode → Short Code

Example:
  ID: 1_000_000
  Base62: "4c92"   (4 chars)

  ID: 3_521_614_606_208
  Base62: "zzzzzz" (6 chars at maximum)
```

**Base62 Alphabet:**
```
0-9 a-z A-Z
```

**Implementation:**
```java
public class Base62Encoder {
    private static final String ALPHABET =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String encode(long id) {
        if (id == 0) return String.valueOf(ALPHABET.charAt(0));
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(ALPHABET.charAt((int)(id % 62)));
            id /= 62;
        }
        return sb.reverse().toString();
    }
}
```

> **Why not UUID?** UUIDs are 36 chars and not human-friendly. Base62 from auto-increment gives short (4-7 char), unique, URL-safe codes with predictable length growth.

---

## 5. API Specification

**Base URL:** `https://api.yourdomain.com/api/v1`  
**Content-Type:** `application/json`  
**Authentication:** Bearer JWT (except public endpoints)

---

### 5.1 Authentication Endpoints

#### `POST /auth/register`

Register a new user.

**Request:**
```json
{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "Str0ngP@ss!"
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**Validation Rules:**
- `email`: valid format, max 255 chars, unique
- `username`: 3–50 chars, alphanumeric + underscore
- `password`: min 8 chars, at least one uppercase, one digit

---

#### `POST /auth/login`

Authenticate and receive tokens.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "Str0ngP@ss!"
}
```

**Response `200 OK`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "d3a4f5b6...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

---

#### `POST /auth/refresh`

Exchange refresh token for a new access token.

**Request:**
```json
{
  "refreshToken": "d3a4f5b6..."
}
```

**Response `200 OK`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 900
}
```

---

#### `POST /auth/logout`

Revoke current refresh token. Requires `Authorization: Bearer <token>`.

**Response `204 No Content`**

---

### 5.2 URL Endpoints

#### `POST /urls` *(authenticated)*

Create a new short URL.

**Request:**
```json
{
  "originalUrl": "https://www.example.com/very/long/path?with=params",
  "alias": "my-link",        // optional custom alias
  "expireAt": "2025-01-01T00:00:00Z"  // optional
}
```

**Response `201 Created`:**
```json
{
  "id": 101,
  "shortCode": "4c92",
  "shortUrl": "https://yourdomain.com/4c92",
  "originalUrl": "https://www.example.com/very/long/path?with=params",
  "alias": null,
  "expireAt": null,
  "clickCount": 0,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**Error Responses:**
| Status | Code                   | Reason                              |
|--------|------------------------|-------------------------------------|
| 400    | INVALID_URL            | Malformed URL                       |
| 400    | BLACKLISTED_DOMAIN     | Domain is on blocklist              |
| 409    | ALIAS_ALREADY_EXISTS   | Custom alias is taken               |
| 429    | RATE_LIMIT_EXCEEDED    | Too many requests                   |

---

#### `GET /urls` *(authenticated)*

List all URLs created by the authenticated user.

**Query Params:**
- `page` (default: 0)
- `size` (default: 20, max: 100)
- `sort` (default: `createdAt,desc`)

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": 101,
      "shortCode": "4c92",
      "shortUrl": "https://yourdomain.com/4c92",
      "originalUrl": "https://example.com/...",
      "clickCount": 1500,
      "createdAt": "2024-01-01T00:00:00Z",
      "expireAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

#### `GET /urls/{shortCode}` *(authenticated)*

Get metadata for a specific short URL.

**Response `200 OK`:** *(same as single URL object above)*

---

#### `DELETE /urls/{shortCode}` *(authenticated)*

Soft-delete (deactivate) a short URL. Sets `is_active = false`.

**Response `204 No Content`**

---

#### `GET /urls/{shortCode}/analytics` *(authenticated)*

Get click analytics for a specific short URL.

**Query Params:**
- `from` (ISO date, optional)
- `to` (ISO date, optional)
- `groupBy`: `hour` | `day` | `month` (default: `day`)

**Response `200 OK`:**
```json
{
  "shortCode": "4c92",
  "totalClicks": 1500,
  "period": { "from": "2024-01-01", "to": "2024-01-31" },
  "series": [
    { "date": "2024-01-01", "clicks": 120 },
    { "date": "2024-01-02", "clicks": 95 }
  ],
  "topCountries": [
    { "country": "US", "clicks": 800 }
  ]
}
```

---

### 5.3 Redirect Endpoint

#### `GET /{shortCode}` *(public, served by Redirect Service)*

Resolve a short code and redirect.

**Success:** `302 Found` with `Location: <originalUrl>` header

**Error Responses:**
| Status | Reason                                      |
|--------|---------------------------------------------|
| 404    | Short code not found or not in bloom filter |
| 410    | URL has expired (`expire_at` in the past)   |

> **Note:** Use `302 Found` (not 301) for analytics accuracy. 301 is cached by browsers and skips your server, preventing click tracking. Use 301 only for Cloudflare edge caching layer.

---

### 5.4 Health Endpoints

```
GET /actuator/health         → 200 { "status": "UP" }
GET /actuator/health/redis   → Redis connectivity
GET /actuator/health/db      → DB connectivity
```

---

### 5.5 Response Envelope

**Success:**
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

**Error:**
```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Try again in 30 seconds.",
    "details": null
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## 6. Core Flows

### 6.1 Create Short URL Flow

```
Client                 API Service          Redis           PostgreSQL
  │                        │                  │                  │
  │─── POST /urls ─────────▶│                  │                  │
  │                        │                  │                  │
  │                        │── rate_limit ────▶│                  │
  │                        │◀─ OK / BLOCKED ───│                  │
  │                        │                  │                  │
  │                        │── validate URL    │                  │
  │                        │── check blacklist │                  │
  │                        │                  │                  │
  │                        │─────────────── INSERT url ──────────▶│
  │                        │◀───────────── id (auto-inc) ─────────│
  │                        │                  │                  │
  │                        │── Base62(id) → shortCode             │
  │                        │                  │                  │
  │                        │── SET short:{code} ▶│                │
  │                        │                  │                  │
  │◀─── 201 { shortUrl } ──│                  │                  │
```

### 6.2 Redirect Flow (Cache Hit)

```
User         Cloudflare       NGINX        Redirect Svc       Redis
  │               │              │               │               │
  │─GET /{code}──▶│              │               │               │
  │               │ (Cache HIT)  │               │               │
  │◀── 301 ───────│              │               │               │
  │  (≈ 5ms edge response)       │               │               │
```

### 6.3 Redirect Flow (Cache Miss → DB)

```
User      NGINX     Redirect Svc     Bloom Filter    Redis       DB        Kafka
  │         │            │                │             │          │          │
  │─GET /{c}▶│            │                │             │          │          │
  │         │──────────▶ │                │             │          │          │
  │         │            │── check ───────▶│             │          │          │
  │         │            │◀─ "possibly present"          │          │          │
  │         │            │                │             │          │          │
  │         │            │── GET short:{c} ────────────▶│          │          │
  │         │            │◀─ nil ──────────────────────│           │          │
  │         │            │                             │           │          │
  │         │            │─────────────────── SELECT ──────────────▶│         │
  │         │            │◀──────────────────────────────────────── │(url row) │
  │         │            │                             │           │          │
  │         │            │── SET short:{c} ───────────▶│           │          │
  │         │            │                             │           │          │
  │         │            │── publish AnalyticsEvent ───────────────────────── ▶│
  │         │            │                             │           │          │
  │◀─ 302 ──│            │                             │           │          │
```

### 6.4 Analytics Flow (Async)

```
Redirect Svc     Kafka Topic        Analytics Worker       PostgreSQL
     │        (analytics-events)          │                     │
     │                │                  │                     │
     │── publish ────▶│                  │                     │
     │                │── consume ───────▶│                     │
     │                │                  │── batch accumulate   │
     │                │                  │── (100 events)       │
     │                │                  │── INSERT batch ─────▶│
     │                │                  │◀─ OK ────────────────│
```

---

## 7. Caching Strategy

### 7.1 Cache-First Pattern

```
1. Check Bloom Filter  → if absent, return 404 immediately
2. Check Redis         → if hit, redirect
3. Check PostgreSQL    → if hit, populate Redis, redirect
4. Not found           → return 404
```

### 7.2 Redis Key Schema

| Key                      | Type   | Value          | TTL           |
|--------------------------|--------|----------------|---------------|
| `short:{code}`           | String | `original_url` | 24h (default) |
| `rate_limit:ip:{ip}`     | String | request count  | 60s window    |
| `rate_limit:user:{id}`   | String | request count  | 60s window    |
| `bloom:urls`             | String | Bloom filter   | No TTL        |
| `session:refresh:{token}`| String | user_id        | 7d            |

### 7.3 TTL Strategy

```
URL with no expiration  →  Cache TTL: 24 hours (rolling)
URL with expiration     →  Cache TTL: min(24h, time_until_expiry)
Expired URLs            →  Not cached; return 410
```

### 7.4 Cache Invalidation

- **On URL deletion:** `DEL short:{code}`
- **On URL deactivation:** `DEL short:{code}`
- **On URL expiration:** Expired check in Redirect Service; no cache write

### 7.5 Cache Warming (Optional Phase 3)

On startup, pre-load top-N most-clicked URLs into Redis from PostgreSQL using:
```sql
SELECT short_code, original_url FROM urls
ORDER BY click_count DESC
LIMIT 1000;
```

---

## 8. Rate Limiting

### 8.1 Strategy: Sliding Window Counter (Redis)

```
Key:    rate_limit:ip:{ip_address}
Value:  INCR count per minute window
Expire: 60 seconds (reset window)
```

### 8.2 Limits

| Context              | Limit         | Window |
|----------------------|---------------|--------|
| Anonymous (by IP)    | 30 req        | 1 min  |
| Authenticated user   | 100 req       | 1 min  |
| URL creation (auth)  | 20 URLs       | 1 min  |
| Redirect (by IP)     | 200 redirects | 1 min  |

### 8.3 Implementation

```java
// Redis Lua script for atomic sliding window
String luaScript = """
    local current = redis.call('INCR', KEYS[1])
    if current == 1 then
        redis.call('EXPIRE', KEYS[1], ARGV[1])
    end
    return current
""";
```

### 8.4 Response Headers

Every API response includes:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1704067260
```

### 8.5 Rate Limit Exceeded Response

```
HTTP 429 Too Many Requests
Retry-After: 42

{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Retry after 42 seconds."
  }
}
```

---

## 9. Bloom Filter

### 9.1 Purpose

Reject requests for non-existent short codes **before** touching Redis or PostgreSQL. A Bloom filter can answer "definitely not in set" or "probably in set" in O(1) with minimal memory.

### 9.2 Configuration

| Parameter         | Value                     |
|-------------------|---------------------------|
| Expected elements | 10,000,000 (10M URLs)     |
| False positive %  | 1%                        |
| Memory usage      | ~11.4 MB                  |
| Library           | `Redisson` (Redis-backed) |

### 9.3 Flow

```
Incoming {shortCode}
        │
        ▼
  Bloom filter check
   ┌────┴────┐
   │ ABSENT  │  → Return 404 immediately (no DB hit)
   └─────────┘
   │ PRESENT │  → Continue to Redis / DB lookup
   └─────────┘
```

### 9.4 Maintenance

- **Add entry:** When a new short URL is created, add `shortCode` to bloom filter.
- **Rebuild:** Periodic full rebuild from DB (weekly) to eliminate accumulated false positives from deleted entries. Deletion is not supported in standard bloom filters.

### 9.5 Redisson Setup

```java
@Bean
public RBloomFilter<String> urlBloomFilter(RedissonClient redisson) {
    RBloomFilter<String> filter = redisson.getBloomFilter("bloom:urls");
    filter.tryInit(10_000_000L, 0.01);  // 10M items, 1% FP rate
    return filter;
}
```

---

## 10. Authentication & Authorization

### 10.1 JWT Token Design

| Token         | Lifetime | Storage          |
|---------------|----------|------------------|
| Access Token  | 15 min   | Memory (client)  |
| Refresh Token | 7 days   | HTTP-only cookie |

**Access Token Payload:**
```json
{
  "sub": "1",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1704067200,
  "exp": 1704068100
}
```

### 10.2 Token Refresh Flow

```
Client             API Service            DB
  │                     │                  │
  │── POST /auth/refresh ▶│                 │
  │                     │── validate refresh token        │
  │                     │── check is_revoked ─────────────▶│
  │                     │◀─ valid ─────────────────────────│
  │                     │── generate new access token      │
  │◀── 200 { accessToken }│                 │
```

### 10.3 Security Configuration (Spring Security)

```java
// Public routes
.requestMatchers("/api/v1/auth/**").permitAll()
.requestMatchers(HttpMethod.GET, "/{shortCode}").permitAll()
.requestMatchers("/actuator/health").permitAll()

// Authenticated routes
.anyRequest().authenticated()
```

### 10.4 RBAC Roles

| Role  | Permissions                            |
|-------|----------------------------------------|
| USER  | CRUD own URLs, view own analytics      |
| ADMIN | All URLs, manage blacklist, view stats |

### 10.5 Password Hashing

Use **BCrypt** with cost factor 12:
```java
PasswordEncoder encoder = new BCryptPasswordEncoder(12);
```

---

## 11. Analytics Pipeline

### 11.1 Kafka Topic

| Property      | Value                         |
|---------------|-------------------------------|
| Topic name    | `analytics-events`            |
| Partitions    | 3                             |
| Replication   | 1 (Aiven free tier)           |
| Retention     | 7 days                        |
| Key           | `shortCode` (for ordering)    |

### 11.2 Event Schema

```java
// AnalyticsEvent.java (Kafka message)
public record AnalyticsEvent(
    String shortCode,
    String ip,
    String userAgent,
    String referer,
    Instant timestamp
) {}
```

**Serialization:** JSON (Jackson)

### 11.3 Producer Configuration

```yaml
spring.kafka:
  producer:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    acks: 1            # Leader ACK only (balance durability vs speed)
    retries: 3
    batch-size: 16384  # 16KB batch
    linger-ms: 5       # Wait 5ms to batch
```

### 11.4 Consumer Configuration

```yaml
spring.kafka:
  consumer:
    group-id: analytics-worker-group
    auto-offset-reset: earliest
    enable-auto-commit: false
    max-poll-records: 500
```

### 11.5 Batch Insert (Analytics Worker)

```java
@KafkaListener(topics = "analytics-events", containerFactory = "batchFactory")
public void consume(List<AnalyticsEvent> events) {
    // Accumulate up to 500 events, then batch insert
    analyticsRepository.batchInsert(events);
}
```

This avoids single-row inserts and dramatically improves DB write throughput.

---

## 12. Anti-Spam & Abuse Detection

### 12.1 URL Validation

Validation happens synchronously on `POST /urls` before DB write.

```java
// Checks:
// 1. Valid URI format (java.net.URI)
// 2. Allowed schemes: http, https only
// 3. Has host (no bare IPs unless allowed)
// 4. URL length: max 2048 chars
// 5. No private IP ranges (10.x, 192.168.x, 127.x)
```

### 12.2 Domain Blacklist Check

```java
// Extract domain from URL
String domain = URI.create(originalUrl).getHost();

// Check DB blacklist table (cached in Redis for 10 min)
boolean blocked = domainBlacklistService.isBlocked(domain);
```

**Redis key:** `blacklist:domain:{domain}` → `"1"` (exists = blocked), TTL 10 min

### 12.3 Behavioral Abuse Detection

Tracked via Redis counters (decay per hour):

| Signal                          | Key                             | Threshold        |
|---------------------------------|---------------------------------|------------------|
| URL creation count per IP/hr    | `abuse:create:ip:{ip}`          | > 50 → flag      |
| Same domain repeated by one IP  | `abuse:domain:ip:{ip}:{domain}` | > 10 → flag      |
| Suspicious User-Agent           | Pattern match list              | Block immediately |

### 12.4 Optional: Google Safe Browsing API

```java
// Phase 3 enhancement
// POST https://safebrowsing.googleapis.com/v4/threatMatches:find
// Check URL against known malware/phishing lists
```

### 12.5 Action on Detection

| Severity | Action                                   |
|----------|------------------------------------------|
| Low      | Log warning, continue                    |
| Medium   | Rate limit tightened (10 req/min)        |
| High     | Block IP for 1 hour in Redis             |
| Critical | Block IP permanently + alert admin       |

---

## 13. CDN & Edge Caching

### 13.1 Cloudflare Setup

**DNS:** Proxy all traffic through Cloudflare (orange cloud).

**Cache Rule for Redirects:**
- URL pattern: `yourdomain.com/*` (short code paths)
- Cache level: Cache Everything
- Edge TTL: 1 hour (for 301 responses)

> **Important:** Redirect Service returns `302` for analytics tracking. Cloudflare is configured to **also** cache 302 responses for popular short codes via a Page Rule.

### 13.2 Cache-Control Headers

```java
// In RedirectController.java
response.setHeader("Cache-Control", "public, max-age=3600");
response.setHeader("Vary", "Accept-Encoding");
```

### 13.3 Cache Invalidation at Edge

When a URL is deactivated or deleted:
1. Set `is_active = false` in DB
2. Delete from Redis: `DEL short:{code}`
3. Call Cloudflare Cache Purge API:
```
POST https://api.cloudflare.com/client/v4/zones/{zone_id}/purge_cache
{ "files": ["https://yourdomain.com/{shortCode}"] }
```

### 13.4 Security Headers (via Cloudflare)

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
```

---

## 14. Infrastructure & Deployment

### 14.1 Target Infrastructure

| Component      | Provider              | Cost       |
|----------------|-----------------------|------------|
| App Server     | VPS (Hetzner/DigitalOcean) | ~$5–10/mo |
| PostgreSQL     | Supabase (free tier)  | $0         |
| Redis          | Self-hosted on VPS    | $0         |
| Kafka          | Aiven (free trial)    | $0         |
| CDN / DNS      | Cloudflare (free)     | $0         |
| CI/CD          | GitHub Actions        | $0         |
| Docker Registry| Docker Hub (free)     | $0         |

### 14.2 Docker Compose (Local / VPS)

```yaml
version: '3.9'

services:
  api-service:
    image: yourrepo/api-service:latest
    ports: ["8080:8080"]
    environment:
      - DB_URL=${DB_URL}
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP=${KAFKA_BOOTSTRAP}
      - JWT_SECRET=${JWT_SECRET}
    depends_on: [redis]

  redirect-service:
    image: yourrepo/redirect-service:latest
    ports: ["8081:8081"]
    environment:
      - DB_URL=${DB_URL}
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP=${KAFKA_BOOTSTRAP}
    depends_on: [redis]

  analytics-worker:
    image: yourrepo/analytics-worker:latest
    ports: ["8082:8082"]
    environment:
      - DB_URL=${DB_URL}
      - KAFKA_BOOTSTRAP=${KAFKA_BOOTSTRAP}

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes

  nginx:
    image: nginx:alpine
    ports: ["80:80", "443:443"]
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/certs:/etc/nginx/certs:ro
    depends_on: [api-service, redirect-service]

volumes:
  redis-data:
```

### 14.3 NGINX Configuration

```nginx
upstream api_backend {
    server api-service:8080;
}

upstream redirect_backend {
    server redirect-service:8081;
}

server {
    listen 443 ssl;
    server_name yourdomain.com;

    # Short code redirect — goes to redirect service
    location ~* ^/([a-zA-Z0-9]{4,10})$ {
        proxy_pass http://redirect_backend;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # API calls
    location /api/ {
        proxy_pass http://api_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Health checks
    location /actuator/health {
        proxy_pass http://api_backend;
    }
}
```

### 14.4 Environment Variables

```env
# Database
DB_URL=jdbc:postgresql://db.supabase.co:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=***

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=***

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka.aiven.io:12345
KAFKA_SSL_TRUSTSTORE_LOCATION=/certs/truststore.jks
KAFKA_SSL_TRUSTSTORE_PASSWORD=***

# JWT
JWT_SECRET=***
JWT_ACCESS_EXPIRY=900       # 15 min in seconds
JWT_REFRESH_EXPIRY=604800   # 7 days in seconds

# Cloudflare
CF_ZONE_ID=***
CF_API_TOKEN=***

# App
SHORT_URL_BASE=https://yourdomain.com
```

---

## 15. CI/CD Pipeline

### 15.1 GitHub Actions Workflow

```yaml
name: Build and Deploy

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run Tests
        run: ./mvnw test

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker Images
        run: |
          docker build -t yourrepo/api-service:${{ github.sha }} ./api-service
          docker build -t yourrepo/redirect-service:${{ github.sha }} ./redirect-service
          docker build -t yourrepo/analytics-worker:${{ github.sha }} ./analytics-worker
      - name: Push to Docker Hub
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push yourrepo/api-service:${{ github.sha }}
          docker push yourrepo/redirect-service:${{ github.sha }}
          docker push yourrepo/analytics-worker:${{ github.sha }}

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to VPS
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            cd /opt/url-shortener
            export IMAGE_TAG=${{ github.sha }}
            docker compose pull
            docker compose up -d --no-build
            docker system prune -f
```

### 15.2 Pipeline Stages

```
Push to main
    │
    ▼
┌─────────────────────┐
│ 1. Run unit tests   │ ← Fail fast
│    + integration    │
└──────────┬──────────┘
           │ Pass
           ▼
┌─────────────────────┐
│ 2. Build Docker     │
│    images           │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ 3. Push to          │
│    Docker Hub       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ 4. SSH → VPS        │
│    docker compose   │
│    up -d            │
└─────────────────────┘
```

### 15.3 Testing Strategy

| Layer             | Framework            | Coverage Target |
|-------------------|----------------------|-----------------|
| Unit tests        | JUnit 5 + Mockito    | 80%             |
| Integration tests | Spring Boot Test     | Key flows       |
| Redis tests       | Testcontainers       | Cache + RL      |
| DB tests          | Testcontainers + PG  | Repository layer|
| API tests         | MockMvc / REST Assured| All endpoints  |

---

## 16. Error Handling & Resilience

### 16.1 Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ShortCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(...)  // 404

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpired(...)   // 410

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(...) // 429

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(...) // 400
}
```

### 16.2 Redis Failure Mode

If Redis is unavailable, the system degrades gracefully:
- **Cache miss** → fall through to DB (higher latency, not outage)
- **Rate limiter** → fail open (allow request) with warning log
- **Bloom filter** → skip check, go straight to DB

```java
try {
    return redisCache.get(shortCode);
} catch (RedisException e) {
    log.warn("Redis unavailable, falling back to DB");
    return urlRepository.findByShortCode(shortCode);
}
```

### 16.3 Kafka Failure Mode

If Kafka is unavailable, analytics events are:
1. Logged to application log (recoverable)
2. Redirect still completes (analytics non-critical path)
3. Optional: local in-memory queue with retry

### 16.4 Circuit Breaker (Phase 3)

Use **Resilience4j** for DB and Kafka:
```java
@CircuitBreaker(name = "database", fallbackMethod = "fallback")
public Url findByShortCode(String code) { ... }
```

---

## 17. Configuration Reference

### 17.1 `application.yml` (Redirect Service)

```yaml
server:
  port: 8081

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 3000

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: 20
          min-idle: 5

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    producer:
      acks: 1
      retries: 3

app:
  bloom-filter:
    expected-insertions: 10000000
    false-positive-probability: 0.01
  cache:
    default-ttl-hours: 24
  short-url-base: ${SHORT_URL_BASE}
```

### 17.2 JVM Tuning (Redirect Service)

```bash
# Optimize for low latency
JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication"
```

---

## 18. Scaling Strategy

### 18.1 Read Path Scaling (Redirect)

```
CDN Edge (Cloudflare)          ← First layer, handles 80%+ traffic
    ↓ (cache miss)
NGINX Load Balancer
    ↓
Redirect Service × N replicas  ← Stateless, scale horizontally
    ↓ (cache hit)
Redis                          ← Single node initially
    ↓ (cache miss)
PostgreSQL (Supabase)          ← Read replicas if needed
```

### 18.2 Write Path Scaling (API)

API Service is stateless → add replicas behind NGINX `upstream` block.

### 18.3 Redis Scaling Path

| Stage      | Setup                  | When to upgrade            |
|------------|------------------------|----------------------------|
| Initial    | Single node            | Default                    |
| Growing    | Redis Sentinel         | > 10k req/s, HA needed     |
| Large scale| Redis Cluster          | > 100k req/s, data > 25GB  |

### 18.4 Kafka Scaling

Aiven free tier → paid plan when:
- Analytics lag > 10 seconds
- > 1M events/day

### 18.5 PostgreSQL Scaling

Supabase → upgrade to paid for:
- > 500MB storage
- > 2 GB RAM (connection pool)
- Read replicas

---

## 19. Trade-offs & Decisions

| Decision                     | Chosen Option               | Rejected Option              | Reason                                          |
|------------------------------|-----------------------------|------------------------------|-------------------------------------------------|
| ID generation                | Auto-increment + Base62     | UUID, Snowflake              | Simpler, shorter codes, no external dependency  |
| Architecture                 | Modular monolith            | Microservices                | Lower operational overhead, faster to build     |
| Redirect response code       | 302 per click, 301 at edge  | Always 301                   | 302 enables analytics; 301 provides CDN speed   |
| Database                     | Supabase (managed PG)       | Self-hosted PostgreSQL       | Zero ops cost, reliable                         |
| Cache                        | Redis (self-hosted)         | Memcached                    | Richer data types, Lua scripts, Bloom filter    |
| Event streaming              | Kafka (Aiven)               | RabbitMQ, inline sync        | Durable, scalable, decoupled analytics          |
| Bloom filter                 | Redis + Redisson            | In-memory (JVM)              | Survives restarts, shared across replicas       |
| Analytics inserts            | Batch via Kafka consumer    | Real-time single row         | Prevents DB bottleneck on high traffic          |
| Token storage (refresh)      | HTTP-only cookie            | localStorage                 | XSS protection                                  |
| CDN                          | Cloudflare (free)           | AWS CloudFront               | Cost: $0 vs. usage-based billing               |

---

## 20. Development Roadmap

### Phase 1 — Core (Week 1–2)

- [x] Project setup (Spring Boot multi-module)
- [ ] Base62 encoder + short code generation
- [ ] URL create endpoint
- [ ] Redirect endpoint (DB only)
- [ ] Redis cache integration
- [ ] Unit tests for core logic

### Phase 2 — Production-Ready (Week 3–4)

- [ ] JWT authentication (register, login, refresh, logout)
- [ ] User URL management (list, delete)
- [ ] Rate limiting (Redis sliding window)
- [ ] Kafka integration (analytics events)
- [ ] Analytics Worker (batch consumer)
- [ ] Analytics API endpoint
- [ ] Docker Compose setup
- [ ] GitHub Actions CI/CD
- [ ] NGINX config

### Phase 3 — Advanced (Week 5–6)

- [ ] Bloom filter (Redisson)
- [ ] Cloudflare CDN integration + cache purge API
- [ ] Domain blacklist
- [ ] Anti-spam behavioral detection
- [ ] Cache warming on startup
- [ ] Resilience4j circuit breakers
- [ ] Admin endpoints
- [ ] Optional: Google Safe Browsing API

---

## 21. Glossary

| Term              | Definition                                                                 |
|-------------------|----------------------------------------------------------------------------|
| **Base62**        | Encoding scheme using chars `0-9`, `a-z`, `A-Z` for compact URL-safe IDs  |
| **Bloom Filter**  | Probabilistic data structure: can say "definitely absent" or "maybe present" |
| **Cache-First**   | Always check cache before hitting the database                             |
| **CDN**           | Content Delivery Network; caches responses at geographically distributed edge nodes |
| **False Positive**| Bloom filter says "present" but item is not actually in the set            |
| **Modular Monolith**| Single codebase split into logical modules, deployed as a unit           |
| **Sliding Window**| Rate limiting strategy: counts requests in a rolling time window          |
| **Short Code**    | The unique identifier in a short URL (e.g., `4c92` in `yourdomain.com/4c92`) |
| **302 vs 301**    | 302 = temporary redirect (not cached by browser); 301 = permanent (cached) |
| **VPS**           | Virtual Private Server; a rented Linux server                              |
| **TTL**           | Time-To-Live; how long a cache entry or URL remains valid                  |

---

*Document maintained by: Engineering*  
*Last updated: 2024*  
*Next review: Before Phase 2 start*

---

## 19. 🧪 Testing Strategy

### 19.1 Goals

The testing strategy ensures three core properties across all services:

- **System correctness** — logic behaves as specified under all conditions
- **Stability under change** — refactors and new features don't silently break existing flows
- **Confidence for CI/CD deployment** — every merge to `main` is safe to deploy automatically

---

### 19.2 Testing Pyramid

```
              /\
             /  \
            / E2E\          ← few, slow, full-system
           /------\
          /        \
         /  Integr. \       ← moderate, Testcontainers
        /------------\
       /              \
      /   Unit Tests   \    ← many, fast, isolated
     /__________________\
```

The majority of tests are fast, isolated unit tests. Integration tests cover critical cross-boundary flows. E2E tests are minimal and target only the most business-critical paths.

---

### 19.3 Unit Testing

**Scope:** Service layer, business logic, utility classes

**Tools:** JUnit 5, Mockito

| Class Under Test | Test Scenario |
|---|---|
| `Base62Encoder` | `encode(0) = "0"`, `encode(1_000_000) = "4c92"`, round-trip decode |
| `ShortCodeGenerator` | Generated code is 4–7 chars, Base62 alphabet only |
| `UrlService` | Rejects malformed URLs, private IPs, disallowed schemes |
| `SpamDetectionService` | Flag IP exceeding creation threshold; block known patterns |
| `RateLimiterService` | Counter increments correctly; resets after window expires |
| `JwtService` | Token generation, expiry validation, tampered token rejection |

```java
// Example: Base62Encoder unit test
@ParameterizedTest
@CsvSource({
    "0,           0",
    "1000000,      4c92",
    "3521614606208, zzzzzz"
})
void encode_shouldProduceExpectedCode(long id, String expected) {
    assertThat(Base62Encoder.encode(id)).isEqualTo(expected);
}
```

---

### 19.4 Integration Testing

**Scope:** Database interaction, Redis cache behaviour, full request flow (API → DB → Cache)

**Tools:** Testcontainers (PostgreSQL + Redis), Spring Boot Test

**Covered scenarios:**

| Scenario | Assertions |
|---|---|
| Create short URL | Row in DB with correct `short_code`; Redis key SET with correct TTL |
| Redirect — cache hit | Returns 302; DB query count = 0 |
| Redirect — cache miss | Redis miss → DB lookup → Redis populated; returns 302 |
| Redirect — expired URL | Returns 410 Gone; no cache write |
| Delete URL | `is_active = false` in DB; Redis key evicted |
| Rate limiter under Redis | Counter increments per request; 429 returned at threshold |
| Refresh token flow | Token stored in DB; reuse after revocation returns 401 |

```java
// Example: cache miss -> DB fallback integration test
@Test
void redirect_cacheMiss_shouldFallBackToDb_andPopulateCache() {
    urlRepository.save(new Url("4c92", "https://example.com"));
    redis.delete("short:4c92");

    var response = mockMvc.perform(get("/4c92")).andReturn();

    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(redis.get("short:4c92")).isEqualTo("https://example.com");
}
```

---

### 19.5 API Testing

**Scope:** All REST endpoints, authentication flow, error handling, input validation

**Tools:** Spring MockMvc, REST Assured (optional)

| Endpoint | Scenarios Tested |
|---|---|
| `POST /api/v1/auth/register` | Success 201, duplicate email 409, weak password 400 |
| `POST /api/v1/auth/login` | Valid credentials 200, wrong password 401, unknown email 401 |
| `POST /api/v1/urls` | Valid URL 201, malformed URL 400, blacklisted domain 400, no auth 401 |
| `GET /{shortCode}` | Exists 302, not found 404, expired 410, bloom filter reject 404 |
| `DELETE /api/v1/urls/{code}` | Owner 204, non-owner 403, already deleted 404 |

```java
// Example: blacklisted domain returns 400
@Test
void createUrl_blacklistedDomain_shouldReturn400() throws Exception {
    domainBlacklistRepo.save(new DomainBlacklist("malware.io", "phishing"));

    mockMvc.perform(post("/api/v1/urls")
            .header("Authorization", "Bearer " + validToken)
            .contentType(APPLICATION_JSON)
            .content("{ \"originalUrl\": \"https://malware.io/page\" }")
    )
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.error.code").value("BLACKLISTED_DOMAIN"));
}
```

---

### 19.6 End-to-End (E2E) Testing

Full system behavior from the perspective of an external user, running against the real Docker Compose stack or a staging environment.

| Scenario | Steps |
|---|---|
| Happy path — create and redirect | Register → Login → POST /urls → GET /{code} → assert 302 |
| Rate limit enforcement | 31 requests in 60s anonymous → assert 31st is 429 |
| Expired URL flow | Create URL with `expire_at = now+2s` → wait → GET → assert 410 |
| Auth token expiry | Login → wait → use expired token → 401 → refresh → retry → 200 |

**Tools:** Postman Collection + Newman (runnable in CI), curl smoke test script

---

### 19.7 Performance Testing *(Important for this project)*

> The primary design goal is sub-10ms redirect latency on cache hits and ≥1,000 req/s on a single VPS. These must be **validated**, not assumed.

**Tools:** k6, Apache JMeter

| Scenario | Config | Pass Criteria |
|---|---|---|
| Baseline redirect (cache warm) | 500 VUs, 60s ramp, 100% cache hit | p95 < 10ms, 0% errors |
| Cache miss storm | 200 VUs, cold cache, 60s | p95 < 100ms, 0% errors |
| Mixed read/write | 400 VUs redirect + 100 VUs create, 120s | No read latency degradation |
| Rate limiter under load | 1000 VUs same IP, burst 10s | 429 at threshold, no 500s |
| Sustained throughput | 200 VUs, 5 min steady state | ≥1,000 req/s, p99 < 50ms |

```javascript
// k6 script — redirect load test
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 500 },   // ramp up
    { duration: '60s', target: 500 },   // hold
    { duration: '10s', target: 0   },   // ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<10'],  // 95th percentile < 10ms
    'http_req_failed':   ['rate<0.01'], // error rate < 1%
  },
};

export default function () {
  const res = http.get('https://yourdomain.com/4c92',
    { redirects: 0 }  // don't follow redirect — measure time to 302
  );
  check(res, { 'status is 302': (r) => r.status === 302 });
}
```

---

### 19.8 Test Coverage Targets

| Layer | Coverage Target | Enforcement |
|---|---|---|
| Unit tests | 70–80% line coverage | Jacoco report in CI; warn below 70% |
| Integration tests | Critical flows only | No coverage %; scenario-based |
| E2E tests | Minimal but meaningful | 3–5 core user journeys |
| Performance tests | All latency SLOs validated | Run on staging before each release |

---

### 19.9 CI Integration

All automated tests run in GitHub Actions on every push and pull request. The build fails if any test fails.

```yaml
# GitHub Actions — test pipeline
jobs:
  test:
    steps:
      - name: Run unit tests
        run: ./mvnw test -Dgroups=unit

      - name: Run integration tests (Testcontainers)
        run: ./mvnw test -Dgroups=integration

      - name: Generate coverage report
        run: ./mvnw jacoco:report

      - name: Upload coverage
        uses: codecov/codecov-action@v4

  build:              # Only runs if test job passes
    needs: test
```

| Rule | Details |
|---|---|
| Fail on test failure | Any failed test blocks build and deploy |
| Coverage threshold (optional) | Jacoco fails build below 70% unit coverage |
| Integration test isolation | Fresh Testcontainers per run; no shared state |
| Performance tests | Not in CI — run manually on staging pre-release |

---

### 19.10 Key Testing Focus Areas

**Critical paths (must be covered):**

| Area | Why Critical | Test Type |
|---|---|---|
| Redirect performance | Core product promise — must be fast | Unit + Perf |
| Cache correctness | Stale cache = wrong redirect destination | Integration |
| Rate limiter accuracy | Incorrect limits = abuse or blocked legit users | Unit + Integration |
| JWT validation | Auth bypass = full system compromise | Unit + API |
| Short code uniqueness | Collision = two URLs share one code | Unit |

**Known risk areas:**

| Risk | Mitigation in Tests |
|---|---|
| Cache inconsistency on URL delete | Integration test: delete → assert Redis key evicted immediately |
| Race condition on custom alias creation | Concurrent test: two requests same alias → assert only one succeeds (409) |
| Spam detection false positives | Unit test: legitimate high-volume user not flagged as abuser |
| Bloom filter false positive → 404 | Integration test: valid code not in filter returns 404 (rebuild test) |
| Analytics event loss on Kafka failure | Chaos test: Kafka down → redirect still completes → event logged locally |

---

### 19.11 Optional Advanced Testing

| Type | Description | When to Add |
|---|---|---|
| Chaos testing | Simulate Redis down → verify DB fallback; Kafka down → verify redirect unaffected | Phase 3 |
| Contract testing (Pact) | If split into microservices later, verify API contracts between services | Post-Phase 3 |
| Benchmark report in README | Include k6 p50/p95/p99 results as a README table or badge | End of Phase 2 |
| Mutation testing (PITest) | Verify tests actually catch bugs by introducing artificial faults | Optional |

---

### 19.12 Summary

The testing strategy is layered to match the architecture: fast unit tests for business logic, Testcontainers-backed integration tests for cross-boundary correctness, and k6 performance tests to validate the system's core latency SLOs. Combined with GitHub Actions enforcement, every deployment is backed by automated confidence.

| Property | How It Is Ensured |
|---|---|
| Reliability | Unit + integration tests cover all business rules and edge cases |
| Performance validation | k6 load tests run against staging before each release |
| Production-readiness | CI blocks deploy on any test failure; coverage enforced |

---

**End of Document**
