package com.banking.transfer.saga;

import com.banking.transfer.client.AccountServiceClient;
import com.banking.transfer.dto.ApiResponse;
import com.banking.transfer.dto.TransactionRequest;
import com.banking.transfer.dto.TransactionResponse;
import com.banking.transfer.exception.TransferException;
import com.banking.transfer.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DebitStep implements SagaStep {

    private final AccountServiceClient accountServiceClient;

    @Override
    public boolean execute(Transfer transfer) {
        log.info("Executing DEBIT step for transfer: {}", transfer.getTransferReference());

        try {
            TransactionRequest debitRequest = TransactionRequest.builder()
                    .amount(transfer.getAmount())
                    .description("Transfer to " + transfer.getToAccountNumber() +
                            " - Ref: " + transfer.getTransferReference())
                    .referenceId(transfer.getTransferReference())
                    .build();

            ApiResponse<TransactionResponse> response = accountServiceClient.debitAccount(
                    transfer.getFromAccountNumber(),
                    debitRequest
            );

            if (response.isSuccess() && response.getData() != null) {
                transfer.setDebitTransactionId(response.getData().getTransactionId());
                log.info("DEBIT step successful - Transaction ID: {}",
                        response.getData().getTransactionId());
                return true;
            } else {
                log.error("DEBIT step failed: {}", response.getMessage());
                transfer.setFailureReason("Debit failed: " + response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("DEBIT step error for transfer {}: {}",
                    transfer.getTransferReference(), e.getMessage(), e);
            transfer.setFailureReason("Debit error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean compensate(Transfer transfer) {
        log.info("Compensating DEBIT step for transfer: {}", transfer.getTransferReference());

        if (transfer.getDebitTransactionId() == null) {
            log.info("No debit transaction to compensate");
            return true;
        }

        try {
            // Refund the debited amount
            TransactionRequest creditRequest = TransactionRequest.builder()
                    .amount(transfer.getAmount())
                    .description("Reversal - Failed transfer to " + transfer.getToAccountNumber() +
                            " - Ref: " + transfer.getTransferReference())
                    .referenceId(transfer.getTransferReference() + "-REVERSAL")
                    .build();

            ApiResponse<TransactionResponse> response = accountServiceClient.creditAccount(
                    transfer.getFromAccountNumber(),
                    creditRequest
            );

            if (response.isSuccess()) {
                log.info("DEBIT compensation successful");
                return true;
            } else {
                log.error("DEBIT compensation failed: {}", response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("DEBIT compensation error: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getStepName() {
        return "DEBIT_STEP";
    }
}