package com.banking.aml.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmlCaseEscalatedEvent implements Serializable {

    private String caseId;
    private String caseNumber;
    private String customerId;
    private String customerName;
    private String caseType;
    private String priority;
    private String riskLevel;
    private String escalatedTo;
    private String escalatedBy;
    private String escalationReason;
    private LocalDateTime escalatedAt;
    private LocalDateTime timestamp;

    public AmlCaseEscalatedEvent(String caseId, String caseNumber, String customerId,
                                 String escalatedTo, String escalationReason) {
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.customerId = customerId;
        this.escalatedTo = escalatedTo;
        this.escalationReason = escalationReason;
        this.escalatedAt = LocalDateTime.now();
        this.timestamp = LocalDateTime.now();
    }
}
