package com.banking.fraud.dto;

import com.banking.fraud.model.FraudCheckStatus;
import com.banking.fraud.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheckResponse {

    private String checkId;
    private String transferReference;
    private String accountNumber;
    private BigDecimal amount;
    private Integer riskScore;
    private RiskLevel riskLevel;
    private FraudCheckStatus status;
    private List<String> reasons;
    private LocalDateTime checkedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
}
