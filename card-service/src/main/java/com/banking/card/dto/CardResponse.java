package com.banking.card.dto;

import com.banking.card.model.CardStatus;
import com.banking.card.model.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponse {
    private String cardNumber;
    private String maskedCardNumber; // e.g., **** **** **** 1234
    private String customerId;
    private String accountNumber;
    private CardType cardType;
    private String cardholderName;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal dailySpent;
    private BigDecimal monthlySpent;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private Boolean isContactlessEnabled;
    private Boolean isOnlineEnabled;
    private Boolean isInternationalEnabled;
    private LocalDateTime createdAt;
}
