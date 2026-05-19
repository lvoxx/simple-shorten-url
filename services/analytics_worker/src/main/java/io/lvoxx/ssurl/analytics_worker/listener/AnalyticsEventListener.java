package io.lvoxx.ssurl.analytics_worker.listener;

import java.time.ZoneId;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.lvoxx.ssurl.analytics_worker.service.AnalyticsService;
import io.lvoxx.ssurl.avro.AnalyticsEvent;
import io.lvoxx.ssurl.common.model.Analytics;
import io.lvoxx.ssurl.common.util.Constants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AnalyticsEventListener {

    private final int maxBatchSize;
    private final AnalyticsService analyticsService;

    public AnalyticsEventListener(
            AnalyticsService analyticsService,
            @Value("${app.kafka.max-batch-size:500}") int maxBatchSize) {
        this.analyticsService = analyticsService;
        this.maxBatchSize = maxBatchSize;
    }

    @KafkaListener(
            topics = Constants.Kafka.TOPIC_ANALYTICS_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = Constants.Beans.KAFKA_LISTENER_CONTAINER_FACTORY
    )
    public void listen(List<ConsumerRecord<String, AnalyticsEvent>> records) {
        if (records.isEmpty()) return;

        List<Analytics> analyticsEvents = records.stream()
                .limit(maxBatchSize)
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
