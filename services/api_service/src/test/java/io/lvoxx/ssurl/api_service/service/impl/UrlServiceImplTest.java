package io.lvoxx.ssurl.api_service.service.impl;

import io.lvoxx.ssurl.api_service.repository.UserRepository;
import io.lvoxx.ssurl.common.dto.response.UserResponse;
import io.lvoxx.ssurl.common.exception.UserNotFoundException;
import io.lvoxx.ssurl.common.mapper.UserMapper;
import io.lvoxx.ssurl.common.model.User;
import io.lvoxx.ssurl.common.util.Constants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBatch;
import org.redisson.api.RKeysAsync;
import org.redisson.api.RedissonClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

/**
 * Unit tests for {@link UserServiceImpl}.
 *
 * <p>
 * Tests cover:
 * <ul>
 * <li>Read paths: {@code getById} and {@code getByUsername} (happy +
 * not-found).</li>
 * <li>Mutation paths: {@code updateEmail} and {@code deactivate} – verifying
 * both
 * DB persistence and that the Redisson atomic cache-evict batch is
 * triggered.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock
    UserRepository userRepository;
    @Mock
    UserMapper userMapper;

    // Redisson chain
    @Mock
    RedissonClient redisson;
    @Mock
    RBatch rBatch;
    @Mock
    RKeysAsync rKeys;

    @InjectMocks
    UserServiceImpl userService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("lvoxx");
        testUser.setEmail("lvoxx@example.com");
        testUser.setRole("USER");
        testUser.setActive(true);

        testUserResponse = new UserResponse(1L, "lvoxx", "lvoxx@example.com", "USER", true, LocalDateTime.now());

        // Redisson batch chain → no-op
        when(redisson.createBatch()).thenReturn(rBatch);
        when(rBatch.getKeys()).thenReturn(rKeys);
        when(rKeys.deleteAsync(any(String[].class))).thenReturn(null);
        when(rBatch.execute()).thenReturn(null);
    }

    // =========================================================================
    // getById
    // =========================================================================

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("existing user – returns UserResponse")
        void getById_exists_returnsResponse() {
            when(userRepository.findById(1L)).thenReturn(Mono.just(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            StepVerifier.create(userService.getById(1L))
                    .assertNext(r -> {
                        assertThat(r.id()).isEqualTo(1L);
                        assertThat(r.username()).isEqualTo("lvoxx");
                        assertThat(r.email()).isEqualTo("lvoxx@example.com");
                    })
                    .verifyComplete();

            verify(userRepository).findById(1L);
            // Read-only path must never touch the cache-evict batch
            verify(redisson, never()).createBatch();
        }

        @Test
        @DisplayName("non-existent id – emits UserNotFoundException")
        void getById_notFound_throwsException() {
            when(userRepository.findById(99L)).thenReturn(Mono.empty());

            StepVerifier.create(userService.getById(99L))
                    .expectError(UserNotFoundException.class)
                    .verify();

            verify(userMapper, never()).toResponse(any());
        }
    }

    // =========================================================================
    // getByUsername
    // =========================================================================

    @Nested
    @DisplayName("getByUsername()")
    class GetByUsername {

        @Test
        @DisplayName("existing username – returns UserResponse")
        void getByUsername_exists_returnsResponse() {
            when(userRepository.findByUsername("lvoxx")).thenReturn(Mono.just(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            StepVerifier.create(userService.getByUsername("lvoxx"))
                    .assertNext(r -> assertThat(r.username()).isEqualTo("lvoxx"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("unknown username – emits UserNotFoundException")
        void getByUsername_notFound_throwsException() {
            when(userRepository.findByUsername("ghost")).thenReturn(Mono.empty());

            StepVerifier.create(userService.getByUsername("ghost"))
                    .expectError(UserNotFoundException.class)
                    .verify();
        }
    }

    // =========================================================================
    // updateEmail
    // =========================================================================

    @Nested
    @DisplayName("updateEmail()")
    class UpdateEmail {

        @Test
        @DisplayName("valid id – saves new email and evicts both cache keys atomically")
        void updateEmail_validId_savesAndEvictsCache() {
            User updatedUser = new User();
            updatedUser.setId(1L);
            updatedUser.setUsername("lvoxx");
            updatedUser.setEmail("new@example.com");
            updatedUser.setRole("USER");
            updatedUser.setActive(true);

            UserResponse updatedResponse = new UserResponse(1L, "lvoxx", "new@example.com", "USER", true,
                    LocalDateTime.now());

            when(userRepository.findById(1L)).thenReturn(Mono.just(testUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));
            when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);

            StepVerifier.create(userService.updateEmail(1L, "new@example.com"))
                    .assertNext(r -> {
                        assertThat(r.email()).isEqualTo("new@example.com");
                        assertThat(r.username()).isEqualTo("lvoxx");
                    })
                    .verifyComplete();

            // Verify email was actually updated on the saved entity
            verify(userRepository).save(argThat(u -> "new@example.com".equals(u.getEmail())));

            // Verify atomic cache eviction was triggered (both by-id and by-name keys)
            verify(redisson, atLeastOnce()).createBatch();
            verify(rKeys, atLeastOnce()).deleteAsync(
                    contains(Constants.Cache.KEY_USER_BY_ID + "1"),
                    contains(Constants.Cache.KEY_USER_BY_NAME + "lvoxx"));
        }

        @Test
        @DisplayName("non-existent id – emits UserNotFoundException without save")
        void updateEmail_notFound_throwsException() {
            when(userRepository.findById(99L)).thenReturn(Mono.empty());

            StepVerifier.create(userService.updateEmail(99L, "new@example.com"))
                    .expectError(UserNotFoundException.class)
                    .verify();

            verify(userRepository, never()).save(any());
            verify(redisson, never()).createBatch();
        }
    }

    // =========================================================================
    // deactivate
    // =========================================================================

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("valid id – sets active=false and evicts both cache keys atomically")
        void deactivate_validId_deactivatesAndEvictsCache() {
            when(userRepository.findById(1L)).thenReturn(Mono.just(testUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

            StepVerifier.create(userService.deactivate(1L))
                    .verifyComplete();

            // User must be saved with active=false
            verify(userRepository).save(argThat(u -> !u.isActive()));

            // Atomic cache eviction must be triggered for both key types
            verify(redisson, atLeastOnce()).createBatch();
            verify(rKeys, atLeastOnce()).deleteAsync(
                    contains(Constants.Cache.KEY_USER_BY_ID + "1"),
                    contains(Constants.Cache.KEY_USER_BY_NAME + "lvoxx"));
        }

        @Test
        @DisplayName("non-existent id – emits UserNotFoundException without save or cache interaction")
        void deactivate_notFound_throwsException() {
            when(userRepository.findById(55L)).thenReturn(Mono.empty());

            StepVerifier.create(userService.deactivate(55L))
                    .expectError(UserNotFoundException.class)
                    .verify();

            verify(userRepository, never()).save(any());
            verify(redisson, never()).createBatch();
        }

        @Test
        @DisplayName("already inactive user – still persists (idempotent) and evicts cache")
        void deactivate_alreadyInactive_isIdempotent() {
            testUser.setActive(false); // already deactivated

            when(userRepository.findById(1L)).thenReturn(Mono.just(testUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

            StepVerifier.create(userService.deactivate(1L))
                    .verifyComplete();

            verify(userRepository).save(argThat(u -> !u.isActive()));
            verify(redisson, atLeastOnce()).createBatch();
        }
    }
}