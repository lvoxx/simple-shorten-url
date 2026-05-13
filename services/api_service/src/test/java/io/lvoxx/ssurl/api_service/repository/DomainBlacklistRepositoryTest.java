package io.lvoxx.ssurl.api_service.repository;

import io.lvoxx.ssurl.common.model.DomainBlacklist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DomainBlacklistRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DomainBlacklistRepository domainBlacklistRepository;

    @Test
    void should_saveDomain_when_validData() {
        domainBlacklistRepository.save(DomainBlacklist.builder()
                        .domain("spam.com")
                        .reason("Known spammer")
                        .build())
                .as(StepVerifier::create)
                .assertNext(d -> {
                    assertThat(d.getId()).isNotNull();
                    assertThat(d.getDomain()).isEqualTo("spam.com");
                    assertThat(d.getReason()).isEqualTo("Known spammer");
                    assertThat(d.getCreatedAt()).isNotNull();
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_saveDomain_when_reasonDefault() {
        domainBlacklistRepository.save(DomainBlacklist.builder()
                        .domain("default-reason.com")
                        .build())
                .as(StepVerifier::create)
                .assertNext(d -> {
                    assertThat(d.getId()).isNotNull();
                    assertThat(d.getReason()).isEqualTo("Security/Abuse prevention");
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnDomain_when_findById() {
        domainBlacklistRepository.save(DomainBlacklist.builder()
                        .domain("findbyid-bl.com")
                        .build())
                .flatMap(saved -> domainBlacklistRepository.findById(saved.getId()))
                .as(StepVerifier::create)
                .assertNext(d -> assertThat(d.getDomain()).isEqualTo("findbyid-bl.com"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnEmpty_when_findByIdNotFound() {
        domainBlacklistRepository.findById(99999L)
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_returnTrue_when_existsByDomain() {
        domainBlacklistRepository.save(DomainBlacklist.builder()
                        .domain("exists-bl.com")
                        .build())
                .flatMap(d -> domainBlacklistRepository.existsByDomain("exists-bl.com"))
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists).isTrue())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnFalse_when_existsByDomainNotFound() {
        domainBlacklistRepository.existsByDomain("safe.com")
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists).isFalse())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_findByDomain_when_exists() {
        domainBlacklistRepository.save(DomainBlacklist.builder()
                        .domain("finddomain-bl.com")
                        .reason("phishing")
                        .build())
                .flatMap(d -> domainBlacklistRepository.findByDomain("finddomain-bl.com"))
                .as(StepVerifier::create)
                .assertNext(d -> assertThat(d.getReason()).isEqualTo("phishing"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnEmpty_when_findByDomainNotFound() {
        domainBlacklistRepository.findByDomain("not-blacklisted.com")
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void should_throw_when_domainNotUnique() {
        var d1 = domainBlacklistRepository.save(DomainBlacklist.builder()
                .domain("uniq-bl.com").build());
        var d2 = domainBlacklistRepository.save(DomainBlacklist.builder()
                .domain("uniq-bl.com").build());

        d1.then(d2)
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_throw_when_domainNull() {
        domainBlacklistRepository.save(DomainBlacklist.builder().build())
                .as(StepVerifier::create)
                .expectError(DataIntegrityViolationException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_updateReason_when_valid() {
        domainBlacklistRepository.save(DomainBlacklist.builder()
                        .domain("update-bl.com")
                        .reason("old reason")
                        .build())
                .flatMap(d -> {
                    d.setReason("new reason");
                    return domainBlacklistRepository.save(d);
                })
                .as(StepVerifier::create)
                .assertNext(d -> assertThat(d.getReason()).isEqualTo("new reason"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_deleteDomain_when_validId() {
        domainBlacklistRepository.save(DomainBlacklist.builder()
                        .domain("delete-bl.com")
                        .build())
                .flatMap(d -> domainBlacklistRepository.deleteById(d.getId())
                        .then(domainBlacklistRepository.findById(d.getId())))
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }
}
