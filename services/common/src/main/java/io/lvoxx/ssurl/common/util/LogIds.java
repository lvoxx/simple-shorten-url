package io.lvoxx.ssurl.common.util;

import org.slf4j.Marker;

import net.logstash.logback.marker.Markers;

public final class LogIds {

    private LogIds() {
    }

    // Auth
    public static final String AUTH_LOGIN_SUCCESS = "AUTH-001";
    public static final String AUTH_LOGIN_FAILED = "AUTH-002";
    public static final String AUTH_REGISTER_SUCCESS = "AUTH-003";
    public static final String AUTH_REGISTER_FAILED = "AUTH-004";
    public static final String AUTH_TOKEN_REFRESH = "AUTH-005";
    public static final String AUTH_LOGOUT = "AUTH-006";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH-007";

    // URL
    public static final String URL_CREATED = "URL-001";
    public static final String URL_DELETED = "URL-002";
    public static final String URL_EXPIRED = "URL-003";
    public static final String URL_ACCESSED = "URL-004";

    // Redirect
    public static final String REDIRECT_RESOLVED = "REDIRECT-001";
    public static final String REDIRECT_NOT_FOUND = "REDIRECT-002";
    public static final String REDIRECT_CACHE_HIT = "REDIRECT-003";
    public static final String REDIRECT_CACHE_MISS = "REDIRECT-004";

    // Domain blacklist
    public static final String DOMAIN_BLACKLISTED = "DOMAIN-001";

    // Rate limit
    public static final String RATE_LIMIT_EXCEEDED = "RATE-001";

    // Analytics
    public static final String ANALYTICS_EVENT_PRODUCED = "ANALYTICS-001";
    public static final String ANALYTICS_BATCH_INSERTED = "ANALYTICS-002";

    // System
    public static final String SYS_STARTED = "SYS-001";
    public static final String SYS_STOPPED = "SYS-002";
    public static final String SYS_ERROR = "SYS-003";
    public static final String SYS_CONFIG_LOADED = "SYS-004";

    public static Marker marker(String eventId) {
        return Markers.append("eventId", eventId);
    }
}
