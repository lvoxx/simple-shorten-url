package io.lvoxx.ssurl.dashboard.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import io.lvoxx.ssurl.common.exception.UnauthorizedException;
import reactor.core.publisher.Mono;

/**
 * Resolves the authenticated username (JWT subject) from the reactive security
 * context. Emits {@link UnauthorizedException} when no principal is present so
 * the {@code GlobalExceptionHandler} can map it to 401.
 */
@Component
public class CurrentUser {

    public Mono<String> username() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .switchIfEmpty(Mono.error(new UnauthorizedException("no authenticated principal")));
    }
}
