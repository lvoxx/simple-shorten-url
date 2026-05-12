package io.lvoxx.ssurl.redirect_service.service.impl;

import io.lvoxx.ssurl.avro.AnalyticsEvent;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UrlExpiredException;
import io.lvoxx.ssurl.redirect_service.cache.UrlCacheService;
import io.lvoxx.ssurl.redirect_service.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectServiceImplTest {

    @Mock private UrlRepository urlRepository;
    @Mock private UrlCacheService urlCacheService;
    @Mock private RBloomFilter<String> urlBloomFilter;
    @Mock private KafkaTemplate<String, AnalyticsEvent> kafkaTemplate;

    private RedirectServiceImpl redirectService;

    @BeforeEach
    void setUp() {
        redirectService = new RedirectServiceImpl(
                urlRepository, urlCacheService, urlBloomFilter, kafkaTemplate);
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("returns original URL when bloom filter and cache hit")
        void resolve_bloomHit_cacheHit_returnsUrl() {
            when(urlBloomFilter.contains("abc123")).thenReturn(true);
            when(urlCacheService.resolveOriginalUrl("abc123")).thenReturn(Mono.just("https://example.com"));
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);
            when(urlRepository.incrementClickCount("abc123")).thenReturn(Mono.empty());

            StepVerifier.create(redirectService.resolve("abc123", "1.2.3.4", "Mozilla", "https://google.com"))
                    .assertNext(url -> assertThat(url).isEqualTo("https://example.com"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws ShortCodeNotFoundException when bloom filter miss")
        void resolve_bloomMiss_throws() {
            when(urlBloomFilter.contains("unknown")).thenReturn(false);

            StepVerifier.create(redirectService.resolve("unknown", "1.2.3.4", null, null))
                    .expectError(ShortCodeNotFoundException.class)
                    .verify();

            verify(urlCacheService, never()).resolveOriginalUrl(anyString());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("throws UrlExpiredException when URL is expired in cache")
        void resolve_cacheReturnsExpired_throws() {
            when(urlBloomFilter.contains("expired123")).thenReturn(true);
            when(urlCacheService.resolveOriginalUrl("expired123"))
                    .thenReturn(Mono.error(new UrlExpiredException("expired123")));

            StepVerifier.create(redirectService.resolve("expired123", "1.2.3.4", null, null))
                    .expectError(UrlExpiredException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws ShortCodeNotFoundException when cache miss and DB miss")
        void resolve_cacheMiss_dbMiss_throws() {
            when(urlBloomFilter.contains("missing")).thenReturn(true);
            when(urlCacheService.resolveOriginalUrl("missing"))
                    .thenReturn(Mono.error(new ShortCodeNotFoundException("missing")));

            StepVerifier.create(redirectService.resolve("missing", "1.2.3.4", null, null))
                    .expectError(ShortCodeNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("analytics recording")
    class Analytics {

        @Captor private ArgumentCaptor<AnalyticsEvent> eventCaptor;

        @Test
        @DisplayName("sends analytics event to Kafka on successful resolve")
        void sendsAnalyticsEvent() {
            when(urlBloomFilter.contains("abc123")).thenReturn(true);
            when(urlCacheService.resolveOriginalUrl("abc123")).thenReturn(Mono.just("https://example.com"));
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);
            when(urlRepository.incrementClickCount("abc123")).thenReturn(Mono.empty());

            StepVerifier.create(redirectService.resolve("abc123", "1.2.3.4", "curl/7.0", "https://referer.com"))
                    .expectNextCount(1)
                    .verifyComplete();

            verify(kafkaTemplate).send(eq("analytics-events"), eq("abc123"), eventCaptor.capture());
            AnalyticsEvent event = eventCaptor.getValue();
            assertThat(event.getShortCode().toString()).isEqualTo("abc123");
            assertThat(event.getIp().toString()).isEqualTo("1.2.3.4");
            assertThat(event.getUserAgent().toString()).isEqualTo("curl/7.0");
            assertThat(event.getReferer().toString()).isEqualTo("https://referer.com");
            assertThat(event.getCreatedAt()).isAfter(Instant.EPOCH);
        }

        @Test
        @DisplayName("sends analytics with null userAgent and referer when not provided")
        void sendsAnalyticsWithNulls() {
            when(urlBloomFilter.contains("abc123")).thenReturn(true);
            when(urlCacheService.resolveOriginalUrl("abc123")).thenReturn(Mono.just("https://example.com"));
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);
            when(urlRepository.incrementClickCount("abc123")).thenReturn(Mono.empty());

            StepVerifier.create(redirectService.resolve("abc123", "1.2.3.4", null, null))
                    .expectNextCount(1)
                    .verifyComplete();

            verify(kafkaTemplate).send(eq("analytics-events"), eq("abc123"), eventCaptor.capture());
            AnalyticsEvent event = eventCaptor.getValue();
            assertThat(event.getUserAgent()).isNull();
            assertThat(event.getReferer()).isNull();
        }
    }
}
