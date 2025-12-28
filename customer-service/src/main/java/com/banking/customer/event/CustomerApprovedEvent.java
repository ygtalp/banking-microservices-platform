package com.banking.customer.event;

import com.banking.customer.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerApprovedEvent {

    private String eventType = "CUSTOMER_APPROVED";
    private String customerId;
    private String email;
    private String approvedBy;
    private RiskLevel riskLevel;
    private LocalDateTime approvedAt;
    private LocalDateTime timestamp;
}
