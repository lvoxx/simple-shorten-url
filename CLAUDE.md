# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A URL shortener built as a modular monolith — four independently deployable Spring Boot services sharing common and starter modules. The system uses a **write/read/async** service split:

- `api_service` (port 8080) — URL creation, auth, user management
- `redirect_service` (port 8081) — short-code resolution and 302 redirect (hot path)
- `analytics_worker` (port 8082) — Kafka consumer for async click analytics
- `dashboard` (port 8083) — per-user analytics read-model: consumes `analytics-events` into its own `click_events` + daily rollup, serves Redis-cached aggregations over REST, and pushes live click ticks over WebSocket (`/ws/dashboard`)

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

**Database schema is initialized by Docker Compose only** — never via Spring (`spring.sql.init`, `Flyway`, `Liquibase`, or `ApplicationRunner`). Run `database/*.sql` manually or mount them as Docker init scripts.

## Tech Stack

- **Java 25**, **Spring Boot 4.0.6**
- **Spring WebFlux** (reactive) — all services use `Mono`/`Flux` throughout
- **R2DBC + r2dbc-postgresql** — reactive DB access (no JPA/Hibernate)
- **Spring Data Redis Reactive** — caching and rate limiting
- **Apache Kafka + Avro** — async analytics pipeline; schemas managed via Confluent Schema Registry
- **MapStruct** — compile-time DTO ↔ model mapping (no manual mapping)
- **Springdoc OpenAPI** — Swagger UI; centralized in `swagger_starter`
- **Testcontainers** — Postgres + Redis spun up per integration test run

## Architecture & Data Flow

```
Client → Cloudflare CDN → NGINX
  → redirect_service: Bloom filter → Redis cache → Postgres → 302
  → api_service: REST API → Postgres + Redis → Kafka (Avro analytics events)
                                                    ↓
                                           analytics_worker (batch insert)
```

**Redis cache keys:**
- `short:{code}` — original URL (24h TTL)
- `rate_limit:ip:{ip}`, `rate_limit:user:{id}` — sliding window counters
- `bloom:urls` — Redisson `RBloomFilter` (10M capacity, 1% false positive)
- `blacklist:domain:{domain}` — 10m TTL

**Short code generation:** BIGSERIAL → Base62 encode (0-9a-zA-Z). Use base62 from io.seruco.encoding — both `api_service` and `redirect_service` need it.

**Redirect strategy:** Always 302 (browser must recheck, enabling analytics). 301 only at Cloudflare edge.

**Analytics pipeline:** `api_service` produces Avro-encoded events to Kafka topic `analytics-events` (3 partitions). `analytics_worker` batch-consumes up to 500 events per poll.

## Module Map

```
services/
├── api_service/          # Write path — auth, URL CRUD, user mgmt
├── redirect_service/     # Read path — high-throughput redirect
├── analytics_worker/     # Async Kafka consumer (IP2Location-enriched analytics)
├── dashboard/            # Per-user analytics read-model (Kafka→click_events +
│                         #   daily rollup; Redis-cached aggregates; WebSocket push)
├── common/               # Shared: domain models, DTOs, exceptions
│                         #   MapStruct mappers, Avro schema definitions
└── starters/
    ├── kafka_starter/    # Kafka producer/consumer auto-config + Avro serializer setup
    ├── postgres_starter/ # R2DBC auto-config + TransactionalOperaBase62tor bean
    ├── redis_starter/    # Redis Reactive auto-config
    ├── swagger_starter/  # Springdoc OpenAPI bean + global OpenAPI config
    └── message_starter/  # MessageSource auto-config, shared i18n YAML files
```

**common** holds shared code that has no infrastructure dependencies (models, DTOs, utilities, exceptions, Avro schemas). **Starters** wrap infrastructure auto-configuration so services import a starter instead of repeating `@Configuration` classes. No service should define its own Kafka/R2DBC/Redis/Swagger/MessageSource `@Bean` — that belongs in the appropriate starter.

`ValidationMessages.properties` (Jakarta constraint messages) stays **per-service** since message keys can differ per domain.

## Layered Package Structure

```
io.lvoxx.ssurl.<service>/
├── controller/     # @RestController — delegates to service, no business logic
├── service/        # Business logic (interface + impl); throws typed exceptions
├── repository/     # ReactiveCrudRepository extensions
├── config/         # @Configuration beans not provided by starters
├── domain/         # R2DBC entity / aggregate classes (live in common if shared)
├── exception/      # Service-specific typed exceptions (shared ones live in common)
└── dto/
    ├── request/    # Inbound: validated with Jakarta annotations
    └── response/   # Outbound: mapped from domain via MapStruct
```

## Testing Patterns

### Service tests — Mockito, no Spring context

```java
@ExtendWith(MockitoExtension.class)
class UrlServiceTest {
    @Mock UrlRepository urlRepository;
    @Mock RedisTemplate<String, String> redisTemplate;
    @InjectMocks UrlServiceImpl urlService;

    @Test
    void createUrl_shouldThrowWhenDomainBlacklisted() {
        when(urlRepository.existsByDomain("spam.com")).thenReturn(Mono.just(true));
        StepVerifier.create(urlService.createUrl(request))
            .expectError(DomainBlacklistedException.class)
            .verify();
    }
}
```

### Controller tests — `@WebFluxTest` + Mockito; cover validation paths

```java
@WebFluxTest(UrlController.class)
class UrlControllerTest {
    @Autowired WebTestClient webTestClient;
    @MockitoBean UrlService urlService;

    @Test
    void createUrl_shouldReturn400WhenOriginalUrlBlank() {
        webTestClient.post().uri("/api/v1/urls")
            .bodyValue(new CreateUrlRequest(""))   // fails @NotBlank
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().jsonPath("$.errors").isNotEmpty();
    }

    @Test
    void createUrl_shouldReturn201OnSuccess() {
        when(urlService.createUrl(any())).thenReturn(Mono.just(urlResponse));
        webTestClient.post().uri("/api/v1/urls")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated();
    }
}
```

### Repository tests — Testcontainers via shared abstract base

Define a reusable abstract class (or custom `@Annotation`) in each service's `test` source set:

```java
// AbstractRepositoryTest.java (in src/test/java/.../repository)
@DataR2dbcTest
@Testcontainers
@Import(PostgresTestContainerConfig.class)
abstract class AbstractRepositoryTest {
    // shared container lifecycle — subclasses just @Autowired their repo
}

class UrlRepositoryTest extends AbstractRepositoryTest {
    @Autowired UrlRepository urlRepository;

    @Test
    void findByShortCode_shouldReturnUrl() { ... }
}
```

## Error Handling

**Never return hardcoded 4xx/5xx responses from controllers or services.** All errors are communicated by throwing typed exceptions. A single `@RestControllerAdvice` per service (or one in `common`) maps exceptions to `ProblemDetail` responses.

```java
// In common or per-service exception package
public class ShortCodeNotFoundException extends RuntimeException {
    public ShortCodeNotFoundException(String code) {
        super("Short code not found: " + code);
    }
}

// GlobalExceptionHandler (one per service, or shared in common)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(ShortCodeNotFoundException.class)
    public ProblemDetail handleNotFound(ShortCodeNotFoundException ex, Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setDetail(messageSource.getMessage("error.shortcode.notfound", null, locale));
        return pd;
    }

    @ExceptionHandler(WebExchangeBindException.class)   // WebFlux validation errors
    public ProblemDetail handleValidation(WebExchangeBindException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList());
        return pd;
    }
}
```

## DTOs and Validation

- **Request DTOs** live in `dto/request/` and carry Jakarta validation annotations (`@NotBlank`, `@URL`, `@Size`, etc.).
- **Response DTOs** live in `dto/response/` — never expose domain/entity classes directly.
- Use **MapStruct** for all DTO ↔ domain conversions. No hand-written mapping code.

```java
// request/CreateUrlRequest.java
public record CreateUrlRequest(
    @NotBlank @URL String originalUrl,
    @Size(max = 100) String title,
    Instant expireAt
) {}

// response/UrlResponse.java
public record UrlResponse(String shortCode, String originalUrl, String title,
                          long clickCount, Instant createdAt) {}

// mapper/UrlMapper.java (in common)
@Mapper(componentModel = "spring")
public interface UrlMapper {
    UrlResponse toResponse(Url url);
    Url toDomain(CreateUrlRequest request);
}
```

## API Documentation (Swagger / OpenAPI)

All controller methods **must** be annotated with Springdoc OpenAPI annotations. This is non-negotiable.

```java
@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URLs", description = "Short URL management")
public class UrlController {

    @Operation(summary = "Create a short URL")
    @ApiResponse(responseCode = "201", description = "URL created",
        content = @Content(schema = @Schema(implementation = UrlResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    @PostMapping
    public Mono<ResponseEntity<UrlResponse>> createUrl(@Valid @RequestBody CreateUrlRequest req) { ... }
}
```

Global `OpenAPI` bean (title, version, security scheme, servers) lives in `swagger_starter` — services just add it as a dependency and get Swagger UI at `/swagger-ui.html` with no additional config.

## Internationalization (i18n)

Error messages must be externalized — no hardcoded strings in exception handlers.

**`message_starter`** provides the `MessageSource` bean pointing to shared YAML files (`messages/errors.yml`, `messages/common.yml`). Services import the starter and optionally add their own message files.

**`ValidationMessages.properties`** stays per-service (`src/main/resources/ValidationMessages.properties`) because constraint message keys are domain-specific.

```yaml
# messages/errors.yml (in message_starter resources)
error:
  shortcode:
    notfound: "Short code ''{0}'' does not exist"
    expired: "Short code ''{0}'' has expired"
  domain:
    blacklisted: "The domain is not allowed"
  ratelimit:
    exceeded: "Too many requests. Try again in {0} seconds"
```

```java
// Accept-Language header drives locale; Spring resolves it automatically via
// LocaleContextHolder when MessageSource is wired through message_starter
messageSource.getMessage("error.shortcode.notfound", new Object[]{code}, locale)
```

## Data Integrity (ACID)

All multi-step database writes must be wrapped in a reactive transaction.

- Use `@Transactional` on service methods (R2DBC supports it via `TransactionalOperator` under the hood).
- For programmatic control, inject `TransactionalOperator` (provided by `postgres_starter`).
- Read-only queries: annotate with `@Transactional(readOnly = true)` to allow connection pool optimizations.

```java
@Service
@Transactional
public class UrlServiceImpl implements UrlService {

    @Override
    public Mono<UrlResponse> createUrl(CreateUrlRequest request, Long userId) {
        return urlRepository.save(urlMapper.toDomain(request))
            .flatMap(url -> bloomFilterService.add(url.getShortCode()).thenReturn(url))
            .map(urlMapper::toResponse);
        // If bloomFilterService fails, the DB write is rolled back
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<UrlResponse> listByUser(Long userId) {
        return urlRepository.findAllByUserId(userId).map(urlMapper::toResponse);
    }
}
```

## Avro for Kafka

All Kafka messages use **Avro** serialization with the Confluent Schema Registry. Schema `.avsc` files live in `common/src/main/avro/`. The `kafka_starter` configures the Avro serializer/deserializer beans globally.

```json
// common/src/main/avro/AnalyticsEvent.avsc
{
  "type": "record",
  "name": "AnalyticsEvent",
  "namespace": "io.lvoxx.ssurl.avro",
  "fields": [
    {"name": "shortCode", "type": "string"},
    {"name": "ip", "type": "string"},
    {"name": "userAgent", "type": ["null", "string"], "default": null},
    {"name": "referer", "type": ["null", "string"], "default": null},
    {"name": "createdAt", "type": "long", "logicalType": "timestamp-millis"}
  ]
}
```

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
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry (for Avro) |
| `JWT_SECRET`, `JWT_ACCESS_EXPIRY` (900s), `JWT_REFRESH_EXPIRY` (604800s) | Auth tokens |
| `SHORT_URL_BASE` | Base URL for generated short links |

JWT refresh tokens are stored in HTTP-only cookies (XSS protection); access tokens in-memory only.

## Caching

Spring Cache (`@EnableCaching`, `RedisCacheManager`) is configured in `redis_starter`. Cache name: **`short-urls`**, Redis key prefix: `short:` (e.g. `short:abc123`), TTL: 24h.

- `redirect_service`: `UrlCacheService.resolveOriginalUrl()` is annotated `@Cacheable("short-urls")` — Spring handles read-through automatically.
- `api_service`: `UrlCacheOperations` provides `@CachePut` (on create) and `@CacheEvict` (on delete/deactivation). Injected into `UrlServiceImpl`.
- Never call `@CachePut`/`@CacheEvict` methods from within the same class — Spring AOP won't intercept self-invocations.

## URL Expiry Policy

- **Anonymous users**: always expire after 7 days (set in `UrlServiceImpl.createUrl` when `userId == null`).
- **Authenticated users**: no default expiry; client may optionally supply `expireAt` in the request.
- `POST /api/v1/urls` is public (no auth required). Anonymous requests receive 7-day TTL.

## Cursor-Based Pagination

`GET /api/v1/urls/my` supports cursor-based pagination (requires authentication):

| Query param | Default | Description |
|---|---|---|
| `cursor` | _(none)_ | Last `id` from previous page (exclusive). Omit for first page. |
| `size` | 20 | Items per page (max 100). |

Response is `CursorPage<UrlResponse>` with `content`, `nextCursor` (null on last page), and `hasNext`.

## Exception Messages

All domain exceptions extend `AppException` (in `common`) which carries `Object[] args` for `MessageSource` interpolation. `GlobalExceptionHandler` in each service calls `messageSource.getMessage(code, ex.getArgs(), locale)`. Message keys live in `message_starter/src/main/resources/messages/errors.properties`.

## Starters

| Starter | Value | Beans defined |
|---|---|---|
| `postgres_starter` | default R2DBC pool config via `application.yaml` | none (Spring Boot R2DBC auto-config provides all beans) |
| `redis_starter` | default Redis config + `@EnableCaching` + `RedisCacheManager` | `RBloomFilter`, `RedisCacheManager` |
| `kafka_starter` | default Kafka consumer config via `application.yaml` | `NewTopic` |
| `swagger_starter` | Swagger UI config | `OpenAPI` |
| `message_starter` | i18n message files | `MessageSource` |

## Dockerfiles

Each service has a `Dockerfile` at its directory root. **Build context must be `services/`** (the root containing `pom.xml`) because services depend on shared `common` and starter modules.

```bash
# From services/ directory
docker build -f api_service/Dockerfile -t ssurl-api .
docker build -f redirect_service/Dockerfile -t ssurl-redirect .
docker build -f analytics_worker/Dockerfile -t ssurl-analytics .
docker build -f dashboard/Dockerfile -t ssurl-dashboard .
```

## Known Issues

1. **SQL syntax error** in `database/urls.sql` — missing commas before `created_by`/`updated_by`.
2. **Cache TTL vs expireAt mismatch** — URLs cached with 24h Redis TTL even if `expireAt` is sooner. Expired URLs may be served from cache until the 24h TTL expires. Fix: compute TTL as `min(24h, expireAt - now)` in `UrlCacheService`.
3. **`@WebFluxTest` unavailable in Spring Boot 4.0.6** — use plain Mockito unit tests for controllers instead of `@WebFluxTest` slices.

## Reference

Full API spec, flow diagrams, rate-limiting details, and deployment configuration are in [`url-shortener-technical-docs.md`](url-shortener-technical-docs.md).

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **simple-shorten-url** (2037 symbols, 4011 relationships, 113 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/simple-shorten-url/context` | Codebase overview, check index freshness |
| `gitnexus://repo/simple-shorten-url/clusters` | All functional areas |
| `gitnexus://repo/simple-shorten-url/processes` | All execution flows |
| `gitnexus://repo/simple-shorten-url/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
