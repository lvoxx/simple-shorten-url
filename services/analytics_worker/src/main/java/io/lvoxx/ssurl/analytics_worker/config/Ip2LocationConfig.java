package io.lvoxx.ssurl.analytics_worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ip2location.IP2Location;

import io.lvoxx.ssurl.analytics_worker.properties.Ip2LocationProperties;

@Configuration
public class Ip2LocationConfig {

    @Bean
    public IP2Location ip2Location(Ip2LocationProperties props) throws Exception {
        IP2Location db = new IP2Location();
        db.Open(props.getBinPath());
        return db;
    }
}
