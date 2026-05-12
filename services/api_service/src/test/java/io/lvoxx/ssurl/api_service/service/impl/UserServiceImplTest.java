package io.lvoxx.ssurl.api_service.service.impl;

import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.common.model.User;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import io.lvoxx.ssurl.common.mapper.UserMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper);
    }

    private User createUser(Long id, String username, String email, boolean active) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole("USER");
        user.setActive(active);
        return user;
    }

    private UserResponse createResponse(Long id, String username, String email, boolean active) {
        return new UserResponse(id, username, email, "USER", active, LocalDateTime.now());
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns user when found")
        void getById_found() {
            User user = createUser(1L, "alice", "alice@example.com", true);
            UserResponse response = createResponse(1L, "alice", "alice@example.com", true);

            when(userRepository.findById(1L)).thenReturn(Mono.just(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            StepVerifier.create(userService.getById(1L))
                    .assertNext(r -> {
                        assertThat(r.id()).isEqualTo(1L);
                        assertThat(r.username()).isEqualTo("alice");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws UserNotFoundException when not found")
        void getById_notFound_throws() {
            when(userRepository.findById(999L)).thenReturn(Mono.empty());

            StepVerifier.create(userService.getById(999L))
                    .expectError(UserNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("getByUsername")
    class GetByUsername {

        @Test
        @DisplayName("returns user when found")
        void getByUsername_found() {
            User user = createUser(1L, "alice", "alice@example.com", true);
            UserResponse response = createResponse(1L, "alice", "alice@example.com", true);

            when(userRepository.findByUsername("alice")).thenReturn(Mono.just(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            StepVerifier.create(userService.getByUsername("alice"))
                    .assertNext(r -> assertThat(r.username()).isEqualTo("alice"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws UserNotFoundException when not found")
        void getByUsername_notFound_throws() {
            when(userRepository.findByUsername("unknown")).thenReturn(Mono.empty());

            StepVerifier.create(userService.getByUsername("unknown"))
                    .expectError(UserNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("updateEmail")
    class UpdateEmail {

        @Test
        @DisplayName("updates and returns user with new email")
        void updateEmail_success() {
            User user = createUser(1L, "alice", "old@example.com", true);
            User updatedUser = createUser(1L, "alice", "new@example.com", true);
            UserResponse response = createResponse(1L, "alice", "new@example.com", true);

            when(userRepository.findById(1L)).thenReturn(Mono.just(user));
            when(userRepository.save(any())).thenReturn(Mono.just(updatedUser));
            when(userMapper.toResponse(updatedUser)).thenReturn(response);

            StepVerifier.create(userService.updateEmail(1L, "new@example.com"))
                    .assertNext(r -> assertThat(r.email()).isEqualTo("new@example.com"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("throws UserNotFoundException when user not found")
        void updateEmail_notFound_throws() {
            when(userRepository.findById(999L)).thenReturn(Mono.empty());

            StepVerifier.create(userService.updateEmail(999L, "new@example.com"))
                    .expectError(UserNotFoundException.class)
                    .verify();

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("sets user active to false")
        void deactivate_success() {
            User user = createUser(1L, "alice", "alice@example.com", true);

            when(userRepository.findById(1L)).thenReturn(Mono.just(user));
            when(userRepository.save(any())).thenAnswer(inv -> {
                User saved = inv.getArgument(0);
                return Mono.just(saved);
            });

            StepVerifier.create(userService.deactivate(1L))
                    .verifyComplete();

            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user not found")
        void deactivate_notFound_throws() {
            when(userRepository.findById(999L)).thenReturn(Mono.empty());

            StepVerifier.create(userService.deactivate(999L))
                    .expectError(UserNotFoundException.class)
                    .verify();

            verify(userRepository, never()).save(any());
        }
    }
}
