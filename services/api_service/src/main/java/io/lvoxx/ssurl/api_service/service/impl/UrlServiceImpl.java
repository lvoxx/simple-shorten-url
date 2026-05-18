package io.lvoxx.ssurl.api_service.service.impl;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.lvoxx.ssurl.api_service.cache.UrlCacheOperations;
import io.lvoxx.ssurl.api_service.config.AppProperties;
import io.lvoxx.ssurl.api_service.repository.DomainBlacklistRepository;
import io.lvoxx.ssurl.api_service.repository.UrlRepository;
import io.lvoxx.ssurl.api_service.service.UrlService;
import io.lvoxx.ssurl.common.dto.request.CreateUrlRequest;
import io.lvoxx.ssurl.common.dto.request.UpdateUrlRequest;
import io.lvoxx.ssurl.common.dto.response.CursorPage;
import io.lvoxx.ssurl.common.dto.response.UrlResponse;
import io.lvoxx.ssurl.common.exception.DomainBlacklistedException;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UrlNotFoundException;
import io.lvoxx.ssurl.common.mapper.UrlMapper;
import io.lvoxx.ssurl.common.model.Url;
import io.lvoxx.ssurl.common.service.AbstractCacheableService;
import io.lvoxx.ssurl.common.util.Constants;
import io.lvoxx.ssurl.common.util.NumberToBytes;
import io.seruco.encoding.base62.Base62;
import reactor.core.publisher.Mono;

/**
 * URL shortening service with layered caching:
 *
 * <ol>
 * <li><b>Bloom filter</b> – pre-filters non-existent short codes before any
 * cache or DB lookup (false-positive rate is acceptable; the DB is the
 * authoritative gate).</li>
 * <li><b>Hot-path short-code cache</b> – {@link UrlCacheOperations} manages a
 * dedicated Redis hash/string structure for the redirect endpoint. This
 * path is intentionally kept outside Spring Cache so that it can be
 * accessed without AOP proxying overhead.</li>
 * <li><b>Spring {@code @Cacheable} / {@code @CacheEvict}</b> – applied to
 * {@code getByShortCode} (full {@link UrlResponse}) and all mutating
 * operations.</li>
 * <li><b>Redisson atomic batches</b> – every cache mutation that accompanies a
 * DB write is wrapped in {@link AbstractCacheableService#atomicCacheWrite}
 * or {@link AbstractCacheableService#atomicCacheEvict} so the Redis
 * commands are sent as a single MULTI/EXEC block <em>after</em> the DB
 * transaction commits.</li>
 * </ol>
 */
@Service
@Transactional
public class UrlServiceImpl extends AbstractCacheableService implements UrlService {

    private final UrlRepository urlRepository;
    private final DomainBlacklistRepository domainBlacklistRepository;
    private final UrlCacheOperations urlCacheOperations;
    private final RBloomFilter<String> urlBloomFilter;
    private final UrlMapper urlMapper;
    private final AppProperties appProperties;
    private final Base62 base62;

    public UrlServiceImpl(
            UrlRepository urlRepository,
            DomainBlacklistRepository domainBlacklistRepository,
            UrlCacheOperations urlCacheOperations,
            RBloomFilter<String> urlBloomFilter,
            UrlMapper urlMapper,
            AppProperties appProperties,
            Base62 base62,
            RedissonClient redisson) {
        super(redisson);
        this.urlRepository = urlRepository;
        this.domainBlacklistRepository = domainBlacklistRepository;
        this.urlCacheOperations = urlCacheOperations;
        this.urlBloomFilter = urlBloomFilter;
        this.urlMapper = urlMapper;
        this.appProperties = appProperties;
        this.base62 = base62;
    }

    // -------------------------------------------------------------------------
    // createUrl
    // -------------------------------------------------------------------------

    /**
     * Creates a shortened URL.
     *
     * <p>
     * Cache strategy (write-around + write-through):
     * <ol>
     * <li>DB INSERT (two saves: entity + shortCode back-fill).</li>
     * <li>Bloom filter populated.</li>
     * <li>Atomic Redisson batch:
     * <ul>
     * <li>SET {@code url:<shortCode>} → originalUrl (hot redirect path)</li>
     * <li>SET {@code url:id:<id>} → fullUrlJson (full-response path)</li>
     * </ul>
     * </li>
     * </ol>
     * The two Redis SETs are batched as one MULTI/EXEC so either both land or
     * neither does (idempotent – a cache miss simply falls back to the DB).
     */
    @Override
    public Mono<UrlResponse> createUrl(CreateUrlRequest request, Long userId, String createdBy) {
        String domain = extractDomain(request.originalUrl());
        return domainBlacklistRepository.existsByDomain(domain)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return Mono.error(new DomainBlacklistedException(domain));
                    }
                    Url url = urlMapper.toDomain(request);
                    url.setUserId(userId);
                    url.setCreatedBy(createdBy);
                    url.setUpdatedBy(createdBy);
                    if (userId == null && url.getExpireAt() == null) {
                        url.setExpireAt(LocalDateTime.now().plusDays(Constants.Business.ANONYMOUS_EXPIRY_DAYS));
                    }
                    return urlRepository.save(url);
                })
                .flatMap(saved -> {
                    String shortCode = new String(
                            base62.encode(NumberToBytes.longToBytes(saved.getId())),
                            StandardCharsets.US_ASCII);
                    saved.setShortCode(shortCode);
                    return urlRepository.save(saved);
                })
                .flatMap(saved -> {
                    urlBloomFilter.add(saved.getShortCode());
                    UrlResponse response = buildUrlResponse(saved);

                    // Atomic batch: hot-path + full-response caches together
                    return atomicCacheWrite(
                            // chain the existing UrlCacheOperations.put inside the DB Mono
                            urlCacheOperations.put(saved.getShortCode(), saved.getOriginalUrl())
                                    .thenReturn(saved),
                            Constants.Cache.KEY_URL_BY_ID + saved.getId(),
                            saved.getShortCode(), // lightweight pointer; full response via getByShortCode
                            Constants.TTL.TTL_URL).thenReturn(response);
                });
    }

    // -------------------------------------------------------------------------
    // getByShortCode
    // -------------------------------------------------------------------------

    /**
     * Looks up a URL by its short code.
     *
     * <p>
     * Cache key: {@code url:<shortCode>} in region {@value #CACHE_SHORT_CODES}.
     * On a cache miss, Spring falls through to the DB. The bloom filter is
     * checked by the redirect controller before this method is called, so by the
     * time we reach here the short code is likely valid.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = Constants.Cache.CACHE_SHORT_CODES, key = "'" + Constants.Cache.KEY_URL_BY_SHORT
            + "' + #shortCode", unless = "#result == null")
    public Mono<UrlResponse> getByShortCode(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .switchIfEmpty(Mono.error(new ShortCodeNotFoundException(shortCode)))
                .map(this::buildUrlResponse);
    }

    // -------------------------------------------------------------------------
    // listByUser
    // -------------------------------------------------------------------------

    /**
     * Paginated listing – not cached (cursor-based pages are not cache-friendly).
     */
    @Override
    @Transactional(readOnly = true)
    public Mono<CursorPage<UrlResponse>> listByUser(Long userId, Long cursor, int size) {
        int safeSize = Math.min(size, 100);
        return (cursor == null
                ? urlRepository.findTopByUserIdOrderByIdDesc(userId, safeSize)
                : urlRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, safeSize))
                .map(this::buildUrlResponse)
                .collectList()
                .map(items -> {
                    Long nextCursor = items.size() == safeSize
                            ? items.get(items.size() - 1).id()
                            : null;
                    return new CursorPage<>(items, nextCursor, nextCursor != null);
                });
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    /**
     * Updates a URL record.
     *
     * <p>
     * Cache strategy (write-through + invalidation):
     * <ul>
     * <li>{@code @CacheEvict} on {@value #CACHE_SHORT_CODES} and
     * {@value #CACHE_URLS}
     * removes stale full-response entries.</li>
     * <li>If the URL is being <em>deactivated</em>, the hot-path redirect cache
     * is also evicted via an atomic Redisson batch so the redirect controller
     * stops serving the entry immediately.</li>
     * </ul>
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.Cache.CACHE_SHORT_CODES, key = "'" + Constants.Cache.KEY_URL_BY_SHORT
                    + "' + #result.shortCode()", condition = "#result != null"),
            @CacheEvict(cacheNames = Constants.Cache.CACHE_URLS, key = "'" + Constants.Cache.KEY_URL_BY_ID + "' + #id")
    })
    public Mono<UrlResponse> update(Long id, UpdateUrlRequest request, Long userId) {
        return urlRepository.findById(id)
                .switchIfEmpty(Mono.error(new UrlNotFoundException(id)))
                .flatMap(url -> {
                    if (!userId.equals(url.getUserId())) {
                        return Mono.error(new UnauthorizedException("You do not own this URL"));
                    }
                    if (request.title() != null)
                        url.setTitle(request.title());
                    if (request.expireAt() != null)
                        url.setExpireAt(request.expireAt());

                    boolean deactivating = Boolean.FALSE.equals(request.isActive());
                    if (request.isActive() != null)
                        url.setActive(request.isActive());

                    Mono<Url> dbSave = urlRepository.save(url);

                    if (deactivating) {
                        // Atomic: DB save + evict hot-path redirect cache together
                        return atomicCacheEvict(dbSave, Constants.Cache.KEY_URL_BY_SHORT + url.getShortCode())
                                .doOnNext(saved -> urlCacheOperations.evict(saved.getShortCode()));
                    }
                    return dbSave;
                })
                .map(this::buildUrlResponse);
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    /**
     * Soft-deletes (deactivates) a URL.
     *
     * <p>
     * Both the hot-path redirect cache and the full-response cache are evicted
     * atomically via a Redisson batch after the DB update commits.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.Cache.CACHE_SHORT_CODES, allEntries = false, key = "'"
                    + Constants.Cache.KEY_URL_BY_SHORT
                    + "' + #root.target.getShortCodeById(#id)"),
            @CacheEvict(cacheNames = Constants.Cache.CACHE_URLS, key = "'" + Constants.Cache.KEY_URL_BY_ID + "' + #id")
    })
    public Mono<Void> delete(Long id, Long userId) {
        return urlRepository.findById(id)
                .switchIfEmpty(Mono.error(new UrlNotFoundException(id)))
                .flatMap(url -> {
                    if (!userId.equals(url.getUserId())) {
                        return Mono.error(new UnauthorizedException("You do not own this URL"));
                    }
                    url.setActive(false);
                    Mono<Url> dbSave = urlRepository.save(url);
                    // Atomic: soft-delete DB write + evict both cache regions
                    return atomicCacheEvict(
                            dbSave,
                            Constants.Cache.KEY_URL_BY_SHORT + url.getShortCode(),
                            Constants.Cache.KEY_URL_BY_ID + url.getId())
                            .doOnNext(saved -> urlCacheOperations.evict(saved.getShortCode()));
                })
                .then();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UrlResponse buildUrlResponse(Url url) {
        String shortUrl = appProperties.getShortUrlBase() + "/" + url.getShortCode();
        return new UrlResponse(
                url.getId(),
                url.getShortCode(),
                shortUrl,
                url.getOriginalUrl(),
                url.getTitle(),
                url.isActive(),
                url.getClickCount(),
                url.getExpireAt(),
                url.getCreatedAt());
    }

    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null && host.startsWith(Constants.Patterns.WWW_PREFIX)) {
                return host.substring(Constants.Patterns.WWW_PREFIX.length());
            }
            return host != null ? host : url;
        } catch (IllegalArgumentException e) {
            return url;
        }
    }
}
