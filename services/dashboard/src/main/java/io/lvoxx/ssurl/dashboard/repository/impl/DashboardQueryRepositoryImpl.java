package io.lvoxx.ssurl.dashboard.repository.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import io.lvoxx.ssurl.dashboard.dto.response.TimeSeriesPoint;
import io.lvoxx.ssurl.dashboard.dto.response.TopItem;
import io.lvoxx.ssurl.dashboard.repository.DashboardQueryRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link DashboardQueryRepository} backed by {@link DatabaseClient}. SQL is kept
 * here (one place) since the aggregations need joins and {@code GROUP BY} that
 * derived R2DBC queries can't express.
 */
@Repository
public class DashboardQueryRepositoryImpl implements DashboardQueryRepository {

    /** Ownership join shared by every scoped read. */
    private static final String OWNED =
            " JOIN urls u ON %1$s.short_code = u.short_code"
            + " JOIN users us ON u.user_id = us.id"
            + " WHERE us.username = :username";

    private final DatabaseClient db;

    public DashboardQueryRepositoryImpl(DatabaseClient db) {
        this.db = db;
    }

    // ── Writes ────────────────────────────────────────────────────────────────

    @Override
    public Mono<Void> upsertDailyRollup(Map<RollupKey, Long> increments) {
        if (increments == null || increments.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(increments.entrySet())
                .concatMap(e -> db.sql("""
                        INSERT INTO click_daily_rollup (short_code, day, clicks)
                        VALUES (:code, :day, :inc)
                        ON CONFLICT (short_code, day)
                        DO UPDATE SET clicks = click_daily_rollup.clicks + EXCLUDED.clicks
                        """)
                        .bind("code", e.getKey().shortCode())
                        .bind("day", e.getKey().day())
                        .bind("inc", e.getValue())
                        .fetch()
                        .rowsUpdated())
                .then();
    }

    // ── Reads ──────────────────────────────────────────────────────────────────

    @Override
    public Mono<Long> totalClicks(String username, LocalDate from) {
        return db.sql("SELECT COALESCE(SUM(r.clicks), 0)"
                + " FROM click_daily_rollup r" + owned("r")
                + " AND r.day >= :from")
                .bind("username", username)
                .bind("from", from)
                .map((row, meta) -> longOf(row.get(0)))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Long> clicksBetween(String username, LocalDate fromInclusive, LocalDate toInclusive) {
        return db.sql("SELECT COALESCE(SUM(r.clicks), 0)"
                + " FROM click_daily_rollup r" + owned("r")
                + " AND r.day >= :from AND r.day <= :to")
                .bind("username", username)
                .bind("from", fromInclusive)
                .bind("to", toInclusive)
                .map((row, meta) -> longOf(row.get(0)))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Long> activeLinks(String username) {
        return db.sql("SELECT COUNT(*) FROM urls u"
                + " JOIN users us ON u.user_id = us.id"
                + " WHERE us.username = :username AND u.is_active = true")
                .bind("username", username)
                .map((row, meta) -> longOf(row.get(0)))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Long> uniqueVisitors(String username, String code, LocalDateTime from) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT ce.ip)"
                + " FROM click_events ce" + owned("ce")
                + " AND ce.created_at >= :from");
        if (code != null) {
            sql.append(" AND ce.short_code = :code");
        }
        DatabaseClient.GenericExecuteSpec spec = db.sql(sql.toString())
                .bind("username", username)
                .bind("from", from);
        if (code != null) {
            spec = spec.bind("code", code);
        }
        return spec.map((row, meta) -> longOf(row.get(0))).one().defaultIfEmpty(0L);
    }

    @Override
    public Flux<TimeSeriesPoint> timeseries(String username, String code, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT r.day AS day, SUM(r.clicks) AS clicks"
                + " FROM click_daily_rollup r" + owned("r")
                + " AND r.day >= :from AND r.day <= :to");
        if (code != null) {
            sql.append(" AND r.short_code = :code");
        }
        sql.append(" GROUP BY r.day ORDER BY r.day");
        DatabaseClient.GenericExecuteSpec spec = db.sql(sql.toString())
                .bind("username", username)
                .bind("from", from)
                .bind("to", to);
        if (code != null) {
            spec = spec.bind("code", code);
        }
        return spec.map((row, meta) -> new TimeSeriesPoint(
                row.get("day", LocalDate.class),
                longOf(row.get("clicks")))).all();
    }

    @Override
    public Flux<TopItem> topLinks(String username, LocalDate from, int limit) {
        return db.sql("SELECT r.short_code AS short_code, u.title AS title, SUM(r.clicks) AS clicks"
                + " FROM click_daily_rollup r" + owned("r")
                + " AND r.day >= :from"
                + " GROUP BY r.short_code, u.title ORDER BY clicks DESC LIMIT :limit")
                .bind("username", username)
                .bind("from", from)
                .bind("limit", limit)
                .map((row, meta) -> {
                    String code = row.get("short_code", String.class);
                    String title = row.get("title", String.class);
                    return new TopItem(code, title != null ? title : code, longOf(row.get("clicks")));
                })
                .all();
    }

    @Override
    public Flux<TopItem> topReferers(String username, String code, LocalDateTime from, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(NULLIF(ce.referer, ''), '(direct)') AS referer, COUNT(*) AS clicks"
                + " FROM click_events ce" + owned("ce")
                + " AND ce.created_at >= :from");
        if (code != null) {
            sql.append(" AND ce.short_code = :code");
        }
        sql.append(" GROUP BY referer ORDER BY clicks DESC LIMIT :limit");
        DatabaseClient.GenericExecuteSpec spec = db.sql(sql.toString())
                .bind("username", username)
                .bind("from", from)
                .bind("limit", limit);
        if (code != null) {
            spec = spec.bind("code", code);
        }
        return spec.map((row, meta) -> {
            String referer = row.get("referer", String.class);
            return new TopItem(referer, referer, longOf(row.get("clicks")));
        }).all();
    }

    @Override
    public Mono<Boolean> ownsCode(String username, String shortCode) {
        return db.sql("SELECT EXISTS(SELECT 1 FROM urls u"
                + " JOIN users us ON u.user_id = us.id"
                + " WHERE us.username = :username AND u.short_code = :code)")
                .bind("username", username)
                .bind("code", shortCode)
                .map((row, meta) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<String> titleOf(String shortCode) {
        return db.sql("SELECT title FROM urls WHERE short_code = :code")
                .bind("code", shortCode)
                .map((row, meta) -> row.get("title", String.class))
                .one();
    }

    @Override
    public Mono<LocalDateTime> lastClickedAt(String shortCode) {
        return db.sql("SELECT MAX(created_at) FROM click_events WHERE short_code = :code")
                .bind("code", shortCode)
                .map((row, meta) -> row.get(0, LocalDateTime.class))
                .one();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String owned(String alias) {
        return String.format(OWNED, alias);
    }

    /** SUM/COUNT come back as Long or BigDecimal depending on driver — normalise. */
    private static long longOf(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }
}
