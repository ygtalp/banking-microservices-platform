package com.banking.sepa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sepa_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SepaBatch implements Serializable {

    @Id
    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "batch_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BatchType batchType; // SCT, SCT_INST, SDD_CORE, SDD_B2B

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BatchStatus status = BatchStatus.PENDING;

    // Batch Details
    @Column(name = "message_id", unique = true, length = 35)
    private String messageId; // ISO 20022 Message ID

    @Column(name = "creation_date_time", nullable = false)
    private LocalDateTime creationDateTime;

    @Column(name = "requested_execution_date")
    private java.time.LocalDate requestedExecutionDate;

    // Initiating Party (who creates the batch)
    @Column(name = "initiating_party_name", length = 140)
    private String initiatingPartyName;

    @Column(name = "initiating_party_id", length = 35)
    private String initiatingPartyId;

    // Batch Statistics
    @Column(name = "number_of_transactions", nullable = false)
    private Integer numberOfTransactions = 0;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "EUR";

    // Processing Statistics
    @Column(name = "successful_transactions")
    private Integer successfulTransactions = 0;

    @Column(name = "failed_transactions")
    private Integer failedTransactions = 0;

    @Column(name = "pending_transactions")
    private Integer pendingTransactions = 0;

    @Column(name = "successful_amount", precision = 19, scale = 2)
    private BigDecimal successfulAmount = BigDecimal.ZERO;

    // Transfer References (stored as comma-separated list)
    @ElementCollection
    @CollectionTable(name = "sepa_batch_transfers", joinColumns = @JoinColumn(name = "batch_id"))
    @Column(name = "transfer_reference")
    private List<String> transferReferences = new ArrayList<>();

    // ISO 20022 XML
    @Column(name = "pain_xml", columnDefinition = "TEXT")
    private String painXml; // pain.001 XML for credit transfers

    @Column(name = "pacs_xml", columnDefinition = "TEXT")
    private String pacsXml; // pacs.008 XML for interbank transfers

    // Submission
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "submission_reference", length = 50)
    private String submissionReference; // External reference from SEPA network

    // Processing
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    // Errors
    @Column(name = "has_errors", nullable = false)
    private Boolean hasErrors = false;

    @Column(name = "error_count")
    private Integer errorCount = 0;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    // File Information (if batch uploaded via file)
    @Column(name = "source_file_name", length = 200)
    private String sourceFileName;

    @Column(name = "source_file_hash", length = 64)
    private String sourceFileHash;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (batchId == null) {
            batchId = generateBatchId();
        }
        if (creationDateTime == null) {
            creationDateTime = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (messageId == null) {
            messageId = generateMessageId();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Recalculate pending transactions
        if (numberOfTransactions != null && successfulTransactions != null && failedTransactions != null) {
            pendingTransactions = numberOfTransactions - successfulTransactions - failedTransactions;
        }
    }

    private String generateBatchId() {
        return "BATCH-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private String generateMessageId() {
        // ISO 20022 Message ID format
        return "MSG-" + System.currentTimeMillis();
    }

    public enum BatchType {
        SCT,         // SEPA Credit Transfer
        SCT_INST,    // SEPA Instant Credit Transfer
        SDD_CORE,    // SEPA Direct Debit Core
        SDD_B2B      // SEPA Direct Debit B2B
    }

    public enum BatchStatus {
        PENDING,           // Created but not submitted
        VALIDATING,        // Validation in progress
        VALIDATED,         // Validation successful
        SUBMITTED,         // Submitted to SEPA network
        PROCESSING,        // Being processed
        PARTIALLY_COMPLETE,// Some transfers completed
        COMPLETED,         // All transfers completed
        FAILED,            // Batch failed
        CANCELLED          // Batch cancelled
    }
}
