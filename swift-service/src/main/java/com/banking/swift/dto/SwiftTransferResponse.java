package com.banking.swift.dto;

import com.banking.swift.model.ChargeType;
import com.banking.swift.model.SwiftTransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for SWIFT transfer response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwiftTransferResponse {

    private Long id;
    private String transactionReference;
    private String messageType;
    
    // Value & Amount
    private LocalDate valueDate;
    private String currency;
    private BigDecimal amount;
    
    // Ordering Customer
    private String orderingCustomerName;
    private String orderingCustomerAddress;
    private String orderingCustomerAccount;
    
    // Sender's Bank
    private String senderBic;
    private String senderName;
    
    // Correspondent Bank
    private String correspondentBic;
    private String correspondentName;
    private String correspondentAccount;
    
    // Beneficiary's Bank
    private String beneficiaryBankBic;
    private String beneficiaryBankName;
    
    // Beneficiary Customer
    private String beneficiaryName;
    private String beneficiaryAddress;
    private String beneficiaryAccount;
    
    // Remittance Info
    private String remittanceInfo;
    
    // Charges
    private ChargeType chargeType;
    private BigDecimal fixedFee;
    private BigDecimal percentageFee;
    private BigDecimal totalFee;
    
    // Status
    private SwiftTransferStatus status;
    private String statusReason;
    
    // Compliance
    private Boolean ofacChecked;
    private Boolean sanctionsChecked;
    private Boolean complianceCleared;
    private String complianceNotes;
    
    // Settlement
    private LocalDate settlementDate;
    private String settlementReference;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
