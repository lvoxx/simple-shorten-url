package io.lvoxx.ssurl.redirect_service.cache;

import io.lvoxx.ssurl.common.model.Url;
import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UrlExpiredException;
import io.lvoxx.ssurl.redirect_service.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlCacheServiceTest {

    @Mock private UrlRepository urlRepository;

    private UrlCacheService urlCacheService;

    @BeforeEach
    void setUp() {
        urlCacheService = new UrlCacheService(urlRepository);
    }

    @Nested
    @DisplayName("resolveOriginalUrl")
    class ResolveOriginalUrl {

        @Test
        @DisplayName("returns original URL when URL is active and not expired")
        void resolve_activeNotExpired_returnsUrl() {
            Url url = new Url();
            url.setShortCode("abc123");
            url.setOriginalUrl("https://example.com");
            url.setActive(true);
            url.setExpireAt(LocalDateTime.now().plusDays(1));

            when(urlRepository.findByShortCodeAndIsActive("abc123", true)).thenReturn(Mono.just(url));

            StepVerifier.create(urlCacheService.resolveOriginalUrl("abc123"))
                    .assertNext(originalUrl -> assertThat(originalUrl).isEqualTo("https://example.com"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("returns original URL when expireAt is null")
        void resolve_noExpiry_returnsUrl() {
            Url url = new Url();
            url.setShortCode("abc123");
            url.setOriginalUrl("https://example.com");
            url.setActive(true);
            url.setExpireAt(null);

            when(urlRepository.findByShortCodeAndIsActive("abc123", true)).thenReturn(Mono.just(url));

            StepVerifier.create(urlCacheService.resolveOriginalUrl("abc123"))
                    .assertNext(originalUrl -> assertThat(originalUrl).isEqualTo("https://example.com"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws UrlExpiredException when URL is expired")
        void resolve_expired_throws() {
            Url url = new Url();
            url.setShortCode("expired123");
            url.setOriginalUrl("https://example.com");
            url.setActive(true);
            url.setExpireAt(LocalDateTime.now().minusDays(1));

            when(urlRepository.findByShortCodeAndIsActive("expired123", true)).thenReturn(Mono.just(url));

            StepVerifier.create(urlCacheService.resolveOriginalUrl("expired123"))
                    .expectError(UrlExpiredException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws ShortCodeNotFoundException when URL is inactive")
        void resolve_inactive_throws() {
            when(urlRepository.findByShortCodeAndIsActive("inactive", true)).thenReturn(Mono.empty());

            StepVerifier.create(urlCacheService.resolveOriginalUrl("inactive"))
                    .expectError(ShortCodeNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws ShortCodeNotFoundException when short code does not exist")
        void resolve_notFound_throws() {
            when(urlRepository.findByShortCodeAndIsActive("unknown", true)).thenReturn(Mono.empty());

            StepVerifier.create(urlCacheService.resolveOriginalUrl("unknown"))
                    .expectError(ShortCodeNotFoundException.class)
                    .verify();
        }
    }
}
