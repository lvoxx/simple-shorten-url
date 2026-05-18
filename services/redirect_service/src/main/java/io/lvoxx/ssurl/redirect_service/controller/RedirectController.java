package io.lvoxx.ssurl.redirect_service.controller;

import io.lvoxx.ssurl.common.util.Constants;
import io.lvoxx.ssurl.redirect_service.service.RedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
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

        @Operation(summary = "Redirect to original URL by short code")
        @ApiResponse(responseCode = "302", description = "Redirect to original URL", headers = @Header(name = Constants.Headers.LOCATION, description = "Original URL", schema = @Schema(type = "string")))
        @ApiResponse(responseCode = "404", description = "Short code not found")
        @ApiResponse(responseCode = "410", description = "URL has expired")
        @ApiResponse(responseCode = "500", description = "Internal server error")
        @GetMapping("/{shortCode:" + Constants.Patterns.SHORT_CODE + "}")
        public Mono<ResponseEntity<Void>> redirect(
                        @Parameter(description = "Short code (Base62 encoded ID)", example = "1aB3xZ", required = true) @PathVariable String shortCode,
                        ServerHttpRequest request) {
                String ip = Objects.requireNonNullElse(
                                request.getHeaders().getFirst(Constants.Headers.X_FORWARDED_FOR),
                                Objects.requireNonNullElse(
                                                request.getRemoteAddress() != null
                                                                ? request.getRemoteAddress().getAddress()
                                                                                .getHostAddress()
                                                                : null,
                                                Constants.Defaults.UNKNOWN_IP));
                String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
                String referer = request.getHeaders().getFirst(HttpHeaders.REFERER);

                return redirectService.resolve(shortCode, ip, userAgent, referer)
                                .map(url -> ResponseEntity.status(HttpStatus.FOUND)
                                                .location(URI.create(url))
                                                .<Void>build());
        }
}
