package com.banking.account.dto;

import com.banking.account.model.AccountType;
import com.banking.account.model.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    private BigDecimal initialBalance;
}