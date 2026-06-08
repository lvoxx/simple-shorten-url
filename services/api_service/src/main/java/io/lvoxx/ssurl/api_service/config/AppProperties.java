package io.lvoxx.ssurl.api_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String shortUrlBase = "http://localhost:8081";
    private Jwt jwt = new Jwt();

    /**
     * Whether auth cookies (the refresh token) are marked {@code Secure}.
     * Defaults to {@code true} so production never ships tokens over plaintext
     * HTTP; local HTTP dev can override via {@code app.secure-cookies=false}.
     */
    private boolean secureCookies = true;

    public record Jwt(String secret, long accessExpiry, long refreshExpiry) {
        public Jwt() {
            this("", 900L, 604800L);
        }
    }

    public String getShortUrlBase() {
        return shortUrlBase;
    }

    public void setShortUrlBase(String shortUrlBase) {
        this.shortUrlBase = shortUrlBase;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public boolean isSecureCookies() {
        return secureCookies;
    }

    public void setSecureCookies(boolean secureCookies) {
        this.secureCookies = secureCookies;
    }
}
