package io.lvoxx.ssurl.redirect_service.repository;

import io.lvoxx.ssurl.common.model.Url;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class UrlRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UrlRepository urlRepository;

    @Test
    void should_saveUrl_when_validData() {
        urlRepository.save(Url.builder()
                        .shortCode("rdr01")
                        .originalUrl("https://redirect-example.com")
                        .build())
                .as(StepVerifier::create)
                .assertNext(url -> {
                    assertThat(url.getId()).isNotNull();
                    assertThat(url.getShortCode()).isEqualTo("rdr01");
                    assertThat(url.getOriginalUrl()).isEqualTo("https://redirect-example.com");
                    assertThat(url.isActive()).isTrue();
                    assertThat(url.getClickCount()).isZero();
                    assertThat(url.getCreatedAt()).isNotNull();
                    assertThat(url.getUserId()).isNull();
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnUrl_when_findById() {
        urlRepository.save(Url.builder()
                        .shortCode("rdrfind")
                        .originalUrl("https://redirect-find.com")
                        .build())
                .flatMap(saved -> urlRepository.findById(saved.getId()))
                .as(StepVerifier::create)
                .assertNext(url -> assertThat(url.getShortCode()).isEqualTo("rdrfind"))
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
    void should_findByShortCodeAndIsActive_when_active() {
        urlRepository.save(Url.builder()
                        .shortCode("rdractv")
                        .originalUrl("https://redirect-active.com")
                        .isActive(true)
                        .build())
                .flatMap(saved -> urlRepository.findByShortCodeAndIsActive("rdractv", true))
                .as(StepVerifier::create)
                .assertNext(url -> {
                    assertThat(url.getShortCode()).isEqualTo("rdractv");
                    assertThat(url.isActive()).isTrue();
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnEmpty_when_findByShortCodeAndIsActive_inactive() {
        urlRepository.save(Url.builder()
                        .shortCode("rdrinact")
                        .originalUrl("https://redirect-inactive.com")
                        .isActive(false)
                        .build())
                .flatMap(saved -> urlRepository.findByShortCodeAndIsActive("rdrinact", true))
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_returnEmpty_when_findByShortCodeAndIsActive_notFound() {
        urlRepository.findByShortCodeAndIsActive("ghost", true)
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_incrementClickCount_when_validShortCode() {
        urlRepository.save(Url.builder()
                        .shortCode("rdrclk")
                        .originalUrl("https://redirect-click.com")
                        .clickCount(10)
                        .build())
                .flatMap(saved -> urlRepository.incrementClickCount("rdrclk")
                        .then(urlRepository.findById(saved.getId())))
                .as(StepVerifier::create)
                .assertNext(url -> assertThat(url.getClickCount()).isEqualTo(11))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_shortCodeNotUnique() {
        var u1 = urlRepository.save(Url.builder()
                .shortCode("rdrdup")
                .originalUrl("https://dup1.com")
                .build());
        var u2 = urlRepository.save(Url.builder()
                .shortCode("rdrdup")
                .originalUrl("https://dup2.com")
                .build());

        u1.then(u2)
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_shortCodeNull() {
        urlRepository.save(Url.builder()
                        .originalUrl("https://nonull.com")
                        .build())
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_originalUrlNull() {
        urlRepository.save(Url.builder()
                        .shortCode("rdrnourl")
                        .build())
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_updateUrl_when_validChanges() {
        urlRepository.save(Url.builder()
                        .shortCode("rdrupd")
                        .originalUrl("https://before.com")
                        .title("old")
                        .isActive(true)
                        .build())
                .flatMap(saved -> {
                    saved.setTitle("updated");
                    saved.setOriginalUrl("https://after.com");
                    return urlRepository.save(saved);
                })
                .as(StepVerifier::create)
                .assertNext(url -> {
                    assertThat(url.getTitle()).isEqualTo("updated");
                    assertThat(url.getOriginalUrl()).isEqualTo("https://after.com");
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_deleteUrl_when_validId() {
        urlRepository.save(Url.builder()
                        .shortCode("rdrdel")
                        .originalUrl("https://delete.com")
                        .build())
                .flatMap(saved -> urlRepository.deleteById(saved.getId())
                        .then(urlRepository.findById(saved.getId())))
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_returnEmpty_when_findByShortCodeAndIsActive_afterDeactivation() {
        urlRepository.save(Url.builder()
                        .shortCode("rdrdeact")
                        .originalUrl("https://deactivate.com")
                        .isActive(true)
                        .build())
                .flatMap(saved -> {
                    saved.setActive(false);
                    return urlRepository.save(saved);
                })
                .flatMap(saved -> urlRepository.findByShortCodeAndIsActive("rdrdeact", true))
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }
}
