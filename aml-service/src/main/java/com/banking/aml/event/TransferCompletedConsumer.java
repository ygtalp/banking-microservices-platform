package com.banking.aml.event;

import com.banking.aml.service.TransactionMonitoringService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferCompletedConsumer {

    private final TransactionMonitoringService transactionMonitoringService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transfer.completed", groupId = "aml-service-group")
    public void handleTransferCompleted(String message) {
        try {
            log.info("Received transfer.completed event");

            JsonNode event = objectMapper.readTree(message);

            String transferReference = event.get("transferReference").asText();
            String fromAccount = event.get("fromAccountNumber").asText();
            BigDecimal amount = new BigDecimal(event.get("amount").asText());
            String currency = event.get("currency").asText();
            LocalDateTime transactionDate = LocalDateTime.parse(
                event.has("completedAt") ? event.get("completedAt").asText() : LocalDateTime.now().toString()
            );

            // Monitor the transaction
            transactionMonitoringService.monitorTransaction(
                fromAccount, transferReference, amount, currency, transactionDate
            );

            log.info("Transfer {} monitored for AML compliance", transferReference);

        } catch (Exception e) {
            log.error("Failed to process transfer.completed event", e);
        }
    }
}
