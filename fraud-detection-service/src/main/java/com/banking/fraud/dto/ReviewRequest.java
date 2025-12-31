package com.banking.fraud.dto;

import com.banking.fraud.model.FraudCheckStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    @NotNull(message = "Status is required")
    private FraudCheckStatus status;

    @NotBlank(message = "Reviewer name is required")
    private String reviewedBy;

    private String reviewNotes;
}
