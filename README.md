# 🔗 URL Shortener — Production-Oriented Backend System

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot" />
  <img src="https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis" />
  <img src="https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql" />
  <img src="https://img.shields.io/badge/Kafka-Aiven-231F20?style=flat-square&logo=apachekafka" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker" />
  <img src="https://img.shields.io/badge/Cloudflare-CDN-F38020?style=flat-square&logo=cloudflare" />
  <img src="https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF?style=flat-square&logo=githubactions" />
</p>

<p align="center">
  A <strong>high-performance, production-oriented URL shortener</strong> built for a mid-level backend portfolio.<br/>
  Demonstrates real-world system design: caching, rate limiting, async analytics, and CDN integration — all on a near-zero budget.
</p>

---

## ✨ Features

| Feature | Details |
|---|---|
| ⚡ Fast Redirects | Sub-10ms on cache hit via Redis + Cloudflare edge |
| 🔐 JWT Auth | Access + Refresh token with HTTP-only cookie |
| 📊 Analytics | Async click tracking via Kafka (IP, UA, referer) |
| 🛡️ Rate Limiting | Per-IP and per-user sliding window (Redis) |
| 🌸 Bloom Filter | Instant rejection of invalid codes before any DB hit |
| 🌐 CDN | Cloudflare edge caching for global low latency |
| 🚫 Anti-Spam | Domain blacklist + behavioral abuse detection |
| 🐳 Docker | Fully containerized, one-command local setup |

---

## 🏗️ Architecture

```
User → Cloudflare CDN → NGINX → Redirect Service → Redis → 302 Redirect
                                      ↘ (miss) → PostgreSQL → cache → Redirect
                                                      ↘ Kafka → Analytics Worker → DB
       Client → NGINX → API Service → PostgreSQL + Redis
```

**Services:**

| Service | Role | Port |
|---|---|---|
| `api-service` | URL creation, auth, user management | 8080 |
| `redirect-service` | High-performance short-code resolution | 8081 |
| `analytics-worker` | Async Kafka consumer, batch analytics | 8082 |
| `redis` | Cache, rate limiter, bloom filter | 6379 |
| `nginx` | Reverse proxy, routing, SSL termination | 80/443 |

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- (Optional) Supabase account, Aiven account, Cloudflare account

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/url-shortener.git
cd url-shortener
```

### 2. Configure environment variables

```bash
cp .env.example .env
# Edit .env with your values
```

```env
# .env.example
DB_URL=jdbc:postgresql://localhost:5432/urlshortener
DB_USERNAME=postgres
DB_PASSWORD=postgres

REDIS_HOST=redis
REDIS_PORT=6379

KAFKA_BOOTSTRAP_SERVERS=localhost:9092

JWT_SECRET=your-256-bit-secret
JWT_ACCESS_EXPIRY=900
JWT_REFRESH_EXPIRY=604800

SHORT_URL_BASE=http://localhost
```

### 3. Run with Docker Compose

```bash
docker compose up -d
```

Services will be available at:
- API: `http://localhost:8080`
- Redirect: `http://localhost:8081`
- Health: `http://localhost:8080/actuator/health`

### 4. Create your first short URL

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","username":"you","password":"Str0ngP@ss!"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"Str0ngP@ss!"}'

# Create short URL
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer <your_token>" \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://www.example.com/very/long/path"}'

# Redirect
curl -L http://localhost:8081/4c92
```

---

## 📁 Project Structure

```
url-shortener/
├── api-service/             # Write path (auth, URL creation, user management)
│   └── src/main/java/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       └── config/
├── redirect-service/        # Read path (hot path, fast redirects)
│   └── src/main/java/
│       ├── controller/
│       ├── service/
│       └── publisher/
├── analytics-worker/        # Kafka consumer, batch analytics insert
│   └── src/main/java/
│       ├── consumer/
│       └── service/
├── shared/                  # DTOs, utilities, domain models
│   └── src/main/java/
│       ├── dto/
│       └── util/
├── nginx/
│   └── nginx.conf
├── docker-compose.yml
├── .env.example
└── .github/
    └── workflows/
        └── ci-cd.yml
```

---

## 📡 API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | ❌ | Register new user |
| `POST` | `/api/v1/auth/login` | ❌ | Login, receive tokens |
| `POST` | `/api/v1/auth/refresh` | ❌ | Refresh access token |
| `POST` | `/api/v1/auth/logout` | ✅ | Revoke refresh token |

### URLs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/urls` | ✅ | Create short URL |
| `GET` | `/api/v1/urls` | ✅ | List user's URLs |
| `GET` | `/api/v1/urls/{code}` | ✅ | Get URL metadata |
| `DELETE` | `/api/v1/urls/{code}` | ✅ | Delete short URL |
| `GET` | `/api/v1/urls/{code}/analytics` | ✅ | View click analytics |

### Redirect (Public)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/{shortCode}` | ❌ | Redirect to original URL |

Full API documentation → [`docs/api-spec.md`](docs/api-spec.md)

---

## ⚙️ Core Design Decisions

### ID Generation: Base62 from Auto-Increment

```
DB Auto-Increment ID → Base62 Encode → Short Code
1,000,000 → "4c92"  (4 chars)
```

Avoids UUID verbosity. Gives short, URL-safe, collision-free codes.

### Redirect Path: Cache-First

```
Bloom Filter → Redis Cache → PostgreSQL → 302 Redirect
      ↘ absent?     ↘ miss?
    404 immediately  DB lookup + cache populate
```

Invalid codes are rejected instantly (no Redis/DB hit). Cache hits return in < 10ms.

### Rate Limiting: Redis Sliding Window

```
INCR rate_limit:ip:{ip}
EXPIRE 60s
→ reject at threshold
```

### Analytics: Fully Async via Kafka

Redirect completes immediately. Click data is published to Kafka and consumed by the analytics worker in batches — zero impact on redirect latency.

---

## 🧪 Running Tests

```bash
# All tests
./mvnw test

# With coverage report
./mvnw test jacoco:report

# Integration tests only (requires Docker for Testcontainers)
./mvnw test -Dgroups=integration
```

---

## 🐳 Docker

```bash
# Build all images
docker compose build

# Start all services
docker compose up -d

# View logs
docker compose logs -f redirect-service

# Stop everything
docker compose down
```

---

## 🚢 Deployment

This system targets a **single VPS** (Hetzner/DigitalOcean ~$5/mo) with managed services:

| Service | Provider | Cost |
|---|---|---|
| PostgreSQL | Supabase (free tier) | $0 |
| Kafka | Aiven (free trial) | $0 |
| CDN / DNS | Cloudflare (free) | $0 |
| Docker Registry | Docker Hub (free) | $0 |

CI/CD via **GitHub Actions** → push to `main` triggers test → build → deploy.

Full deployment guide → [`docs/deployment.md`](docs/deployment.md)

---

## 📈 Performance Targets

| Metric | Target |
|---|---|
| Redirect latency (Cloudflare cache hit) | < 5ms |
| Redirect latency (Redis cache hit) | < 10ms |
| Redirect latency (DB fallback) | < 100ms |
| Write throughput (URL creation) | > 500 req/s |
| Read throughput (redirects, single VPS) | > 1,000 req/s |

---

## 🗺️ Roadmap

- [x] **Phase 1** — URL shortening, redirect, Redis cache
- [ ] **Phase 2** — JWT auth, rate limiting, Kafka analytics
- [ ] **Phase 3** — Bloom filter, CDN, anti-spam detection

---

## 📄 Documentation

| Document | Description |
|---|---|
| [`docs/technical-spec.md`](docs/technical-spec.md) | Full system design & architecture |
| [`docs/api-spec.md`](docs/api-spec.md) | API endpoint reference |
| [`docs/deployment.md`](docs/deployment.md) | Infrastructure & deployment guide |
| [`docs/adr/`](docs/adr/) | Architecture Decision Records |

---

## 🤝 Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for guidelines.

---

## 📝 License

MIT License — see [`LICENSE`](LICENSE) for details.
