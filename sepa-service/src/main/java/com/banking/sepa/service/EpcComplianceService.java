package com.banking.sepa.service;

import com.banking.sepa.model.SepaTransfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for validating SEPA transactions against EPC (European Payments Council) rules.
 * Ensures compliance with SEPA Credit Transfer (SCT) and SEPA Instant Credit Transfer (SCT Inst) schemes.
 */
@Service
@Slf4j
public class EpcComplianceService {

    // EPC Character Set: a-z, A-Z, 0-9, and specific special characters
    private static final Pattern EPC_CHAR_SET = Pattern.compile("^[a-zA-Z0-9 .,\\-()/:?'+]*$");

    // Amount limits
    private static final BigDecimal SCT_MAX_AMOUNT = new BigDecimal("999999999.99"); // No practical limit
    private static final BigDecimal SCT_INST_MAX_AMOUNT = new BigDecimal("100000.00"); // €100,000 for instant
    private static final BigDecimal SCT_INST_MIN_AMOUNT = new BigDecimal("0.01"); // Minimum €0.01

    // Text field length limits (EPC Rulebook)
    private static final int MAX_NAME_LENGTH = 140;
    private static final int MAX_ADDRESS_LENGTH = 70;
    private static final int MAX_REMITTANCE_INFO_LENGTH = 140;
    private static final int MAX_END_TO_END_ID_LENGTH = 35;
    private static final int MAX_PURPOSE_CODE_LENGTH = 4;

    // Valid purpose codes (ISO 20022 - subset commonly used in SEPA)
    private static final List<String> VALID_PURPOSE_CODES = Arrays.asList(
        "CBFF", // Capital Building - Collective Payment of Building Contribution
        "CDCD", // Credit Card Payment
        "CDQC", // Cash Disbursement/Withdrawal From An ATM
        "CHAR", // Charity Payment
        "COMC", // Commercial Payment
        "CPKC", // Car Park Charges
        "DIVD", // Dividend Payment
        "GOVI", // Government Insurance
        "GRLT", // Grain
        "INST", // Instalment/Hire-Purchase Agreement
        "INTE", // Interest
        "LBRI", // Labor Insurance
        "LOAN", // Loan
        "OTHR", // Other Payment
        "PENS", // Pension Payment
        "SALA", // Salary Payment
        "SECU", // Securities
        "SSBE", // Social Security Benefit
        "SUPP", // Supplier Payment
        "TAXS", // Tax Payment
        "TRAD", // Trade
        "TREA", // Treasury Payment
        "VATX", // Value Added Tax Payment
        "WHLD"  // With Holding
    );

    /**
     * Validates a SEPA transfer against EPC rules.
     *
     * @param transfer The SEPA transfer to validate
     * @return ComplianceResult containing validation status and list of violations
     */
    public ComplianceResult validate(SepaTransfer transfer) {
        List<String> violations = new ArrayList<>();

        // Validate amount limits
        if (transfer.getTransferType() == SepaTransfer.TransferType.SCT_INST) {
            validateSctInstAmount(transfer.getAmount(), violations);
        } else {
            validateSctAmount(transfer.getAmount(), violations);
        }

        // Validate character sets
        validateCharacterSet("Debtor Name", transfer.getDebtorName(), violations);
        validateCharacterSet("Creditor Name", transfer.getCreditorName(), violations);
        validateCharacterSet("Remittance Information", transfer.getRemittanceInformation(), violations);

        if (transfer.getDebtorAddress() != null) {
            validateCharacterSet("Debtor Address", transfer.getDebtorAddress(), violations);
        }
        if (transfer.getCreditorAddress() != null) {
            validateCharacterSet("Creditor Address", transfer.getCreditorAddress(), violations);
        }

        // Validate text field lengths
        validateLength("Debtor Name", transfer.getDebtorName(), MAX_NAME_LENGTH, violations);
        validateLength("Creditor Name", transfer.getCreditorName(), MAX_NAME_LENGTH, violations);
        validateLength("Remittance Information", transfer.getRemittanceInformation(), MAX_REMITTANCE_INFO_LENGTH, violations);
        validateLength("End-to-End ID", transfer.getEndToEndId(), MAX_END_TO_END_ID_LENGTH, violations);

        if (transfer.getDebtorAddress() != null) {
            validateLength("Debtor Address", transfer.getDebtorAddress(), MAX_ADDRESS_LENGTH, violations);
        }
        if (transfer.getCreditorAddress() != null) {
            validateLength("Creditor Address", transfer.getCreditorAddress(), MAX_ADDRESS_LENGTH, violations);
        }

        // Validate purpose code if present
        if (transfer.getPurposeCode() != null && !transfer.getPurposeCode().isEmpty()) {
            validatePurposeCode(transfer.getPurposeCode(), violations);
        }

        // SCT Inst specific validations
        if (transfer.getTransferType() == SepaTransfer.TransferType.SCT_INST) {
            validateSctInstSpecific(transfer, violations);
        }

        if (violations.isEmpty()) {
            log.debug("EPC compliance validation passed for transfer: {}", transfer.getSepaReference());
            return ComplianceResult.compliant();
        } else {
            log.warn("EPC compliance violations found for transfer {}: {}", transfer.getSepaReference(), violations);
            return ComplianceResult.nonCompliant(violations);
        }
    }

    /**
     * Validates SCT (standard) amount limits.
     */
    private void validateSctAmount(BigDecimal amount, List<String> violations) {
        if (amount == null) {
            violations.add("Amount is required");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            violations.add("Amount must be positive");
        }

        if (amount.compareTo(SCT_MAX_AMOUNT) > 0) {
            violations.add("Amount exceeds maximum for SCT: €999,999,999.99");
        }

        // Check for more than 2 decimal places
        if (amount.scale() > 2) {
            violations.add("Amount can have maximum 2 decimal places");
        }
    }

    /**
     * Validates SCT Inst (instant) amount limits.
     */
    private void validateSctInstAmount(BigDecimal amount, List<String> violations) {
        if (amount == null) {
            violations.add("Amount is required");
            return;
        }

        if (amount.compareTo(SCT_INST_MIN_AMOUNT) < 0) {
            violations.add("SCT Inst amount must be at least €0.01");
        }

        if (amount.compareTo(SCT_INST_MAX_AMOUNT) > 0) {
            violations.add("SCT Inst amount exceeds maximum: €100,000.00");
        }

        // Check for more than 2 decimal places
        if (amount.scale() > 2) {
            violations.add("Amount can have maximum 2 decimal places");
        }
    }

    /**
     * Validates character set compliance (EPC allowed characters).
     */
    private void validateCharacterSet(String fieldName, String value, List<String> violations) {
        if (value == null || value.isEmpty()) {
            return; // Handled by required field validation
        }

        if (!EPC_CHAR_SET.matcher(value).matches()) {
            violations.add(fieldName + " contains invalid characters. Only a-z, A-Z, 0-9, space, . , - ( ) / : ? ' + are allowed");
        }
    }

    /**
     * Validates text field length compliance.
     */
    private void validateLength(String fieldName, String value, int maxLength, List<String> violations) {
        if (value == null) {
            return;
        }

        if (value.length() > maxLength) {
            violations.add(fieldName + " exceeds maximum length of " + maxLength + " characters (actual: " + value.length() + ")");
        }
    }

    /**
     * Validates purpose code against allowed list.
     */
    private void validatePurposeCode(String purposeCode, List<String> violations) {
        if (purposeCode.length() > MAX_PURPOSE_CODE_LENGTH) {
            violations.add("Purpose code exceeds maximum length of " + MAX_PURPOSE_CODE_LENGTH + " characters");
        }

        if (!VALID_PURPOSE_CODES.contains(purposeCode.toUpperCase())) {
            violations.add("Invalid purpose code: " + purposeCode + ". Must be one of: " + VALID_PURPOSE_CODES);
        }
    }

    /**
     * SCT Inst specific validations.
     */
    private void validateSctInstSpecific(SepaTransfer transfer, List<String> violations) {
        // SCT Inst requires immediate execution
        if (transfer.getRequestedExecutionDate() != null) {
            violations.add("SCT Inst does not support requested execution date (must be immediate)");
        }

        // Currency must be EUR for SCT Inst
        if (!transfer.getCurrency().equals("EUR")) {
            violations.add("SCT Inst only supports EUR currency");
        }
    }

    /**
     * Checks if an amount is compliant for SCT Inst.
     */
    public boolean isAmountValidForSctInst(BigDecimal amount) {
        return amount != null &&
               amount.compareTo(SCT_INST_MIN_AMOUNT) >= 0 &&
               amount.compareTo(SCT_INST_MAX_AMOUNT) <= 0 &&
               amount.scale() <= 2;
    }

    /**
     * Checks if a text field uses only EPC-compliant characters.
     */
    public boolean isCharacterSetCompliant(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        return EPC_CHAR_SET.matcher(text).matches();
    }

    /**
     * Compliance result class.
     */
    public static class ComplianceResult {
        private final boolean compliant;
        private final List<String> violations;

        private ComplianceResult(boolean compliant, List<String> violations) {
            this.compliant = compliant;
            this.violations = violations != null ? violations : new ArrayList<>();
        }

        public static ComplianceResult compliant() {
            return new ComplianceResult(true, new ArrayList<>());
        }

        public static ComplianceResult nonCompliant(List<String> violations) {
            return new ComplianceResult(false, violations);
        }

        public boolean isCompliant() {
            return compliant;
        }

        public List<String> getViolations() {
            return new ArrayList<>(violations);
        }

        public String getViolationSummary() {
            return String.join("; ", violations);
        }
    }
}
