package com.banking.account.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String ACCOUNT_CREATED_TOPIC = "account.created";
    public static final String ACCOUNT_UPDATED_TOPIC = "account.updated";
    public static final String ACCOUNT_FROZEN_TOPIC = "account.frozen";
    public static final String BALANCE_CHANGED_TOPIC = "account.balance.changed";

    // Topics for consuming transfer events
    public static final String TRANSFER_DEBIT_TOPIC = "transfer.debit";
    public static final String TRANSFER_CREDIT_TOPIC = "transfer.credit";

    @Bean
    public NewTopic accountCreatedTopic() {
        return TopicBuilder.name(ACCOUNT_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic accountUpdatedTopic() {
        return TopicBuilder.name(ACCOUNT_UPDATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic accountFrozenTopic() {
        return TopicBuilder.name(ACCOUNT_FROZEN_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic balanceChangedTopic() {
        return TopicBuilder.name(BALANCE_CHANGED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}