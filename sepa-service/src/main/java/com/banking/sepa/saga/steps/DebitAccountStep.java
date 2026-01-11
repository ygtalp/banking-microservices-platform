package com.banking.sepa.saga.steps;

import com.banking.sepa.client.AccountServiceClient;
import com.banking.sepa.model.SepaTransfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DebitAccountStep {

    private final AccountServiceClient accountServiceClient;

    public String execute(SepaTransfer transfer) {
        log.info("Debiting account for SEPA transfer: {}", transfer.getSepaReference());

        if (transfer.getDebtorAccountNumber() == null) {
            throw new RuntimeException("Debtor account number is required");
        }

        try {
            // Call Account Service to debit the amount
            String transactionId = accountServiceClient.debitAccount(
                transfer.getDebtorAccountNumber(),
                transfer.getAmount(),
                "SEPA Transfer: " + transfer.getSepaReference()
            );

            log.info("Account debited successfully, transaction ID: {}", transactionId);
            return transactionId;

        } catch (Exception e) {
            log.error("Failed to debit account for SEPA transfer: {}", transfer.getSepaReference(), e);
            throw new RuntimeException("Account debit failed: " + e.getMessage());
        }
    }

    public void compensate(SepaTransfer transfer) {
        log.warn("Compensating debit for SEPA transfer: {}", transfer.getSepaReference());

        if (transfer.getDebitTransactionId() == null) {
            log.info("No debit transaction to compensate");
            return;
        }

        try {
            // Call Account Service to credit back the amount
            accountServiceClient.creditAccount(
                transfer.getDebtorAccountNumber(),
                transfer.getAmount(),
                "SEPA Transfer Compensation: " + transfer.getSepaReference()
            );

            log.info("Debit compensated successfully for: {}", transfer.getSepaReference());

        } catch (Exception e) {
            log.error("Failed to compensate debit for SEPA transfer: {}", transfer.getSepaReference(), e);
            throw new RuntimeException("Debit compensation failed: " + e.getMessage());
        }
    }
}
