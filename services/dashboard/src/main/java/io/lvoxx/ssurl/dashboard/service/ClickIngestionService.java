package io.lvoxx.ssurl.dashboard.service;

import java.util.List;
import java.util.Map;

import io.lvoxx.ssurl.dashboard.domain.ClickEvent;
import io.lvoxx.ssurl.dashboard.repository.DashboardQueryRepository.RollupKey;
import reactor.core.publisher.Mono;

/** Transactional writer for the read model: raw rows + daily rollup, atomically. */
public interface ClickIngestionService {

    /**
     * Persist a consumed batch: insert raw {@code click_events} and increment the
     * matching {@code click_daily_rollup} buckets in a single transaction so the
     * rollup never drifts from the raw rows.
     */
    Mono<Void> persist(List<ClickEvent> events, Map<RollupKey, Long> increments);
}
