package io.lvoxx.ssurl.api_service.controller;

import io.lvoxx.ssurl.api_service.service.AuthService;
import io.lvoxx.ssurl.common.dto.request.LoginRequest;
import io.lvoxx.ssurl.common.dto.request.RegisterRequest;
import io.lvoxx.ssurl.common.dto.response.AuthResponse;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User registered",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "User already exists")
    @PostMapping("/register")
    public Mono<ResponseEntity<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }

    @Operation(summary = "Login with username and password")
    @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            ServerHttpResponse response) {
        return authService.login(request, response)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Refresh access token using refresh token cookie")
    @ApiResponse(responseCode = "200", description = "Token refreshed",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
            ServerHttpRequest request) {
        String refreshToken = refreshTokenFromCookie;
        if (refreshToken == null) {
            String authHeader = request.getHeaders().getFirst("X-Refresh-Token");
            refreshToken = authHeader;
        }
        final String token = refreshToken;
        return authService.refresh(token)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Logout and invalidate refresh token")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            ServerHttpResponse response) {
        return authService.logout(refreshToken, response)
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }
}
