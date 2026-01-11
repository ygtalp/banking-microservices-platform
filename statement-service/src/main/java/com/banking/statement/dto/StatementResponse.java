package com.banking.statement.dto;

import com.banking.statement.model.StatementStatus;
import com.banking.statement.model.StatementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementResponse {

    private String statementId;
    private String accountNumber;
    private String customerId;
    private StatementType statementType;
    private LocalDate statementDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private Integer transactionCount;
    private StatementStatus status;
    private Long pdfFileSize;
    private LocalDateTime generatedAt;
    private LocalDateTime sentAt;
    private LocalDateTime downloadedAt;
    private Boolean notificationSent;
    private String downloadUrl;
    private LocalDateTime createdAt;
}
