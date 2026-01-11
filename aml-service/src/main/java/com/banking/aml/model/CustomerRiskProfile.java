package com.banking.aml.model;

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

@Entity
@Table(name = "customer_risk_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CustomerRiskProfile implements Serializable {

    @Id
    @Column(name = "profile_id", length = 50)
    private String profileId;

    @Column(name = "customer_id", nullable = false, unique = true, length = 50)
    private String customerId;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "customer_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CustomerType customerType; // INDIVIDUAL, CORPORATE, PEP, HIGH_NET_WORTH

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore = 0; // 0-100

    @Column(name = "risk_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.LOW;

    // Transaction Statistics
    @Column(name = "total_transactions", nullable = false)
    private Long totalTransactions = 0L;

    @Column(name = "flagged_transactions", nullable = false)
    private Long flaggedTransactions = 0L;

    @Column(name = "blocked_transactions", nullable = false)
    private Long blockedTransactions = 0L;

    @Column(name = "total_transaction_amount", precision = 19, scale = 2)
    private BigDecimal totalTransactionAmount = BigDecimal.ZERO;

    @Column(name = "average_transaction_amount", precision = 19, scale = 2)
    private BigDecimal averageTransactionAmount = BigDecimal.ZERO;

    @Column(name = "max_transaction_amount", precision = 19, scale = 2)
    private BigDecimal maxTransactionAmount = BigDecimal.ZERO;

    // Alert Statistics
    @Column(name = "total_alerts", nullable = false)
    private Long totalAlerts = 0L;

    @Column(name = "open_alerts", nullable = false)
    private Long openAlerts = 0L;

    @Column(name = "cleared_alerts", nullable = false)
    private Long clearedAlerts = 0L;

    @Column(name = "sar_filed_count", nullable = false)
    private Long sarFiledCount = 0L; // Suspicious Activity Reports filed

    // Sanction Screening
    @Column(name = "sanction_matches", nullable = false)
    private Long sanctionMatches = 0L;

    @Column(name = "last_sanction_check_at")
    private LocalDateTime lastSanctionCheckAt;

    @Column(name = "is_sanctioned", nullable = false)
    private Boolean isSanctioned = false;

    // PEP Status (Politically Exposed Person)
    @Column(name = "is_pep", nullable = false)
    private Boolean isPep = false;

    @Column(name = "pep_category", length = 50)
    private String pepCategory; // e.g., "SENIOR_OFFICIAL", "FAMILY_MEMBER", "CLOSE_ASSOCIATE"

    // Geographic Risk
    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "high_risk_jurisdiction", nullable = false)
    private Boolean highRiskJurisdiction = false;

    // Business Risk
    @Column(name = "business_sector", length = 100)
    private String businessSector;

    @Column(name = "high_risk_business", nullable = false)
    private Boolean highRiskBusiness = false; // e.g., cash-intensive, MSB, crypto

    // Customer Due Diligence Level
    @Column(name = "cdd_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CddLevel cddLevel = CddLevel.STANDARD; // SIMPLIFIED, STANDARD, ENHANCED

    @Column(name = "last_cdd_review_at")
    private LocalDateTime lastCddReviewAt;

    @Column(name = "next_cdd_review_at")
    private LocalDateTime nextCddReviewAt;

    // Status
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProfileStatus status = ProfileStatus.ACTIVE;

    @Column(name = "blocked_reason", columnDefinition = "TEXT")
    private String blockedReason;

    // Timestamps
    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    @Column(name = "last_alert_at")
    private LocalDateTime lastAlertAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (profileId == null) {
            profileId = "RISK-" + customerId + "-" + System.currentTimeMillis();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        calculateRiskScore();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateRiskScore();
    }

    /**
     * Calculate risk score based on multiple factors
     */
    private void calculateRiskScore() {
        int score = 0;

        // Base score from alert ratio
        if (totalTransactions > 0) {
            double alertRatio = (double) flaggedTransactions / totalTransactions;
            score += (int) (alertRatio * 30); // Max 30 points
        }

        // Sanction screening
        if (isSanctioned) {
            score += 50; // Critical
        } else if (sanctionMatches > 0) {
            score += 20;
        }

        // PEP status
        if (isPep) {
            score += 15;
        }

        // High-risk jurisdiction
        if (highRiskJurisdiction) {
            score += 10;
        }

        // High-risk business
        if (highRiskBusiness) {
            score += 10;
        }

        // SAR filing history
        score += Math.min(sarFiledCount.intValue() * 5, 15); // Max 15 points

        // Blocked transactions ratio
        if (totalTransactions > 0) {
            double blockedRatio = (double) blockedTransactions / totalTransactions;
            score += (int) (blockedRatio * 10); // Max 10 points
        }

        this.riskScore = Math.min(score, 100);
        this.riskLevel = RiskLevel.fromScore(this.riskScore);
    }

    public enum CustomerType {
        INDIVIDUAL,
        CORPORATE,
        PEP,              // Politically Exposed Person
        HIGH_NET_WORTH,
        MONEY_SERVICE_BUSINESS,
        NON_PROFIT
    }

    public enum CddLevel {
        SIMPLIFIED,  // Low risk customers
        STANDARD,    // Normal customers
        ENHANCED     // High risk customers (PEP, high-risk countries)
    }

    public enum ProfileStatus {
        ACTIVE,
        UNDER_REVIEW,
        BLOCKED,
        CLOSED
    }
}
