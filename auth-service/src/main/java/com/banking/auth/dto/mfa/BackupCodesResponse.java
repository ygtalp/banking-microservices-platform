package com.banking.auth.dto.mfa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupCodesResponse {
    private List<String> codes;
    private String message;
}
