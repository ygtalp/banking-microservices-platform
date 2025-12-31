package com.banking.fraud.event;

import com.banking.fraud.dto.FraudCheckRequest;
import com.banking.fraud.dto.FraudCheckResponse;
import com.banking.fraud.model.FraudCheckStatus;
import com.banking.fraud.service.FraudDetectionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventConsumer {

    private final FraudDetectionService fraudDetectionService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transfer.completed", groupId = "fraud-detection-service")
    public void handleTransferCompletedEvent(String message) {
        try {
            log.info("Received transfer.completed event: {}", message);

            JsonNode event = objectMapper.readTree(message);

            String transferReference = event.get("transferReference").asText();
            String fromAccountNumber = event.get("fromAccountNumber").asText();
            String toAccountNumber = event.get("toAccountNumber").asText();
            BigDecimal amount = new BigDecimal(event.get("amount").asText());

            // Perform fraud check on source account (debit)
            FraudCheckRequest checkRequest = FraudCheckRequest.builder()
                    .transferReference(transferReference)
                    .accountNumber(fromAccountNumber)
                    .amount(amount)
                    .metadata("Auto-check for transfer: " + transferReference)
                    .build();

            FraudCheckResponse checkResponse = fraudDetectionService.performFraudCheck(checkRequest);

            log.info("Fraud check completed for transfer {}: status={}, riskLevel={}",
                    transferReference, checkResponse.getStatus(), checkResponse.getRiskLevel());

            // Publish fraud detection result
            if (checkResponse.getStatus() == FraudCheckStatus.BLOCKED) {
                publishFraudBlockedEvent(checkResponse);
            } else if (checkResponse.getStatus() == FraudCheckStatus.FLAGGED) {
                publishFraudDetectedEvent(checkResponse);
            }

        } catch (Exception e) {
            log.error("Error processing transfer.completed event", e);
        }
    }

    private void publishFraudDetectedEvent(FraudCheckResponse checkResponse) {
        try {
            String event = objectMapper.writeValueAsString(checkResponse);
            kafkaTemplate.send("fraud.detected", event);
            log.info("Published fraud.detected event for check: {}", checkResponse.getCheckId());
        } catch (Exception e) {
            log.error("Error publishing fraud.detected event", e);
        }
    }

    private void publishFraudBlockedEvent(FraudCheckResponse checkResponse) {
        try {
            String event = objectMapper.writeValueAsString(checkResponse);
            kafkaTemplate.send("fraud.blocked", event);
            log.warn("Published fraud.blocked event for check: {}", checkResponse.getCheckId());
        } catch (Exception e) {
            log.error("Error publishing fraud.blocked event", e);
        }
    }
}
