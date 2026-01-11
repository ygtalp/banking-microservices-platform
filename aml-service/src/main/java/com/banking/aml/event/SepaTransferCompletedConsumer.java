package com.banking.aml.event;

import com.banking.aml.service.TransactionMonitoringService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class SepaTransferCompletedConsumer {

    private final TransactionMonitoringService transactionMonitoringService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "sepa.transfer.completed", groupId = "aml-service-group")
    public void handleSepaTransferCompleted(String message) {
        try {
            log.info("Received SEPA transfer completed event");

            JsonNode event = objectMapper.readTree(message);

            String sepaReference = event.get("sepaReference").asText();
            String debtorAccountNumber = event.has("debtorAccountNumber") ?
                    event.get("debtorAccountNumber").asText() : null;
            String debtorIban = event.get("debtorIban").asText();
            BigDecimal amount = new BigDecimal(event.get("amount").asText());
            String currency = event.get("currency").asText();

            // Monitor SEPA transfer for AML compliance
            if (debtorAccountNumber != null) {
                transactionMonitoringService.monitorTransaction(
                        debtorAccountNumber,
                        sepaReference,
                        amount,
                        currency
                );
                log.info("Monitored SEPA transfer: {} for account: {}",
                        sepaReference, debtorAccountNumber);
            } else {
                log.info("SEPA transfer {} from external IBAN {} - amount: {} {}",
                        sepaReference, debtorIban, amount, currency);
            }

        } catch (Exception e) {
            log.error("Error processing SEPA transfer completed event: {}", e.getMessage(), e);
        }
    }
}
