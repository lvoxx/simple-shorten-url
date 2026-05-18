package io.lvoxx.ssurl.kafka_starter.config;

import io.lvoxx.ssurl.common.util.Constants;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;

@AutoConfiguration
@ConditionalOnClass(KafkaAdmin.class)
public class KafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = Constants.Beans.ANALYTICS_EVENTS_TOPIC)
    public NewTopic analyticsEventsTopic() {
        return new NewTopic(Constants.Kafka.TOPIC_ANALYTICS_EVENTS,
                Constants.Kafka.PARTITIONS,
                Constants.Kafka.REPLICATION);
    }
}
