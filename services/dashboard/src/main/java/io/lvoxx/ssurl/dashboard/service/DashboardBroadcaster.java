package io.lvoxx.ssurl.dashboard.service;

import org.springframework.stereotype.Component;

import io.lvoxx.ssurl.dashboard.dto.response.DashboardLiveTick;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * In-memory fan-out hub for live click ticks. The Kafka ingestion path
 * {@link #publish}es a tick per affected code; the WebSocket handler subscribes
 * to {@link #stream()} and forwards ticks to connected clients.
 *
 * <p>
 * Uses a multicast sink with {@code onBackpressureBuffer} so multiple WebSocket
 * sessions can subscribe independently; slow/disconnected consumers are dropped
 * without blocking the producer.
 */
@Component
public class DashboardBroadcaster {

    private final Sinks.Many<DashboardLiveTick> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publish(DashboardLiveTick tick) {
        // FAIL_FAST is fine here: ticks are best-effort, never retried.
        sink.tryEmitNext(tick);
    }

    public Flux<DashboardLiveTick> stream() {
        return sink.asFlux();
    }
}
