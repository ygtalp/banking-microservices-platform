package com.banking.aml.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SepaTransferCompletedEvent implements Serializable {

    private String sepaReference;
    private String transferType;
    private String status;
    private String debtorIban;
    private String debtorName;
    private String debtorAccountNumber;
    private String creditorIban;
    private String creditorName;
    private BigDecimal amount;
    private String currency;
    private String remittanceInformation;
    private String endToEndId;
    private LocalDateTime processedAt;
    private LocalDateTime timestamp;
}
