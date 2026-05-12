package io.lvoxx.ssurl.api_service.controller;

import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.api_service.service.UserService;
import io.lvoxx.ssurl.common.model.User;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;

    private UserController userController;

    private static final UserResponse USER_RESPONSE = new UserResponse(
            1L, "alice", "alice@example.com", "USER", true, LocalDateTime.now());

    @BeforeEach
    void setUp() {
        userController = new UserController(userService, userRepository);
    }

    @Nested
    @DisplayName("getMe")
    class GetMe {

        @Test
        @DisplayName("returns current user info")
        void getMe_authenticated_returnsUser() {
            when(userService.getById(1L)).thenReturn(Mono.just(USER_RESPONSE));

            StepVerifier.create(userService.getById(1L))
                    .assertNext(response -> {
                        assertThat(response.id()).isEqualTo(1L);
                        assertThat(response.username()).isEqualTo("alice");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("propagates UserNotFoundException")
        void getMe_userNotFound_propagatesError() {
            when(userService.getById(999L)).thenReturn(Mono.error(new UserNotFoundException("999")));

            StepVerifier.create(userService.getById(999L))
                    .expectError(UserNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("updateEmail")
    class UpdateEmail {

        @Test
        @DisplayName("updates email and returns user")
        void updateEmail_success() {
            UserResponse updated = new UserResponse(1L, "alice", "new@example.com", "USER", true, LocalDateTime.now());
            when(userService.updateEmail(1L, "new@example.com")).thenReturn(Mono.just(updated));

            StepVerifier.create(userService.updateEmail(1L, "new@example.com"))
                    .assertNext(response -> {
                        assertThat(response.email()).isEqualTo("new@example.com");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("propagates error when user not found")
        void updateEmail_userNotFound_propagatesError() {
            when(userService.updateEmail(999L, "new@example.com"))
                    .thenReturn(Mono.error(new UserNotFoundException("999")));

            StepVerifier.create(userService.updateEmail(999L, "new@example.com"))
                    .expectError(UserNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("deactivateAccount")
    class DeactivateAccount {

        @Test
        @DisplayName("deactivates account and returns 204")
        void deactivateAccount_success() {
            when(userService.deactivate(1L)).thenReturn(Mono.empty());

            StepVerifier.create(userService.deactivate(1L))
                    .verifyComplete();
        }
    }
}
