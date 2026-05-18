package io.lvoxx.ssurl.common.service;

import java.time.Duration;

import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

/**
 * Abstract base for all service implementations that participate in
 * cache-backed, ACID-safe operations via Redisson batches.
 *
 * <p>
 * Cache key conventions (all {@code static final}):
 * <ul>
 * <li>{@code url:<shortCode>} – resolved original URL (hot path)</li>
 * <li>{@code url:id:<id>} – full URL entity by PK</li>
 * <li>{@code user:id:<id>} – UserResponse by PK</li>
 * <li>{@code user:name:<name>} – UserResponse by username</li>
 * <li>{@code auth:rt:<token>} – presence flag for a live refresh token</li>
 * </ul>
 *
 * <p>
 * <b>ACID contract with Redisson:</b><br>
 * Sub-classes call {@link #atomicCacheWrite} or {@link #atomicCacheEvict} which
 * use a {@link RBatch} (Redis pipeline + MULTI/EXEC) so that every cache
 * mutation is sent to Redis as a single atomic command group. The reactive DB
 * write must complete successfully before the batch is executed – the helper
 * returns a {@link Mono} that chains them in the correct order, giving
 * read-your-writes consistency for the happy path and leaving the cache
 * untouched on DB failure.
 *
 * <p>
 * Spring's declarative {@code @Cacheable} / {@code @CacheEvict} annotations
 * are layered on top of these helpers at the service method level for
 * secondary cache regions (e.g. {@code "users"}, {@code "urls"}) managed by
 * Spring Cache's Redisson adapter, while hot-path short-code lookups bypass
 * the abstraction layer and call {@link UrlCacheOperations} directly for
 * maximum throughput.
 */
public abstract class AbstractCacheableService {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final RedissonClient redisson;

    public AbstractCacheableService(RedissonClient redisson) {
        this.redisson = redisson;
    }

    // -------------------------------------------------------------------------
    // Atomic cache helpers
    // -------------------------------------------------------------------------

    /**
     * Writes {@code value} under {@code key} with {@code ttl} inside a Redisson
     * {@link RBatch} (Redis MULTI/EXEC). The batch is executed <em>after</em>
     * {@code dbMono} emits successfully, ensuring the DB is the source of truth.
     *
     * @param <T>    type emitted by the DB mono
     * @param dbMono reactive DB operation that must succeed first
     * @param key    full Redis key (use the {@code KEY_*} constants + suffix)
     * @param value  serialised value to store
     * @param ttl    time-to-live; pass {@link Duration#ZERO} for no expiry
     * @return a Mono that emits the DB result after both DB write and cache SET
     *         succeed
     */
    protected <T> Mono<T> atomicCacheWrite(Mono<T> dbMono, String key, String value, Duration ttl) {
        return dbMono.flatMap(result -> Mono.fromCallable(() -> {
            RBatch batch = redisson.createBatch();
            if (ttl.isZero()) {
                batch.getBucket(key).setAsync(value);
            } else {
                batch.getBucket(key).setAsync(value, ttl);
            }
            batch.execute();
            return result;
        }).doOnError(
                ex -> log.warn("Cache write failed for key={} – continuing (non-fatal): {}", key, ex.getMessage())));
    }

    /**
     * Evicts one or more {@code keys} inside a Redisson {@link RBatch} after
     * {@code dbMono} completes successfully.
     *
     * @param <T>    type emitted by the DB mono
     * @param dbMono reactive DB operation that must succeed first
     * @param keys   one or more full Redis keys to delete
     * @return a Mono that emits the DB result after both DB write and cache DEL
     *         succeed
     */
    protected <T> Mono<T> atomicCacheEvict(Mono<T> dbMono, String... keys) {
        return dbMono.flatMap(result -> Mono.fromCallable(() -> {
            RBatch batch = redisson.createBatch();
            for (String key : keys) {
                batch.getKeys().deleteAsync(key);
            }
            batch.execute();
            return result;
        }).doOnError(
                ex -> log.warn("Cache evict failed for keys={} – continuing (non-fatal): {}", keys, ex.getMessage())));
    }

    /**
     * Convenience overload of {@link #atomicCacheEvict} for
     * {@link Mono}{@code <Void>}
     * pipelines (delete / deactivate flows).
     */
    protected Mono<Void> atomicCacheEvictVoid(Mono<Void> dbMono, String... keys) {
        return dbMono.then(Mono.fromRunnable(() -> {
            try {
                RBatch batch = redisson.createBatch();
                for (String key : keys) {
                    batch.getKeys().deleteAsync(key);
                }
                batch.execute();
            } catch (Exception ex) {
                log.warn("Cache evict (void) failed for keys={} – continuing (non-fatal): {}", keys, ex.getMessage());
            }
        }));
    }
}