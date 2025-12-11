package com.banking.transfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {
    private Long id;
    private String accountNumber;
    private String customerId;
    private String customerName;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
}