package io.lvoxx.ssurl.dashboard.dto.response;

/**
 * Real-time click notification pushed over the {@code /ws/dashboard} WebSocket.
 *
 * @param shortCode the code that was just clicked
 * @param clicks    running click count for the code today (from Redis)
 * @param timestamp epoch millis when the tick was emitted
 */
public record DashboardLiveTick(String shortCode, long clicks, long timestamp) {
}
