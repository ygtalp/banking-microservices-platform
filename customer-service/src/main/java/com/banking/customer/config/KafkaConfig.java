package com.banking.customer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    private static final String CUSTOMER_EVENTS_TOPIC = "customer.events";

    @Bean
    public NewTopic customerEventsTopic() {
        return TopicBuilder.name(CUSTOMER_EVENTS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
