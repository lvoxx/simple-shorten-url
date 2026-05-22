package io.lvoxx.ssurl.analytics_worker.service;

import com.ip2location.IPResult;

import reactor.core.publisher.Mono;

public interface Ip2LocationService {

    /**
     * Looks up the IP address and returns the location information.
     *
     * @param ip the IP address to look up
     * @return a Mono containing the IP result
     */
    public Mono<IPResult> lookup(String ip);
}
