package com.banking.statement.model;

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
@Table(name = "statements", indexes = {
        @Index(name = "idx_account_number", columnList = "account_number"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_statement_date", columnList = "statement_date"),
        @Index(name = "idx_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_id", unique = true, nullable = false, length = 50)
    private String statementId;

    @Column(name = "account_number", nullable = false, length = 26)
    private String accountNumber;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 20)
    private StatementType statementType;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "opening_balance", precision = 19, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", precision = 19, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "total_credits", precision = 19, scale = 2)
    private BigDecimal totalCredits;

    @Column(name = "total_debits", precision = 19, scale = 2)
    private BigDecimal totalDebits;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatementStatus status = StatementStatus.GENERATING;

    @Column(name = "pdf_file_path", length = 500)
    private String pdfFilePath;

    @Column(name = "pdf_file_size")
    private Long pdfFileSize;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "requested_by", length = 50)
    private String requestedBy;

    @Column(name = "notification_sent", nullable = false)
    @Builder.Default
    private Boolean notificationSent = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void markAsGenerated(String filePath, Long fileSize) {
        this.status = StatementStatus.GENERATED;
        this.pdfFilePath = filePath;
        this.pdfFileSize = fileSize;
        this.generatedAt = LocalDateTime.now();
    }

    public void markAsSent() {
        this.status = StatementStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.notificationSent = true;
    }

    public void markAsDownloaded() {
        this.status = StatementStatus.DOWNLOADED;
        this.downloadedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = StatementStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public boolean isGenerated() {
        return this.status == StatementStatus.GENERATED ||
               this.status == StatementStatus.SENT ||
               this.status == StatementStatus.DOWNLOADED;
    }

    public boolean isAvailableForDownload() {
        return this.pdfFilePath != null && !this.pdfFilePath.isEmpty();
    }
}
