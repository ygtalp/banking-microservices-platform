package com.banking.swift.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "swift_transfers", indexes = {
        @Index(name = "idx_reference", columnList = "transaction_reference"),
        @Index(name = "idx_sender_bic", columnList = "sender_bic"),
        @Index(name = "idx_beneficiary_bic", columnList = "beneficiary_bic"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_value_date", columnList = "value_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwiftTransfer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Transaction Reference (:20)
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 16)
    private String transactionReference;

    // Message Type (always MT103 for customer transfers)
    @Column(name = "message_type", nullable = false, length = 6)
    private String messageType = "MT103";

    // Bank Operation Code (:23B)
    @Column(name = "bank_operation_code", length = 4)
    private String bankOperationCode = "CRED"; // CRED for credit transfer

    // Value Date, Currency, Amount (:32A)
    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // Ordering Customer (:50K)
    @Column(name = "ordering_customer_name", nullable = false, length = 140)
    private String orderingCustomerName;

    @Column(name = "ordering_customer_address", length = 140)
    private String orderingCustomerAddress;

    @Column(name = "ordering_customer_account", length = 34)
    private String orderingCustomerAccount;

    // Ordering Institution (:52A) - Sender's Bank
    @Column(name = "sender_bic", nullable = false, length = 11)
    private String senderBic;

    @Column(name = "sender_name", nullable = false, length = 140)
    private String senderName;

    // Sender's Correspondent (:53A) - Our correspondent bank
    @Column(name = "correspondent_bic", length = 11)
    private String correspondentBic;

    @Column(name = "correspondent_name", length = 140)
    private String correspondentName;

    @Column(name = "correspondent_account", length = 34)
    private String correspondentAccount; // Nostro account

    // Account With Institution (:57A) - Beneficiary's Bank
    @Column(name = "beneficiary_bank_bic", nullable = false, length = 11)
    private String beneficiaryBankBic;

    @Column(name = "beneficiary_bank_name", length = 140)
    private String beneficiaryBankName;

    // Beneficiary Customer (:59)
    @Column(name = "beneficiary_name", nullable = false, length = 140)
    private String beneficiaryName;

    @Column(name = "beneficiary_address", length = 140)
    private String beneficiaryAddress;

    @Column(name = "beneficiary_account", nullable = false, length = 34)
    private String beneficiaryAccount;

    // Remittance Information (:70)
    @Column(name = "remittance_info", length = 140)
    private String remittanceInfo;

    // Details of Charges (:71A)
    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 3)
    private ChargeType chargeType = ChargeType.SHA; // SHA (shared) is most common

    // Fee Calculation
    @Column(name = "fixed_fee", precision = 19, scale = 2)
    private BigDecimal fixedFee;

    @Column(name = "percentage_fee", precision = 19, scale = 4)
    private BigDecimal percentageFee;

    @Column(name = "total_fee", precision = 19, scale = 2)
    private BigDecimal totalFee;

    // MT103 Message
    @Column(name = "mt103_message", columnDefinition = "TEXT")
    private String mt103Message; // Generated SWIFT message

    // Status and Processing
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SwiftTransferStatus status = SwiftTransferStatus.PENDING;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    // Compliance
    @Column(name = "ofac_checked")
    private Boolean ofacChecked = false;

    @Column(name = "sanctions_checked")
    private Boolean sanctionsChecked = false;

    @Column(name = "compliance_cleared")
    private Boolean complianceCleared = false;

    @Column(name = "compliance_notes", length = 500)
    private String complianceNotes;

    // Settlement
    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "settlement_reference", length = 35)
    private String settlementReference;

    // External References
    @Column(name = "internal_account_id", length = 50)
    private String internalAccountId; // Link to our account service

    @Column(name = "external_reference", length = 50)
    private String externalReference;

    // Audit Fields
    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Business Logic Methods
    public void calculateFees(BigDecimal fixedFeeAmount, BigDecimal percentageRate) {
        this.fixedFee = fixedFeeAmount;
        this.percentageFee = percentageRate;
        this.totalFee = fixedFeeAmount.add(this.amount.multiply(percentageRate));
    }

    public void markAsProcessing() {
        this.status = SwiftTransferStatus.PROCESSING;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsCompleted(String settlementRef) {
        this.status = SwiftTransferStatus.COMPLETED;
        this.settlementReference = settlementRef;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = SwiftTransferStatus.FAILED;
        this.statusReason = reason;
    }

    public void markComplianceCleared() {
        this.ofacChecked = true;
        this.sanctionsChecked = true;
        this.complianceCleared = true;
    }
}