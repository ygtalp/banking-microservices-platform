package com.banking.sepa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sepa_returns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SepaReturn implements Serializable {

    @Id
    @Column(name = "return_id", length = 50)
    private String returnId;

    @Column(name = "return_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReturnType returnType; // RETURN, REJECTION, REFUND, RECALL

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReturnStatus status = ReturnStatus.INITIATED;

    // Original Transaction Reference
    @Column(name = "original_sepa_reference", nullable = false, length = 50)
    private String originalSepaReference;

    @Column(name = "original_end_to_end_id", length = 35)
    private String originalEndToEndId;

    @Column(name = "original_transaction_id", length = 35)
    private String originalTransactionId;

    // Return Details
    @Column(name = "return_reason_code", nullable = false, length = 10)
    private String returnReasonCode; // SEPA reason codes (e.g., AC01, AC04, MD07)

    @Column(name = "return_reason_description", columnDefinition = "TEXT")
    private String returnReasonDescription;

    @Column(name = "return_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal returnAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "EUR";

    // Parties Involved
    @Column(name = "debtor_name", length = 140)
    private String debtorName;

    @Column(name = "debtor_iban", length = 34)
    private String debtorIban;

    @Column(name = "creditor_name", length = 140)
    private String creditorName;

    @Column(name = "creditor_iban", length = 34)
    private String creditorIban;

    // Return Initiator
    @Column(name = "initiated_by", nullable = false, length = 100)
    private String initiatedBy; // DEBTOR_BANK, CREDITOR_BANK, DEBTOR, CREDITOR

    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    // Processing
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Refund Details (if return results in refund)
    @Column(name = "refund_sepa_reference", length = 50)
    private String refundSepaReference;

    @Column(name = "refund_transaction_id", length = 35)
    private String refundTransactionId;

    @Column(name = "refund_completed_at")
    private LocalDateTime refundCompletedAt;

    // Additional Information
    @Column(name = "additional_information", columnDefinition = "TEXT")
    private String additionalInformation;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    // Error Handling
    @Column(name = "has_errors", nullable = false)
    private Boolean hasErrors = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (returnId == null) {
            returnId = generateReturnId();
        }
        if (initiatedAt == null) {
            initiatedAt = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private String generateReturnId() {
        return "RET-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    public enum ReturnType {
        RETURN,      // Standard return of a payment
        REJECTION,   // Rejection before processing
        REFUND,      // Customer-requested refund
        RECALL,      // Originator recall request
        REVERSAL     // System reversal
    }

    public enum ReturnStatus {
        INITIATED,       // Return initiated
        VALIDATED,       // Return validated
        PROCESSING,      // Being processed
        COMPLETED,       // Return completed
        REFUNDED,        // Refund issued
        FAILED,          // Return failed
        CANCELLED        // Return cancelled
    }

    /**
     * SEPA Return Reason Codes (Common ones)
     */
    public static class ReasonCode {
        // Account Related
        public static final String AC01 = "AC01"; // IncorrectAccountNumber
        public static final String AC04 = "AC04"; // ClosedAccountNumber
        public static final String AC06 = "AC06"; // BlockedAccount

        // Amount Related
        public static final String AM04 = "AM04"; // InsufficientFunds
        public static final String AM05 = "AM05"; // Duplication

        // Customer Decision
        public static final String MD01 = "MD01"; // No Mandate
        public static final String MD02 = "MD02"; // MissingMandatoryInformation
        public static final String MD06 = "MD06"; // RefusedByCustomer
        public static final String MD07 = "MD07"; // EndCustomerDeceased

        // Technical
        public static final String MS02 = "MS02"; // NotSpecifiedReasonCustomerGenerated
        public static final String MS03 = "MS03"; // NotSpecifiedReasonAgentGenerated

        // Regulatory
        public static final String RR01 = "RR01"; // MissingDebtorAccountOrIdentification
        public static final String RR02 = "RR02"; // MissingDebtorNameOrAddress
        public static final String RR03 = "RR03"; // MissingCreditorNameOrAddress
        public static final String RR04 = "RR04"; // RegulatoryReason
    }
}
