package com.banking.transaction.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalanceUpdatedEvent {
    private String accountNumber;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private String currency;
    private String transactionType;  // CREDIT or DEBIT
    private BigDecimal amount;
    private String reference;
}
