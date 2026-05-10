package io.lvoxx.ssurl.api_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String shortUrlBase = "http://localhost:8081";
    private Jwt jwt = new Jwt();

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
}
