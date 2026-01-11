package com.banking.aml.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmlAlertCreatedEvent implements Serializable {

    private String alertId;
    private String alertType;
    private String status;
    private String accountNumber;
    private String customerId;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private Integer riskScore;
    private String riskLevel;
    private List<String> reasons;
    private String transferReference;
    private LocalDateTime timestamp;

    public AmlAlertCreatedEvent(String alertId, String accountNumber, String customerId,
                                Integer riskScore, String riskLevel, List<String> reasons) {
        this.alertId = alertId;
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.reasons = reasons;
        this.timestamp = LocalDateTime.now();
    }
}
