package com.banking.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummaryResponse {
    private String accountNumber;
    private long totalTransactions;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private BigDecimal netAmount;
    private String currency;
}
