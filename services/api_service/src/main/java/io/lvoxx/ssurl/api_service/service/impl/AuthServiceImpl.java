package io.lvoxx.ssurl.api_service.service.impl;

import java.time.LocalDateTime;

import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.lvoxx.ssurl.api_service.config.AppProperties;
import io.lvoxx.ssurl.api_service.repository.RefreshTokenRepository;
import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.security.JwtTokenProvider;
import io.lvoxx.ssurl.api_service.service.AuthService;
import io.lvoxx.ssurl.common.dto.request.LoginRequest;
import io.lvoxx.ssurl.common.dto.request.RegisterRequest;
import io.lvoxx.ssurl.common.dto.response.AuthResponse;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import io.lvoxx.ssurl.common.exception.UserAlreadyExistsException;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import io.lvoxx.ssurl.common.mapper.UserMapper;
import io.lvoxx.ssurl.common.model.RefreshToken;
import io.lvoxx.ssurl.common.model.User;
import io.lvoxx.ssurl.common.service.AbstractCacheableService;
import io.lvoxx.ssurl.common.util.Constants;
import io.lvoxx.ssurl.common.util.TokenHasher;
import reactor.core.publisher.Mono;

/**
 * Auth service with:
 * <ul>
 * <li>{@code @Cacheable} on read paths (token refresh validation).</li>
 * <li>{@code @CacheEvict} on mutations (login rotates the refresh token entry;
 * logout removes it).</li>
 * <li>Redisson {@link org.redisson.api.RBatch} atomics via
 * {@link AbstractCacheableService#atomicCacheEvict} to keep the Redis state
 * consistent with the DB within a single MULTI/EXEC block.</li>
 * </ul>
 *
 * <h3>Refresh-token expiry calculation fix</h3>
 * The original code computed {@code expiresAt} as
 * {@code accessExpiryMs / 1000 * 800}, which effectively set a refresh token
 * TTL of 800× the access token lifetime – almost certainly unintentional.
 * The corrected formula uses a dedicated refresh-expiry constant (30 days)
 * represented by {@link AbstractCacheableService#TTL_REFRESH_TOKEN}.
 */
@Service
@Transactional
public class AuthServiceImpl extends AbstractCacheableService implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AppProperties appProperties;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            AppProperties appProperties,
            RedissonClient redisson) {
        super(redisson);
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.appProperties = appProperties;
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    /**
     * No cache warming on register – the caller will populate caches lazily on
     * first read. Spring's @CachePut is avoided here because reactive return
     * types (Mono/Flux) are not directly unwrappable by the synchronous Spring
     * Cache abstraction; cache population is handled imperatively instead.
     */
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
                    user.setPasswordHash(passwordEncoder.encode(request.password()));
                    user.setRole(Constants.Defaults.ROLE);
                    user.setActive(true);
                    return userRepository.save(user);
                })
                .map(userMapper::toResponse);
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    /**
     * On login:
     * <ol>
     * <li>DB: delete old refresh token, save new one (within
     * {@code @Transactional}).</li>
     * <li>Cache (atomic batch): evict the old refresh-token presence flag and
     * write the new one so the {@link #refresh} path benefits immediately.</li>
     * </ol>
     *
     * Spring {@code @CacheEvict} clears the user-level cache entry keyed by
     * username so that a stale cached role/profile cannot survive across
     * credential changes.
     */
    @Override
    @CacheEvict(cacheNames = Constants.Cache.CACHE_USERS, key = "#request.username()")
    public Mono<AuthResponse> login(LoginRequest request, ServerHttpResponse response) {
        return userRepository.findByUsername(request.username())
                .switchIfEmpty(Mono.error(new UserNotFoundException(request.username())))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                        return Mono.error(new UnauthorizedException("Invalid credentials"));
                    }

                    String accessToken = jwtTokenProvider.createAccessToken(user.getUsername(), user.getRole());
                    String rtValue = jwtTokenProvider.createRefreshToken(user.getUsername());
                    // Persist/cache only the digest; the raw token lives solely in the
                    // client cookie so a DB/Redis read cannot replay sessions.
                    String rtHash = TokenHasher.sha256Hex(rtValue);

                    RefreshToken refreshToken = new RefreshToken();
                    refreshToken.setUserId(user.getId());
                    refreshToken.setToken(rtHash);

                    refreshToken.setExpiresAt(LocalDateTime.now().plus(Constants.TTL.TTL_REFRESH_TOKEN));

                    // DB write (wrapped in outer @Transactional)
                    Mono<RefreshToken> dbWrite = refreshTokenRepository.deleteByUserId(user.getId())
                            .then(refreshTokenRepository.save(refreshToken));

                    // After DB succeeds → atomically write RT presence flag to Redis
                    return atomicCacheWrite(
                            dbWrite,
                            Constants.Cache.KEY_AUTH_RT + rtHash,
                            "1",
                            Constants.TTL.TTL_REFRESH_TOKEN).map(saved -> {
                                setRefreshTokenCookie(response, rtValue);
                                return new AuthResponse(
                                        accessToken,
                                        "Bearer",
                                        jwtTokenProvider.getAccessExpiryMs() / 1000,
                                        userMapper.toResponse(user));
                            });
                });
    }

    // -------------------------------------------------------------------------
    // refresh
    // -------------------------------------------------------------------------

    /**
     * Validates the stored refresh token and issues a new access token.
     * The refresh token DB record itself is <em>not</em> rotated here (single-use
     * rotation is a separate concern); the Redis presence flag is checked first
     * via {@code @Cacheable} to short-circuit the DB round-trip on the hot path.
     *
     * <p>
     * Cache key: {@code auth:rt:<token>} in region {@value #CACHE_REFRESH_TOKENS}.
     * A cache miss falls through to the DB. If the DB also misses, the token is
     * invalid and an {@link UnauthorizedException} is thrown (and nothing is
     * cached).
     */
    @Override
    @Cacheable(cacheNames = Constants.Cache.CACHE_REFRESH_TOKENS, key = "'" + Constants.Cache.KEY_AUTH_RT
            + "' + #refreshToken", unless = "#result == null")
    public Mono<AuthResponse> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new UnauthorizedException("Invalid or expired refresh token"));
        }
        // Stored/cached records are keyed by digest; the client presents the raw token.
        String rtHash = TokenHasher.sha256Hex(refreshToken);
        return refreshTokenRepository.findByToken(rtHash)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Invalid or expired refresh token")))
                .flatMap(storedToken -> {
                    // Validate the raw token the client presented (the digest is not a JWT).
                    if (!jwtTokenProvider.validateToken(refreshToken)) {
                        // Token expired in JWT layer – evict from cache and DB atomically
                        Mono<Void> dbDelete = refreshTokenRepository.deleteByToken(rtHash);
                        return atomicCacheEvictVoid(
                                dbDelete,
                                Constants.Cache.KEY_AUTH_RT + rtHash)
                                .then(Mono.error(new UnauthorizedException("Refresh token has expired")));
                    }
                    String username = jwtTokenProvider.getUsername(refreshToken);
                    return userRepository.findByUsername(username)
                            .switchIfEmpty(Mono.error(new UserNotFoundException(username)))
                            .map(user -> {
                                String newAccessToken = jwtTokenProvider.createAccessToken(
                                        user.getUsername(), user.getRole());
                                UserResponse userResponse = userMapper.toResponse(user);
                                return new AuthResponse(
                                        newAccessToken,
                                        Constants.Jwt.TOKEN_TYPE,
                                        jwtTokenProvider.getAccessExpiryMs() / 1000,
                                        userResponse);
                            });
                });
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    /**
     * Evicts the refresh-token cache entry and deletes the DB record in one
     * atomic Redisson batch after the DB {@code DELETE} completes.
     */
    @Override
    public Mono<Void> logout(String refreshToken, ServerHttpResponse response) {
        clearRefreshTokenCookie(response);
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.empty();
        }
        String rtHash = TokenHasher.sha256Hex(refreshToken);
        Mono<Void> dbDelete = refreshTokenRepository.deleteByToken(rtHash);
        return atomicCacheEvictVoid(dbDelete, Constants.Cache.KEY_AUTH_RT + rtHash);
    }

    // -------------------------------------------------------------------------
    // Cookie helpers
    // -------------------------------------------------------------------------

    private void setRefreshTokenCookie(ServerHttpResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(Constants.Headers.COOKIE_REFRESH_TOKEN, token)
                .httpOnly(true)
                .secure(appProperties.isSecureCookies())
                .path("/api/v1/auth")
                .maxAge(Constants.TTL.TTL_REFRESH_TOKEN.getSeconds())
                .sameSite("Strict")
                .build();
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(ServerHttpResponse response) {
        ResponseCookie cookie = ResponseCookie.from(Constants.Headers.COOKIE_REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(appProperties.isSecureCookies())
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addCookie(cookie);
    }
}
