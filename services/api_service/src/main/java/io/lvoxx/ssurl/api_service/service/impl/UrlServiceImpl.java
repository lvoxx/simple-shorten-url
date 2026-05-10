package io.lvoxx.ssurl.api_service.service.impl;

import io.lvoxx.ssurl.api_service.config.AppProperties;
import io.lvoxx.ssurl.api_service.repository.DomainBlacklistRepository;
import io.lvoxx.ssurl.api_service.repository.UrlRepository;
import io.lvoxx.ssurl.api_service.service.UrlService;
import io.lvoxx.ssurl.common.domain.Url;
import io.lvoxx.ssurl.common.dto.request.CreateUrlRequest;
import io.lvoxx.ssurl.common.dto.request.UpdateUrlRequest;
import io.lvoxx.ssurl.common.dto.response.UrlResponse;
import io.lvoxx.ssurl.common.exception.DomainBlacklistedException;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UrlNotFoundException;
import io.lvoxx.ssurl.common.mapper.UrlMapper;
import io.lvoxx.ssurl.common.util;
import io.seruco.encoding.base62;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

@Service
@Transactional
public class UrlServiceImpl implements UrlService {

    private static final String CACHE_PREFIX = "short:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final UrlRepository urlRepository;
    private final DomainBlacklistRepository domainBlacklistRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final UrlMapper urlMapper;
    private final AppProperties appProperties;
    private final Base62 base62;

    public UrlServiceImpl(
            UrlRepository urlRepository,
            DomainBlacklistRepository domainBlacklistRepository,
            ReactiveRedisTemplate<String, String> redisTemplate,
            UrlMapper urlMapper,
            AppProperties appProperties,
            Base62 base62) {
        this.urlRepository = urlRepository;
        this.domainBlacklistRepository = domainBlacklistRepository;
        this.redisTemplate = redisTemplate;
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
                    return urlRepository.save(url);
                })
                .flatMap(saved -> {
                    String shortCode = base62.encode(NumberToBytes.longToBytes(saved.getId()));
                    saved.getId()
                    saved.setShortCode(shortCode);
                    return urlRepository.save(saved);
                })
                .flatMap(saved -> {
                    String cacheKey = CACHE_PREFIX + saved.getShortCode();
                    return redisTemplate.opsForValue()
                            .set(cacheKey, saved.getOriginalUrl(), CACHE_TTL)
                            .thenReturn(saved);
                })
                .map(saved -> buildUrlResponse(saved));
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
    public Flux<UrlResponse> listByUser(Long userId) {
        return urlRepository.findAllByUserId(userId)
                .map(this::buildUrlResponse);
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
