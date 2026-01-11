package com.banking.loan.service;

import com.banking.loan.dto.LoanApplicationRequest;
import com.banking.loan.dto.LoanResponse;
import com.banking.loan.model.Loan;
import com.banking.loan.model.LoanStatus;
import com.banking.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;

    @Transactional
    public LoanResponse applyForLoan(LoanApplicationRequest request) {
        // Generate loan ID
        String loanId = "LON-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // Calculate monthly payment and total interest
        BigDecimal monthlyPayment = calculateMonthlyPayment(
            request.getAmount(),
            request.getInterestRate(),
            request.getTermMonths()
        );

        BigDecimal totalAmount = monthlyPayment.multiply(new BigDecimal(request.getTermMonths()));
        BigDecimal totalInterest = totalAmount.subtract(request.getAmount());

        // Calculate credit score (simplified - in reality would call credit bureau)
        Integer creditScore = calculateCreditScore(request.getCustomerId());

        Loan loan = Loan.builder()
                .loanId(loanId)
                .customerId(request.getCustomerId())
                .accountNumber(request.getAccountNumber())
                .loanType(request.getLoanType())
                .amount(request.getAmount())
                .interestRate(request.getInterestRate())
                .termMonths(request.getTermMonths())
                .monthlyPayment(monthlyPayment)
                .totalInterest(totalInterest)
                .totalAmount(totalAmount)
                .creditScore(creditScore)
                .riskLevel(determineRiskLevel(creditScore))
                .status(LoanStatus.PENDING)
                .build();

        Loan savedLoan = loanRepository.save(loan);
        log.info("Loan application created: {}", loanId);

        return mapToResponse(savedLoan);
    }

    public LoanResponse getLoan(String loanId) {
        Loan loan = loanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        return mapToResponse(loan);
    }

    public List<LoanResponse> getCustomerLoans(String customerId) {
        return loanRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanResponse approveLoan(String loanId, String approvedBy) {
        Loan loan = loanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));

        loan.approve(approvedBy);
        Loan savedLoan = loanRepository.save(loan);

        log.info("Loan approved: {} by {}", loanId, approvedBy);
        return mapToResponse(savedLoan);
    }

    @Transactional
    public LoanResponse rejectLoan(String loanId, String reviewedBy, String reason) {
        Loan loan = loanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));

        loan.reject(reviewedBy, reason);
        Loan savedLoan = loanRepository.save(loan);

        log.info("Loan rejected: {} by {}", loanId, reviewedBy);
        return mapToResponse(savedLoan);
    }

    @Transactional
    public LoanResponse disburseLoan(String loanId, String transferReference) {
        Loan loan = loanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new RuntimeException("Loan must be approved before disbursement");
        }

        loan.disburse(transferReference);
        loan.activate();

        // Set next payment date (30 days from now)
        loan.setNextPaymentDate(LocalDateTime.now().plusDays(30));
        loan.setEndDate(LocalDateTime.now().plusMonths(loan.getTermMonths()));

        Loan savedLoan = loanRepository.save(loan);

        log.info("Loan disbursed: {} with reference {}", loanId, transferReference);
        return mapToResponse(savedLoan);
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, Integer months) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = onePlusRate.pow(months);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private Integer calculateCreditScore(String customerId) {
        // Simplified credit score calculation
        // In reality, this would call a credit bureau API
        return 650 + (int)(Math.random() * 200); // 650-850
    }

    private String determineRiskLevel(Integer creditScore) {
        if (creditScore >= 750) return "LOW";
        if (creditScore >= 650) return "MEDIUM";
        return "HIGH";
    }

    private LoanResponse mapToResponse(Loan loan) {
        return LoanResponse.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomerId())
                .accountNumber(loan.getAccountNumber())
                .loanType(loan.getLoanType())
                .amount(loan.getAmount())
                .interestRate(loan.getInterestRate())
                .termMonths(loan.getTermMonths())
                .monthlyPayment(loan.getMonthlyPayment())
                .totalAmount(loan.getTotalAmount())
                .status(loan.getStatus())
                .creditScore(loan.getCreditScore())
                .createdAt(loan.getCreatedAt())
                .startDate(loan.getStartDate())
                .nextPaymentDate(loan.getNextPaymentDate())
                .paymentsMade(loan.getPaymentsMade())
                .amountPaid(loan.getAmountPaid())
                .outstandingBalance(loan.getOutstandingBalance())
                .build();
    }
}
