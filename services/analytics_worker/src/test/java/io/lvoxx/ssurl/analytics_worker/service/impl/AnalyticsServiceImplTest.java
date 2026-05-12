package io.lvoxx.ssurl.analytics_worker.service.impl;

import io.lvoxx.ssurl.analytics_worker.repository.AnalyticsRepository;
import io.lvoxx.ssurl.common.model.Analytics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock private AnalyticsRepository analyticsRepository;

    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(analyticsRepository);
    }

    @Test
    @DisplayName("batchInsert saves all events")
    void batchInsert_savesAll() {
        Analytics a1 = new Analytics();
        a1.setShortCode("abc123");
        a1.setIp("1.2.3.4");

        Analytics a2 = new Analytics();
        a2.setShortCode("def456");
        a2.setIp("5.6.7.8");

        List<Analytics> events = List.of(a1, a2);

        when(analyticsRepository.saveAll(events)).thenReturn(Flux.just(a1, a2));

        StepVerifier.create(analyticsService.batchInsert(events))
                .verifyComplete();

        verify(analyticsRepository).saveAll(events);
    }

    @Test
    @DisplayName("batchInsert handles empty list")
    void batchInsert_emptyList() {
        List<Analytics> empty = List.of();

        when(analyticsRepository.saveAll(empty)).thenReturn(Flux.empty());

        StepVerifier.create(analyticsService.batchInsert(empty))
                .verifyComplete();
    }
}
