package io.lvoxx.ssurl.analytics_worker.config;

import io.lvoxx.ssurl.avro.AnalyticsEvent;
import io.lvoxx.ssurl.common.util.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

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
