package com.banking.account.event;

import com.banking.account.model.AccountType;
import com.banking.account.model.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccountCreatedEvent extends AccountEvent {

    private String customerName;
    private BigDecimal initialBalance;
    private Currency currency;
    private AccountType accountType;
}