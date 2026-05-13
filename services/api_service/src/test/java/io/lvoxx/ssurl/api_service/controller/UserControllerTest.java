package io.lvoxx.ssurl.api_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.service.UserService;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import io.lvoxx.ssurl.common.model.User;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;

    private UserController userController;

    private static final UserResponse USER_RESPONSE = new UserResponse(
            1L, "alice", "alice@example.com", "USER", true, LocalDateTime.now());
    private static final User USER_ENTITY = User.builder()
            .id(1L)
            .username("alice")
            .email("alice@example.com")
            .role("USER")
            .isActive(true)
            .build();

    @BeforeEach
    void setUp() {
        userController = new UserController(userService, userRepository);
    }

    private MockedStatic<ReactiveSecurityContextHolder> mockSecurityContext() {
        var securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        var authentication = org.mockito.Mockito.mock(Authentication.class);

        var ctx = mockStatic(ReactiveSecurityContextHolder.class);
        ctx.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(USER_ENTITY));
        return ctx;
    }

    @Nested
    @DisplayName("getMe")
    class GetMe {

        @Test
        @DisplayName("returns current user info")
        void getMe_authenticated_returnsUser() {
            try (MockedStatic<ReactiveSecurityContextHolder> ctx = mockSecurityContext()) {
                when(userService.getById(1L)).thenReturn(Mono.just(USER_RESPONSE));

                StepVerifier.create(userController.getMe())
                        .assertNext(response -> {
                            assertThat(response.getStatusCode().value()).isEqualTo(200);
                            assertThat(response.getBody()).isNotNull();
                            assertThat(response.getBody().id()).isEqualTo(1L);
                            assertThat(response.getBody().username()).isEqualTo("alice");
                        })
                        .verifyComplete();
            }
        }

        @Test
        @DisplayName("propagates UserNotFoundException")
        void getMe_userNotFound_propagatesError() {
            try (MockedStatic<ReactiveSecurityContextHolder> ctx = mockSecurityContext()) {
                when(userService.getById(1L)).thenReturn(Mono.error(new UserNotFoundException("1")));

                StepVerifier.create(userController.getMe())
                        .expectError(UserNotFoundException.class)
                        .verify();
            }
        }
    }

    @Nested
    @DisplayName("updateEmail")
    class UpdateEmail {

        @Test
        @DisplayName("updates email and returns user")
        void updateEmail_success() {
            try (MockedStatic<ReactiveSecurityContextHolder> ctx = mockSecurityContext()) {
                UserResponse updated = new UserResponse(1L, "alice", "new@example.com", "USER", true,
                        LocalDateTime.now());
                when(userService.updateEmail(1L, "new@example.com")).thenReturn(Mono.just(updated));

                StepVerifier.create(userController.updateEmail("new@example.com"))
                        .assertNext(response -> {
                            assertThat(response.getStatusCode().value()).isEqualTo(200);
                            assertThat(response.getBody()).isNotNull();
                            assertThat(response.getBody().email()).isEqualTo("new@example.com");
                        })
                        .verifyComplete();
            }
        }

        @Test
        @DisplayName("propagates error when user not found")
        void updateEmail_userNotFound_propagatesError() {
            try (MockedStatic<ReactiveSecurityContextHolder> ctx = mockSecurityContext()) {
                when(userService.updateEmail(1L, "new@example.com"))
                        .thenReturn(Mono.error(new UserNotFoundException("1")));

                StepVerifier.create(userController.updateEmail("new@example.com"))
                        .expectError(UserNotFoundException.class)
                        .verify();
            }
        }
    }

    @Nested
    @DisplayName("deactivateAccount")
    class DeactivateAccount {

        @Test
        @DisplayName("deactivates account and returns 204")
        void deactivateAccount_success() {
            try (MockedStatic<ReactiveSecurityContextHolder> ctx = mockSecurityContext()) {
                when(userService.deactivate(1L)).thenReturn(Mono.empty());

                StepVerifier.create(userController.deactivateAccount())
                        .assertNext(response -> assertThat(response.getStatusCode().value()).isEqualTo(204))
                        .verifyComplete();
            }
        }
    }
}
