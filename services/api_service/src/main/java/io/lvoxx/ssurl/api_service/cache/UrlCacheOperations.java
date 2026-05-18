package io.lvoxx.ssurl.api_service.cache;

import io.lvoxx.ssurl.common.util.Constants;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UrlCacheOperations {

    @CachePut(cacheNames = Constants.Cache.SHORT_URLS, key = "#shortCode")
    public Mono<String> put(String shortCode, String originalUrl) {
        return Mono.just(originalUrl);
    }

    @CacheEvict(cacheNames = Constants.Cache.SHORT_URLS, key = "#shortCode")
    public void evict(String shortCode) {
    }
}
