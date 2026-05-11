package io.lvoxx.ssurl.analytics_worker.repository;

import io.lvoxx.ssurl.common.model.Analytics;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AnalyticsRepository extends ReactiveCrudRepository<Analytics, Long> {

    Flux<Analytics> findByShortCode(String shortCode);
}
