package com.banking.fraud.dto;

import com.banking.fraud.model.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRuleResponse {

    private String ruleId;
    private String ruleName;
    private String description;
    private RuleType ruleType;
    private Boolean enabled;
    private BigDecimal threshold;
    private Integer timeWindowMinutes;
    private Integer maxCount;
    private Integer riskPoints;
    private Integer startHour;
    private Integer endHour;
}
