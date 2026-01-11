package com.banking.sepa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sepa_mandates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SepaMandate implements Serializable {

    @Id
    @Column(name = "mandate_id", length = 50)
    private String mandateId; // Unique Mandate Reference (UMR)

    @Column(name = "mandate_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MandateType mandateType; // SDD_CORE, SDD_B2B

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MandateStatus status = MandateStatus.PENDING;

    // Debtor Information (Customer who authorizes debit)
    @Column(name = "debtor_name", nullable = false, length = 140)
    private String debtorName;

    @Column(name = "debtor_iban", nullable = false, length = 34)
    private String debtorIban;

    @Column(name = "debtor_bic", length = 11)
    private String debtorBic;

    @Column(name = "debtor_account_number", length = 50)
    private String debtorAccountNumber; // Internal account reference

    @Column(name = "debtor_address", length = 200)
    private String debtorAddress;

    @Column(name = "debtor_country", length = 2)
    private String debtorCountry; // ISO country code

    // Creditor Information (Merchant/Company collecting payment)
    @Column(name = "creditor_name", nullable = false, length = 140)
    private String creditorName;

    @Column(name = "creditor_id", length = 35)
    private String creditorId; // Creditor Identifier (mandatory for SDD)

    @Column(name = "creditor_iban", nullable = false, length = 34)
    private String creditorIban;

    @Column(name = "creditor_bic", length = 11)
    private String creditorBic;

    // Mandate Details
    @Column(name = "sequence_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SequenceType sequenceType = SequenceType.OOFF; // FRST, RCUR, FNAL, OOFF

    @Column(name = "amendment_indicator", nullable = false)
    private Boolean amendmentIndicator = false;

    @Column(name = "original_mandate_id", length = 50)
    private String originalMandateId; // For amendments

    @Column(name = "original_creditor_id", length = 35)
    private String originalCreditorId; // For amendments

    // Authorization
    @Column(name = "signature_date", nullable = false)
    private LocalDate signatureDate;

    @Column(name = "signed_by", length = 100)
    private String signedBy;

    @Column(name = "signature_method", length = 50)
    private String signatureMethod; // PAPER, ELECTRONIC, VERBAL

    @Column(name = "signature_location", length = 100)
    private String signatureLocation;

    // Activation
    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "first_collection_date")
    private LocalDate firstCollectionDate;

    @Column(name = "last_collection_date")
    private LocalDate lastCollectionDate;

    // Limits and Frequency
    @Column(name = "max_amount", precision = 19, scale = 2)
    private java.math.BigDecimal maxAmount;

    @Column(name = "frequency", length = 20)
    private String frequency; // MONTHLY, WEEKLY, ANNUAL, etc.

    // Cancellation
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // Statistics
    @Column(name = "total_collections", nullable = false)
    private Long totalCollections = 0L;

    @Column(name = "successful_collections", nullable = false)
    private Long successfulCollections = 0L;

    @Column(name = "failed_collections", nullable = false)
    private Long failedCollections = 0L;

    @Column(name =="total_amount_collected", precision = 19, scale = 2)
    private java.math.BigDecimal totalAmountCollected = java.math.BigDecimal.ZERO;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (mandateId == null) {
            mandateId = generateMandateId();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private String generateMandateId() {
        // UMR format: creditorId + unique identifier
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "UMR-" + timestamp.substring(timestamp.length() - 10);
    }

    public enum MandateType {
        SDD_CORE,    // SEPA Direct Debit Core Scheme
        SDD_B2B      // SEPA Direct Debit Business-to-Business
    }

    public enum MandateStatus {
        PENDING,      // Awaiting signature
        ACTIVE,       // Active and can be used
        SUSPENDED,    // Temporarily suspended
        CANCELLED,    // Permanently cancelled
        EXPIRED       // Past expiration date
    }

    public enum SequenceType {
        FRST,  // First collection
        RCUR,  // Recurring collection
        FNAL,  // Final collection
        OOFF   // One-off collection
    }
}
