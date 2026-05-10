package io.lvoxx.ssurl.api_service.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UrlCacheOperations {

    @CachePut(cacheNames = "short-urls", key = "#shortCode")
    public Mono<String> put(String shortCode, String originalUrl) {
        return Mono.just(originalUrl);
    }

    @CacheEvict(cacheNames = "short-urls", key = "#shortCode")
    public void evict(String shortCode) {
    }
}
