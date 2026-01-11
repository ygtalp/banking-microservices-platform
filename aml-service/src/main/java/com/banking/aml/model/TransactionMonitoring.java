package com.banking.aml.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_monitoring", indexes = {
    @Index(name = "idx_txn_account", columnList = "account_number"),
    @Index(name = "idx_txn_transfer", columnList = "transfer_reference"),
    @Index(name = "idx_txn_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionMonitoring implements Serializable {

    @Id
    @Column(name = "monitoring_id", nullable = false, length = 50)
    private String monitoringId;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "transfer_reference", nullable = false, length = 50)
    private String transferReference;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "triggered_rules", columnDefinition = "TEXT")
    private String triggeredRules; // Comma-separated rule IDs

    @Column(name = "flagged", nullable = false)
    private Boolean flagged;

    @Column(name = "alert_id", length = 50)
    private String alertId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.monitoringId == null || this.monitoringId.isEmpty()) {
            this.monitoringId = generateMonitoringId();
        }
        if (this.flagged == null) {
            this.flagged = false;
        }
    }

    private String generateMonitoringId() {
        return "TXM-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
