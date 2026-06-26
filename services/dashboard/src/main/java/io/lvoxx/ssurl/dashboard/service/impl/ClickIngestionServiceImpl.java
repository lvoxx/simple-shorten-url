package io.lvoxx.ssurl.dashboard.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.lvoxx.ssurl.dashboard.domain.ClickEvent;
import io.lvoxx.ssurl.dashboard.repository.ClickEventRepository;
import io.lvoxx.ssurl.dashboard.repository.DashboardQueryRepository;
import io.lvoxx.ssurl.dashboard.repository.DashboardQueryRepository.RollupKey;
import io.lvoxx.ssurl.dashboard.service.ClickIngestionService;
import reactor.core.publisher.Mono;

@Service
public class ClickIngestionServiceImpl implements ClickIngestionService {

    private final ClickEventRepository clickEventRepository;
    private final DashboardQueryRepository queryRepository;

    public ClickIngestionServiceImpl(ClickEventRepository clickEventRepository,
            DashboardQueryRepository queryRepository) {
        this.clickEventRepository = clickEventRepository;
        this.queryRepository = queryRepository;
    }

    @Override
    @Transactional
    public Mono<Void> persist(List<ClickEvent> events, Map<RollupKey, Long> increments) {
        return clickEventRepository.saveAll(events)
                .then(queryRepository.upsertDailyRollup(increments));
    }
}
