package com.banking.transfer.service;

import com.banking.transfer.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransferInitiated(Transfer transfer) {
        String topic = "transfer.initiated";
        Map<String, Object> event = buildTransferEvent(transfer);

        kafkaTemplate.send(topic, transfer.getTransferReference(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Transfer initiated event published: {}", transfer.getTransferReference());
                    } else {
                        log.error("Failed to publish transfer initiated event: {}", ex.getMessage());
                    }
                });
    }

    public void publishTransferCompleted(Transfer transfer) {
        String topic = "transfer.completed";
        Map<String, Object> event = buildTransferEvent(transfer);

        kafkaTemplate.send(topic, transfer.getTransferReference(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Transfer completed event published: {}", transfer.getTransferReference());
                    } else {
                        log.error("Failed to publish transfer completed event: {}", ex.getMessage());
                    }
                });
    }

    public void publishTransferFailed(Transfer transfer) {
        String topic = "transfer.failed";
        Map<String, Object> event = buildTransferEvent(transfer);
        event.put("failureReason", transfer.getFailureReason());

        kafkaTemplate.send(topic, transfer.getTransferReference(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Transfer failed event published: {}", transfer.getTransferReference());
                    } else {
                        log.error("Failed to publish transfer failed event: {}", ex.getMessage());
                    }
                });
    }

    public void publishTransferCompensated(Transfer transfer) {
        String topic = "transfer.compensated";
        Map<String, Object> event = buildTransferEvent(transfer);
        event.put("failureReason", transfer.getFailureReason());

        kafkaTemplate.send(topic, transfer.getTransferReference(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Transfer compensated event published: {}", transfer.getTransferReference());
                    } else {
                        log.error("Failed to publish transfer compensated event: {}", ex.getMessage());
                    }
                });
    }

    private Map<String, Object> buildTransferEvent(Transfer transfer) {
        Map<String, Object> event = new HashMap<>();
        event.put("transferReference", transfer.getTransferReference());
        event.put("fromAccountNumber", transfer.getFromAccountNumber());
        event.put("toAccountNumber", transfer.getToAccountNumber());
        event.put("amount", transfer.getAmount());
        event.put("currency", transfer.getCurrency());
        event.put("status", transfer.getStatus().name());
        event.put("transferType", transfer.getTransferType().name());
        event.put("description", transfer.getDescription());
        event.put("initiatedAt", transfer.getInitiatedAt());
        event.put("completedAt", transfer.getCompletedAt());
        event.put("debitTransactionId", transfer.getDebitTransactionId());
        event.put("creditTransactionId", transfer.getCreditTransactionId());
        return event;
    }
}