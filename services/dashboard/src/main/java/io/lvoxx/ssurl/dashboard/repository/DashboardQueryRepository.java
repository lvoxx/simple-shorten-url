package io.lvoxx.ssurl.dashboard.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.lvoxx.ssurl.dashboard.dto.response.TimeSeriesPoint;
import io.lvoxx.ssurl.dashboard.dto.response.TopItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Aggregation queries for the dashboard, plus the daily-rollup writer.
 *
 * <p>
 * Every read is <b>ownership-scoped</b> by joining {@code click_events} /
 * {@code click_daily_rollup} → {@code urls} → {@code users} and filtering on the
 * authenticated {@code username} (JWT subject). Time-series and top-link reads
 * hit the small pre-aggregated {@code click_daily_rollup} table rather than
 * scanning raw {@code click_events}; referer/unique-visitor breakdowns fall back
 * to the indexed raw table.
 */
public interface DashboardQueryRepository {

    // ── Writes (ingestion) ───────────────────────────────────────────────────

    /**
     * Increment per-{@code (shortCode, day)} click counts in
     * {@code click_daily_rollup} via {@code INSERT … ON CONFLICT DO UPDATE}.
     *
     * @param increments aggregated counts for the consumed batch
     */
    Mono<Void> upsertDailyRollup(Map<RollupKey, Long> increments);

    // ── Reads (ownership-scoped) ─────────────────────────────────────────────

    Mono<Long> totalClicks(String username, LocalDate from);

    Mono<Long> activeLinks(String username);

    Mono<Long> uniqueVisitors(String username, String code, LocalDateTime from);

    /** Clicks within an inclusive day range, from the rollup (used for trend deltas). */
    Mono<Long> clicksBetween(String username, LocalDate fromInclusive, LocalDate toInclusive);

    Flux<TimeSeriesPoint> timeseries(String username, String code, LocalDate from, LocalDate to);

    Flux<TopItem> topLinks(String username, LocalDate from, int limit);

    Flux<TopItem> topReferers(String username, String code, LocalDateTime from, int limit);

    Mono<Boolean> ownsCode(String username, String shortCode);

    Mono<String> titleOf(String shortCode);

    Mono<LocalDateTime> lastClickedAt(String shortCode);

    /** Aggregation key for {@link #upsertDailyRollup}. */
    record RollupKey(String shortCode, LocalDate day) {
    }

    /** Convenience: collect a Flux of points into a list (keeps service code terse). */
    default Mono<List<TimeSeriesPoint>> timeseriesList(String username, String code, LocalDate from, LocalDate to) {
        return timeseries(username, code, from, to).collectList();
    }
}
