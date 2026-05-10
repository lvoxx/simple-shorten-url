package io.lvoxx.ssurl.redirect_service.service.impl;

import io.lvoxx.ssurl.avro.AnalyticsEvent;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UrlExpiredException;
import io.lvoxx.ssurl.redirect_service.repository.UrlRepository;
import io.lvoxx.ssurl.redirect_service.service.RedirectService;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class RedirectServiceImpl implements RedirectService {

    private final UrlRepository urlRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RBloomFilter<String> urlBloomFilter;
    private final KafkaTemplate<String, AnalyticsEvent> kafkaTemplate;

    public RedirectServiceImpl(
            UrlRepository urlRepository,
            ReactiveRedisTemplate<String, String> redisTemplate,
            RBloomFilter<String> urlBloomFilter,
            KafkaTemplate<String, AnalyticsEvent> kafkaTemplate) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
        this.urlBloomFilter = urlBloomFilter;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Mono<String> resolve(String shortCode, String ip, String userAgent, String referer) {
        if (!urlBloomFilter.contains(shortCode)) {
            return Mono.error(new ShortCodeNotFoundException(shortCode));
        }
        String cacheKey = "short:" + shortCode;
        return redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(loadFromDatabase(shortCode, cacheKey))
                .doOnSuccess(url -> recordAnalytics(shortCode, ip, userAgent, referer));
    }

    private Mono<String> loadFromDatabase(String shortCode, String cacheKey) {
        return urlRepository.findByShortCodeAndIsActive(shortCode, true)
                .switchIfEmpty(Mono.error(new ShortCodeNotFoundException(shortCode)))
                .flatMap(url -> {
                    if (url.getExpireAt() != null && url.getExpireAt().isBefore(LocalDateTime.now())) {
                        return Mono.error(new UrlExpiredException(shortCode));
                    }
                    return redisTemplate.opsForValue()
                            .set(cacheKey, url.getOriginalUrl(), Duration.ofHours(24))
                            .thenReturn(url.getOriginalUrl());
                });
    }

    private void recordAnalytics(String shortCode, String ip, String userAgent, String referer) {
        AnalyticsEvent event = AnalyticsEvent.newBuilder()
                .setShortCode(shortCode)
                .setIp(ip)
                .setUserAgent(userAgent)
                .setReferer(referer)
                .setCreatedAt(Instant.now().toEpochMilli())
                .build();
        kafkaTemplate.send("analytics-events", shortCode, event);
        urlRepository.incrementClickCount(shortCode).subscribe();
    }
}
