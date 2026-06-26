package io.lvoxx.ssurl.dashboard.dto.response;

/**
 * Headline metrics for the authenticated user's links over the selected range.
 *
 * @param totalClicks    total clicks across the user's links within the range
 * @param activeLinks    number of the user's links currently active
 * @param clicksToday    clicks recorded today (served from Redis live counters)
 * @param uniqueVisitors approximate distinct visitors (Redis HyperLogLog)
 * @param trendPct       percent change vs. the immediately preceding window
 */
public record DashboardOverview(
        long totalClicks,
        long activeLinks,
        long clicksToday,
        long uniqueVisitors,
        double trendPct) {
}
