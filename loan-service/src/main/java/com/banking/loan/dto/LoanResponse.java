package com.banking.loan.dto;

import com.banking.loan.model.LoanStatus;
import com.banking.loan.model.LoanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {
    private String loanId;
    private String customerId;
    private String accountNumber;
    private LoanType loanType;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private BigDecimal monthlyPayment;
    private BigDecimal totalAmount;
    private LoanStatus status;
    private Integer creditScore;
    private LocalDateTime createdAt;
    private LocalDateTime startDate;
    private LocalDateTime nextPaymentDate;
    private Integer paymentsMade;
    private BigDecimal amountPaid;
    private BigDecimal outstandingBalance;
}
