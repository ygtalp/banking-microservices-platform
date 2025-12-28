package com.banking.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCustomerRequest {

    @NotBlank(message = "Verified by is required")
    @Size(max = 100, message = "Verified by must not exceed 100 characters")
    private String verifiedBy;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
