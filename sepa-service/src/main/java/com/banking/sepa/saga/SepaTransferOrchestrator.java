package com.banking.sepa.saga;

import com.banking.sepa.model.SepaTransfer;
import com.banking.sepa.model.SepaTransfer.SepaTransferStatus;
import com.banking.sepa.repository.SepaTransferRepository;
import com.banking.sepa.saga.steps.ConfirmTransferStep;
import com.banking.sepa.saga.steps.DebitAccountStep;
import com.banking.sepa.saga.steps.SubmitToSepaNetworkStep;
import com.banking.sepa.saga.steps.ValidateSepaTransferStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SepaTransferOrchestrator {

    private final SepaTransferRepository transferRepository;
    private final ValidateSepaTransferStep validateStep;
    private final DebitAccountStep debitStep;
    private final SubmitToSepaNetworkStep submitStep;
    private final ConfirmTransferStep confirmStep;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public SepaTransfer executeTransfer(SepaTransfer transfer) {
        log.info("Starting SEPA transfer SAGA for: {}", transfer.getSepaReference());

        List<String> executedSteps = new ArrayList<>();

        try {
            // Step 1: Validate SEPA transfer
            transfer.setStatus(SepaTransferStatus.VALIDATING);
            transfer = transferRepository.save(transfer);

            validateStep.execute(transfer);
            executedSteps.add("VALIDATE");
            log.info("Step 1 completed: Validation successful for {}", transfer.getSepaReference());

            transfer.setStatus(SepaTransferStatus.VALIDATED);
            transfer = transferRepository.save(transfer);

            // Step 2: Debit account
            transfer.setStatus(SepaTransferStatus.DEBITING);
            transfer = transferRepository.save(transfer);

            String debitTxnId = debitStep.execute(transfer);
            transfer.setDebitTransactionId(debitTxnId);
            executedSteps.add("DEBIT");
            log.info("Step 2 completed: Account debited, txn: {}", debitTxnId);

            transfer.setStatus(SepaTransferStatus.DEBITED);
            transfer = transferRepository.save(transfer);

            // Step 3: Submit to SEPA network
            transfer.setStatus(SepaTransferStatus.SUBMITTING);
            transfer = transferRepository.save(transfer);

            String submissionId = submitStep.execute(transfer);
            transfer.setSubmissionId(submissionId);
            executedSteps.add("SUBMIT");
            log.info("Step 3 completed: Submitted to SEPA network, id: {}", submissionId);

            transfer.setStatus(SepaTransferStatus.SUBMITTED);
            transfer.setProcessedAt(LocalDateTime.now());
            transfer = transferRepository.save(transfer);

            // Step 4: Confirm transfer completion
            transfer.setStatus(SepaTransferStatus.COMPLETING);
            transfer = transferRepository.save(transfer);

            String confirmationMessage = confirmStep.execute(transfer);
            executedSteps.add("CONFIRM");
            log.info("Step 4 completed: Transfer confirmed, message: {}", confirmationMessage);

            // Transfer status is now COMPLETED (set by ConfirmTransferStep)
            transfer = transferRepository.findById(transfer.getSepaReference())
                    .orElseThrow(() -> new RuntimeException("Transfer not found after confirmation"));

            log.info("SEPA transfer SAGA completed successfully: {} (status: {})",
                    transfer.getSepaReference(), transfer.getStatus());
            return transfer;

        } catch (Exception e) {
            log.error("SEPA transfer SAGA failed at step: {}, reference: {}",
                     executedSteps.size() + 1, transfer.getSepaReference(), e);

            transfer = compensate(transfer, executedSteps, e.getMessage());
            publishTransferEvent(transfer, "sepa.transfer.failed");

            return transfer;
        }
    }

    @Transactional
    protected SepaTransfer compensate(SepaTransfer transfer, List<String> executedSteps, String errorMessage) {
        log.warn("Starting compensation for SEPA transfer: {}", transfer.getSepaReference());

        transfer.setStatus(SepaTransferStatus.COMPENSATING);
        transfer.setErrorMessage(errorMessage);
        transfer = transferRepository.save(transfer);

        // Compensate in reverse order
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            String step = executedSteps.get(i);
            try {
                switch (step) {
                    case "CONFIRM":
                        confirmStep.compensate(transfer);
                        log.info("Compensated CONFIRM step");
                        break;
                    case "SUBMIT":
                        log.info("Compensating SUBMIT step (no action needed)");
                        break;
                    case "DEBIT":
                        debitStep.compensate(transfer);
                        log.info("Compensated DEBIT step");
                        break;
                    case "VALIDATE":
                        log.info("Compensating VALIDATE step (no action needed)");
                        break;
                }
            } catch (Exception e) {
                log.error("Compensation failed for step: {}", step, e);
                transfer.setStatus(SepaTransferStatus.FAILED);
                transfer = transferRepository.save(transfer);
                return transfer;
            }
        }

        transfer.setStatus(SepaTransferStatus.COMPENSATED);
        transfer = transferRepository.save(transfer);

        log.info("Compensation completed for: {}", transfer.getSepaReference());
        return transfer;
    }

    private void publishTransferEvent(SepaTransfer transfer, String topic) {
        try {
            kafkaTemplate.send(topic, transfer.getSepaReference(), transfer);
            log.debug("Published event to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish event to topic: {}", topic, e);
        }
    }
}
