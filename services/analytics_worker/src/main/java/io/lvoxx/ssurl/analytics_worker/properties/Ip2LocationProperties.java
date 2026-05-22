package io.lvoxx.ssurl.analytics_worker.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "ip2location")
public class Ip2LocationProperties {
    private String binPath;
}