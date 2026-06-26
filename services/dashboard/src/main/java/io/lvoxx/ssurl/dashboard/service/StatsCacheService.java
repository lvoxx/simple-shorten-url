package io.lvoxx.ssurl.dashboard.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Redis layer that keeps the dashboard read path off Postgres for hot metrics:
 *
 * <ul>
 * <li><b>Live counters</b> {@code dash:cnt:{code}:{yyyymmdd}} — O(1) {@code INCR}
 * per click; powers the live WebSocket tick.</li>
 * <li><b>Unique visitors</b> {@code dash:hll:{code}:{yyyymmdd}} — HyperLogLog
 * ({@code PFADD}/{@code PFCOUNT}); cheap approximate cardinality without a
 * {@code COUNT(DISTINCT)} scan.</li>
 * <li><b>Aggregation cache</b> {@code dash:cache:{key}} — JSON-serialised
 * responses with a short TTL so repeated dashboard loads don't recompute.</li>
 * </ul>
 *
 * <p>
 * Every Redis call is <b>best-effort</b>: failures fall back to the caller's
 * source-of-truth (Postgres) and never break ingestion or reads.
 */
@Slf4j
@Service
public class StatsCacheService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE; // yyyymmdd
    private static final String CNT_PREFIX = "dash:cnt:";
    private static final String HLL_PREFIX = "dash:hll:";
    private static final String CACHE_PREFIX = "dash:cache:";
    /** Counter/HLL keys self-expire so the keyspace stays bounded. */
    private static final Duration COUNTER_TTL = Duration.ofDays(100);

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public StatsCacheService(ReactiveStringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    // ── Ingestion side ─────────────────────────────────────────────────────────

    /**
     * Record a click for the live counters: {@code INCR} today's count and
     * {@code PFADD} the visitor IP into the daily HyperLogLog.
     *
     * @return the running click count for the code today (for the live tick)
     */
    public Mono<Long> recordClick(String code, String ip, LocalDate day) {
        String cntKey = CNT_PREFIX + code + ":" + DAY.format(day);
        String hllKey = HLL_PREFIX + code + ":" + DAY.format(day);
        Mono<Long> incr = redis.opsForValue().increment(cntKey)
                .flatMap(v -> redis.expire(cntKey, COUNTER_TTL).thenReturn(v));
        Mono<Long> pfadd = ip == null ? Mono.just(0L)
                : redis.opsForHyperLogLog().add(hllKey, ip)
                        .flatMap(v -> redis.expire(hllKey, COUNTER_TTL).thenReturn(v));
        return incr.flatMap(count -> pfadd.thenReturn(count))
                .onErrorResume(e -> {
                    log.warn("Redis recordClick failed for code={} (non-fatal): {}", code, e.getMessage());
                    return Mono.just(0L);
                });
    }

    // ── Read side ──────────────────────────────────────────────────────────────

    /** Approximate unique visitors for one code across the given days (HLL union). */
    public Mono<Long> uniqueVisitorsForCode(String code, List<LocalDate> days) {
        if (days.isEmpty()) {
            return Mono.just(0L);
        }
        String[] keys = days.stream().map(d -> HLL_PREFIX + code + ":" + DAY.format(d)).toArray(String[]::new);
        return redis.opsForHyperLogLog().size(keys)
                .onErrorResume(e -> {
                    log.warn("Redis PFCOUNT failed for code={} (non-fatal): {}", code, e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Read-through cache: return the cached value for {@code key}, otherwise run
     * {@code loader}, cache its result for {@code ttl}, and return it. Cache
     * read/write failures degrade gracefully to {@code loader}.
     */
    public <T> Mono<T> cached(String key, TypeReference<T> type, Duration ttl, Mono<T> loader) {
        String redisKey = CACHE_PREFIX + key;
        return redis.opsForValue().get(redisKey)
                .<T>handle((json, sink) -> {
                    try {
                        sink.next(objectMapper.readValue(json, type));
                    } catch (Exception e) {
                        // Corrupt/incompatible cache entry — ignore and recompute.
                        log.debug("Ignoring unreadable cache entry {}: {}", redisKey, e.getMessage());
                    }
                })
                .switchIfEmpty(Mono.defer(() -> loader.flatMap(value -> store(redisKey, value, ttl).thenReturn(value))))
                .onErrorResume(e -> {
                    log.warn("Redis cache read failed for {} (non-fatal): {}", redisKey, e.getMessage());
                    return loader;
                });
    }

    private <T> Mono<Boolean> store(String redisKey, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return redis.opsForValue().set(redisKey, json, ttl)
                    .onErrorResume(e -> {
                        log.warn("Redis cache write failed for {} (non-fatal): {}", redisKey, e.getMessage());
                        return Mono.just(false);
                    });
        } catch (Exception e) {
            return Mono.just(false);
        }
    }
}
