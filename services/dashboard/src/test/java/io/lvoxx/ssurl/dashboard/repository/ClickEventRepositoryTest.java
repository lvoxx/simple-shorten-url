package io.lvoxx.ssurl.dashboard.repository;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;

import io.lvoxx.ssurl.dashboard.domain.ClickEvent;
import io.lvoxx.ssurl.test_starter.AbstractPostgresContainer;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("ClickEvent Repository Tests")
@Tags({ @Tag("Repository"), @Tag("Integration") })
class ClickEventRepositoryTest extends AbstractPostgresContainer {

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Test
    void should_saveClickEvent_andGenerateId() {
        clickEventRepository.save(ClickEvent.builder()
                .shortCode("abc123")
                .ip("10.0.0.1")
                .userAgent("Mozilla/5.0")
                .referer("https://google.com")
                .createdAt(LocalDateTime.now())
                .build())
                .as(StepVerifier::create)
                .assertNext(e -> {
                    org.assertj.core.api.Assertions.assertThat(e.getId()).isNotNull();
                    org.assertj.core.api.Assertions.assertThat(e.getShortCode()).isEqualTo("abc123");
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_returnRecentEventsForCode_orderedByCreatedAtDesc() {
        LocalDateTime base = LocalDateTime.now();
        clickEventRepository.save(ClickEvent.builder().shortCode("rec").ip("1.1.1.1").createdAt(base).build())
                .then(clickEventRepository.save(
                        ClickEvent.builder().shortCode("rec").ip("2.2.2.2").createdAt(base.plusSeconds(5)).build()))
                .thenMany(clickEventRepository.findTop20ByShortCodeOrderByCreatedAtDesc("rec"))
                .as(StepVerifier::create)
                .assertNext(e -> org.assertj.core.api.Assertions.assertThat(e.getIp()).isEqualTo("2.2.2.2"))
                .assertNext(e -> org.assertj.core.api.Assertions.assertThat(e.getIp()).isEqualTo("1.1.1.1"))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }
}
