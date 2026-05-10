package io.lvoxx.ssurl.analytics_worker.listener;

import io.lvoxx.ssurl.analytics_worker.service.AnalyticsService;
import io.lvoxx.ssurl.avro.AnalyticsEvent;
import io.lvoxx.ssurl.common.domain.Analytics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Component
public class AnalyticsEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventListener.class);
    private static final int MAX_BATCH_SIZE = 500;

    private final AnalyticsService analyticsService;

    public AnalyticsEventListener(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @KafkaListener(
            topics = "analytics-events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(List<ConsumerRecord<String, AnalyticsEvent>> records) {
        if (records.isEmpty()) return;

        List<Analytics> analyticsEvents = records.stream()
                .limit(MAX_BATCH_SIZE)
                .map(this::toAnalytics)
                .toList();

        analyticsService.batchInsert(analyticsEvents)
                .doOnSuccess(v -> log.debug("Batch inserted {} analytics events", analyticsEvents.size()))
                .doOnError(e -> log.error("Failed to insert analytics batch", e))
                .subscribe();
    }

    private Analytics toAnalytics(ConsumerRecord<String, AnalyticsEvent> record) {
        AnalyticsEvent event = record.value();
        Analytics a = new Analytics();
        a.setShortCode(event.getShortCode().toString());
        a.setIp(event.getIp() != null ? event.getIp().toString() : null);
        a.setUserAgent(event.getUserAgent() != null ? event.getUserAgent().toString() : null);
        a.setReferer(event.getReferer() != null ? event.getReferer().toString() : null);
        a.setCreatedAt(
                event.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
        );
        return a;
    }
}
