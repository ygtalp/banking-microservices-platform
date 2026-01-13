package com.banking.swift.dto;

import com.banking.swift.model.ChargeType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new SWIFT transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSwiftTransferRequest {

    @NotNull(message = "Value date is required")
    @FutureOrPresent(message = "Value date must be today or in the future")
    private LocalDate valueDate;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., USD, EUR)")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    private String currency;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "500000.00", message = "Amount cannot exceed 500,000")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    // Ordering Customer (Sender)
    @NotBlank(message = "Ordering customer name is required")
    @Size(max = 140, message = "Ordering customer name cannot exceed 140 characters")
    private String orderingCustomerName;

    @Size(max = 140, message = "Ordering customer address cannot exceed 140 characters")
    private String orderingCustomerAddress;

    @Size(max = 34, message = "Ordering customer account cannot exceed 34 characters")
    private String orderingCustomerAccount;

    // Sender's Bank
    @NotBlank(message = "Sender BIC is required")
    @Size(min = 8, max = 11, message = "BIC must be 8 or 11 characters")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$", message = "Invalid BIC format")
    private String senderBic;

    @NotBlank(message = "Sender name is required")
    @Size(max = 140, message = "Sender name cannot exceed 140 characters")
    private String senderName;

    // Beneficiary's Bank
    @NotBlank(message = "Beneficiary bank BIC is required")
    @Size(min = 8, max = 11, message = "BIC must be 8 or 11 characters")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$", message = "Invalid BIC format")
    private String beneficiaryBankBic;

    @Size(max = 140, message = "Beneficiary bank name cannot exceed 140 characters")
    private String beneficiaryBankName;

    // Beneficiary Customer (Receiver)
    @NotBlank(message = "Beneficiary name is required")
    @Size(max = 140, message = "Beneficiary name cannot exceed 140 characters")
    private String beneficiaryName;

    @Size(max = 140, message = "Beneficiary address cannot exceed 140 characters")
    private String beneficiaryAddress;

    @NotBlank(message = "Beneficiary account is required")
    @Size(max = 34, message = "Beneficiary account cannot exceed 34 characters")
    private String beneficiaryAccount;

    // Remittance Information
    @Size(max = 140, message = "Remittance info cannot exceed 140 characters")
    private String remittanceInfo;

    // Charge Type
    @NotNull(message = "Charge type is required")
    private ChargeType chargeType;
}
