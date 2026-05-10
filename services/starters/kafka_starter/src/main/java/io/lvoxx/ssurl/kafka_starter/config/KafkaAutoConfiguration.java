package io.lvoxx.ssurl.kafka_starter.config;

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
    @ConditionalOnMissingBean(name = "analyticsEventsTopic")
    public NewTopic analyticsEventsTopic() {
        return new NewTopic("analytics-events", 3, (short) 1);
    }
}
