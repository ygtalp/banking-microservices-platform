package com.banking.sepa.saga.steps;

import com.banking.sepa.model.SepaTransfer;
import com.banking.sepa.repository.SepaTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Final step in SEPA transfer SAGA: Confirm transfer completion and publish events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfirmTransferStep {

    private final SepaTransferRepository transferRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Executes the confirm step: Updates transfer status to COMPLETED and publishes event.
     *
     * @param transfer The SEPA transfer
     * @return Confirmation message
     */
    public String execute(SepaTransfer transfer) {
        log.info("Confirming SEPA transfer completion: {}", transfer.getSepaReference());

        try {
            // Update transfer status
            transfer.setStatus(SepaTransfer.TransferStatus.COMPLETED);
            transfer.setCompletedAt(LocalDateTime.now());
            transfer.setHasErrors(false);

            // Save transfer
            transferRepository.save(transfer);

            // Publish transfer completed event
            publishTransferCompletedEvent(transfer);

            log.info("SEPA transfer confirmed and completed: {}", transfer.getSepaReference());
            return "Transfer confirmed successfully";

        } catch (Exception e) {
            log.error("Failed to confirm SEPA transfer: {}", transfer.getSepaReference(), e);
            throw new RuntimeException("Transfer confirmation failed: " + e.getMessage());
        }
    }

    /**
     * Compensates the confirm step: Marks transfer as failed.
     *
     * @param transfer The SEPA transfer
     */
    public void compensate(SepaTransfer transfer) {
        log.warn("Compensating confirm step for SEPA transfer: {}", transfer.getSepaReference());

        try {
            // Update transfer status to FAILED
            transfer.setStatus(SepaTransfer.TransferStatus.FAILED);
            transfer.setHasErrors(true);
            transfer.setErrorMessage("Transfer failed during confirmation");

            // Save transfer
            transferRepository.save(transfer);

            // Publish transfer failed event
            publishTransferFailedEvent(transfer);

            log.info("Confirm step compensated for: {}", transfer.getSepaReference());

        } catch (Exception e) {
            log.error("Failed to compensate confirm step for SEPA transfer: {}", transfer.getSepaReference(), e);
            throw new RuntimeException("Confirm compensation failed: " + e.getMessage());
        }
    }

    /**
     * Publishes SEPA transfer completed event to Kafka.
     */
    private void publishTransferCompletedEvent(SepaTransfer transfer) {
        Map<String, Object> event = new HashMap<>();
        event.put("sepaReference", transfer.getSepaReference());
        event.put("transferType", transfer.getTransferType().name());
        event.put("debtorIban", transfer.getDebtorIban());
        event.put("creditorIban", transfer.getCreditorIban());
        event.put("amount", transfer.getAmount());
        event.put("currency", transfer.getCurrency());
        event.put("debtorAccountNumber", transfer.getDebtorAccountNumber());
        event.put("endToEndId", transfer.getEndToEndId());
        event.put("completedAt", transfer.getCompletedAt());
        event.put("timestamp", LocalDateTime.now());

        kafkaTemplate.send("sepa.transfer.completed", event);
        log.debug("Published SEPA transfer completed event: {}", transfer.getSepaReference());
    }

    /**
     * Publishes SEPA transfer failed event to Kafka.
     */
    private void publishTransferFailedEvent(SepaTransfer transfer) {
        Map<String, Object> event = new HashMap<>();
        event.put("sepaReference", transfer.getSepaReference());
        event.put("transferType", transfer.getTransferType().name());
        event.put("debtorIban", transfer.getDebtorIban());
        event.put("creditorIban", transfer.getCreditorIban());
        event.put("amount", transfer.getAmount());
        event.put("currency", transfer.getCurrency());
        event.put("errorMessage", transfer.getErrorMessage());
        event.put("timestamp", LocalDateTime.now());

        kafkaTemplate.send("sepa.transfer.failed", event);
        log.debug("Published SEPA transfer failed event: {}", transfer.getSepaReference());
    }
}
