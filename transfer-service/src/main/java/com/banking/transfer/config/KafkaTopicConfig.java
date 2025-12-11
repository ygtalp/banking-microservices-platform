package com.banking.transfer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transferInitiatedTopic() {
        return TopicBuilder.name("transfer.initiated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transferCompletedTopic() {
        return TopicBuilder.name("transfer.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transferFailedTopic() {
        return TopicBuilder.name("transfer.failed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transferCompensatedTopic() {
        return TopicBuilder.name("transfer.compensated")
                .partitions(3)
                .replicas(1)
                .build();
    }
}