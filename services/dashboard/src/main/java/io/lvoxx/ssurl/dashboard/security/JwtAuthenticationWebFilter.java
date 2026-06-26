package io.lvoxx.ssurl.dashboard.security;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.lvoxx.ssurl.common.util.Constants;
import reactor.core.publisher.Mono;

/**
 * Populates the reactive security context from a {@code Bearer} access token.
 * Mirrors api_service's filter; the dashboard validates, never issues, tokens.
 */
@Component
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtAccessTokenValidator validator;

    public JwtAuthenticationWebFilter(JwtAccessTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = resolveToken(exchange.getRequest());
        if (token != null && validator.isValidAccessToken(token)) {
            String username = validator.getUsername(token);
            String role = validator.getRole(token);
            List<GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(Constants.Jwt.ROLE_PREFIX + role));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        }
        return chain.filter(exchange);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith(Constants.Headers.AUTHORIZATION_PREFIX)) {
            return bearer.substring(Constants.Headers.AUTHORIZATION_PREFIX.length());
        }
        return null;
    }
}
