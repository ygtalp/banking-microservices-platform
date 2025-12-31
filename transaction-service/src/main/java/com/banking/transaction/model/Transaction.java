package com.banking.transaction.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_account_number", columnList = "accountNumber"),
    @Index(name = "idx_transaction_date", columnList = "transactionDate"),
    @Index(name = "idx_reference", columnList = "reference")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String transactionId;

    @Column(nullable = false, length = 50)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 100)
    private String reference;  // Transfer reference, external reference, etc.

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 100)
    private String sourceAccount;  // For transfers

    @Column(length = 100)
    private String destinationAccount;  // For transfers

    @Column(columnDefinition = "TEXT")
    private String metadata;  // JSON metadata for flexible data

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    @PrePersist
    public void prePersist() {
        if (transactionId == null) {
            transactionId = generateTransactionId();
        }
        if (status == null) {
            status = TransactionStatus.COMPLETED;
        }
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }
}
