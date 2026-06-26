package io.lvoxx.ssurl.dashboard.listener;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.lvoxx.ssurl.avro.AnalyticsEvent;
import io.lvoxx.ssurl.common.util.Constants;
import io.lvoxx.ssurl.dashboard.domain.ClickEvent;
import io.lvoxx.ssurl.dashboard.dto.response.DashboardLiveTick;
import io.lvoxx.ssurl.dashboard.repository.DashboardQueryRepository.RollupKey;
import io.lvoxx.ssurl.dashboard.service.ClickIngestionService;
import io.lvoxx.ssurl.dashboard.service.DashboardBroadcaster;
import io.lvoxx.ssurl.dashboard.service.StatsCacheService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Read-model ingestion: batch-consumes {@code analytics-events}, persists raw
 * rows + daily rollup transactionally, then (best-effort, post-commit) updates
 * Redis live counters/HLL and broadcasts a live tick per click.
 */
@Slf4j
@Component
public class ClickEventListener {

    private final int maxBatchSize;
    private final ClickIngestionService ingestionService;
    private final StatsCacheService statsCache;
    private final DashboardBroadcaster broadcaster;

    public ClickEventListener(
            ClickIngestionService ingestionService,
            StatsCacheService statsCache,
            DashboardBroadcaster broadcaster,
            @Value("${app.kafka.max-batch-size:500}") int maxBatchSize) {
        this.ingestionService = ingestionService;
        this.statsCache = statsCache;
        this.broadcaster = broadcaster;
        this.maxBatchSize = maxBatchSize;
    }

    @KafkaListener(
            topics = Constants.Kafka.TOPIC_ANALYTICS_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = Constants.Beans.KAFKA_LISTENER_CONTAINER_FACTORY)
    public void listen(List<ConsumerRecord<String, AnalyticsEvent>> records) {
        if (records.isEmpty()) {
            return;
        }

        List<ClickEvent> events = records.stream()
                .limit(maxBatchSize)
                .map(this::toClickEvent)
                .toList();

        Map<RollupKey, Long> increments = events.stream()
                .collect(Collectors.groupingBy(
                        e -> new RollupKey(e.getShortCode(), e.getCreatedAt().toLocalDate()),
                        Collectors.counting()));

        ingestionService.persist(events, increments)
                .thenMany(Flux.fromIterable(events))
                .flatMap(e -> statsCache
                        .recordClick(e.getShortCode(), e.getIp(), e.getCreatedAt().toLocalDate())
                        .doOnNext(count -> broadcaster.publish(new DashboardLiveTick(
                                e.getShortCode(), count, System.currentTimeMillis()))))
                .doOnComplete(() -> log.debug("Ingested {} click events", events.size()))
                .doOnError(err -> log.error("Failed to ingest click-event batch", err))
                .subscribe();
    }

    private ClickEvent toClickEvent(ConsumerRecord<String, AnalyticsEvent> record) {
        AnalyticsEvent event = record.value();
        return ClickEvent.builder()
                .shortCode(event.getShortCode().toString())
                .ip(event.getIp() != null ? event.getIp().toString() : null)
                .userAgent(event.getUserAgent() != null ? event.getUserAgent().toString() : null)
                .referer(event.getReferer() != null ? event.getReferer().toString() : null)
                .createdAt(event.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }
}
