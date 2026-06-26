package io.lvoxx.ssurl.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import io.lvoxx.ssurl.avro.AnalyticsEvent;
import io.lvoxx.ssurl.common.util.Constants;

/**
 * Batch listener container factory for the dashboard's own consumer group.
 * Mirrors analytics_worker — both groups independently read {@code analytics-events}.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean(name = Constants.Beans.KAFKA_LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, AnalyticsEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, AnalyticsEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, AnalyticsEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }
}
