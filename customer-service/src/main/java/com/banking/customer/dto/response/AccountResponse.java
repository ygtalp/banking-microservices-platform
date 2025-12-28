package com.banking.customer.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Account Service responses
 * Mirrors AccountResponse from Account Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private String customerId;
    private String customerName;
    private BigDecimal balance;
    private String currency;
    private String status;
    private String accountType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
