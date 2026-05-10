package io.lvoxx.ssurl.redirect_service.controller;

import io.lvoxx.ssurl.redirect_service.service.RedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;

@RestController
@Tag(name = "Redirect", description = "Short URL redirect endpoint")
public class RedirectController {

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    @Operation(summary = "Redirect to original URL")
    @ApiResponse(responseCode = "302", description = "Redirect to original URL")
    @ApiResponse(responseCode = "404", description = "Short code not found")
    @ApiResponse(responseCode = "410", description = "URL has expired")
    @GetMapping("/{shortCode:[a-zA-Z0-9]+}")
    public Mono<ResponseEntity<Void>> redirect(
            @PathVariable String shortCode,
            ServerHttpRequest request) {
        String ip = Objects.requireNonNullElse(
                request.getHeaders().getFirst("X-Forwarded-For"),
                Objects.requireNonNullElse(
                        request.getRemoteAddress() != null
                                ? request.getRemoteAddress().getAddress().getHostAddress()
                                : null,
                        "unknown"
                )
        );
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        String referer = request.getHeaders().getFirst(HttpHeaders.REFERER);

        return redirectService.resolve(shortCode, ip, userAgent, referer)
                .map(url -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(url))
                        .<Void>build());
    }
}
