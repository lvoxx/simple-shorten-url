package io.lvoxx.ssurl.api_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import io.lvoxx.ssurl.api_service.service.AuthService;
import io.lvoxx.ssurl.common.dto.request.LoginRequest;
import io.lvoxx.ssurl.common.dto.request.RegisterRequest;
import io.lvoxx.ssurl.common.dto.response.AuthResponse;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UserAlreadyExistsException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    private AuthController authController;

    private static final UserResponse USER_RESPONSE = new UserResponse(
            1L, "alice", "alice@example.com", "USER", true, LocalDateTime.now());
    private static final AuthResponse AUTH_RESPONSE = new AuthResponse(
            "access-token", "Bearer", 900L, USER_RESPONSE);

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("returns 201 with user response on success")
        void register_success() {
            RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
            when(authService.register(request)).thenReturn(Mono.just(USER_RESPONSE));

            StepVerifier.create(authController.register(request))
                    .assertNext(response -> {
                        assertThat(response.getStatusCode().value()).isEqualTo(201);
                        assertThat(response.getBody()).isNotNull();
                        assertThat(response.getBody().username()).isEqualTo("alice");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("propagates UserAlreadyExistsException")
        void register_userExists_propagatesError() {
            RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
            when(authService.register(request)).thenReturn(Mono.error(new UserAlreadyExistsException("alice")));

            StepVerifier.create(authController.register(request))
                    .expectError(UserAlreadyExistsException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns 200 with auth response on success")
        void login_success() {
            LoginRequest request = new LoginRequest("alice", "password123");
            ServerHttpResponse response = mock(ServerHttpResponse.class);
            when(authService.login(request, response)).thenReturn(Mono.just(AUTH_RESPONSE));

            StepVerifier.create(authController.login(request, response))
                    .assertNext(res -> {
                        assertThat(res.getStatusCode().value()).isEqualTo(200);
                        assertThat(res.getBody()).isNotNull();
                        assertThat(res.getBody().accessToken()).isEqualTo("access-token");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("propagates UnauthorizedException on bad credentials")
        void login_badCredentials_propagatesError() {
            LoginRequest request = new LoginRequest("alice", "wrong");
            ServerHttpResponse response = mock(ServerHttpResponse.class);
            when(authService.login(request, response)).thenReturn(Mono.error(new UnauthorizedException("Invalid credentials")));

            StepVerifier.create(authController.login(request, response))
                    .expectError(UnauthorizedException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("returns 200 with new auth response using cookie")
        void refresh_withCookie_success() {
            when(authService.refresh("refresh-token-from-cookie")).thenReturn(Mono.just(AUTH_RESPONSE));

            StepVerifier.create(authController.refresh("refresh-token-from-cookie", mock(ServerHttpRequest.class)))
                    .assertNext(res -> {
                        assertThat(res.getStatusCode().value()).isEqualTo(200);
                        assertThat(res.getBody().accessToken()).isEqualTo("access-token");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("falls back to X-Refresh-Token header when cookie is absent")
        void refresh_withHeader_fallback() {
            var headers = new org.springframework.http.HttpHeaders();
            headers.add("X-Refresh-Token", "refresh-token-from-header");
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            when(request.getHeaders()).thenReturn(headers);
            when(authService.refresh("refresh-token-from-header")).thenReturn(Mono.just(AUTH_RESPONSE));

            StepVerifier.create(authController.refresh(null, request))
                    .assertNext(res -> assertThat(res.getStatusCode().value()).isEqualTo(200))
                    .verifyComplete();
        }

        @Test
        @DisplayName("propagates error when both cookie and header are missing")
        void refresh_noToken_propagatesError() {
            ServerHttpRequest request = mock(ServerHttpRequest.class);
            when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
            when(authService.refresh(null)).thenReturn(Mono.error(new UnauthorizedException("Invalid or expired refresh token")));

            StepVerifier.create(authController.refresh(null, request))
                    .expectError(UnauthorizedException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("returns 204 on successful logout")
        void logout_success() {
            ServerHttpResponse response = mock(ServerHttpResponse.class);
            when(authService.logout("refresh-token", response)).thenReturn(Mono.empty());

            StepVerifier.create(authController.logout("refresh-token", response))
                    .assertNext(res -> assertThat(res.getStatusCode().value()).isEqualTo(204))
                    .verifyComplete();
        }

        @Test
        @DisplayName("returns 204 even without a refresh token")
        void logout_noToken_stillReturns204() {
            ServerHttpResponse response = mock(ServerHttpResponse.class);
            when(authService.logout(null, response)).thenReturn(Mono.empty());

            StepVerifier.create(authController.logout(null, response))
                    .assertNext(res -> assertThat(res.getStatusCode().value()).isEqualTo(204))
                    .verifyComplete();
        }
    }
}
