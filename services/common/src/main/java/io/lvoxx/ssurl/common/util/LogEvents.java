package io.lvoxx.ssurl.common.util;

import org.slf4j.Marker;

import net.logstash.logback.marker.Markers;

public final class LogEvents {

    private LogEvents() {
    }

    // Auth
    public static final String AUTH_LOGIN_SUCCESS = "User login successful";
    public static final String AUTH_LOGIN_FAILED = "User login failed";
    public static final String AUTH_REGISTER_SUCCESS = "User registration successful";
    public static final String AUTH_REGISTER_FAILED = "User registration failed";
    public static final String AUTH_TOKEN_REFRESH = "Token refreshed";
    public static final String AUTH_LOGOUT = "User logged out";
    public static final String AUTH_TOKEN_EXPIRED = "Access token expired";

    // URL
    public static final String URL_CREATED = "Short URL created";
    public static final String URL_DELETED = "Short URL deleted";
    public static final String URL_EXPIRED = "Short URL expired";
    public static final String URL_ACCESSED = "Short URL accessed";

    // Redirect
    public static final String REDIRECT_RESOLVED = "Redirect resolved successfully";
    public static final String REDIRECT_NOT_FOUND = "Redirect target not found";
    public static final String REDIRECT_CACHE_HIT = "Redirect cache hit";
    public static final String REDIRECT_CACHE_MISS = "Redirect cache miss";

    // Domain blacklist
    public static final String DOMAIN_BLACKLISTED = "Domain is blacklisted";

    // Rate limit
    public static final String RATE_LIMIT_EXCEEDED = "Rate limit exceeded";

    // Analytics
    public static final String ANALYTICS_EVENT_PRODUCED = "Analytics event produced";
    public static final String ANALYTICS_BATCH_INSERTED = "Analytics batch inserted";

    // System
    public static final String SYS_STARTED = "Application started";
    public static final String SYS_STOPPED = "Application stopped";
    public static final String SYS_ERROR = "Unexpected system error";
    public static final String SYS_CONFIG_LOADED = "Configuration loaded";

    public static Marker marker(String eventName) {
        return Markers.append("event", eventName);
    }
}
