package com.banking.sepa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sepa_transfers", indexes = {
    @Index(name = "idx_sepa_debtor_iban", columnList = "debtor_iban"),
    @Index(name = "idx_sepa_creditor_iban", columnList = "creditor_iban"),
    @Index(name = "idx_sepa_status", columnList = "status"),
    @Index(name = "idx_sepa_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SepaTransfer implements Serializable {

    @Id
    @Column(name = "sepa_reference", nullable = false, length = 50)
    private String sepaReference;

    @Column(name = "transfer_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SepaTransferType transferType; // SCT, SCT_INST, SDD_CORE, SDD_B2B

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SepaTransferStatus status;

    // Debtor (sender) information
    @Column(name = "debtor_iban", nullable = false, length = 34)
    private String debtorIban;

    @Column(name = "debtor_name", nullable = false, length = 140)
    private String debtorName;

    @Column(name = "debtor_account_number", length = 20)
    private String debtorAccountNumber; // Internal account number

    @Column(name = "debtor_bic", length = 11)
    private String debtorBic;

    // Creditor (receiver) information
    @Column(name = "creditor_iban", nullable = false, length = 34)
    private String creditorIban;

    @Column(name = "creditor_name", nullable = false, length = 140)
    private String creditorName;

    @Column(name = "creditor_bic", length = 11)
    private String creditorBic;

    // Payment details
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "remittance_information", length = 140)
    private String remittanceInformation;

    @Column(name = "end_to_end_id", nullable = false, length = 35)
    private String endToEndId;

    @Column(name = "message_id", length = 35)
    private String messageId;

    // SEPA specific
    @Column(name = "requested_execution_date")
    private LocalDate requestedExecutionDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "charge_bearer", length = 10)
    private String chargeBearer; // SLEV (default), SHAR, etc.

    // ISO 20022 XML
    @Column(name = "iso_xml", columnDefinition = "TEXT")
    private String isoXml;

    // SAGA tracking
    @Column(name = "debit_transaction_id", length = 50)
    private String debitTransactionId;

    @Column(name = "submission_id", length = 50)
    private String submissionId;

    @Column(name = "external_reference", length = 50)
    private String externalReference;

    // Processing
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.sepaReference == null || this.sepaReference.isEmpty()) {
            this.sepaReference = generateSepaReference();
        }
        if (this.status == null) {
            this.status = SepaTransferStatus.PENDING;
        }
        if (this.currency == null || this.currency.isEmpty()) {
            this.currency = "EUR";
        }
        if (this.chargeBearer == null || this.chargeBearer.isEmpty()) {
            this.chargeBearer = "SLEV";
        }
    }

    private String generateSepaReference() {
        return "SEPA-" + System.currentTimeMillis();
    }

    public enum SepaTransferType {
        SCT,        // SEPA Credit Transfer (standard)
        SCT_INST,   // SEPA Instant Credit Transfer
        SDD_CORE,   // SEPA Direct Debit Core
        SDD_B2B     // SEPA Direct Debit Business-to-Business
    }

    public enum SepaTransferStatus {
        PENDING,
        VALIDATING,
        VALIDATED,
        DEBITING,
        DEBITED,
        SUBMITTING,
        SUBMITTED,
        PROCESSING,
        COMPLETED,
        COMPENSATING,
        COMPENSATED,
        FAILED,
        REJECTED
    }
}
