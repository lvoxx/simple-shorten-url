package io.lvoxx.ssurl.api_service.service.impl;

import io.lvoxx.ssurl.api_service.repository.RefreshTokenRepository;
import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.security.JwtTokenProvider;
import io.lvoxx.ssurl.common.model.RefreshToken;
import io.lvoxx.ssurl.common.model.User;
import io.lvoxx.ssurl.common.dto.request.LoginRequest;
import io.lvoxx.ssurl.common.dto.request.RegisterRequest;
import io.lvoxx.ssurl.common.dto.response.AuthResponse;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UserAlreadyExistsException;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import io.lvoxx.ssurl.common.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, refreshTokenRepository, jwtTokenProvider,
                passwordEncoder, userMapper);
    }

    @Nested
    @DisplayName("register")
    class Register {

        private RegisterRequest request;

        @BeforeEach
        void setUp() {
            request = new RegisterRequest("alice", "alice@example.com", "password123");
        }

        @Test
        @DisplayName("creates user when username and email are unique")
        void register_success() {
            User savedUser = new User();
            savedUser.setId(1L);
            savedUser.setUsername("alice");
            savedUser.setEmail("alice@example.com");
            savedUser.setRole("USER");
            savedUser.setActive(true);

            UserResponse expectedResponse = new UserResponse(1L, "alice", "alice@example.com", "USER", true, LocalDateTime.now());

            when(userRepository.existsByUsername("alice")).thenReturn(Mono.just(false));
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(Mono.just(false));
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(userRepository.save(any())).thenReturn(Mono.just(savedUser));
            when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

            StepVerifier.create(authService.register(request))
                    .assertNext(response -> {
                        assertThat(response.username()).isEqualTo("alice");
                        assertThat(response.role()).isEqualTo("USER");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws UserAlreadyExistsException when username is taken")
        void register_usernameTaken_throws() {
            when(userRepository.existsByUsername("alice")).thenReturn(Mono.just(true));

            StepVerifier.create(authService.register(request))
                    .expectError(UserAlreadyExistsException.class)
                    .verify();

            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserAlreadyExistsException when email is taken")
        void register_emailTaken_throws() {
            when(userRepository.existsByUsername("alice")).thenReturn(Mono.just(false));
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(Mono.just(true));

            StepVerifier.create(authService.register(request))
                    .expectError(UserAlreadyExistsException.class)
                    .verify();

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private LoginRequest request;
        private ServerHttpResponse response;
        private User user;

        @BeforeEach
        void setUp() {
            request = new LoginRequest("alice", "password123");
            response = mock(ServerHttpResponse.class);
            user = new User();
            user.setId(1L);
            user.setUsername("alice");
            user.setEmail("alice@example.com");
            user.setRole("USER");
            user.setActive(true);
        }

        @Test
        @DisplayName("returns AuthResponse with valid credentials")
        void login_success() {
            when(userRepository.findByUsername("alice")).thenReturn(Mono.just(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.createAccessToken("alice", "USER")).thenReturn("access-token");
            when(jwtTokenProvider.createRefreshToken("alice")).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessExpiryMs()).thenReturn(900000L);
            when(refreshTokenRepository.deleteByUserId(1L)).thenReturn(Mono.empty());

            RefreshToken savedToken = new RefreshToken(1L, 1L, "refresh-token", LocalDateTime.now().plusDays(7));
            when(refreshTokenRepository.save(any())).thenReturn(Mono.just(savedToken));

            UserResponse userResponse = new UserResponse(1L, "alice", "alice@example.com", "USER", true, LocalDateTime.now());
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            StepVerifier.create(authService.login(request, response))
                    .assertNext(auth -> {
                        assertThat(auth.accessToken()).isEqualTo("access-token");
                        assertThat(auth.tokenType()).isEqualTo("Bearer");
                        assertThat(auth.expiresIn()).isEqualTo(900L);
                        assertThat(auth.user().username()).isEqualTo("alice");
                    })
                    .verifyComplete();

            verify(response).addCookie(any(ResponseCookie.class));
        }

        @Test
        @DisplayName("throws UnauthorizedException when password is wrong")
        void login_wrongPassword_throws() {
            when(userRepository.findByUsername("alice")).thenReturn(Mono.just(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(false);

            StepVerifier.create(authService.login(request, response))
                    .expectError(UnauthorizedException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void login_userNotFound_throws() {
            when(userRepository.findByUsername("alice")).thenReturn(Mono.empty());

            StepVerifier.create(authService.login(request, response))
                    .expectError(UserNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("returns new access token with valid refresh token")
        void refresh_success() {
            RefreshToken storedToken = new RefreshToken(1L, 1L, "valid-refresh-token", LocalDateTime.now().plusDays(7));
            User user = new User();
            user.setId(1L);
            user.setUsername("alice");
            user.setRole("USER");

            when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Mono.just(storedToken));
            when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtTokenProvider.getUsername("valid-refresh-token")).thenReturn("alice");
            when(userRepository.findByUsername("alice")).thenReturn(Mono.just(user));
            when(jwtTokenProvider.createAccessToken("alice", "USER")).thenReturn("new-access-token");
            when(jwtTokenProvider.getAccessExpiryMs()).thenReturn(900000L);

            UserResponse userResponse = new UserResponse(1L, "alice", null, "USER", true, LocalDateTime.now());
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            StepVerifier.create(authService.refresh("valid-refresh-token"))
                    .assertNext(auth -> {
                        assertThat(auth.accessToken()).isEqualTo("new-access-token");
                        assertThat(auth.tokenType()).isEqualTo("Bearer");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws UnauthorizedException when refresh token is not in DB")
        void refresh_tokenNotFound_throws() {
            when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Mono.empty());

            StepVerifier.create(authService.refresh("unknown-token"))
                    .expectError(UnauthorizedException.class)
                    .verify();
        }

        @Test
        @DisplayName("throws UnauthorizedException and deletes token when token is expired")
        void refresh_expiredToken_throws() {
            RefreshToken storedToken = new RefreshToken(1L, 1L, "expired-token", LocalDateTime.now().minusDays(1));

            when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Mono.just(storedToken));
            when(jwtTokenProvider.validateToken("expired-token")).thenReturn(false);
            when(refreshTokenRepository.deleteByToken("expired-token")).thenReturn(Mono.empty());

            StepVerifier.create(authService.refresh("expired-token"))
                    .expectError(UnauthorizedException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        private ServerHttpResponse response;

        @BeforeEach
        void setUp() {
            response = mock(ServerHttpResponse.class);
        }

        @Test
        @DisplayName("deletes refresh token from DB when token is provided")
        void logout_withToken_deletesToken() {
            when(refreshTokenRepository.deleteByToken("valid-token")).thenReturn(Mono.empty());

            StepVerifier.create(authService.logout("valid-token", response))
                    .verifyComplete();

            verify(response).addCookie(any(ResponseCookie.class));
        }

        @Test
        @DisplayName("skips DB deletion when token is null")
        void logout_nullToken_skipsDb() {
            StepVerifier.create(authService.logout(null, response))
                    .verifyComplete();

            verify(refreshTokenRepository, never()).deleteByToken(anyString());
        }

        @Test
        @DisplayName("skips DB deletion when token is blank")
        void logout_blankToken_skipsDb() {
            StepVerifier.create(authService.logout("", response))
                    .verifyComplete();

            verify(refreshTokenRepository, never()).deleteByToken(anyString());
        }
    }
}
