package com.banking.auth.dto.mfa;

import com.banking.auth.model.MfaMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupRequest {

    @NotNull(message = "MFA method is required")
    private MfaMethod method;

    private String phoneNumber; // Required for SMS method
}
