package com.banking.fraud.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fraud_checks", indexes = {
    @Index(name = "idx_transfer_reference", columnList = "transfer_reference"),
    @Index(name = "idx_account_number", columnList = "account_number"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_risk_level", columnList = "risk_level"),
    @Index(name = "idx_checked_at", columnList = "checked_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "check_id", unique = true, nullable = false, length = 50)
    private String checkId;

    @Column(name = "transfer_reference", nullable = false, length = 50)
    private String transferReference;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudCheckStatus status;

    @ElementCollection
    @CollectionTable(name = "fraud_check_reasons", joinColumns = @JoinColumn(name = "fraud_check_id"))
    @Column(name = "reason")
    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        if (checkedAt == null) {
            checkedAt = LocalDateTime.now();
        }
    }
}
