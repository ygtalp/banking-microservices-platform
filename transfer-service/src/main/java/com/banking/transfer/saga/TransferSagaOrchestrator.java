package com.banking.transfer.saga;

import com.banking.transfer.exception.SagaCompensationException;
import com.banking.transfer.model.Transfer;
import com.banking.transfer.model.TransferStatus;
import com.banking.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferSagaOrchestrator {

    private final ValidationStep validationStep;
    private final DebitStep debitStep;
    private final CreditStep creditStep;
    private final TransferRepository transferRepository;

    @Transactional
    public Transfer executeTransfer(Transfer transfer) {
        log.info("Starting SAGA orchestration for transfer: {}", transfer.getTransferReference());

        List<SagaStep> executedSteps = new ArrayList<>();
        transfer.setInitiatedAt(LocalDateTime.now());

        try {
            // Step 1: Validation
            transfer.setStatus(TransferStatus.VALIDATING);
            transferRepository.save(transfer);

            if (!validationStep.execute(transfer)) {
                log.error("Validation failed: {}", transfer.getFailureReason());
                transfer.setStatus(TransferStatus.FAILED);
                transferRepository.save(transfer);
                return transfer;
            }
            executedSteps.add(validationStep);

            // Step 2: Debit from source account
            transfer.setStatus(TransferStatus.DEBIT_PENDING);
            transferRepository.save(transfer);

            if (!debitStep.execute(transfer)) {
                log.error("Debit step failed: {}", transfer.getFailureReason());
                compensate(executedSteps, transfer);
                return transfer;
            }
            executedSteps.add(debitStep);

            transfer.setStatus(TransferStatus.DEBIT_COMPLETED);
            transferRepository.save(transfer);

            // Step 3: Credit to destination account
            transfer.setStatus(TransferStatus.CREDIT_PENDING);
            transferRepository.save(transfer);

            if (!creditStep.execute(transfer)) {
                log.error("Credit step failed: {}", transfer.getFailureReason());
                compensate(executedSteps, transfer);
                return transfer;
            }
            executedSteps.add(creditStep);

            // Success!
            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setCompletedAt(LocalDateTime.now());
            transferRepository.save(transfer);

            log.info("SAGA orchestration completed successfully for transfer: {}",
                    transfer.getTransferReference());
            return transfer;

        } catch (Exception e) {
            log.error("Unexpected error during SAGA execution: {}", e.getMessage(), e);
            transfer.setFailureReason("Unexpected error: " + e.getMessage());
            compensate(executedSteps, transfer);
            return transfer;
        }
    }

    private void compensate(List<SagaStep> executedSteps, Transfer transfer) {
        log.warn("Starting compensation for transfer: {}", transfer.getTransferReference());

        transfer.setStatus(TransferStatus.COMPENSATING);
        transferRepository.save(transfer);

        // Reverse the steps in reverse order
        Collections.reverse(executedSteps);

        boolean compensationSuccessful = true;

        for (SagaStep step : executedSteps) {
            try {
                log.info("Compensating step: {}", step.getStepName());
                if (!step.compensate(transfer)) {
                    log.error("Compensation failed for step: {}", step.getStepName());
                    compensationSuccessful = false;
                }
            } catch (Exception e) {
                log.error("Compensation error for step {}: {}", step.getStepName(), e.getMessage(), e);
                compensationSuccessful = false;
            }
        }

        if (compensationSuccessful) {
            transfer.setStatus(TransferStatus.COMPENSATED);
            log.info("Compensation completed successfully for transfer: {}",
                    transfer.getTransferReference());
        } else {
            transfer.setStatus(TransferStatus.FAILED);
            String errorMsg = "Compensation partially failed - Manual intervention required for transfer: " +
                    transfer.getTransferReference();
            log.error(errorMsg);
            transfer.setFailureReason(transfer.getFailureReason() + " | " + errorMsg);
        }

        transferRepository.save(transfer);
    }
}