package io.lvoxx.ssurl.api_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import io.lvoxx.ssurl.common.model.RefreshToken;
import io.lvoxx.ssurl.common.model.User;
import io.lvoxx.ssurl.test_starter.AbstractPostgresContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("Refresh Token Repository Tests")
@Tags({
                @Tag("Repository"), @Tag("Integration")
})
class RefreshTokenRepositoryTest extends AbstractPostgresContainer {

        @Autowired
        private RefreshTokenRepository refreshTokenRepository;

        @Autowired
        private UserRepository userRepository;

        private Mono<User> createTestUser() {
                return userRepository.save(User.builder()
                                .username("rtuser")
                                .email("rtuser@test.com")
                                .passwordHash("hash")
                                .build());
        }

        private Mono<RefreshToken> createToken(String tokenVal, Long userId) {
                return refreshTokenRepository.save(RefreshToken.builder()
                                .token(tokenVal)
                                .userId(userId)
                                .expiresAt(LocalDateTime.now().plusDays(7))
                                .build());
        }

        @Test
        void should_saveRefreshToken_when_validData() {
                createTestUser()
                                .flatMap(user -> createToken("rt_save_token_1", user.getId()))
                                .as(StepVerifier::create)
                                .assertNext(rt -> {
                                        assertThat(rt.getId()).isNotNull();
                                        assertThat(rt.getToken()).isEqualTo("rt_save_token_1");
                                        assertThat(rt.getUserId()).isNotNull();
                                        assertThat(rt.getExpiresAt()).isNotNull();
                                        assertThat(rt.getCreatedAt()).isNotNull();
                                })
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_returnToken_when_findById() {
                createTestUser()
                                .flatMap(user -> createToken("rt_find_id", user.getId()))
                                .flatMap(saved -> refreshTokenRepository.findById(saved.getId()))
                                .as(StepVerifier::create)
                                .assertNext(rt -> assertThat(rt.getToken()).isEqualTo("rt_find_id"))
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_returnEmpty_when_findByIdNotFound() {
                refreshTokenRepository.findById(99999L)
                                .as(StepVerifier::create)
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        void should_findByToken_when_exists() {
                createTestUser()
                                .flatMap(user -> createToken("rt_find_by_token_val", user.getId()))
                                .flatMap(saved -> refreshTokenRepository.findByToken("rt_find_by_token_val"))
                                .as(StepVerifier::create)
                                .assertNext(rt -> assertThat(rt.getUserId()).isNotNull())
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_returnEmpty_when_findByTokenNotFound() {
                refreshTokenRepository.findByToken("nonexistent_token")
                                .as(StepVerifier::create)
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        void should_deleteByUserId_when_tokensExist() {
                createTestUser()
                                .flatMapMany(user -> {
                                        var t1 = createToken("rt_del_user_1", user.getId());
                                        var t2 = createToken("rt_del_user_2", user.getId());
                                        return t1.then(t2).thenReturn(user);
                                })
                                .flatMap(user -> refreshTokenRepository.deleteByUserId(user.getId())
                                                .thenMany(refreshTokenRepository.findAll()))
                                .as(StepVerifier::create)
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        void should_deleteByToken_when_tokenExists() {
                createTestUser()
                                .flatMap(user -> createToken("rt_del_by_token", user.getId()))
                                .flatMap(saved -> refreshTokenRepository.deleteByToken("rt_del_by_token")
                                                .then(refreshTokenRepository.findByToken("rt_del_by_token")))
                                .as(StepVerifier::create)
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        void should_cascadeDelete_when_userDeleted() {
                var savedUser = userRepository.save(User.builder()
                                .username("rtcascade")
                                .email("rtcascade@test.com")
                                .passwordHash("hash")
                                .build());

                savedUser
                                .flatMap(user -> createToken("rt_cascade_token", user.getId()).thenReturn(user))
                                .flatMap(user -> userRepository.deleteById(user.getId()))
                                .then(savedUser.flatMap(u -> refreshTokenRepository.findByToken("rt_cascade_token")))
                                .as(StepVerifier::create)
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        void should_throw_when_tokenNull() {
                createTestUser()
                                .flatMap(user -> refreshTokenRepository.save(RefreshToken.builder()
                                                .userId(user.getId())
                                                .expiresAt(LocalDateTime.now().plusDays(7))
                                                .build()))
                                .as(StepVerifier::create)
                                .expectError(DataIntegrityViolationException.class)
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_throw_when_expiresAtNull() {
                createTestUser()
                                .flatMap(user -> refreshTokenRepository.save(RefreshToken.builder()
                                                .token("rt_no_expires")
                                                .userId(user.getId())
                                                .build()))
                                .as(StepVerifier::create)
                                .expectError(DataIntegrityViolationException.class)
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_throw_when_tokenNotUnique() {
                createTestUser()
                                .flatMapMany(user -> {
                                        var t1 = createToken("rt_dup_token", user.getId());
                                        var t2 = createToken("rt_dup_token", user.getId());
                                        return t1.then(t2).thenReturn(user);
                                })
                                .as(StepVerifier::create)
                                .expectError(DataIntegrityViolationException.class)
                                .verify(Duration.ofSeconds(10));
        }
}
