package io.lvoxx.ssurl.api_service.controller;

import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.service.UrlService;
import io.lvoxx.ssurl.common.dto.request.CreateUrlRequest;
import io.lvoxx.ssurl.common.dto.request.UpdateUrlRequest;
import io.lvoxx.ssurl.common.dto.response.CursorPage;
import io.lvoxx.ssurl.common.dto.response.UrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URLs", description = "Short URL management")
public class UrlController {

        private final UrlService urlService;
        private final UserRepository userRepository;

        public UrlController(UrlService urlService, UserRepository userRepository) {
                this.urlService = urlService;
                this.userRepository = userRepository;
        }

        @Operation(summary = "Create a short URL (works for both authenticated and anonymous users)", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "URL to shorten with optional title and expiration", content = @Content(schema = @Schema(implementation = CreateUrlRequest.class))))
        @ApiResponse(responseCode = "201", description = "URL created", content = @Content(schema = @Schema(implementation = UrlResponse.class)))
        @ApiResponse(responseCode = "400", description = "Validation failed")
        @ApiResponse(responseCode = "422", description = "Domain is blacklisted")
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        @ApiResponse(responseCode = "500", description = "Internal server error")
        @PostMapping
        public Mono<ResponseEntity<UrlResponse>> createUrl(@Valid @RequestBody CreateUrlRequest request) {
                return ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> ctx.getAuthentication().getName())
                                .flatMap(userRepository::findByUsername)
                                .flatMap(user -> urlService.createUrl(request, user.getId(), user.getUsername()))
                                .switchIfEmpty(urlService.createUrl(request, null, "anonymous"))
                                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
        }

        @Operation(summary = "Get URL by short code")
        @ApiResponse(responseCode = "200", description = "URL found", content = @Content(schema = @Schema(implementation = UrlResponse.class)))
        @ApiResponse(responseCode = "404", description = "Short code not found")
        @ApiResponse(responseCode = "410", description = "Short code has expired")
        @ApiResponse(responseCode = "500", description = "Internal server error")
        @GetMapping("/{shortCode}")
        public Mono<ResponseEntity<UrlResponse>> getByShortCode(
                        @Parameter(description = "Short code (Base62 encoded ID)", example = "1aB3xZ") @PathVariable String shortCode) {
                return urlService.getByShortCode(shortCode)
                                .map(ResponseEntity::ok);
        }

        @Operation(summary = "List current user's URLs with cursor-based pagination")
        @ApiResponse(responseCode = "200", description = "Paginated list of URLs", content = @Content(schema = @Schema(implementation = CursorPage.class)))
        @ApiResponse(responseCode = "401", description = "Unauthorized")
        @ApiResponse(responseCode = "500", description = "Internal server error")
        @GetMapping("/my")
        public Mono<CursorPage<UrlResponse>> listMyUrls(
                        @Parameter(description = "Cursor: ID of the last item from the previous page (omit for first page)", example = "42") @RequestParam(required = false) Long cursor,
                        @Parameter(description = "Page size (max 100, default 20)", example = "20") @RequestParam(defaultValue = "20") int size) {
                return getCurrentUserId()
                                .flatMap(userId -> urlService.listByUser(userId, cursor, size));
        }

        @Operation(summary = "Update a URL", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (title, expireAt, isActive)", content = @Content(schema = @Schema(implementation = UpdateUrlRequest.class))))
        @ApiResponse(responseCode = "200", description = "URL updated", content = @Content(schema = @Schema(implementation = UrlResponse.class)))
        @ApiResponse(responseCode = "400", description = "Validation failed")
        @ApiResponse(responseCode = "401", description = "Unauthorized")
        @ApiResponse(responseCode = "404", description = "URL not found")
        @ApiResponse(responseCode = "500", description = "Internal server error")
        @PutMapping("/{id}")
        public Mono<ResponseEntity<UrlResponse>> updateUrl(
                        @Parameter(description = "URL record ID", example = "1") @PathVariable Long id,
                        @Valid @RequestBody UpdateUrlRequest request) {
                return getCurrentUserId()
                                .flatMap(userId -> urlService.update(id, request, userId))
                                .map(ResponseEntity::ok);
        }

        @Operation(summary = "Delete (deactivate) a URL")
        @ApiResponse(responseCode = "204", description = "URL deleted")
        @ApiResponse(responseCode = "401", description = "Unauthorized")
        @ApiResponse(responseCode = "404", description = "URL not found")
        @ApiResponse(responseCode = "500", description = "Internal server error")
        @DeleteMapping("/{id}")
        public Mono<ResponseEntity<?>> deleteUrl(
                        @Parameter(description = "URL record ID", example = "1") @PathVariable Long id) {
                return getCurrentUserId()
                                .flatMap(userId -> urlService.delete(id, userId))
                                .thenReturn(ResponseEntity.noContent().build());
        }

        private Mono<Long> getCurrentUserId() {
                return ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> ctx.getAuthentication().getName())
                                .flatMap(userRepository::findByUsername)
                                .map(user -> user.getId());
        }
}
