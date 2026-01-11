package com.banking.auth.dto.mfa;

import com.banking.auth.model.MfaMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaStatusResponse {
    private boolean enabled;
    private MfaMethod preferredMethod;
    private boolean totpEnabled;
    private boolean smsEnabled;
    private boolean emailEnabled;
    private int remainingBackupCodes;
}
