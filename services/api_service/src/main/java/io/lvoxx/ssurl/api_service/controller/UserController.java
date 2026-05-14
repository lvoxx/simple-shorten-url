package io.lvoxx.ssurl.api_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.service.UserService;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management")
public class UserController {

        private final UserService userService;
        private final UserRepository userRepository;

        public UserController(UserService userService, UserRepository userRepository) {
                this.userService = userService;
                this.userRepository = userRepository;
        }

        @Operation(summary = "Get current authenticated user info")
        @ApiResponse(responseCode = "200", description = "User info", content = @Content(schema = @Schema(implementation = UserResponse.class)))
        @ApiResponse(responseCode = "401", description = "Unauthorized")
        @ApiResponse(responseCode = "500", description = "Internal server error")
        @GetMapping("/me")
        public Mono<ResponseEntity<UserResponse>> getMe() {
                return getCurrentUserId()
                                .flatMap(userService::getById)
                                .map(ResponseEntity::ok);
        }

        @Operation(summary = "Update current user's email address")
        @ApiResponse(responseCode = "200", description = "Email updated", content = @Content(schema = @Schema(implementation = UserResponse.class)))
        @ApiResponse(responseCode = "400", description = "Invalid email")
        @ApiResponse(responseCode = "401", description = "Unauthorized")
        @ApiResponse(responseCode = "500", description = "Internal server error")
        @PutMapping("/me/email")
        public Mono<ResponseEntity<UserResponse>> updateEmail(
                        @Parameter(description = "New email address", example = "user@example.com", required = true) @RequestParam String email) {
                return getCurrentUserId()
                                .flatMap(id -> userService.updateEmail(id, email))
                                .map(ResponseEntity::ok);
        }

        @Operation(summary = "Deactivate current user account")
        @ApiResponse(responseCode = "204", description = "Account deactivated")
        @ApiResponse(responseCode = "401", description = "Unauthorized")
        @ApiResponse(responseCode = "500", description = "Internal server error")
        @DeleteMapping("/me")
        public Mono<ResponseEntity<?>> deactivateAccount() {
                return getCurrentUserId()
                                .flatMap(userService::deactivate)
                                .thenReturn(ResponseEntity.noContent().build());
        }

        private Mono<Long> getCurrentUserId() {
                return ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> ctx.getAuthentication().getName())
                                .flatMap(userRepository::findByUsername)
                                .map(user -> user.getId());
        }
}
