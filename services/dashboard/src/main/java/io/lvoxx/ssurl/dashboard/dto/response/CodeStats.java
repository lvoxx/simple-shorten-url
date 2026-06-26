package io.lvoxx.ssurl.dashboard.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Per-link detail view: totals, uniques, last activity, daily series and top referers. */
public record CodeStats(
        String shortCode,
        String title,
        long totalClicks,
        long uniqueVisitors,
        LocalDateTime lastClickedAt,
        List<TimeSeriesPoint> series,
        List<TopItem> topReferers) {
}
