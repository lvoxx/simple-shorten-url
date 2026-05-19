package io.lvoxx.ssurl.api_service.service;

import io.lvoxx.ssurl.common.dto.response.UserResponse;
import reactor.core.publisher.Mono;

/**
 * Resolves the currently authenticated user from the reactive security context.
 *
 * <p>Provides convenience methods to retrieve the authenticated user's ID,
 * full DTO, or username without duplicating
 * {@link org.springframework.security.core.context.ReactiveSecurityContextHolder}
 * lookups across controllers.
 *
 * <p>All methods return {@link Mono#empty()} when no authenticated principal
 * is present — callers should use {@code .switchIfEmpty(...)} to handle
 * anonymous requests.
 */
public interface ContextUserService {

    /**
     * Returns the database ID of the currently authenticated user.
     *
     * @return the user ID, or {@link Mono#empty()} if no authenticated principal is present
     */
    Mono<Long> getCurrentUserId();

    /**
     * Returns the {@link UserResponse} of the currently authenticated user.
     *
     * @return the user response DTO, or {@link Mono#empty()} if no authenticated principal is present
     */
    Mono<UserResponse> getCurrentUser();

    /**
     * Returns the username (principal name) from the reactive security context.
     *
     * @return the username, or {@link Mono#empty()} if no authentication is available
     */
    Mono<String> getCurrentUsername();
}
