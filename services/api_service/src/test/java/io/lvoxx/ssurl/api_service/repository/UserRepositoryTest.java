package io.lvoxx.ssurl.api_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import io.lvoxx.ssurl.common.model.User;
import io.lvoxx.ssurl.test_starter.AbstractPostgresContainer;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("User Repository Tests")
@Tags({
                @Tag("Repository"), @Tag("Integration")
})
class UserRepositoryTest extends AbstractPostgresContainer {

    @Autowired
    private UserRepository userRepository;

    @Test
    void should_saveUser_when_validData() {
        var user = User.builder()
                .username("savetest")
                .email("save@test.com")
                .passwordHash("secret123")
                .build();

        userRepository.save(user)
                .as(StepVerifier::create)
                .assertNext(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getUsername()).isEqualTo("savetest");
                    assertThat(saved.getEmail()).isEqualTo("save@test.com");
                    assertThat(saved.getPasswordHash()).isEqualTo("secret123");
                    assertThat(saved.getRole()).isEqualTo("USER");
                    assertThat(saved.isActive()).isTrue();
                    assertThat(saved.getCreatedAt()).isNotNull();
                    assertThat(saved.getUpdatedAt()).isNotNull();
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnUser_when_findById() {
        var saved = userRepository.save(User.builder()
                .username("findbyid")
                .email("findbyid@test.com")
                .passwordHash("hash")
                .build());

        saved.flatMap(u -> userRepository.findById(u.getId()))
                .as(StepVerifier::create)
                .assertNext(u -> assertThat(u.getUsername()).isEqualTo("findbyid"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnEmpty_when_findByIdNotFound() {
        userRepository.findById(99999L)
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_findByUsername_when_exists() {
        userRepository.save(User.builder()
                        .username("finduser")
                        .email("finduser@test.com")
                        .passwordHash("hash")
                        .build())
                .flatMap(u -> userRepository.findByUsername("finduser"))
                .as(StepVerifier::create)
                .assertNext(u -> assertThat(u.getEmail()).isEqualTo("finduser@test.com"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnEmpty_when_findByUsernameNotFound() {
        userRepository.findByUsername("nobody")
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_findByEmail_when_exists() {
        userRepository.save(User.builder()
                        .username("findemail")
                        .email("findemail@test.com")
                        .passwordHash("hash")
                        .build())
                .flatMap(u -> userRepository.findByEmail("findemail@test.com"))
                .as(StepVerifier::create)
                .assertNext(u -> assertThat(u.getUsername()).isEqualTo("findemail"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnEmpty_when_findByEmailNotFound() {
        userRepository.findByEmail("no@test.com")
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_returnTrue_when_existsByUsername() {
        userRepository.save(User.builder()
                        .username("existuser")
                        .email("existuser@test.com")
                        .passwordHash("hash")
                        .build())
                .flatMap(u -> userRepository.existsByUsername("existuser"))
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists).isTrue())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnFalse_when_existsByUsernameNotFound() {
        userRepository.existsByUsername("ghost")
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists).isFalse())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnTrue_when_existsByEmail() {
        userRepository.save(User.builder()
                        .username("existemail")
                        .email("existemail@test.com")
                        .passwordHash("hash")
                        .build())
                .flatMap(u -> userRepository.existsByEmail("existemail@test.com"))
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists).isTrue())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnFalse_when_existsByEmailNotFound() {
        userRepository.existsByEmail("ghost@test.com")
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists).isFalse())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_usernameNotUnique() {
        var user1 = userRepository.save(User.builder()
                .username("uniqname")
                .email("uniq1@test.com")
                .passwordHash("hash")
                .build());
        var user2 = userRepository.save(User.builder()
                .username("uniqname")
                .email("uniq2@test.com")
                .passwordHash("hash")
                .build());

        user1.then(user2)
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_emailNotUnique() {
        var user1 = userRepository.save(User.builder()
                .username("uniqemail1")
                .email("same@test.com")
                .passwordHash("hash")
                .build());
        var user2 = userRepository.save(User.builder()
                .username("uniqemail2")
                .email("same@test.com")
                .passwordHash("hash")
                .build());

        user1.then(user2)
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_emailNull() {
        userRepository.save(User.builder()
                        .username("nonullmail")
                        .passwordHash("hash")
                        .build())
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_usernameNull() {
        userRepository.save(User.builder()
                        .email("nonulluser@test.com")
                        .passwordHash("hash")
                        .build())
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_passwordHashNull() {
        userRepository.save(User.builder()
                        .username("nonullpwd")
                        .email("nonullpwd@test.com")
                        .build())
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_updateEmail_when_valid() {
        userRepository.save(User.builder()
                        .username("updemail")
                        .email("old@test.com")
                        .passwordHash("hash")
                        .build())
                .flatMap(u -> {
                    u.setEmail("new@test.com");
                    return userRepository.save(u);
                })
                .as(StepVerifier::create)
                .assertNext(u -> assertThat(u.getEmail()).isEqualTo("new@test.com"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_deleteUser_when_validId() {
        userRepository.save(User.builder()
                        .username("deluser")
                        .email("deluser@test.com")
                        .passwordHash("hash")
                        .build())
                .flatMap(u -> userRepository.deleteById(u.getId())
                        .then(userRepository.findById(u.getId())))
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }
}
