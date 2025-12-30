package com.banking.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent {
    private String accountNumber;
    private String iban;
    private String customerName;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String customerId;
}
