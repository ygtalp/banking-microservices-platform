package com.banking.aml.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "sanction_matches", indexes = {
    @Index(name = "idx_sanction_account", columnList = "account_number"),
    @Index(name = "idx_sanction_status", columnList = "match_status"),
    @Index(name = "idx_sanction_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanctionMatch implements Serializable {

    @Id
    @Column(name = "match_id", nullable = false, length = 50)
    private String matchId;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "sanctioned_name", nullable = false, length = 200)
    private String sanctionedName;

    @Column(name = "sanctioned_entity_id", length = 100)
    private String sanctionedEntityId;

    @Column(name = "sanction_list", nullable = false, length = 50)
    private String sanctionList; // OFAC, EU, UN, etc.

    @Column(name = "match_score", nullable = false)
    private Integer matchScore; // 0-100

    @Column(name = "match_status", nullable = false, length = 20)
    private String matchStatus; // POTENTIAL, CONFIRMED, FALSE_POSITIVE

    @Column(name = "match_reason", columnDefinition = "TEXT")
    private String matchReason;

    @Column(name = "alert_id", length = 50)
    private String alertId;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.matchId == null || this.matchId.isEmpty()) {
            this.matchId = generateMatchId();
        }
        if (this.matchStatus == null || this.matchStatus.isEmpty()) {
            this.matchStatus = "POTENTIAL";
        }
    }

    private String generateMatchId() {
        return "SM-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
