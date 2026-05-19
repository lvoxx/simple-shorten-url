package io.lvoxx.ssurl.api_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.lvoxx.ssurl.api_service.service.ContextUserService;
import io.lvoxx.ssurl.api_service.service.UserService;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(Constants.ApiPaths.USERS)
@Tag(name = "Users", description = "User management")
public class UserController {

        private final UserService userService;
        private final ContextUserService contextUserService;

        public UserController(UserService userService, ContextUserService contextUserService) {
                this.userService = userService;
                this.contextUserService = contextUserService;
        }

        @Operation(summary = "Get current authenticated user info")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Current user info", content = @Content(schema = @Schema(implementation = UserResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/me")
        public Mono<ResponseEntity<UserResponse>> getMe() {
                return contextUserService.getCurrentUserId()
                                .flatMap(userService::getById)
                                .map(ResponseEntity::ok);
        }

        @Operation(summary = "Update current user's email address")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Email updated", content = @Content(schema = @Schema(implementation = UserResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid email"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @PutMapping("/me/email")
        public Mono<ResponseEntity<UserResponse>> updateEmail(
                        @Parameter(description = "New email address", example = "user@example.com", required = true) @RequestParam String email) {
                return contextUserService.getCurrentUserId()
                                .flatMap(id -> userService.updateEmail(id, email))
                                .map(ResponseEntity::ok);
        }

        @Operation(summary = "Deactivate current user account")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Account deactivated"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @DeleteMapping("/me")
        public Mono<ResponseEntity<?>> deactivateAccount() {
                return contextUserService.getCurrentUserId()
                                .flatMap(userService::deactivate)
                                .thenReturn(ResponseEntity.noContent().build());
        }

}
