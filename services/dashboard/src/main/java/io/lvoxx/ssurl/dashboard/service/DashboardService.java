package io.lvoxx.ssurl.dashboard.service;

import java.util.List;

import io.lvoxx.ssurl.dashboard.dto.response.CodeStats;
import io.lvoxx.ssurl.dashboard.dto.response.DashboardOverview;
import io.lvoxx.ssurl.dashboard.dto.response.TimeSeriesPoint;
import io.lvoxx.ssurl.dashboard.dto.response.TopItem;
import reactor.core.publisher.Mono;

/**
 * Read-side API for the dashboard. All methods are scoped to {@code username}
 * (the JWT subject) and served from the rollup table / Redis with a short-TTL
 * aggregation cache in front.
 */
public interface DashboardService {

    Mono<DashboardOverview> overview(String username, int days);

    Mono<List<TimeSeriesPoint>> timeseries(String username, String code, int days);

    Mono<List<TopItem>> topLinks(String username, int days, int limit);

    Mono<List<TopItem>> topReferers(String username, int days, int limit);

    /** Per-code detail; errors with {@code UrlNotFoundException} if not owned by the user. */
    Mono<CodeStats> codeStats(String username, String code, int days);
}
