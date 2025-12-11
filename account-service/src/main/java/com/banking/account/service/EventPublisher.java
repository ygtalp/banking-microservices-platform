package com.banking.account.service;

import com.banking.account.config.KafkaConfig;
import com.banking.account.event.AccountCreatedEvent;
import com.banking.account.event.AccountStatusChangedEvent;
import com.banking.account.event.BalanceChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishAccountCreated(AccountCreatedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.ACCOUNT_CREATED_TOPIC, event.getAccountNumber(), eventJson);
            log.info("Published AccountCreatedEvent: {}", event.getAccountNumber());
        } catch (JsonProcessingException e) {
            log.error("Error publishing AccountCreatedEvent", e);
        }
    }

    public void publishBalanceChanged(BalanceChangedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.BALANCE_CHANGED_TOPIC, event.getAccountNumber(), eventJson);
            log.info("Published BalanceChangedEvent: {} - {}", event.getAccountNumber(), event.getOperation());
        } catch (JsonProcessingException e) {
            log.error("Error publishing BalanceChangedEvent", e);
        }
    }

    public void publishAccountStatusChanged(AccountStatusChangedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            String topic = event.getNewStatus().name().equals("FROZEN")
                    ? KafkaConfig.ACCOUNT_FROZEN_TOPIC
                    : KafkaConfig.ACCOUNT_UPDATED_TOPIC;
            kafkaTemplate.send(topic, event.getAccountNumber(), eventJson);
            log.info("Published AccountStatusChangedEvent: {} - {}", event.getAccountNumber(), event.getNewStatus());
        } catch (JsonProcessingException e) {
            log.error("Error publishing AccountStatusChangedEvent", e);
        }
    }
}