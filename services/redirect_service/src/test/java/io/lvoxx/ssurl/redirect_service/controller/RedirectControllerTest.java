package io.lvoxx.ssurl.redirect_service.controller;

import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.common.exception.UrlExpiredException;
import io.lvoxx.ssurl.common.util.Constants;
import io.lvoxx.ssurl.redirect_service.service.RedirectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectControllerTest {

    @Mock private RedirectService redirectService;

    private RedirectController redirectController;

    @BeforeEach
    void setUp() {
        redirectController = new RedirectController(redirectService);
    }

    @Nested
    @DisplayName("redirect")
    class Redirect {

        @Test
        @DisplayName("returns 302 with location header on successful resolution")
        void redirect_validShortCode_returns302() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
            when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 80));

            when(redirectService.resolve("abc123", "127.0.0.1", null, null))
                    .thenReturn(Mono.just("https://example.com"));

            StepVerifier.create(redirectController.redirect("abc123", request))
                    .assertNext(response -> {
                        assertThat(response.getStatusCode().value()).isEqualTo(302);
                        assertThat(response.getHeaders().getLocation()).isNotNull();
                        assertThat(response.getHeaders().getLocation().toString()).isEqualTo("https://example.com");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("extracts IP from X-Forwarded-For header")
        void redirect_usesXForwardedFor() {
            var headers = new org.springframework.http.HttpHeaders();
            headers.add(Constants.Headers.X_FORWARDED_FOR, "203.0.113.5");
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            when(request.getHeaders()).thenReturn(headers);
            when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 80));

            when(redirectService.resolve("abc123", "203.0.113.5", null, null))
                    .thenReturn(Mono.just("https://example.com"));

            StepVerifier.create(redirectController.redirect("abc123", request))
                    .assertNext(response -> assertThat(response.getStatusCode().value()).isEqualTo(302))
                    .verifyComplete();
        }

        @Test
        @DisplayName("passes User-Agent and Referer headers to service")
        void redirect_passesHeaders() {
            var headers = new org.springframework.http.HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0");
            headers.add("Referer", "https://google.com");
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            when(request.getHeaders()).thenReturn(headers);
            when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 80));

            when(redirectService.resolve("abc123", "127.0.0.1", "Mozilla/5.0", "https://google.com"))
                    .thenReturn(Mono.just("https://example.com"));

            StepVerifier.create(redirectController.redirect("abc123", request))
                    .assertNext(response -> assertThat(response.getStatusCode().value()).isEqualTo(302))
                    .verifyComplete();
        }

        @Test
        @DisplayName("propagates ShortCodeNotFoundException")
        void redirect_notFound_propagatesError() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
            when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 80));

            when(redirectService.resolve("unknown", "127.0.0.1", null, null))
                    .thenReturn(Mono.error(new ShortCodeNotFoundException("unknown")));

            StepVerifier.create(redirectController.redirect("unknown", request))
                    .expectError(ShortCodeNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("propagates UrlExpiredException")
        void redirect_expired_propagatesError() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
            when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 80));

            when(redirectService.resolve("expired", "127.0.0.1", null, null))
                    .thenReturn(Mono.error(new UrlExpiredException("expired")));

            StepVerifier.create(redirectController.redirect("expired", request))
                    .expectError(UrlExpiredException.class)
                    .verify();
        }
    }
}
