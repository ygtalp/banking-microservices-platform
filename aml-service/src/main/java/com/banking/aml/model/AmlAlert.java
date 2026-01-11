package com.banking.aml.model;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "aml_alerts", indexes = {
    @Index(name = "idx_alert_account", columnList = "account_number"),
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_alert_risk", columnList = "risk_level"),
    @Index(name = "idx_alert_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmlAlert implements Serializable {

    @Id
    @Column(name = "alert_id", nullable = false, length = 50)
    private String alertId;

    @Column(name = "alert_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "customer_id", length = 50)
    private String customerId;

    @Column(name = "transfer_reference", length = 50)
    private String transferReference;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "risk_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @ElementCollection
    @CollectionTable(name = "alert_reasons", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "reason")
    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.alertId == null || this.alertId.isEmpty()) {
            this.alertId = generateAlertId();
        }
        if (this.status == null) {
            this.status = AlertStatus.OPEN;
        }
        if (this.riskLevel == null && this.riskScore != null) {
            this.riskLevel = RiskLevel.fromScore(this.riskScore);
        }
    }

    private String generateAlertId() {
        return "AML-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    public void updateRiskLevel() {
        if (this.riskScore != null) {
            this.riskLevel = RiskLevel.fromScore(this.riskScore);
        }
    }

    public void addReason(String reason) {
        if (this.reasons == null) {
            this.reasons = new ArrayList<>();
        }
        this.reasons.add(reason);
    }
}
