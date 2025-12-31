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
public class AccountCreatedEvent {
    private String accountNumber;
    private String iban;
    private String customerName;
    private String accountType;
    private BigDecimal balance;
    private String currency;
}
