package io.lvoxx.ssurl.analytics_worker.service.impl;

import java.io.IOException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.ip2location.IP2Location;
import com.ip2location.IPResult;

import io.lvoxx.ssurl.analytics_worker.service.Ip2LocationService;
import io.lvoxx.ssurl.common.exception.IP2LocationFileReadException;
import lombok.Data;
import reactor.core.publisher.Mono;

@Data
@Service
public class Ip2LocationServiceImpl implements Ip2LocationService {
    private final IP2Location ip2Location;

    public Ip2LocationServiceImpl(IP2Location ip2Location) {
        this.ip2Location = ip2Location;
    }

    @Cacheable(value = "ip-geo", key = "#ip", unless = "#result == null")
    @Override
    public Mono<IPResult> lookup(String ip) {
        try {
            return Mono.just(ip2Location.IPQuery(ip));
        } catch (IOException e) {
            throw new IP2LocationFileReadException();
        }
    }

}
