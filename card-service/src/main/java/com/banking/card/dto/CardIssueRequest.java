package com.banking.card.dto;

import com.banking.card.model.CardType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardIssueRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Card type is required")
    private CardType cardType;

    @NotBlank(message = "Cardholder name is required")
    private String cardholderName;

    @NotNull
    @DecimalMin(value = "100.00")
    private BigDecimal dailyLimit;

    @NotNull
    @DecimalMin(value = "1000.00")
    private BigDecimal monthlyLimit;

    private BigDecimal creditLimit; // For credit cards
}
