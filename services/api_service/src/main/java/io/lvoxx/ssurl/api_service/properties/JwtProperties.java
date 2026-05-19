package io.lvoxx.ssurl.api_service.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private Long accessExpiry;
    private Long refreshExpiry;
    private String secret;
}
