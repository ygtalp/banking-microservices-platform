package com.banking.transfer.saga;

import com.banking.transfer.client.AccountServiceClient;
import com.banking.transfer.dto.AccountBalanceResponse;
import com.banking.transfer.dto.ApiResponse;
import com.banking.transfer.exception.AccountNotFoundException;
import com.banking.transfer.exception.InsufficientBalanceException;
import com.banking.transfer.exception.InvalidTransferException;
import com.banking.transfer.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationStep implements SagaStep {

    private final AccountServiceClient accountServiceClient;

    @Override
    public boolean execute(Transfer transfer) {
        log.info("Executing VALIDATION step for transfer: {}", transfer.getTransferReference());

        try {
            // 1. Validate same account transfer
            if (transfer.getFromAccountNumber().equals(transfer.getToAccountNumber())) {
                transfer.setFailureReason("Cannot transfer to the same account");
                return false;
            }

            // 2. Validate FROM account exists and is active
            ApiResponse<AccountBalanceResponse> fromAccountResponse =
                    accountServiceClient.getAccountByNumber(transfer.getFromAccountNumber());

            if (!fromAccountResponse.isSuccess() || fromAccountResponse.getData() == null) {
                transfer.setFailureReason("Source account not found: " + transfer.getFromAccountNumber());
                return false;
            }

            AccountBalanceResponse fromAccount = fromAccountResponse.getData();

            if (!"ACTIVE".equals(fromAccount.getStatus())) {
                transfer.setFailureReason("Source account is not active");
                return false;
            }

            // 3. Validate TO account exists and is active
            ApiResponse<AccountBalanceResponse> toAccountResponse =
                    accountServiceClient.getAccountByNumber(transfer.getToAccountNumber());

            if (!toAccountResponse.isSuccess() || toAccountResponse.getData() == null) {
                transfer.setFailureReason("Destination account not found: " + transfer.getToAccountNumber());
                return false;
            }

            AccountBalanceResponse toAccount = toAccountResponse.getData();

            if (!"ACTIVE".equals(toAccount.getStatus())) {
                transfer.setFailureReason("Destination account is not active");
                return false;
            }

            // 4. Validate currency match
            if (!fromAccount.getCurrency().equals(transfer.getCurrency())) {
                transfer.setFailureReason("Currency mismatch - Source account: " +
                        fromAccount.getCurrency() + ", Transfer: " + transfer.getCurrency());
                return false;
            }

            if (!toAccount.getCurrency().equals(transfer.getCurrency())) {
                transfer.setFailureReason("Currency mismatch - Destination account: " +
                        toAccount.getCurrency() + ", Transfer: " + transfer.getCurrency());
                return false;
            }

            // 5. Validate sufficient balance
            if (fromAccount.getBalance().compareTo(transfer.getAmount()) < 0) {
                transfer.setFailureReason("Insufficient balance - Available: " +
                        fromAccount.getBalance() + ", Required: " + transfer.getAmount());
                return false;
            }

            // 6. Validate amount is positive
            if (transfer.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                transfer.setFailureReason("Transfer amount must be greater than zero");
                return false;
            }

            log.info("VALIDATION step successful for transfer: {}", transfer.getTransferReference());
            return true;

        } catch (Exception e) {
            log.error("VALIDATION step error: {}", e.getMessage(), e);
            transfer.setFailureReason("Validation error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean compensate(Transfer transfer) {
        // Validation step has no side effects, no compensation needed
        log.info("No compensation needed for VALIDATION step");
        return true;
    }

    @Override
    public String getStepName() {
        return "VALIDATION_STEP";
    }
}