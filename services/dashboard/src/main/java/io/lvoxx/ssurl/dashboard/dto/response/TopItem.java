package io.lvoxx.ssurl.dashboard.dto.response;

/**
 * A ranked entry in a "top N" list. {@code key} is the stable identifier
 * (short code or raw referer); {@code label} is the human-friendly display
 * (URL title, or the referer itself).
 */
public record TopItem(String key, String label, long clicks) {
}
