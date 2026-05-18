package io.lvoxx.ssurl.redirect_service.service.impl;

import io.lvoxx.ssurl.avro.AnalyticsEvent;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.util.Constants;
import io.lvoxx.ssurl.redirect_service.cache.UrlCacheService;
import io.lvoxx.ssurl.redirect_service.repository.UrlRepository;
import io.lvoxx.ssurl.redirect_service.service.RedirectService;
import org.redisson.api.RBloomFilter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

@Service
public class RedirectServiceImpl implements RedirectService {

    private final UrlRepository urlRepository;
    private final UrlCacheService urlCacheService;
    private final RBloomFilter<String> urlBloomFilter;
    private final KafkaTemplate<String, AnalyticsEvent> kafkaTemplate;

    public RedirectServiceImpl(
            UrlRepository urlRepository,
            UrlCacheService urlCacheService,
            RBloomFilter<String> urlBloomFilter,
            KafkaTemplate<String, AnalyticsEvent> kafkaTemplate) {
        this.urlRepository = urlRepository;
        this.urlCacheService = urlCacheService;
        this.urlBloomFilter = urlBloomFilter;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Mono<String> resolve(String shortCode, String ip, String userAgent, String referer) {
        // Bloom filter uses a synchronous Redisson call; run off the event loop
        return Mono.fromCallable(() -> urlBloomFilter.contains(shortCode))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ShortCodeNotFoundException(shortCode));
                    }
                    return urlCacheService.resolveOriginalUrl(shortCode);
                })
                .doOnSuccess(url -> recordAnalytics(shortCode, ip, userAgent, referer));
    }

    private void recordAnalytics(String shortCode, String ip, String userAgent, String referer) {
        AnalyticsEvent event = AnalyticsEvent.newBuilder()
                .setShortCode(shortCode)
                .setIp(ip)
                .setUserAgent(userAgent)
                .setReferer(referer)
                .setCreatedAt(Instant.now())
                .build();
        kafkaTemplate.send(Constants.Kafka.TOPIC_ANALYTICS_EVENTS, shortCode, event);
        urlRepository.incrementClickCount(shortCode).subscribe();
    }
}
