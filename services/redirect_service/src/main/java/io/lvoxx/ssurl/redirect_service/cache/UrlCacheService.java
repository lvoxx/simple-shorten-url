package io.lvoxx.ssurl.redirect_service.cache;

import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UrlExpiredException;
import io.lvoxx.ssurl.redirect_service.repository.UrlRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class UrlCacheService {

    private final UrlRepository urlRepository;

    public UrlCacheService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Cacheable(cacheNames = "short-urls", key = "#shortCode")
    public Mono<String> resolveOriginalUrl(String shortCode) {
        return urlRepository.findByShortCodeAndIsActive(shortCode, true)
                .switchIfEmpty(Mono.error(new ShortCodeNotFoundException(shortCode)))
                .flatMap(url -> {
                    if (url.getExpireAt() != null && url.getExpireAt().isBefore(LocalDateTime.now())) {
                        return Mono.error(new UrlExpiredException(shortCode));
                    }
                    return Mono.just(url.getOriginalUrl());
                });
    }
}
