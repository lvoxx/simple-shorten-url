package io.lvoxx.ssurl.analytics_worker.service;

import io.lvoxx.ssurl.common.domain.Analytics;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AnalyticsService {

    /**
     * Batch-inserts a list of analytics events into the database.
     *
     * @param events the list of analytics events to persist
     * @return a Mono that completes when all events have been saved
     */
    Mono<Void> batchInsert(List<Analytics> events);
}
