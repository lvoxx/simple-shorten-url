package io.lvoxx.ssurl.dashboard.util;

/** Maps human range tokens ({@code 24h}, {@code 7d}, {@code 30d}, {@code 90d}) to day counts. */
public final class Ranges {

    public static final int DEFAULT_DAYS = 7;
    public static final int MAX_DAYS = 365;

    private Ranges() {
    }

    /** Parse a range token to an inclusive day count; unknown/blank → {@link #DEFAULT_DAYS}. */
    public static int toDays(String range) {
        if (range == null || range.isBlank()) {
            return DEFAULT_DAYS;
        }
        return switch (range.trim().toLowerCase()) {
            case "24h", "1d" -> 1;
            case "7d" -> 7;
            case "14d" -> 14;
            case "30d" -> 30;
            case "90d" -> 90;
            default -> DEFAULT_DAYS;
        };
    }

    /** Clamp a requested top-N size to a sane bound. */
    public static int clampLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 50);
    }
}
