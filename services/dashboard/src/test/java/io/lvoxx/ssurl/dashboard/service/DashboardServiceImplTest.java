package io.lvoxx.ssurl.dashboard.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.dashboard.dto.response.TimeSeriesPoint;
import io.lvoxx.ssurl.dashboard.dto.response.TopItem;
import io.lvoxx.ssurl.dashboard.repository.DashboardQueryRepository;
import io.lvoxx.ssurl.dashboard.service.impl.DashboardServiceImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
@Tag("Unit")
class DashboardServiceImplTest {

    @Mock
    private DashboardQueryRepository repo;
    @Mock
    private StatsCacheService cache;

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(repo, cache, 60);
        // Cache is a pass-through in unit tests: run the loader (4th arg) directly.
        when(cache.cached(anyString(), any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(3));
    }

    @Test
    void overview_computesTrendVsPreviousWindow() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);
        LocalDate prevFrom = from.minusDays(7);
        LocalDate prevTo = from.minusDays(1);

        when(repo.totalClicks("alice", from)).thenReturn(Mono.just(100L));
        when(repo.activeLinks("alice")).thenReturn(Mono.just(5L));
        when(repo.clicksBetween("alice", today, today)).thenReturn(Mono.just(20L));
        when(repo.uniqueVisitors(eq("alice"), isNull(), any())).thenReturn(Mono.just(42L));
        when(repo.clicksBetween("alice", prevFrom, prevTo)).thenReturn(Mono.just(50L));

        service.overview("alice", 7)
                .as(StepVerifier::create)
                .assertNext(o -> {
                    org.assertj.core.api.Assertions.assertThat(o.totalClicks()).isEqualTo(100L);
                    org.assertj.core.api.Assertions.assertThat(o.activeLinks()).isEqualTo(5L);
                    org.assertj.core.api.Assertions.assertThat(o.clicksToday()).isEqualTo(20L);
                    org.assertj.core.api.Assertions.assertThat(o.uniqueVisitors()).isEqualTo(42L);
                    org.assertj.core.api.Assertions.assertThat(o.trendPct()).isEqualTo(100.0);
                })
                .verifyComplete();
    }

    @Test
    void codeStats_rejectsUnownedCode() {
        when(repo.ownsCode("alice", "x9")).thenReturn(Mono.just(false));

        service.codeStats("alice", "x9", 30)
                .as(StepVerifier::create)
                .expectError(ShortCodeNotFoundException.class)
                .verify();
    }

    @Test
    void codeStats_aggregatesOwnedCode() {
        LocalDate today = LocalDate.now();
        when(repo.ownsCode("alice", "a1")).thenReturn(Mono.just(true));
        when(repo.timeseries(eq("alice"), eq("a1"), any(), any()))
                .thenReturn(Flux.just(new TimeSeriesPoint(today, 4L), new TimeSeriesPoint(today.minusDays(1), 6L)));
        when(cache.uniqueVisitorsForCode(eq("a1"), any())).thenReturn(Mono.just(7L));
        // switchIfEmpty builds its fallback arg eagerly, so the DB call must be stubbed
        // (non-null) even though the HLL value wins and the fallback is never subscribed.
        when(repo.uniqueVisitors(eq("alice"), eq("a1"), any())).thenReturn(Mono.just(0L));
        when(repo.topReferers(eq("alice"), eq("a1"), any(), eq(5)))
                .thenReturn(Flux.just(new TopItem("https://x.com", "https://x.com", 3L)));
        when(repo.titleOf("a1")).thenReturn(Mono.just("Hello"));
        when(repo.lastClickedAt("a1")).thenReturn(Mono.just(LocalDateTime.now()));

        service.codeStats("alice", "a1", 30)
                .as(StepVerifier::create)
                .assertNext(s -> {
                    org.assertj.core.api.Assertions.assertThat(s.shortCode()).isEqualTo("a1");
                    org.assertj.core.api.Assertions.assertThat(s.title()).isEqualTo("Hello");
                    org.assertj.core.api.Assertions.assertThat(s.totalClicks()).isEqualTo(10L);
                    org.assertj.core.api.Assertions.assertThat(s.uniqueVisitors()).isEqualTo(7L);
                    org.assertj.core.api.Assertions.assertThat(s.topReferers()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void codeStats_fallsBackToDbUniquesWhenRedisEmpty() {
        when(repo.ownsCode("alice", "a1")).thenReturn(Mono.just(true));
        when(repo.timeseries(eq("alice"), eq("a1"), any(), any())).thenReturn(Flux.empty());
        when(cache.uniqueVisitorsForCode(eq("a1"), any())).thenReturn(Mono.empty());
        when(repo.uniqueVisitors(eq("alice"), eq("a1"), any())).thenReturn(Mono.just(99L));
        when(repo.topReferers(eq("alice"), eq("a1"), any(), eq(5))).thenReturn(Flux.empty());
        when(repo.titleOf("a1")).thenReturn(Mono.just("Hello"));
        when(repo.lastClickedAt("a1")).thenReturn(Mono.empty());

        service.codeStats("alice", "a1", 30)
                .as(StepVerifier::create)
                .assertNext(s -> {
                    org.assertj.core.api.Assertions.assertThat(s.uniqueVisitors()).isEqualTo(99L);
                    org.assertj.core.api.Assertions.assertThat(s.lastClickedAt()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void timeseries_returnsList() {
        LocalDate today = LocalDate.now();
        when(repo.timeseries(eq("alice"), isNull(), any(), any()))
                .thenReturn(Flux.just(new TimeSeriesPoint(today, 3L)));

        service.timeseries("alice", null, 30)
                .as(StepVerifier::create)
                .assertNext((List<TimeSeriesPoint> list) ->
                        org.assertj.core.api.Assertions.assertThat(list).hasSize(1))
                .verifyComplete();
    }
}
