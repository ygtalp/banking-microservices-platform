package com.banking.card.model;

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
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_number", columnList = "card_number"),
        @Index(name = "idx_account_number", columnList = "account_number"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", nullable = false, unique = true, length = 16)
    private String cardNumber; // 16-digit card number

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber; // Linked account

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType;

    @Column(name = "cardholder_name", nullable = false, length = 100)
    private String cardholderName;

    @Column(name = "cvv", nullable = false, length = 3)
    private String cvv; // Should be encrypted in production

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "pin_hash", length = 255)
    private String pinHash; // BCrypt hashed PIN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

    // Limits
    @Column(name = "daily_limit", precision = 19, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(name = "transaction_limit", precision = 19, scale = 2)
    private BigDecimal transactionLimit; // Per transaction

    // Credit Card specific
    @Column(name = "credit_limit", precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "available_credit", precision = 19, scale = 2)
    private BigDecimal availableCredit;

    // Usage tracking
    @Column(name = "daily_spent", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal dailySpent = BigDecimal.ZERO;

    @Column(name = "monthly_spent", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal monthlySpent = BigDecimal.ZERO;

    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    // Security
    @Column(name = "is_contactless_enabled")
    @Builder.Default
    private Boolean isContactlessEnabled = true;

    @Column(name = "is_online_enabled")
    @Builder.Default
    private Boolean isOnlineEnabled = true;

    @Column(name = "is_international_enabled")
    @Builder.Default
    private Boolean isInternationalEnabled = false;

    @Column(name = "blocked_reason", columnDefinition = "TEXT")
    private String blockedReason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void activate() {
        this.status = CardStatus.ACTIVE;
    }

    public void block(String reason) {
        this.status = CardStatus.BLOCKED;
        this.blockedReason = reason;
        this.blockedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = CardStatus.CANCELLED;
    }

    public boolean isExpired() {
        return this.expiryDate.isBefore(LocalDate.now());
    }

    public boolean isActive() {
        return this.status == CardStatus.ACTIVE && !isExpired();
    }
}
