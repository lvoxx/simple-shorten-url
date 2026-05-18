package io.lvoxx.ssurl.api_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBatch;
import org.redisson.api.RBucketAsync;
import org.redisson.api.RKeysAsync;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.lvoxx.ssurl.api_service.repository.RefreshTokenRepository;
import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.security.JwtTokenProvider;
import io.lvoxx.ssurl.common.dto.request.LoginRequest;
import io.lvoxx.ssurl.common.dto.request.RegisterRequest;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UserAlreadyExistsException;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import io.lvoxx.ssurl.common.mapper.UserMapper;
import io.lvoxx.ssurl.common.model.RefreshToken;
import io.lvoxx.ssurl.common.model.User;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link AuthServiceImpl}.
 *
 * <p>
 * Strategy:
 * <ul>
 * <li>All repositories and infrastructure collaborators are Mockito mocks.</li>
 * <li>Redisson batch chain ({@code createBatch → getBucket/getKeys → execute})
 * is
 * stubbed to no-op so cache helpers complete without a real Redis
 * connection.</li>
 * <li>{@link reactor.test.StepVerifier} is used to assert reactive
 * pipelines.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    // ── Infrastructure mocks ──────────────────────────────────────────────────

    @Mock
    UserRepository userRepository;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    JwtTokenProvider jwtTokenProvider;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    UserMapper userMapper;

    // Redisson chain: client → batch → bucket / keys
    @Mock
    RedissonClient redisson;
    @Mock
    RBatch rBatch;
    @Mock
    RBucketAsync<Object> rBucket;
    @Mock
    RKeysAsync rKeys;

    @Mock
    ServerHttpResponse httpResponse;

    @InjectMocks
    AuthServiceImpl authService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("lvoxx");
        testUser.setEmail("lvoxx@example.com");
        testUser.setPasswordHash("$2a$hashed");
        testUser.setRole("USER");
        testUser.setActive(true);

        testUserResponse = new UserResponse(1L, "lvoxx", "lvoxx@example.com", "USER", true, LocalDateTime.now());

        // Stub the Redisson batch chain used by atomicCacheWrite / atomicCacheEvict
        when(redisson.createBatch()).thenReturn(rBatch);
        when(rBatch.getBucket(anyString())).thenReturn(rBucket);
        when(rBatch.getKeys()).thenReturn(rKeys);
        when(rKeys.deleteAsync(any(String[].class))).thenReturn(null);
        when(rBatch.execute()).thenReturn(null);

        // Stub HttpResponse so addCookie doesn't throw
        when(httpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(new HttpHeaders()));
    }

    // =========================================================================
    // register
    // =========================================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("happy path – returns saved UserResponse")
        void register_success() {
            RegisterRequest req = new RegisterRequest("lvoxx", "lvoxx@example.com", "secret123");

            when(userRepository.existsByUsername("lvoxx")).thenReturn(Mono.just(false));
            when(userRepository.existsByEmail("lvoxx@example.com")).thenReturn(Mono.just(false));
            when(passwordEncoder.encode("secret123")).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            StepVerifier.create(authService.register(req))
                    .assertNext(response -> {
                        assertThat(response.username()).isEqualTo("lvoxx");
                        assertThat(response.email()).isEqualTo("lvoxx@example.com");
                    })
                    .verifyComplete();

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("duplicate username – emits UserAlreadyExistsException")
        void register_duplicateUsername_throwsException() {
            RegisterRequest req = new RegisterRequest("lvoxx", "other@example.com", "secret123");

            when(userRepository.existsByUsername("lvoxx")).thenReturn(Mono.just(true));

            StepVerifier.create(authService.register(req))
                    .expectError(UserAlreadyExistsException.class)
                    .verify();

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("duplicate email – emits UserAlreadyExistsException")
        void register_duplicateEmail_throwsException() {
            RegisterRequest req = new RegisterRequest("newuser", "lvoxx@example.com", "secret123");

            when(userRepository.existsByUsername("newuser")).thenReturn(Mono.just(false));
            when(userRepository.existsByEmail("lvoxx@example.com")).thenReturn(Mono.just(true));

            StepVerifier.create(authService.register(req))
                    .expectError(UserAlreadyExistsException.class)
                    .verify();

            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // login
    // =========================================================================

    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest loginReq;

        @BeforeEach
        void loginSetup() {
            loginReq = new LoginRequest("lvoxx", "secret123");
        }

        @Test
        @DisplayName("valid credentials – returns AuthResponse and writes RT to cache")
        void login_validCredentials_returnsAuthResponse() {
            RefreshToken savedRt = new RefreshToken();
            savedRt.setToken("rt-value");

            when(userRepository.findByUsername("lvoxx")).thenReturn(Mono.just(testUser));
            when(passwordEncoder.matches("secret123", "$2a$hashed")).thenReturn(true);
            when(jwtTokenProvider.createAccessToken("lvoxx", "USER")).thenReturn("access-jwt");
            when(jwtTokenProvider.createRefreshToken("lvoxx")).thenReturn("rt-value");
            when(jwtTokenProvider.getAccessExpiryMs()).thenReturn(900_000L);
            when(refreshTokenRepository.deleteByUserId(1L)).thenReturn(Mono.empty());
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(Mono.just(savedRt));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            StepVerifier.create(authService.login(loginReq, httpResponse))
                    .assertNext(auth -> {
                        assertThat(auth.accessToken()).isEqualTo("access-jwt");
                        assertThat(auth.tokenType()).isEqualTo("Bearer");
                        assertThat(auth.user().username()).isEqualTo("lvoxx");
                    })
                    .verifyComplete();

            // Verify cache write was attempted (batch created after DB save)
            verify(redisson, atLeastOnce()).createBatch();
            verify(refreshTokenRepository).deleteByUserId(1L);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("wrong password – emits UnauthorizedException, no token saved")
        void login_wrongPassword_throwsUnauthorized() {
            when(userRepository.findByUsername("lvoxx")).thenReturn(Mono.just(testUser));
            when(passwordEncoder.matches("secret123", "$2a$hashed")).thenReturn(false);

            StepVerifier.create(authService.login(loginReq, httpResponse))
                    .expectError(UnauthorizedException.class)
                    .verify();

            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("unknown username – emits UserNotFoundException")
        void login_unknownUser_throwsNotFoundException() {
            when(userRepository.findByUsername("lvoxx")).thenReturn(Mono.empty());

            StepVerifier.create(authService.login(loginReq, httpResponse))
                    .expectError(UserNotFoundException.class)
                    .verify();
        }
    }

    // =========================================================================
    // refresh
    // =========================================================================

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("valid token – issues new access token")
        void refresh_validToken_returnsNewAccessToken() {
            RefreshToken stored = new RefreshToken();
            stored.setToken("valid-rt");

            when(refreshTokenRepository.findByToken("valid-rt")).thenReturn(Mono.just(stored));
            when(jwtTokenProvider.validateToken("valid-rt")).thenReturn(true);
            when(jwtTokenProvider.getUsername("valid-rt")).thenReturn("lvoxx");
            when(userRepository.findByUsername("lvoxx")).thenReturn(Mono.just(testUser));
            when(jwtTokenProvider.createAccessToken("lvoxx", "USER")).thenReturn("new-access-jwt");
            when(jwtTokenProvider.getAccessExpiryMs()).thenReturn(900_000L);
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            StepVerifier.create(authService.refresh("valid-rt"))
                    .assertNext(auth -> assertThat(auth.accessToken()).isEqualTo("new-access-jwt"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("expired JWT in stored token – evicts cache and emits UnauthorizedException")
        void refresh_expiredToken_evictsCacheAndThrows() {
            RefreshToken stored = new RefreshToken();
            stored.setToken("expired-rt");

            when(refreshTokenRepository.findByToken("expired-rt")).thenReturn(Mono.just(stored));
            when(jwtTokenProvider.validateToken("expired-rt")).thenReturn(false);
            when(refreshTokenRepository.deleteByToken("expired-rt")).thenReturn(Mono.empty());

            StepVerifier.create(authService.refresh("expired-rt"))
                    .expectError(UnauthorizedException.class)
                    .verify();

            // Cache evict batch must be triggered even when token is expired
            verify(redisson, atLeastOnce()).createBatch();
            verify(refreshTokenRepository).deleteByToken("expired-rt");
        }

        @Test
        @DisplayName("token not found in DB – emits UnauthorizedException without cache interaction")
        void refresh_tokenNotFound_throwsUnauthorized() {
            when(refreshTokenRepository.findByToken("ghost-rt")).thenReturn(Mono.empty());

            StepVerifier.create(authService.refresh("ghost-rt"))
                    .expectError(UnauthorizedException.class)
                    .verify();

            verify(redisson, never()).createBatch();
        }
    }

    // =========================================================================
    // logout
    // =========================================================================

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("valid token – deletes from DB and evicts cache atomically")
        void logout_validToken_deletesAndEvicts() {
            when(refreshTokenRepository.deleteByToken("rt-value")).thenReturn(Mono.empty());

            StepVerifier.create(authService.logout("rt-value", httpResponse))
                    .verifyComplete();

            verify(refreshTokenRepository).deleteByToken("rt-value");
            verify(redisson, atLeastOnce()).createBatch();
        }

        @Test
        @DisplayName("null token – completes immediately with no DB/cache interaction")
        void logout_nullToken_completesImmediately() {
            StepVerifier.create(authService.logout(null, httpResponse))
                    .verifyComplete();

            verify(refreshTokenRepository, never()).deleteByToken(any());
            verify(redisson, never()).createBatch();
        }

        @Test
        @DisplayName("blank token – completes immediately with no DB/cache interaction")
        void logout_blankToken_completesImmediately() {
            StepVerifier.create(authService.logout("   ", httpResponse))
                    .verifyComplete();

            verify(refreshTokenRepository, never()).deleteByToken(any());
        }
    }
}