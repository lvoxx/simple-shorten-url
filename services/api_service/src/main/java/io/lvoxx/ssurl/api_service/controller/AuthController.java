package io.lvoxx.ssurl.api_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.lvoxx.ssurl.api_service.service.AuthService;
import io.lvoxx.ssurl.common.dto.request.LoginRequest;
import io.lvoxx.ssurl.common.dto.request.RegisterRequest;
import io.lvoxx.ssurl.common.dto.response.AuthResponse;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(Constants.ApiPaths.AUTH)
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

        private final AuthService authService;

        public AuthController(AuthService authService) {
                this.authService = authService;
        }

        @Operation(summary = "Register a new user", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Registration details with username, email, and password", content = @Content(schema = @Schema(implementation = RegisterRequest.class))))
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "User registered", content = @Content(schema = @Schema(implementation = UserResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Validation failed"),
                        @ApiResponse(responseCode = "409", description = "Username or email already exists"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping("/register")
        public Mono<ResponseEntity<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
                return authService.register(request)
                                .map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
        }

        @Operation(summary = "Login with username and password", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Login credentials", content = @Content(schema = @Schema(implementation = LoginRequest.class))))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Validation failed"),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping("/login")
        public Mono<ResponseEntity<AuthResponse>> login(
                        @Valid @RequestBody LoginRequest request,
                        ServerHttpResponse response) {
                return authService.login(request, response)
                                .map(ResponseEntity::ok);
        }

        @Operation(summary = "Refresh access token using refresh token cookie or X-Refresh-Token header")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Token refreshed", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping("/refresh")
        public Mono<ResponseEntity<AuthResponse>> refresh(
                        @Parameter(description = "Refresh token sent as an HTTP-only cookie") @CookieValue(name = Constants.Headers.COOKIE_REFRESH_TOKEN, required = false) String refreshTokenFromCookie,
                        ServerHttpRequest request) {
                String refreshToken = refreshTokenFromCookie;
                if (refreshToken == null) {
                        String authHeader = request.getHeaders().getFirst(Constants.Headers.X_REFRESH_TOKEN);
                        refreshToken = authHeader;
                }
                final String token = refreshToken;
                return authService.refresh(token)
                                .map(ResponseEntity::ok);
        }

        @Operation(summary = "Logout and invalidate refresh token")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Logged out successfully"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PostMapping("/logout")
        public Mono<ResponseEntity<?>> logout(
                        @Parameter(description = "Refresh token sent as an HTTP-only cookie") @CookieValue(name = Constants.Headers.COOKIE_REFRESH_TOKEN, required = false) String refreshToken,
                        ServerHttpResponse response) {
                return authService.logout(refreshToken, response)
                                .thenReturn(ResponseEntity.noContent().build());
        }
}
