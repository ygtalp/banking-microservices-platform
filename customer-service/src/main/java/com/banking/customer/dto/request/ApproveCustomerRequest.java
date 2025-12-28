package com.banking.customer.dto.request;

import com.banking.customer.model.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveCustomerRequest {

    @NotBlank(message = "Approved by is required")
    @Size(max = 100, message = "Approved by must not exceed 100 characters")
    private String approvedBy;

    @NotNull(message = "Risk level is required")
    private RiskLevel riskLevel;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
