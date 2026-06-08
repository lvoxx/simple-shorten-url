package io.lvoxx.ssurl.api_service.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Tunable limits for {@code RateLimitWebFilter}. Bound from {@code app.rate-limit.*}.
 *
 * <p>
 * Two independent fixed-window buckets:
 * <ul>
 * <li><b>auth</b> – {@code POST /auth/login} and {@code /auth/register}
 * (credential stuffing / account-spam protection).</li>
 * <li><b>create</b> – {@code POST /urls} (public link-creation abuse).</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Master switch; disable in tests or trusted internal deployments. */
    private boolean enabled = true;

    /** Max auth requests per IP per {@link #authWindowSeconds}. */
    private int authLimit = 10;

    /** Auth window length in seconds. */
    private long authWindowSeconds = 60;

    /** Max URL-creation requests per IP per {@link #createWindowSeconds}. */
    private int createLimit = 30;

    /** URL-creation window length in seconds. */
    private long createWindowSeconds = 60;
}
