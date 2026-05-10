package io.lvoxx.ssurl.redirect_service.service;

import reactor.core.publisher.Mono;

public interface RedirectService {

    /**
     * Resolves a short code to the original URL.
     * Records analytics event asynchronously.
     *
     * @param shortCode the short code to resolve
     * @param ip        the client IP address
     * @param userAgent the User-Agent header value
     * @param referer   the Referer header value
     * @return the original URL as a Mono
     */
    Mono<String> resolve(String shortCode, String ip, String userAgent, String referer);
}
