package io.lvoxx.ssurl.api_service.service.impl;

import io.lvoxx.ssurl.api_service.repository.RefreshTokenRepository;
import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.security.JwtTokenProvider;
import io.lvoxx.ssurl.api_service.service.AuthService;
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
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    public Mono<UserResponse> register(RegisterRequest request) {
        return userRepository.existsByUsername(request.username())
                .flatMap(usernameExists -> {
                    if (Boolean.TRUE.equals(usernameExists)) {
                        return Mono.error(new UserAlreadyExistsException(request.username()));
                    }
                    return userRepository.existsByEmail(request.email());
                })
                .flatMap(emailExists -> {
                    if (Boolean.TRUE.equals(emailExists)) {
                        return Mono.error(new UserAlreadyExistsException(request.email()));
                    }
                    User user = new User();
                    user.setUsername(request.username());
                    user.setEmail(request.email());
                    user.setPassword(passwordEncoder.encode(request.password()));
                    user.setRole("USER");
                    user.setActive(true);
                    return userRepository.save(user);
                })
                .map(userMapper::toResponse);
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request, ServerHttpResponse response) {
        return userRepository.findByUsername(request.username())
                .switchIfEmpty(Mono.error(new UserNotFoundException(request.username())))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                        return Mono.error(new UnauthorizedException("Invalid credentials"));
                    }
                    String accessToken = jwtTokenProvider.createAccessToken(user.getUsername(), user.getRole());
                    String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getUsername());
                    RefreshToken refreshToken = new RefreshToken(
                            user.getId(),
                            refreshTokenValue,
                            LocalDateTime.now().plusSeconds(
                                    jwtTokenProvider.getAccessExpiryMs() / 1000 * 800)
                    );
                    return refreshTokenRepository.deleteByUserId(user.getId())
                            .then(refreshTokenRepository.save(refreshToken))
                            .map(saved -> {
                                setRefreshTokenCookie(response, refreshTokenValue);
                                UserResponse userResponse = userMapper.toResponse(user);
                                return new AuthResponse(
                                        accessToken,
                                        "Bearer",
                                        jwtTokenProvider.getAccessExpiryMs() / 1000,
                                        userResponse
                                );
                            });
                });
    }

    @Override
    public Mono<AuthResponse> refresh(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Invalid or expired refresh token")))
                .flatMap(storedToken -> {
                    if (!jwtTokenProvider.validateToken(storedToken.getToken())) {
                        return refreshTokenRepository.deleteByToken(refreshToken)
                                .then(Mono.error(new UnauthorizedException("Refresh token has expired")));
                    }
                    String username = jwtTokenProvider.getUsername(storedToken.getToken());
                    return userRepository.findByUsername(username)
                            .switchIfEmpty(Mono.error(new UserNotFoundException(username)))
                            .map(user -> {
                                String newAccessToken = jwtTokenProvider.createAccessToken(
                                        user.getUsername(), user.getRole());
                                UserResponse userResponse = userMapper.toResponse(user);
                                return new AuthResponse(
                                        newAccessToken,
                                        "Bearer",
                                        jwtTokenProvider.getAccessExpiryMs() / 1000,
                                        userResponse
                                );
                            });
                });
    }

    @Override
    public Mono<Void> logout(String refreshToken, ServerHttpResponse response) {
        clearRefreshTokenCookie(response);
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.empty();
        }
        return refreshTokenRepository.deleteByToken(refreshToken);
    }

    private void setRefreshTokenCookie(ServerHttpResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth")
                .maxAge(jwtTokenProvider.getAccessExpiryMs() * 800 / 1000)
                .sameSite("Strict")
                .build();
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(ServerHttpResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addCookie(cookie);
    }
}
