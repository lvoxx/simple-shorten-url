package io.lvoxx.ssurl.api_service.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.lvoxx.ssurl.api_service.properties.RateLimitProperties;
import io.lvoxx.ssurl.common.util.Constants;
import reactor.core.publisher.Mono;

/**
 * Per-IP fixed-window rate limiter for abuse-prone unauthenticated endpoints.
 *
 * <p>
 * Backed by a Redis {@code INCR} + {@code EXPIRE} on {@code rate_limit:ip:*}.
 * Ordered ahead of Spring Security's filter chain so floods are rejected
 * cheaply before authentication work runs. On a Redis outage it
 * <b>fails open</b> — availability of login is preferred over hard enforcement,
 * since this is a defence-in-depth control.
 *
 * <p>
 * Limited routes:
 * <ul>
 * <li>{@code POST /api/v1/auth/login}, {@code POST /api/v1/auth/register}</li>
 * <li>{@code POST /api/v1/urls}</li>
 * </ul>
 *
 * <p>
 * The client IP is taken from the first {@code X-Forwarded-For} hop when
 * present (matching the redirect service). A trusted-proxy check should be
 * added so the header cannot be spoofed to bypass the limit when not behind a
 * known proxy.
 */
@Component
public class RateLimitWebFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitWebFilter.class);

    /** Run before Spring Security's {@code WebFilterChainProxy} (order -100). */
    private static final int FILTER_ORDER = -101;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final MessageSource messageSource;

    public RateLimitWebFilter(
            ReactiveStringRedisTemplate redisTemplate,
            RateLimitProperties properties,
            MessageSource messageSource) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.messageSource = messageSource;
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        Bucket bucket = resolveBucket(exchange.getRequest());
        if (bucket == null) {
            return chain.filter(exchange);
        }

        String ip = resolveIp(exchange.getRequest());
        String key = Constants.Redis.RATE_LIMIT_IP + ip + ":" + bucket.name;
        Duration window = Duration.ofSeconds(bucket.windowSeconds);

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> ensureTtl = (count != null && count == 1L)
                            ? redisTemplate.expire(key, window)
                            : Mono.just(Boolean.TRUE);
                    return ensureTtl.then(Mono.defer(() -> {
                        if (count != null && count > bucket.limit) {
                            return tooManyRequests(exchange, bucket.windowSeconds);
                        }
                        return chain.filter(exchange);
                    }));
                })
                // Fail open: never let a Redis problem break the request path.
                .onErrorResume(ex -> {
                    log.warn("Rate-limit check failed for key={} – allowing request (fail-open): {}",
                            key, ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    private Bucket resolveBucket(ServerHttpRequest request) {
        if (!HttpMethod.POST.equals(request.getMethod())) {
            return null;
        }
        String path = request.getPath().value();
        if (path.equals(Constants.ApiPaths.AUTH + "/login")
                || path.equals(Constants.ApiPaths.AUTH + "/register")) {
            return new Bucket("auth", properties.getAuthLimit(), properties.getAuthWindowSeconds());
        }
        if (path.equals(Constants.ApiPaths.URLS)) {
            return new Bucket("create", properties.getCreateLimit(), properties.getCreateWindowSeconds());
        }
        return null;
    }

    private String resolveIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst(Constants.Headers.X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return Constants.Defaults.UNKNOWN_IP;
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, long retryAfterSeconds) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        String detail;
        try {
            detail = messageSource.getMessage(
                    Constants.Messages.RATELIMIT_EXCEEDED,
                    new Object[] { retryAfterSeconds },
                    resolveLocale(exchange));
        } catch (Exception e) {
            detail = "Too many requests. Try again in " + retryAfterSeconds + " seconds";
        }

        String body = "{\"title\":\"Rate Limit Exceeded\",\"status\":429,\"detail\":\""
                + detail.replace("\\", "\\\\").replace("\"", "\\\"")
                + "\",\"retryAfterSeconds\":" + retryAfterSeconds + "}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private Locale resolveLocale(ServerWebExchange exchange) {
        if (exchange.getLocaleContext() != null && exchange.getLocaleContext().getLocale() != null) {
            return exchange.getLocaleContext().getLocale();
        }
        return Locale.getDefault();
    }

    private record Bucket(String name, int limit, long windowSeconds) {
    }
}
