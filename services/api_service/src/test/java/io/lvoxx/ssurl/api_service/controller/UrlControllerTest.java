package io.lvoxx.ssurl.api_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lvoxx.ssurl.api_service.service.ContextUserService;
import io.lvoxx.ssurl.api_service.service.UrlService;
import io.lvoxx.ssurl.common.dto.request.CreateUrlRequest;
import io.lvoxx.ssurl.common.dto.response.CursorPage;
import io.lvoxx.ssurl.common.dto.response.UrlResponse;
import io.lvoxx.ssurl.common.exception.DomainBlacklistedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UrlControllerTest {

    @Mock
    private UrlService urlService;
    @Mock
    private ContextUserService contextUserService;

    private UrlController urlController;

    private static final UrlResponse SAMPLE_RESPONSE = new UrlResponse(
            1L, "abc123", "http://short.ly/abc123",
            "https://example.com/long", "My Link", true, 0L, null, LocalDateTime.now());

    @BeforeEach
    void setUp() {
        urlController = new UrlController(urlService, contextUserService);
    }

    @Nested
    @DisplayName("createUrl")
    class CreateUrl {

        @Test
        @DisplayName("delegates to service with null userId for anonymous request")
        void createUrl_noSecurityContext_callsServiceWithNullUserId() {
            CreateUrlRequest request = new CreateUrlRequest("https://example.com/long", null, null);
            when(urlService.createUrl(any(), isNull(), anyString()))
                    .thenReturn(Mono.just(SAMPLE_RESPONSE));

            StepVerifier.create(urlController.createUrl(request))
                    .assertNext(response -> {
                        assertThat(response.getStatusCode().value()).isEqualTo(201);
                        assertThat(response.getBody()).isNotNull();
                        assertThat(response.getBody().shortCode()).isEqualTo("abc123");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("propagates DomainBlacklistedException from service")
        void createUrl_blacklistedDomain_propagatesError() {
            CreateUrlRequest request = new CreateUrlRequest("https://spam.com/path", null, null);
            when(urlService.createUrl(any(), isNull(), anyString()))
                    .thenReturn(Mono.error(new DomainBlacklistedException("spam.com")));

            StepVerifier.create(urlController.createUrl(request))
                    .expectError(DomainBlacklistedException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("listMyUrls")
    class ListMyUrls {

        @Test
        @DisplayName("returns cursor page with default size when no parameters given")
        void listMyUrls_withAuthenticatedUser_returnsCursorPage() {
            CursorPage<UrlResponse> page = new CursorPage<>(List.of(SAMPLE_RESPONSE), null, false);
            when(urlService.listByUser(anyLong(), isNull(), anyInt())).thenReturn(Mono.just(page));

            StepVerifier.create(urlService.listByUser(1L, null, 20))
                    .assertNext(p -> {
                        assertThat(p.content()).hasSize(1);
                        assertThat(p.hasNext()).isFalse();
                        assertThat(p.nextCursor()).isNull();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("passes cursor and size to service")
        void listMyUrls_withCursorAndSize_passesParamsToService() {
            CursorPage<UrlResponse> page = new CursorPage<>(List.of(), null, false);
            when(urlService.listByUser(1L, 50L, 10)).thenReturn(Mono.just(page));

            StepVerifier.create(urlService.listByUser(1L, 50L, 10))
                    .assertNext(p -> {
                        assertThat(p.content()).isEmpty();
                        assertThat(p.hasNext()).isFalse();
                    })
                    .verifyComplete();
        }
    }
}
