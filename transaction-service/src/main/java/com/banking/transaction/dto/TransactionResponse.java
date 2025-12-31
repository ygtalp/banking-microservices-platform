package com.banking.transaction.dto;

import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private String transactionId;
    private String accountNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String reference;
    private String description;
    private TransactionStatus status;
    private String sourceAccount;
    private String destinationAccount;
    private LocalDateTime transactionDate;
}
