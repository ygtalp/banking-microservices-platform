package com.banking.fraud.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic fraudDetectedTopic() {
        return TopicBuilder.name("fraud.detected")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudBlockedTopic() {
        return TopicBuilder.name("fraud.blocked")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
