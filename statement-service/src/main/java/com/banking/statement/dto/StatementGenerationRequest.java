package com.banking.statement.dto;

import com.banking.statement.model.StatementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementGenerationRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Statement type is required")
    private StatementType statementType;

    // For CUSTOM type
    private LocalDate periodStart;

    // For CUSTOM type
    private LocalDate periodEnd;

    // For MONTHLY type (default: current month if null)
    private Integer month;

    // For MONTHLY/QUARTERLY/ANNUAL type (default: current year if null)
    private Integer year;

    // For QUARTERLY type (1-4)
    private Integer quarter;

    private Boolean sendEmail;
}
