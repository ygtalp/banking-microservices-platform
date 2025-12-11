package com.banking.transfer.saga;

import com.banking.transfer.client.AccountServiceClient;
import com.banking.transfer.dto.ApiResponse;
import com.banking.transfer.dto.TransactionRequest;
import com.banking.transfer.dto.TransactionResponse;
import com.banking.transfer.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditStep implements SagaStep {

    private final AccountServiceClient accountServiceClient;

    @Override
    public boolean execute(Transfer transfer) {
        log.info("Executing CREDIT step for transfer: {}", transfer.getTransferReference());

        try {
            TransactionRequest creditRequest = TransactionRequest.builder()
                    .amount(transfer.getAmount())
                    .description("Transfer from " + transfer.getFromAccountNumber() +
                            " - Ref: " + transfer.getTransferReference())
                    .referenceId(transfer.getTransferReference())
                    .build();

            ApiResponse<TransactionResponse> response = accountServiceClient.creditAccount(
                    transfer.getToAccountNumber(),
                    creditRequest
            );

            if (response.isSuccess() && response.getData() != null) {
                transfer.setCreditTransactionId(response.getData().getTransactionId());
                log.info("CREDIT step successful - Transaction ID: {}",
                        response.getData().getTransactionId());
                return true;
            } else {
                log.error("CREDIT step failed: {}", response.getMessage());
                transfer.setFailureReason("Credit failed: " + response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("CREDIT step error for transfer {}: {}",
                    transfer.getTransferReference(), e.getMessage(), e);
            transfer.setFailureReason("Credit error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean compensate(Transfer transfer) {
        log.info("Compensating CREDIT step for transfer: {}", transfer.getTransferReference());

        if (transfer.getCreditTransactionId() == null) {
            log.info("No credit transaction to compensate");
            return true;
        }

        try {
            // Reverse the credit
            TransactionRequest debitRequest = TransactionRequest.builder()
                    .amount(transfer.getAmount())
                    .description("Reversal - Failed transfer from " + transfer.getFromAccountNumber() +
                            " - Ref: " + transfer.getTransferReference())
                    .referenceId(transfer.getTransferReference() + "-REVERSAL")
                    .build();

            ApiResponse<TransactionResponse> response = accountServiceClient.debitAccount(
                    transfer.getToAccountNumber(),
                    debitRequest
            );

            if (response.isSuccess()) {
                log.info("CREDIT compensation successful");
                return true;
            } else {
                log.error("CREDIT compensation failed: {}", response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("CREDIT compensation error: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getStepName() {
        return "CREDIT_STEP";
    }
}