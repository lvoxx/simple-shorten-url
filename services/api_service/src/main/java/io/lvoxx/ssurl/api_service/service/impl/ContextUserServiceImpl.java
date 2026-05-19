package io.lvoxx.ssurl.api_service.service.impl;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;

import io.lvoxx.ssurl.api_service.service.ContextUserService;
import io.lvoxx.ssurl.api_service.service.UserService;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import reactor.core.publisher.Mono;

/**
 * Resolves the currently authenticated user from the reactive security context,
 * delegating to {@link UserService#getByUsername} to benefit from caching.
 *
 * <p>
 * All methods return {@link Mono#empty()} when no authenticated principal is
 * present — controllers should call {@code .switchIfEmpty(...)} to handle
 * anonymous requests.
 */
@Service
public class ContextUserServiceImpl implements ContextUserService {

    private final UserService userService;

    public ContextUserServiceImpl(UserService userService) {
        this.userService = userService;
    }

    /**
     * Delegates to {@link UserService#getByUsername} and maps to the user's ID.
     */
    @Override
    public Mono<Long> getCurrentUserId() {
        return getCurrentUsername()
                .flatMap(userService::getByUsername)
                .map(UserResponse::id);
    }

    /**
     * Delegates to {@link UserService#getByUsername} to retrieve the full
     * {@link UserResponse} (benefits from cached lookup via {@code @Cacheable}).
     */
    @Override
    public Mono<UserResponse> getCurrentUser() {
        return getCurrentUsername()
                .flatMap(userService::getByUsername);
    }

    /**
     * Extracts the username from the {@code Authentication} principal set by
     * {@link io.lvoxx.ssurl.api_service.security.JwtAuthenticationWebFilter}.
     */
    @Override
    public Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName());
    }
}
