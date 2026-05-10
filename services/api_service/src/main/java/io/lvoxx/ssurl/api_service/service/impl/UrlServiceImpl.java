package io.lvoxx.ssurl.api_service.service.impl;

import io.lvoxx.ssurl.api_service.cache.UrlCacheOperations;
import io.lvoxx.ssurl.api_service.config.AppProperties;
import io.lvoxx.ssurl.api_service.repository.DomainBlacklistRepository;
import io.lvoxx.ssurl.api_service.repository.UrlRepository;
import io.lvoxx.ssurl.api_service.service.UrlService;
import io.lvoxx.ssurl.common.domain.Url;
import io.lvoxx.ssurl.common.dto.request.CreateUrlRequest;
import io.lvoxx.ssurl.common.dto.request.UpdateUrlRequest;
import io.lvoxx.ssurl.common.dto.response.CursorPage;
import io.lvoxx.ssurl.common.dto.response.UrlResponse;
import io.lvoxx.ssurl.common.exception.DomainBlacklistedException;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UrlNotFoundException;
import io.lvoxx.ssurl.common.mapper.UrlMapper;
import io.lvoxx.ssurl.common.util.NumberToBytes;
import io.seruco.encoding.base62.Base62;
import org.redisson.api.RBloomFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UrlServiceImpl implements UrlService {

    private static final int ANONYMOUS_EXPIRY_DAYS = 7;

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
            Base62 base62) {
        this.urlRepository = urlRepository;
        this.domainBlacklistRepository = domainBlacklistRepository;
        this.urlCacheOperations = urlCacheOperations;
        this.urlBloomFilter = urlBloomFilter;
        this.urlMapper = urlMapper;
        this.appProperties = appProperties;
        this.base62 = base62;
    }

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
                    // Anonymous users get a 7-day expiry unless one was explicitly provided
                    if (userId == null && url.getExpireAt() == null) {
                        url.setExpireAt(LocalDateTime.now().plusDays(ANONYMOUS_EXPIRY_DAYS));
                    }
                    return urlRepository.save(url);
                })
                .flatMap(saved -> {
                    String shortCode = new String(base62.encode(NumberToBytes.longToBytes(saved.getId())), StandardCharsets.US_ASCII);
                    saved.setShortCode(shortCode);
                    return urlRepository.save(saved);
                })
                .flatMap(saved -> {
                    urlBloomFilter.add(saved.getShortCode());
                    return urlCacheOperations.put(saved.getShortCode(), saved.getOriginalUrl())
                            .thenReturn(saved);
                })
                .map(this::buildUrlResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<UrlResponse> getByShortCode(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .switchIfEmpty(Mono.error(new ShortCodeNotFoundException(shortCode)))
                .map(this::buildUrlResponse);
    }

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
                    Long nextCursor = items.size() == safeSize ? items.get(items.size() - 1).id() : null;
                    return new CursorPage<>(items, nextCursor, nextCursor != null);
                });
    }

    @Override
    public Mono<UrlResponse> update(Long id, UpdateUrlRequest request, Long userId) {
        return urlRepository.findById(id)
                .switchIfEmpty(Mono.error(new UrlNotFoundException(id)))
                .flatMap(url -> {
                    if (!userId.equals(url.getUserId())) {
                        return Mono.error(new UnauthorizedException("You do not own this URL"));
                    }
                    if (request.title() != null) {
                        url.setTitle(request.title());
                    }
                    if (request.expireAt() != null) {
                        url.setExpireAt(request.expireAt());
                    }
                    if (request.isActive() != null) {
                        url.setActive(request.isActive());
                        if (Boolean.FALSE.equals(request.isActive())) {
                            urlCacheOperations.evict(url.getShortCode());
                        }
                    }
                    return urlRepository.save(url);
                })
                .map(this::buildUrlResponse);
    }

    @Override
    public Mono<Void> delete(Long id, Long userId) {
        return urlRepository.findById(id)
                .switchIfEmpty(Mono.error(new UrlNotFoundException(id)))
                .flatMap(url -> {
                    if (!userId.equals(url.getUserId())) {
                        return Mono.error(new UnauthorizedException("You do not own this URL"));
                    }
                    urlCacheOperations.evict(url.getShortCode());
                    url.setActive(false);
                    return urlRepository.save(url);
                })
                .then();
    }

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
                url.getCreatedAt()
        );
    }

    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null && host.startsWith("www.")) {
                return host.substring(4);
            }
            return host != null ? host : url;
        } catch (IllegalArgumentException e) {
            return url;
        }
    }
}
