package io.lvoxx.ssurl.dashboard.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import io.lvoxx.ssurl.dashboard.domain.ClickEvent;
import reactor.core.publisher.Flux;

/**
 * Reactive CRUD over the raw {@code click_events} read model. Used by the Kafka
 * ingestion path ({@code saveAll}) and per-code drill-down queries. Aggregations
 * go through {@link DashboardQueryRepository} (rollup table) — not full scans here.
 */
public interface ClickEventRepository extends ReactiveCrudRepository<ClickEvent, Long> {

    Flux<ClickEvent> findTop20ByShortCodeOrderByCreatedAtDesc(String shortCode);
}
