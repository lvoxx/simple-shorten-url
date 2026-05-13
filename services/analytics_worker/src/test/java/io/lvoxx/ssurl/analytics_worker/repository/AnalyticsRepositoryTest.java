package io.lvoxx.ssurl.analytics_worker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;

import io.lvoxx.ssurl.common.model.Analytics;
import io.lvoxx.ssurl.test_starter.AbstractPostgresContainer;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ActiveProfiles("repo")
@DisplayName("Analytics Repository Tests")
@Tags({
                @Tag("Repository"), @Tag("Integration")
})
class AnalyticsRepositoryTest extends AbstractPostgresContainer {

        @Autowired
        private AnalyticsRepository analyticsRepository;

        @Test
        void should_saveAnalytics_when_validData() {
                analyticsRepository.save(Analytics.builder()
                                .shortCode("evt001")
                                .ip("192.168.1.1")
                                .userAgent("Mozilla/5.0")
                                .referer("https://google.com")
                                .country("US")
                                .createdAt(LocalDateTime.now())
                                .build())
                                .as(StepVerifier::create)
                                .assertNext(a -> {
                                        assertThat(a.getId()).isNotNull();
                                        assertThat(a.getShortCode()).isEqualTo("evt001");
                                        assertThat(a.getIp()).isEqualTo("192.168.1.1");
                                        assertThat(a.getUserAgent()).isEqualTo("Mozilla/5.0");
                                        assertThat(a.getReferer()).isEqualTo("https://google.com");
                                        assertThat(a.getCountry()).isEqualTo("US");
                                        assertThat(a.getCreatedAt()).isNotNull();
                                })
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_saveAnalytics_when_nullableFieldsNull() {
                analyticsRepository.save(Analytics.builder()
                                .shortCode("evt002")
                                .ip("10.0.0.1")
                                .createdAt(LocalDateTime.now())
                                .build())
                                .as(StepVerifier::create)
                                .assertNext(a -> {
                                        assertThat(a.getId()).isNotNull();
                                        assertThat(a.getShortCode()).isEqualTo("evt002");
                                        assertThat(a.getUserAgent()).isNull();
                                        assertThat(a.getReferer()).isNull();
                                        assertThat(a.getCountry()).isNull();
                                })
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_returnAnalytics_when_findById() {
                analyticsRepository.save(Analytics.builder()
                                .shortCode("evt003")
                                .ip("10.0.0.2")
                                .createdAt(LocalDateTime.now())
                                .build())
                                .flatMap(saved -> analyticsRepository.findById(saved.getId()))
                                .as(StepVerifier::create)
                                .assertNext(a -> assertThat(a.getShortCode()).isEqualTo("evt003"))
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_returnEmpty_when_findByIdNotFound() {
                analyticsRepository.findById(99999L)
                                .as(StepVerifier::create)
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        void should_findByShortCode_when_singleEvent() {
                analyticsRepository.save(Analytics.builder()
                                .shortCode("evt004")
                                .ip("10.0.0.3")
                                .createdAt(LocalDateTime.now())
                                .build())
                                .flatMapMany(saved -> analyticsRepository.findByShortCode("evt004"))
                                .as(StepVerifier::create)
                                .assertNext(a -> assertThat(a.getIp()).isEqualTo("10.0.0.3"))
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_findByShortCode_when_multipleEvents() {
                var event1 = analyticsRepository.save(Analytics.builder()
                                .shortCode("evt005")
                                .ip("10.0.0.4")
                                .createdAt(LocalDateTime.now())
                                .build());
                var event2 = analyticsRepository.save(Analytics.builder()
                                .shortCode("evt005")
                                .ip("10.0.0.5")
                                .createdAt(LocalDateTime.now().plusSeconds(1))
                                .build());
                var event3 = analyticsRepository.save(Analytics.builder()
                                .shortCode("evt005")
                                .ip("10.0.0.6")
                                .createdAt(LocalDateTime.now().plusSeconds(2))
                                .build());

                event1.then(event2).then(event3)
                                .thenMany(analyticsRepository.findByShortCode("evt005"))
                                .as(StepVerifier::create)
                                .expectNextCount(3)
                                .verifyComplete();
        }

        @Test
        void should_returnEmpty_when_findByShortCodeNotFound() {
                analyticsRepository.findByShortCode("nonexistent")
                                .as(StepVerifier::create)
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        void should_insertIntoPartitionedTable_when_validData() {
                analyticsRepository.save(Analytics.builder()
                                .shortCode("part001")
                                .ip("172.16.0.1")
                                .createdAt(LocalDateTime.now())
                                .build())
                                .flatMap(saved -> analyticsRepository.findById(saved.getId()))
                                .as(StepVerifier::create)
                                .assertNext(a -> {
                                        assertThat(a.getId()).isNotNull();
                                        assertThat(a.getShortCode()).isEqualTo("part001");
                                })
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_updateAnalytics_when_validChanges() {
                analyticsRepository.save(Analytics.builder()
                                .shortCode("evt006")
                                .ip("10.0.0.7")
                                .country("UK")
                                .createdAt(LocalDateTime.now())
                                .build())
                                .flatMap(saved -> {
                                        saved.setCountry("FR");
                                        return analyticsRepository.save(saved);
                                })
                                .as(StepVerifier::create)
                                .assertNext(a -> assertThat(a.getCountry()).isEqualTo("FR"))
                                .expectComplete()
                                .verify(Duration.ofSeconds(10));
        }

        @Test
        void should_deleteAnalytics_when_validId() {
                analyticsRepository.save(Analytics.builder()
                                .shortCode("evt007")
                                .ip("10.0.0.8")
                                .createdAt(LocalDateTime.now())
                                .build())
                                .flatMap(saved -> analyticsRepository.deleteById(saved.getId())
                                                .then(analyticsRepository.findById(saved.getId())))
                                .as(StepVerifier::create)
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        void should_countEvents_when_multipleExist() {
                var e1 = analyticsRepository.save(Analytics.builder()
                                .shortCode("cnt001").ip("1.1.1.1").createdAt(LocalDateTime.now()).build());
                var e2 = analyticsRepository.save(Analytics.builder()
                                .shortCode("cnt001").ip("1.1.1.2").createdAt(LocalDateTime.now().plusSeconds(1))
                                .build());

                e1.then(e2)
                                .thenMany(analyticsRepository.findByShortCode("cnt001"))
                                .as(StepVerifier::create)
                                .expectNextCount(2)
                                .verifyComplete();
        }
}
