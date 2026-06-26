package io.lvoxx.ssurl.common.util;

import java.time.Duration;

public final class Constants {

    private Constants() {
    }

    // ─────────────────────────────────────────────────────────────────
    // Cache & Bloom Filter
    // ─────────────────────────────────────────────────────────────────
    public static final class Cache {
        public static final String SHORT_URLS = "short-urls";
        public static final String KEY_PREFIX_SHORT = "short:";
        public static final int TTL_HOURS = 24;
        public static final String BLOOM_FILTER = "bloom:urls";
        public static final int BLOOM_CAPACITY = 10_000_000;
        public static final double BLOOM_FPR = 0.01;

        // -------------------------------------------------------------------------
        // Cache region names (must match spring.cache.cache-names in application.yml)
        // -------------------------------------------------------------------------

        /**
         * Spring Cache region for
         * {@link io.lvoxx.ssurl.common.dto.response.UserResponse} objects.
         */
        public static final String CACHE_USERS = "users";

        /**
         * Spring Cache region for full
         * {@link io.lvoxx.ssurl.common.dto.response.UrlResponse} objects.
         */
        public static final String CACHE_URLS = "urls";

        /** Spring Cache region for the hot redirect path: shortCode → originalUrl. */
        public static final String CACHE_SHORT_CODES = "shortCodes";

        /** Spring Cache region that tracks live refresh-token presence flags. */
        public static final String CACHE_REFRESH_TOKENS = "refreshTokens";

        // -------------------------------------------------------------------------
        // Cache key templates
        // -------------------------------------------------------------------------

        /** {@code user:id:<id>} */
        public static final String KEY_USER_BY_ID = "user:id:";

        /** {@code user:name:<username>} */
        public static final String KEY_USER_BY_NAME = "user:name:";

        /** {@code url:<shortCode>} – raw redirect target */
        public static final String KEY_URL_BY_SHORT = "url:";

        /** {@code url:id:<id>} – full UrlResponse */
        public static final String KEY_URL_BY_ID = "url:id:";

        /** {@code auth:rt:<token>} – refresh-token presence flag */
        public static final String KEY_AUTH_RT = "auth:rt:";
    }

    public static final class TTL {
        // -------------------------------------------------------------------------
        // Default TTLs
        // -------------------------------------------------------------------------

        public static final Duration TTL_USER = Duration.ofMinutes(30);
        public static final Duration TTL_URL = Duration.ofMinutes(10);
        public static final Duration TTL_SHORT_CODE = Duration.ofHours(24);
        public static final Duration TTL_REFRESH_TOKEN = Duration.ofDays(30);
    }

    // ─────────────────────────────────────────────────────────────────
    // Redis Key Prefixes
    // ─────────────────────────────────────────────────────────────────
    public static final class Redis {
        public static final String RATE_LIMIT_IP = "rate_limit:ip:";
        public static final String RATE_LIMIT_USER = "rate_limit:user:";
        public static final String BLACKLIST_DOMAIN = "blacklist:domain:";
    }

    // ─────────────────────────────────────────────────────────────────
    // Kafka
    // ─────────────────────────────────────────────────────────────────
    public static final class Kafka {
        public static final String TOPIC_ANALYTICS_EVENTS = "analytics-events";
        public static final int PARTITIONS = 3;
        public static final short REPLICATION = 1;
        public static final int MAX_BATCH_SIZE = 500;
    }

    // ─────────────────────────────────────────────────────────────────
    // HTTP Headers & Cookies
    // ─────────────────────────────────────────────────────────────────
    public static final class Headers {
        public static final String X_FORWARDED_FOR = "X-Forwarded-For";
        public static final String X_REFRESH_TOKEN = "X-Refresh-Token";
        public static final String LOCATION = "Location";
        public static final String AUTHORIZATION_PREFIX = "Bearer ";
        public static final String COOKIE_REFRESH_TOKEN = "refreshToken";
    }

    // ─────────────────────────────────────────────────────────────────
    // API Paths
    // ─────────────────────────────────────────────────────────────────
    public static final class ApiPaths {
        public static final String AUTH = "/api/v1/auth";
        public static final String USERS = "/api/v1/users";
        public static final String URLS = "/api/v1/urls";
        public static final String DASHBOARD = "/api/v1/dashboard";
    }

    // ─────────────────────────────────────────────────────────────────
    // JWT / Auth
    // ─────────────────────────────────────────────────────────────────
    public static final class Jwt {
        public static final String CLAIM_ROLE = "role";
        public static final String CLAIM_TYPE = "type";
        public static final String TYPE_ACCESS = "access";
        public static final String TYPE_REFRESH = "refresh";
        public static final String TOKEN_TYPE = "Bearer";
        public static final String ROLE_PREFIX = "ROLE_";
    }

    // ─────────────────────────────────────────────────────────────────
    // Default Values
    // ─────────────────────────────────────────────────────────────────
    public static final class Defaults {
        public static final String ROLE = "USER";
        public static final String CREATED_BY = "Annonymous";
        public static final String ANONYMOUS_USER = "anonymous";
        public static final String UNKNOWN_IP = "unknown";
    }

    // ─────────────────────────────────────────────────────────────────
    // Bean Names
    // ─────────────────────────────────────────────────────────────────
    public static final class Beans {
        public static final String URL_BLOOM_FILTER = "urlBloomFilter";
        public static final String ANALYTICS_EVENTS_TOPIC = "analyticsEventsTopic";
        public static final String KAFKA_LISTENER_CONTAINER_FACTORY = "kafkaListenerContainerFactory";
        public static final String BEARER_AUTH = "bearerAuth";
        public static final String BASE62 = "base62";
    }

    // ─────────────────────────────────────────────────────────────────
    // Business Constants
    // ─────────────────────────────────────────────────────────────────
    public static final class Business {
        public static final int ANONYMOUS_EXPIRY_DAYS = 7;
        public static final int MAX_PAGE_SIZE = 100;
        public static final long ACCESS_TOKEN_EXPIRY_SEC = 900L;
        public static final long REFRESH_TOKEN_EXPIRY_SEC = 604800L;
    }

    // ─────────────────────────────────────────────────────────────────
    // Regex Patterns
    // ─────────────────────────────────────────────────────────────────
    public static final class Patterns {
        public static final String SHORT_CODE = "[a-zA-Z0-9]+";
        public static final String WWW_PREFIX = "www.";
    }

    // ─────────────────────────────────────────────────────────────────
    // DB Table Names
    // ─────────────────────────────────────────────────────────────────
    public static final class Tables {
        public static final String USERS = "users";
        public static final String URLS = "urls";
        public static final String ANALYTICS = "analytics";
        public static final String REFRESH_TOKENS = "refresh_tokens";
        public static final String DOMAIN_BLACKLIST = "domain_blacklist";
    }

    // ─────────────────────────────────────────────────────────────────
    // DB Column Names
    // ─────────────────────────────────────────────────────────────────
    public static final class Columns {
        public static final String SHORT_CODE = "short_code";
        public static final String ORIGINAL_URL = "original_url";
        public static final String USER_ID = "user_id";
        public static final String IS_ACTIVE = "is_active";
        public static final String CLICK_COUNT = "click_count";
        public static final String EXPIRE_AT = "expire_at";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
        public static final String CREATED_BY = "created_by";
        public static final String UPDATED_BY = "updated_by";
        public static final String PASSWORD_HASH = "password_hash";
        public static final String USER_AGENT = "user_agent";
        public static final String EXPIRES_AT = "expires_at";
    }

    // ─────────────────────────────────────────────────────────────────
    // i18n Message Keys & Bundles
    // ─────────────────────────────────────────────────────────────────
    public static final class Messages {
        public static final String SHORTCODE_NOT_FOUND = "error.shortcode.notfound";
        public static final String SHORTCODE_EXPIRED = "error.shortcode.expired";
        public static final String DOMAIN_BLACKLISTED = "error.domain.blacklisted";
        public static final String RATELIMIT_EXCEEDED = "error.ratelimit.exceeded";
        public static final String USER_NOT_FOUND = "error.user.notfound";
        public static final String USER_EXISTS = "error.user.exists";
        public static final String UNAUTHORIZED = "error.unauthorized";
        public static final String URL_NOT_FOUND = "error.url.notfound";
        public static final String BUNDLE_ERRORS = "messages/errors";
        public static final String BUNDLE_COMMON = "messages/common";
        public static final String ENCODING = "UTF-8";
    }

    // ─────────────────────────────────────────────────────────────────
    // Log Marker Keys (top-level, referenced by LogIds / LogEvents / Logs)
    // ─────────────────────────────────────────────────────────────────
    public static final String LOG_MARKER_EVENT_ID = "eventId";
    public static final String LOG_MARKER_EVENT = "event";
}
