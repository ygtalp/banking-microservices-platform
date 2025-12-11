package com.banking.transfer.dto;

import com.banking.transfer.model.TransferType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "From account number is required")
    @Size(min = 10, max = 50, message = "Account number must be between 10 and 50 characters")
    private String fromAccountNumber;

    @NotBlank(message = "To account number is required")
    @Size(min = 10, max = 50, message = "Account number must be between 10 and 50 characters")
    private String toAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1,000,000")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Invalid currency code")
    private String currency;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Transfer type is required")
    private TransferType transferType;

    // Idempotency key for duplicate prevention
    private String idempotencyKey;
}