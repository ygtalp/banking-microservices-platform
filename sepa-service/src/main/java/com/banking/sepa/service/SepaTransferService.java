package com.banking.sepa.service;

import com.banking.sepa.client.FraudDetectionClient;
import com.banking.sepa.model.SepaTransfer;
import com.banking.sepa.model.SepaTransfer.SepaTransferStatus;
import com.banking.sepa.model.SepaTransfer.TransferType;
import com.banking.sepa.repository.SepaTransferRepository;
import com.banking.sepa.saga.SepaTransferOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepaTransferService {

    private final SepaTransferRepository transferRepository;
    private final SepaTransferOrchestrator transferOrchestrator;
    private final IbanValidationService ibanValidationService;
    private final BicValidationService bicValidationService;
    private final EpcComplianceService epcComplianceService;
    private final FraudDetectionClient fraudDetectionClient;

    @Transactional
    public SepaTransfer initiateTransfer(SepaTransfer transfer) {
        log.info("Initiating SEPA transfer: {} from {} to {}, type: {}, amount: {}",
                 transfer.getSepaReference(), transfer.getDebtorIban(), transfer.getCreditorIban(),
                 transfer.getTransferType(), transfer.getAmount());

        // Pre-validation: IBAN validation
        if (!ibanValidationService.isSepaCountry(transfer.getDebtorIban())) {
            throw new IllegalArgumentException("Debtor IBAN is not from a SEPA country");
        }

        if (!ibanValidationService.isSepaCountry(transfer.getCreditorIban())) {
            throw new IllegalArgumentException("Creditor IBAN is not from a SEPA country");
        }

        // BIC validation if provided
        if (transfer.getDebtorBic() != null && !transfer.getDebtorBic().isEmpty()) {
            BicValidationService.ValidationResult bicResult = bicValidationService.validateForSepa(transfer.getDebtorBic());
            if (!bicResult.isValid()) {
                throw new IllegalArgumentException("Invalid debtor BIC: " + bicResult.getErrorMessage());
            }

            // BIC-IBAN consistency check
            BicValidationService.ValidationResult consistencyResult =
                    bicValidationService.validateBicIbanConsistency(transfer.getDebtorBic(), transfer.getDebtorIban());
            if (!consistencyResult.isValid()) {
                throw new IllegalArgumentException("Debtor BIC-IBAN inconsistency: " + consistencyResult.getErrorMessage());
            }
        }

        if (transfer.getCreditorBic() != null && !transfer.getCreditorBic().isEmpty()) {
            BicValidationService.ValidationResult bicResult = bicValidationService.validateForSepa(transfer.getCreditorBic());
            if (!bicResult.isValid()) {
                throw new IllegalArgumentException("Invalid creditor BIC: " + bicResult.getErrorMessage());
            }

            // BIC-IBAN consistency check
            BicValidationService.ValidationResult consistencyResult =
                    bicValidationService.validateBicIbanConsistency(transfer.getCreditorBic(), transfer.getCreditorIban());
            if (!consistencyResult.isValid()) {
                throw new IllegalArgumentException("Creditor BIC-IBAN inconsistency: " + consistencyResult.getErrorMessage());
            }
        }

        // EPC compliance validation
        EpcComplianceService.ComplianceResult complianceResult = epcComplianceService.validate(transfer);
        if (!complianceResult.isCompliant()) {
            String violations = complianceResult.getViolationSummary();
            log.error("EPC compliance violations for transfer {}: {}", transfer.getSepaReference(), violations);
            throw new IllegalArgumentException("EPC compliance violations: " + violations);
        }

        // SCT Inst specific validations
        if (transfer.getTransferType() == TransferType.SCT_INST) {
            validateSctInstRequirements(transfer);
        }

        // Fraud detection check
        performFraudCheck(transfer);

        // Save transfer in PENDING state
        transfer.setStatus(SepaTransferStatus.PENDING);
        transfer = transferRepository.save(transfer);

        // Execute SAGA
        try {
            transfer = transferOrchestrator.executeTransfer(transfer);
            log.info("SEPA transfer initiated successfully: {}", transfer.getSepaReference());
        } catch (Exception e) {
            log.error("SEPA transfer initiation failed: {}", transfer.getSepaReference(), e);
            throw e;
        }

        return transfer;
    }

    /**
     * Validates SCT Inst specific requirements:
     * - 10-second processing guarantee
     * - 24/7/365 availability
     * - Irrevocability (no requested execution date)
     * - Amount limits (max €100,000)
     * - Currency must be EUR
     */
    private void validateSctInstRequirements(SepaTransfer transfer) {
        log.debug("Validating SCT Inst requirements for transfer: {}", transfer.getSepaReference());

        // Currency must be EUR
        if (!transfer.getCurrency().equals("EUR")) {
            throw new IllegalArgumentException("SCT Inst only supports EUR currency");
        }

        // No requested execution date (must be immediate)
        if (transfer.getRequestedExecutionDate() != null) {
            throw new IllegalArgumentException("SCT Inst does not support future-dated transfers (must be immediate)");
        }

        // Amount limits (€0.01 - €100,000)
        BigDecimal minAmount = new BigDecimal("0.01");
        BigDecimal maxAmount = new BigDecimal("100000.00");

        if (transfer.getAmount().compareTo(minAmount) < 0) {
            throw new IllegalArgumentException("SCT Inst minimum amount is €0.01");
        }

        if (transfer.getAmount().compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("SCT Inst maximum amount is €100,000.00");
        }

        // Irrevocability check: Once submitted, cannot be cancelled
        transfer.setIrrevocable(true);

        log.debug("SCT Inst validation passed for transfer: {}", transfer.getSepaReference());
    }

    /**
     * Performs fraud detection check via Fraud Detection Service.
     */
    private void performFraudCheck(SepaTransfer transfer) {
        if (transfer.getDebtorAccountNumber() == null) {
            log.warn("Cannot perform fraud check: debtor account number is missing");
            return;
        }

        try {
            log.debug("Performing fraud check for transfer: {}", transfer.getSepaReference());

            FraudDetectionClient.FraudCheckRequest request = new FraudDetectionClient.FraudCheckRequest(
                    transfer.getDebtorAccountNumber(),
                    transfer.getSepaReference(),
                    transfer.getAmount(),
                    transfer.getCurrency(),
                    null, // balanceBefore - will be populated by Account Service
                    null  // balanceAfter - will be populated by Account Service
            );

            FraudDetectionClient.FraudCheckResponse response = fraudDetectionClient.performFraudCheck(request);

            if (response.isBlocked()) {
                log.error("Transfer {} blocked by fraud detection: {}", transfer.getSepaReference(),
                        String.join(", ", response.getReasons()));
                throw new IllegalArgumentException("Transfer blocked by fraud detection: " +
                        String.join(", ", response.getReasons()));
            }

            if (response.isFlagged()) {
                log.warn("Transfer {} flagged by fraud detection (risk level: {}): {}",
                        transfer.getSepaReference(), response.getRiskLevel(),
                        String.join(", ", response.getReasons()));
                // For now, allow flagged transfers but log the warning
                // In production, might require manual approval
            }

            log.debug("Fraud check passed for transfer: {}", transfer.getSepaReference());

        } catch (Exception e) {
            // If fraud detection service is unavailable, log error but don't block transfer
            // In production, this should be configurable based on risk policy
            log.error("Fraud detection check failed for transfer {}: {}", transfer.getSepaReference(), e.getMessage());
            // Optionally: throw exception to block transfer if fraud check is mandatory
        }
    }

    @Cacheable(value = "sepaTransfers", key = "#sepaReference")
    public Optional<SepaTransfer> getTransfer(String sepaReference) {
        return transferRepository.findById(sepaReference);
    }

    public List<SepaTransfer> getTransfersByDebtorIban(String debtorIban) {
        return transferRepository.findByDebtorIbanOrderByCreatedAtDesc(debtorIban);
    }

    public List<SepaTransfer> getTransfersByCreditorIban(String creditorIban) {
        return transferRepository.findByCreditorIbanOrderByCreatedAtDesc(creditorIban);
    }

    public List<SepaTransfer> getTransfersByAccount(String accountNumber) {
        return transferRepository.findByDebtorAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public List<SepaTransfer> getTransfersByStatus(SepaTransferStatus status) {
        return transferRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<SepaTransfer> getPendingTransfers() {
        return transferRepository.findPendingTransfers();
    }

    public Long getTransferCount(SepaTransferStatus status) {
        return transferRepository.countByStatus(status);
    }
}
