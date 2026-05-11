package io.lvoxx.ssurl.analytics_worker.service.impl;

import io.lvoxx.ssurl.analytics_worker.repository.AnalyticsRepository;
import io.lvoxx.ssurl.analytics_worker.service.AnalyticsService;
import io.lvoxx.ssurl.common.model.Analytics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Transactional
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsServiceImpl(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public Mono<Void> batchInsert(List<Analytics> events) {
        return analyticsRepository.saveAll(events).then();
    }
}
