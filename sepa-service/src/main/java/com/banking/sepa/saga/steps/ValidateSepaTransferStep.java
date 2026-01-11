package com.banking.sepa.saga.steps;

import com.banking.sepa.model.SepaTransfer;
import com.banking.sepa.service.IbanValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateSepaTransferStep {

    private final IbanValidationService ibanValidationService;

    public void execute(SepaTransfer transfer) {
        log.info("Validating SEPA transfer: {}", transfer.getSepaReference());

        // Validate debtor IBAN
        if (!ibanValidationService.isValidIban(transfer.getDebtorIban())) {
            throw new RuntimeException("Invalid debtor IBAN: " + transfer.getDebtorIban());
        }

        // Validate creditor IBAN
        if (!ibanValidationService.isValidIban(transfer.getCreditorIban())) {
            throw new RuntimeException("Invalid creditor IBAN: " + transfer.getCreditorIban());
        }

        // Validate amount
        if (transfer.getAmount() == null || transfer.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid amount: " + transfer.getAmount());
        }

        // Validate amount limits based on transfer type
        validateAmountLimits(transfer);

        // Validate currency (must be EUR for SEPA)
        if (!"EUR".equals(transfer.getCurrency())) {
            throw new RuntimeException("Invalid currency for SEPA: " + transfer.getCurrency());
        }

        log.info("SEPA transfer validation successful: {}", transfer.getSepaReference());
    }

    private void validateAmountLimits(SepaTransfer transfer) {
        BigDecimal maxAmount;

        switch (transfer.getTransferType()) {
            case SCT_INST:
                maxAmount = new BigDecimal("100000"); // €100,000 for instant
                if (transfer.getAmount().compareTo(maxAmount) > 0) {
                    throw new RuntimeException("Amount exceeds SEPA Instant limit: " + maxAmount);
                }
                break;
            case SCT:
                maxAmount = new BigDecimal("999999999.99");
                if (transfer.getAmount().compareTo(maxAmount) > 0) {
                    throw new RuntimeException("Amount exceeds SEPA Credit Transfer limit");
                }
                break;
            case SDD_CORE:
            case SDD_B2B:
                // Direct debit limits are usually set by mandate
                break;
        }
    }
}
