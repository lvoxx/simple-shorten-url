package io.lvoxx.ssurl.dashboard.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Binds {@code app.jwt.secret}. The dashboard only <em>validates</em> access
 * tokens minted by api_service — it never issues them — so the signing secret
 * is all it needs (shared {@code JWT_SECRET}).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
}
