package io.lvoxx.ssurl.dashboard.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lvoxx.ssurl.avro.AnalyticsEvent;
import io.lvoxx.ssurl.common.util.Constants;
import io.lvoxx.ssurl.dashboard.domain.ClickEvent;
import io.lvoxx.ssurl.dashboard.repository.DashboardQueryRepository.RollupKey;
import io.lvoxx.ssurl.dashboard.service.ClickIngestionService;
import io.lvoxx.ssurl.dashboard.service.DashboardBroadcaster;
import io.lvoxx.ssurl.dashboard.service.StatsCacheService;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClickEventListener Tests")
@Tag("Unit")
class ClickEventListenerTest {

    @Mock
    private ClickIngestionService ingestionService;
    @Mock
    private StatsCacheService statsCache;
    @Mock
    private DashboardBroadcaster broadcaster;

    private ConsumerRecord<String, AnalyticsEvent> record(String code, String ip) {
        AnalyticsEvent event = AnalyticsEvent.newBuilder()
                .setShortCode(code)
                .setIp(ip)
                .setUserAgent("UA")
                .setReferer("https://r.com")
                .setCreatedAt(Instant.now())
                .build();
        return new ConsumerRecord<>(Constants.Kafka.TOPIC_ANALYTICS_EVENTS, 0, 0L, code, event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listen_persistsRawAndRollup_thenBroadcastsPerClick() {
        ClickEventListener listener = new ClickEventListener(ingestionService, statsCache, broadcaster, 500);
        when(ingestionService.persist(anyList(), anyMap())).thenReturn(Mono.empty());
        when(statsCache.recordClick(any(), any(), any())).thenReturn(Mono.just(1L));

        listener.listen(List.of(record("a1", "1.1.1.1"), record("a1", "2.2.2.2"), record("b2", "3.3.3.3")));

        ArgumentCaptor<List<ClickEvent>> events = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<java.util.Map<RollupKey, Long>> increments = ArgumentCaptor.forClass(java.util.Map.class);
        verify(ingestionService).persist(events.capture(), increments.capture());

        org.assertj.core.api.Assertions.assertThat(events.getValue()).hasSize(3);
        // a1 → 2, b2 → 1 for today
        org.assertj.core.api.Assertions.assertThat(increments.getValue().values()).containsExactlyInAnyOrder(2L, 1L);
        verify(broadcaster, times(3)).publish(any());
    }

    @Test
    void listen_emptyBatch_isIgnored() {
        ClickEventListener listener = new ClickEventListener(ingestionService, statsCache, broadcaster, 500);
        listener.listen(List.of());
        verify(ingestionService, times(0)).persist(anyList(), anyMap());
    }
}
