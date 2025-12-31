package com.banking.fraud.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_scores", indexes = {
    @Index(name = "idx_account_number_unique", columnList = "account_number", unique = true),
    @Index(name = "idx_risk_level", columnList = "risk_level")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScore implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "current_score", nullable = false)
    @Builder.Default
    private Integer currentScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "total_checks", nullable = false)
    @Builder.Default
    private Long totalChecks = 0L;

    @Column(name = "flagged_count", nullable = false)
    @Builder.Default
    private Long flaggedCount = 0L;

    @Column(name = "blocked_count", nullable = false)
    @Builder.Default
    private Long blockedCount = 0L;

    @Column(name = "last_check_at")
    private LocalDateTime lastCheckAt;

    @Column(name = "last_incident_at")
    private LocalDateTime lastIncidentAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Update risk level based on current score
        if (currentScore >= 76) {
            riskLevel = RiskLevel.CRITICAL;
        } else if (currentScore >= 51) {
            riskLevel = RiskLevel.HIGH;
        } else if (currentScore >= 26) {
            riskLevel = RiskLevel.MEDIUM;
        } else {
            riskLevel = RiskLevel.LOW;
        }
    }
}
