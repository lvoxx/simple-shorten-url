package io.lvoxx.ssurl.dashboard.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;

import io.lvoxx.ssurl.common.exception.ShortCodeNotFoundException;
import io.lvoxx.ssurl.dashboard.dto.response.CodeStats;
import io.lvoxx.ssurl.dashboard.dto.response.DashboardOverview;
import io.lvoxx.ssurl.dashboard.dto.response.TimeSeriesPoint;
import io.lvoxx.ssurl.dashboard.dto.response.TopItem;
import io.lvoxx.ssurl.dashboard.repository.DashboardQueryRepository;
import io.lvoxx.ssurl.dashboard.service.DashboardService;
import io.lvoxx.ssurl.dashboard.service.StatsCacheService;
import reactor.core.publisher.Mono;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final TypeReference<DashboardOverview> OVERVIEW_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<TimeSeriesPoint>> SERIES_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<TopItem>> TOP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<CodeStats> CODE_STATS_TYPE = new TypeReference<>() {
    };

    private final DashboardQueryRepository repo;
    private final StatsCacheService cache;
    private final Duration cacheTtl;

    public DashboardServiceImpl(DashboardQueryRepository repo, StatsCacheService cache,
            @Value("${app.dashboard.cache-ttl-seconds:60}") long cacheTtlSeconds) {
        this.repo = repo;
        this.cache = cache;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<DashboardOverview> overview(String username, int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        LocalDate prevFrom = from.minusDays(days);
        LocalDate prevTo = from.minusDays(1);

        Mono<DashboardOverview> loader = Mono.zip(
                repo.totalClicks(username, from),
                repo.activeLinks(username),
                repo.clicksBetween(username, today, today),
                repo.uniqueVisitors(username, null, from.atStartOfDay()),
                repo.clicksBetween(username, prevFrom, prevTo))
                .map(t -> new DashboardOverview(
                        t.getT1(), t.getT2(), t.getT3(), t.getT4(),
                        trendPct(t.getT1(), t.getT5())));

        return cache.cached(key("overview", username, days), OVERVIEW_TYPE, cacheTtl, loader);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<List<TimeSeriesPoint>> timeseries(String username, String code, int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        Mono<List<TimeSeriesPoint>> loader = repo.timeseries(username, code, from, today).collectList();
        return cache.cached(key("ts", username, days, code), SERIES_TYPE, cacheTtl, loader);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<List<TopItem>> topLinks(String username, int days, int limit) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        Mono<List<TopItem>> loader = repo.topLinks(username, from, limit).collectList();
        return cache.cached(key("toplinks", username, days, limit), TOP_TYPE, cacheTtl, loader);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<List<TopItem>> topReferers(String username, int days, int limit) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        Mono<List<TopItem>> loader = repo.topReferers(username, null, from.atStartOfDay(), limit).collectList();
        return cache.cached(key("topref", username, days, limit), TOP_TYPE, cacheTtl, loader);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<CodeStats> codeStats(String username, String code, int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        List<LocalDate> daysRange = from.datesUntil(today.plusDays(1)).toList();

        Mono<CodeStats> loader = repo.ownsCode(username, code)
                .flatMap(owned -> {
                    if (!Boolean.TRUE.equals(owned)) {
                        return Mono.error(new ShortCodeNotFoundException(code));
                    }
                    Mono<List<TimeSeriesPoint>> series = repo.timeseries(username, code, from, today).collectList();
                    Mono<Long> uniques = cache.uniqueVisitorsForCode(code, daysRange)
                            .switchIfEmpty(repo.uniqueVisitors(username, code, from.atStartOfDay()));
                    Mono<List<TopItem>> referers = repo.topReferers(username, code, from.atStartOfDay(), 5).collectList();
                    Mono<String> title = repo.titleOf(code).defaultIfEmpty(code);
                    return Mono.zip(series, uniques, referers, title,
                            repo.lastClickedAt(code).map(java.util.Optional::of).defaultIfEmpty(java.util.Optional.empty()))
                            .map(t -> new CodeStats(
                                    code,
                                    t.getT4(),
                                    t.getT1().stream().mapToLong(TimeSeriesPoint::clicks).sum(),
                                    t.getT2(),
                                    t.getT5().orElse(null),
                                    t.getT1(),
                                    t.getT3()));
                });

        return cache.cached(key("code", username, days, code), CODE_STATS_TYPE, cacheTtl, loader);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static double trendPct(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return Math.round(((double) (current - previous) / previous) * 1000.0) / 10.0;
    }

    private static String key(Object... parts) {
        return Stream.of(parts).map(p -> p == null ? "_" : p.toString())
                .reduce((a, b) -> a + ":" + b).orElse("");
    }
}
