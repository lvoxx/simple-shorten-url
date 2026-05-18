package io.lvoxx.ssurl.api_service.service.impl;

import io.lvoxx.ssurl.api_service.cache.UrlCacheOperations;
import io.lvoxx.ssurl.api_service.config.AppProperties;
import io.lvoxx.ssurl.api_service.repository.DomainBlacklistRepository;
import io.lvoxx.ssurl.api_service.repository.UrlRepository;
import io.lvoxx.ssurl.api_service.service.impl.UrlServiceImpl;
import io.lvoxx.ssurl.common.model.Url;
import io.lvoxx.ssurl.common.dto.request.CreateUrlRequest;
import io.lvoxx.ssurl.common.dto.request.UpdateUrlRequest;
import io.lvoxx.ssurl.common.exception.DomainBlacklistedException;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UrlNotFoundException;
import io.lvoxx.ssurl.common.mapper.UrlMapper;
import io.seruco.encoding.base62.Base62;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock private UrlRepository urlRepository;
    @Mock private DomainBlacklistRepository domainBlacklistRepository;
    @Mock private UrlCacheOperations urlCacheOperations;
    @Mock private RBloomFilter<String> urlBloomFilter;
    @Mock private UrlMapper urlMapper;
    @Mock private AppProperties appProperties;
    @Mock private Base62 base62;

    private UrlServiceImpl urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlServiceImpl(
                urlRepository, domainBlacklistRepository, urlCacheOperations,
                urlBloomFilter, urlMapper, appProperties, base62);
    }

    @Nested
    @DisplayName("createUrl")
    class CreateUrl {

        private CreateUrlRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateUrlRequest("https://example.com/long-path", "My Link", null);
        }

        @Test
        @DisplayName("creates URL for authenticated user with no default expiry")
        void createUrl_authenticatedUser_noExpiry() {
            Url mappedUrl = new Url();
            mappedUrl.setOriginalUrl("https://example.com/long-path");
            Url savedUrl = urlWithId(1L, "abc123");

            when(domainBlacklistRepository.existsByDomain("example.com")).thenReturn(Mono.just(false));
            when(urlMapper.toDomain(request)).thenReturn(mappedUrl);
            when(urlRepository.save(any())).thenReturn(Mono.just(savedUrl));
            when(base62.encode(any())).thenReturn("abc123".getBytes());
            when(urlCacheOperations.put(anyString(), anyString())).thenReturn(Mono.just("https://example.com/long-path"));
            when(appProperties.getShortUrlBase()).thenReturn("http://short.ly");

            StepVerifier.create(urlService.createUrl(request, 42L, "alice"))
                    .assertNext(response -> {
                        assertThat(response.shortCode()).isNotNull();
                        assertThat(response.expireAt()).isNull();
                    })
                    .verifyComplete();

            verify(urlBloomFilter).add(anyString());
            verify(urlCacheOperations).put(anyString(), anyString());
        }

        @Test
        @DisplayName("sets 7-day expiry for anonymous users")
        void createUrl_anonymousUser_gets7DayExpiry() {
            Url mappedUrl = new Url();
            mappedUrl.setOriginalUrl("https://example.com/long-path");

            when(domainBlacklistRepository.existsByDomain("example.com")).thenReturn(Mono.just(false));
            when(urlMapper.toDomain(request)).thenReturn(mappedUrl);
            when(urlRepository.save(any())).thenAnswer(inv -> {
                Url u = inv.getArgument(0);
                u.setId(1L);
                u.setShortCode("abc123");
                return Mono.just(u);
            });
            when(base62.encode(any())).thenReturn("abc123".getBytes());
            when(urlCacheOperations.put(anyString(), anyString())).thenReturn(Mono.just("https://example.com/long-path"));
            when(appProperties.getShortUrlBase()).thenReturn("http://short.ly");

            StepVerifier.create(urlService.createUrl(request, null, "anonymous"))
                    .assertNext(response -> {
                        assertThat(response.expireAt()).isNotNull();
                        assertThat(response.expireAt()).isAfter(LocalDateTime.now().plusDays(6));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws DomainBlacklistedException for blacklisted domains")
        void createUrl_blacklistedDomain_throws() {
            CreateUrlRequest spamRequest = new CreateUrlRequest("https://spam.com/path", null, null);
            when(domainBlacklistRepository.existsByDomain("spam.com")).thenReturn(Mono.just(true));

            StepVerifier.create(urlService.createUrl(spamRequest, 1L, "alice"))
                    .expectError(DomainBlacklistedException.class)
                    .verify();

            verify(urlRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listByUser")
    class ListByUser {

        @Test
        @DisplayName("returns first page when cursor is null")
        void listByUser_noCursor_returnsFirstPage() {
            Url url = urlWithId(10L, "abc");
            when(urlRepository.findTopByUserIdOrderByIdDesc(1L, 20)).thenReturn(Flux.just(url));
            when(appProperties.getShortUrlBase()).thenReturn("http://short.ly");

            StepVerifier.create(urlService.listByUser(1L, null, 20))
                    .assertNext(page -> {
                        assertThat(page.content()).hasSize(1);
                        assertThat(page.hasNext()).isFalse();
                        assertThat(page.nextCursor()).isNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("returns next cursor when full page is returned")
        void listByUser_fullPage_includesNextCursor() {
            Url[] urls = new Url[20];
            for (int i = 0; i < 20; i++) {
                urls[i] = urlWithId((long) (20 - i), "code" + i);
            }
            when(urlRepository.findTopByUserIdOrderByIdDesc(1L, 20)).thenReturn(Flux.fromArray(urls));
            when(appProperties.getShortUrlBase()).thenReturn("http://short.ly");

            StepVerifier.create(urlService.listByUser(1L, null, 20))
                    .assertNext(page -> {
                        assertThat(page.content()).hasSize(20);
                        assertThat(page.hasNext()).isTrue();
                        assertThat(page.nextCursor()).isEqualTo(1L);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deactivates URL and evicts from cache")
        void delete_ownedUrl_deactivatesAndEvictsCache() {
            Url url = urlWithId(1L, "abc123");
            url.setUserId(42L);
            when(urlRepository.findById(1L)).thenReturn(Mono.just(url));
            when(urlRepository.save(any())).thenReturn(Mono.just(url));

            StepVerifier.create(urlService.delete(1L, 42L))
                    .verifyComplete();

            verify(urlCacheOperations).evict("abc123");
        }

        @Test
        @DisplayName("throws UnauthorizedException when user does not own the URL")
        void delete_notOwnedUrl_throwsUnauthorized() {
            Url url = urlWithId(1L, "abc123");
            url.setUserId(99L);
            when(urlRepository.findById(1L)).thenReturn(Mono.just(url));

            StepVerifier.create(urlService.delete(1L, 42L))
                    .expectError(UnauthorizedException.class)
                    .verify();

            verify(urlCacheOperations, never()).evict(anyString());
        }

        @Test
        @DisplayName("throws UrlNotFoundException when URL does not exist")
        void delete_missingUrl_throwsNotFound() {
            when(urlRepository.findById(999L)).thenReturn(Mono.empty());

            StepVerifier.create(urlService.delete(999L, 42L))
                    .expectError(UrlNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("evicts cache when URL is deactivated")
        void update_deactivate_evictsCache() {
            Url url = urlWithId(1L, "abc123");
            url.setUserId(42L);
            url.setActive(true);
            when(urlRepository.findById(1L)).thenReturn(Mono.just(url));
            when(urlRepository.save(any())).thenReturn(Mono.just(url));
            when(appProperties.getShortUrlBase()).thenReturn("http://short.ly");

            StepVerifier.create(urlService.update(1L, new UpdateUrlRequest(null, null, false), 42L))
                    .assertNext(response -> assertThat(response.isActive()).isFalse())
                    .verifyComplete();

            verify(urlCacheOperations).evict("abc123");
        }

        @Test
        @DisplayName("does not evict cache when only title is updated")
        void update_titleOnly_noCacheEvict() {
            Url url = urlWithId(1L, "abc123");
            url.setUserId(42L);
            url.setActive(true);
            when(urlRepository.findById(1L)).thenReturn(Mono.just(url));
            when(urlRepository.save(any())).thenReturn(Mono.just(url));
            when(appProperties.getShortUrlBase()).thenReturn("http://short.ly");

            StepVerifier.create(urlService.update(1L, new UpdateUrlRequest("New Title", null, null), 42L))
                    .assertNext(response -> assertThat(response).isNotNull())
                    .verifyComplete();

            verify(urlCacheOperations, never()).evict(anyString());
        }
    }

    private Url urlWithId(Long id, String shortCode) {
        Url url = new Url();
        url.setId(id);
        url.setShortCode(shortCode);
        url.setOriginalUrl("https://example.com");
        url.setActive(true);
        return url;
    }
}
