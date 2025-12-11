package com.banking.account.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_history", indexes = {
        @Index(name = "idx_history_account_id", columnList = "account_id"),
        @Index(name = "idx_history_timestamp", columnList = "timestamp")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "account_number", nullable = false, length = 26)
    private String accountNumber;

    @Column(name = "operation", nullable = false, length = 50)
    private String operation; // CREDIT, DEBIT, FREEZE, ACTIVATE, CLOSE

    @Column(name = "previous_balance", precision = 19, scale = 2)
    private BigDecimal previousBalance;

    @Column(name = "new_balance", precision = 19, scale = 2)
    private BigDecimal newBalance;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_id", length = 100)
    private String referenceId; // Transfer ID, Transaction ID, etc.

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}