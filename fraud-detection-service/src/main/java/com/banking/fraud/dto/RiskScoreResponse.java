package com.banking.fraud.dto;

import com.banking.fraud.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoreResponse {

    private String accountNumber;
    private Integer currentScore;
    private RiskLevel riskLevel;
    private Long totalChecks;
    private Long flaggedCount;
    private Long blockedCount;
    private LocalDateTime lastCheckAt;
    private LocalDateTime lastIncidentAt;
}
