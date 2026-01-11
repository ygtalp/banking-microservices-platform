package com.banking.auth.dto.mfa;

import com.banking.auth.model.MfaMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {

    @NotBlank(message = "Code is required")
    @Size(min = 6, max = 8, message = "Code must be 6-8 characters")
    private String code;

    private MfaMethod method; // Optional, uses preferred if not specified
}
