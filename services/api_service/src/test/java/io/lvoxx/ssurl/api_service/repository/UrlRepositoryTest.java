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

import io.lvoxx.ssurl.common.model.Url;
import io.lvoxx.ssurl.common.model.User;
import io.lvoxx.ssurl.common.util.Constants;
import io.lvoxx.ssurl.test_starter.AbstractPostgresContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("Url Repository Tests")
@Tags({
                @Tag("Repository"), @Tag("Integration")
})
class UrlRepositoryTest extends AbstractPostgresContainer {

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private UserRepository userRepository;

    private Mono<User> createTestUser() {
        return userRepository.save(User.builder()
                .username("urluser")
                .email("urluser@test.com")
                .passwordHash("hash")
                .build());
    }

    @Test
    void should_saveUrl_when_validData() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(user.getId())
                        .build()))
                .as(StepVerifier::create)
                .assertNext(url -> {
                    assertThat(url.getId()).isNotNull();
                    assertThat(url.getShortCode()).isEqualTo("abc123");
                    assertThat(url.getOriginalUrl()).isEqualTo("https://example.com");
                    assertThat(url.isActive()).isTrue();
                    assertThat(url.getClickCount()).isZero();
                    assertThat(url.getCreatedAt()).isNotNull();
                    assertThat(url.getUpdatedAt()).isNotNull();
                    assertThat(url.getCreatedBy()).isEqualTo(Constants.Defaults.CREATED_BY);
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnUrl_when_findById() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("findid1")
                        .originalUrl("https://findbyid.com")
                        .userId(user.getId())
                        .build()))
                .flatMap(saved -> urlRepository.findById(saved.getId()))
                .as(StepVerifier::create)
                .assertNext(url -> assertThat(url.getShortCode()).isEqualTo("findid1"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnEmpty_when_findByIdNotFound() {
        urlRepository.findById(99999L)
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_findByShortCode_when_exists() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("bycode1")
                        .originalUrl("https://bycode.com")
                        .userId(user.getId())
                        .build()))
                .flatMap(saved -> urlRepository.findByShortCode("bycode1"))
                .as(StepVerifier::create)
                .assertNext(url -> assertThat(url.getOriginalUrl()).isEqualTo("https://bycode.com"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnEmpty_when_findByShortCodeNotFound() {
        urlRepository.findByShortCode("nonexistent")
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_findByShortCodeAndIsActive_when_active() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("actv01")
                        .originalUrl("https://active.com")
                        .isActive(true)
                        .userId(user.getId())
                        .build()))
                .flatMap(saved -> urlRepository.findByShortCodeAndIsActive("actv01", true))
                .as(StepVerifier::create)
                .assertNext(url -> assertThat(url.isActive()).isTrue())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnEmpty_when_findByShortCodeAndIsActive_inactive() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("inactv")
                        .originalUrl("https://inactive.com")
                        .isActive(false)
                        .userId(user.getId())
                        .build()))
                .flatMap(saved -> urlRepository.findByShortCodeAndIsActive("inactv", true))
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_returnTopUrls_when_findTopByUserIdOrderByIdDesc() {
        createTestUser()
                .flatMapMany(user -> {
                    var url1 = urlRepository.save(Url.builder()
                            .shortCode("top01").originalUrl("https://top1.com").userId(user.getId()).build());
                    var url2 = urlRepository.save(Url.builder()
                            .shortCode("top02").originalUrl("https://top2.com").userId(user.getId()).build());
                    var url3 = urlRepository.save(Url.builder()
                            .shortCode("top03").originalUrl("https://top3.com").userId(user.getId()).build());
                    return url1.then(url2).then(url3).thenReturn(user);
                })
                .flatMap(user -> urlRepository.findTopByUserIdOrderByIdDesc(user.getId(), 2))
                .as(StepVerifier::create)
                .assertNext(url -> assertThat(url.getShortCode()).isIn("top03", "top02"))
                .assertNext(url -> assertThat(url.getShortCode()).isIn("top03", "top02"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnUrls_when_findByUserIdAndIdLessThanOrderByIdDesc() {
        createTestUser()
                .flatMapMany(user -> {
                    var url1 = urlRepository.save(Url.builder()
                            .shortCode("cur01").originalUrl("https://cur1.com").userId(user.getId()).build());
                    var url2 = urlRepository.save(Url.builder()
                            .shortCode("cur02").originalUrl("https://cur2.com").userId(user.getId()).build());
                    var url3 = urlRepository.save(Url.builder()
                            .shortCode("cur03").originalUrl("https://cur3.com").userId(user.getId()).build());
                    var url4 = urlRepository.save(Url.builder()
                            .shortCode("cur04").originalUrl("https://cur4.com").userId(user.getId()).build());
                    return url1.then(url2).then(url3).then(url4).thenReturn(user);
                })
                .flatMap(user -> urlRepository.findByUserIdAndIdLessThanOrderByIdDesc(
                        user.getId(), 4L, 2))
                .as(StepVerifier::create)
                .assertNext(url -> assertThat(url.getShortCode()).isIn("cur03", "cur02"))
                .assertNext(url -> assertThat(url.getShortCode()).isIn("cur03", "cur02"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnCount_when_countByUserId() {
        createTestUser()
                .flatMapMany(user -> {
                    var url1 = urlRepository.save(Url.builder()
                            .shortCode("cnt01").originalUrl("https://cnt1.com").userId(user.getId()).build());
                    var url2 = urlRepository.save(Url.builder()
                            .shortCode("cnt02").originalUrl("https://cnt2.com").userId(user.getId()).build());
                    return url1.then(url2).thenReturn(user);
                })
                .flatMap(user -> urlRepository.countByUserId(user.getId()))
                .as(StepVerifier::create)
                .assertNext(count -> assertThat(count).isEqualTo(2))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_incrementClickCount_when_validShortCode() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("clk01")
                        .originalUrl("https://click.com")
                        .userId(user.getId())
                        .clickCount(5)
                        .build()))
                .flatMap(saved -> urlRepository.incrementClickCount("clk01")
                        .then(urlRepository.findByShortCode("clk01")))
                .as(StepVerifier::create)
                .assertNext(url -> assertThat(url.getClickCount()).isEqualTo(6))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_shortCodeNotUnique() {
        createTestUser()
                .flatMapMany(user -> {
                    var url1 = urlRepository.save(Url.builder()
                            .shortCode("dup").originalUrl("https://dup1.com").userId(user.getId()).build());
                    var url2 = urlRepository.save(Url.builder()
                            .shortCode("dup").originalUrl("https://dup2.com").userId(user.getId()).build());
                    return url1.then(url2);
                })
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_shortCodeNull() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .originalUrl("https://nonull.com")
                        .userId(user.getId())
                        .build()))
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_originalUrlNull() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("nourl")
                        .userId(user.getId())
                        .build()))
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_updateUrl_when_validChanges() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("upd01")
                        .originalUrl("https://before.com")
                        .title("old title")
                        .userId(user.getId())
                        .build()))
                .flatMap(saved -> {
                    saved.setTitle("new title");
                    saved.setOriginalUrl("https://after.com");
                    return urlRepository.save(saved);
                })
                .as(StepVerifier::create)
                .assertNext(url -> {
                    assertThat(url.getTitle()).isEqualTo("new title");
                    assertThat(url.getOriginalUrl()).isEqualTo("https://after.com");
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_deleteUrl_when_validId() {
        createTestUser()
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("del01")
                        .originalUrl("https://delete.com")
                        .userId(user.getId())
                        .build()))
                .flatMap(saved -> urlRepository.deleteById(saved.getId())
                        .then(urlRepository.findById(saved.getId())))
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_resetUserIdToNull_when_userDeleted() {
        var savedUser = userRepository.save(User.builder()
                .username("deluser")
                .email("deluser@test.com")
                .passwordHash("hash")
                .build());
        savedUser
                .flatMap(user -> urlRepository.save(Url.builder()
                        .shortCode("fkdel")
                        .originalUrl("https://fkdel.com")
                        .userId(user.getId())
                        .build())
                        .thenReturn(user))
                .flatMap(user -> userRepository.deleteById(user.getId()))
                .then(savedUser.flatMap(u -> urlRepository.findByShortCode("fkdel")))
                .as(StepVerifier::create)
                .assertNext(url -> assertThat(url.getUserId()).isNull())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_rollback_when_constraintViolationInBatch() {
        createTestUser()
                .flatMapMany(user -> {
                    var url1 = urlRepository.save(Url.builder()
                            .shortCode("rll01").originalUrl("https://roll1.com").userId(user.getId()).build());
                    var url2 = urlRepository.save(Url.builder()
                            .shortCode("rll01").originalUrl("https://roll2.com").userId(user.getId()).build());
                    return url1.then(url2).thenReturn(user);
                })
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }
}
