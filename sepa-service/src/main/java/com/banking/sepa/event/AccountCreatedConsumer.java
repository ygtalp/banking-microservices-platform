package com.banking.sepa.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for account.created events.
 * Listens for new account creation to enable SEPA transfers for those accounts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountCreatedConsumer {

    private final ObjectMapper objectMapper;

    /**
     * Handles account.created events from Kafka.
     * This allows SEPA service to be aware of new accounts and validate account existence.
     *
     * @param message The event message
     */
    @KafkaListener(topics = "account.created", groupId = "sepa-service-group")
    public void handleAccountCreated(String message) {
        log.info("Received account.created event");

        try {
            JsonNode event = objectMapper.readTree(message);

            String accountNumber = event.get("accountNumber").asText();
            String iban = event.has("iban") ? event.get("iban").asText() : null;
            String customerName = event.has("customerName") ? event.get("customerName").asText() : null;
            String accountType = event.has("accountType") ? event.get("accountType").asText() : null;
            String currency = event.has("currency") ? event.get("currency").asText() : null;

            log.info("Account created - Account Number: {}, IBAN: {}, Customer: {}, Type: {}, Currency: {}",
                    accountNumber, iban, customerName, accountType, currency);

            // In a production system, we might:
            // 1. Cache account details for validation purposes
            // 2. Update local account registry
            // 3. Enable SEPA transfer capabilities for this account
            // 4. Send welcome notification about SEPA services

            // For now, we just log the event
            log.debug("SEPA service is now aware of account: {}", accountNumber);

        } catch (Exception e) {
            log.error("Error processing account.created event", e);
            // In production, implement proper error handling:
            // - Dead letter queue
            // - Retry mechanism
            // - Alert monitoring
        }
    }
}
