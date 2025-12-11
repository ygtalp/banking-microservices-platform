package com.banking.transfer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfer_reference", columnList = "transferReference"),
        @Index(name = "idx_from_account", columnList = "fromAccountNumber"),
        @Index(name = "idx_to_account", columnList = "toAccountNumber"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String transferReference;

    @Column(nullable = false, length = 50)
    private String fromAccountNumber;

    @Column(nullable = false, length = 50)
    private String toAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferType transferType;

    @Column(length = 1000)
    private String failureReason;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    // Saga orchestration fields
    @Column(length = 50)
    private String debitTransactionId;

    @Column(length = 50)
    private String creditTransactionId;

    @Column
    private LocalDateTime initiatedAt;

    @Column
    private LocalDateTime completedAt;

    @Version
    private Long version; // For optimistic locking

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isCompleted() {
        return status == TransferStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == TransferStatus.FAILED;
    }

    public boolean isPending() {
        return status == TransferStatus.PENDING ||
                status == TransferStatus.DEBIT_PENDING ||
                status == TransferStatus.CREDIT_PENDING;
    }
}