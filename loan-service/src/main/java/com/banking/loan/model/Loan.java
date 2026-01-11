package com.banking.loan.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loan_id", columnList = "loan_id"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_account_number", columnList = "account_number")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false, unique = true, length = 50)
    private String loanId;  // LON-XXXXXXXXXXXX

    // Customer Information
    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;  // Disbursement account

    // Loan Details
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;  // Principal amount

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;  // Annual interest rate (e.g., 7.50 for 7.5%)

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;  // Loan term in months

    // Calculated Fields
    @Column(name = "monthly_payment", precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "total_interest", precision = 19, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;  // Principal + Interest

    // Credit Assessment
    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;  // LOW, MEDIUM, HIGH

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    // Workflow
    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Disbursement
    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "disbursement_reference", length = 50)
    private String disbursementReference;  // Transfer reference

    // Dates
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "next_payment_date")
    private LocalDateTime nextPaymentDate;

    // Payment Tracking
    @Column(name = "payments_made", nullable = false)
    @Builder.Default
    private Integer paymentsMade = 0;

    @Column(name = "amount_paid", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "outstanding_balance", precision = 19, scale = 2)
    private BigDecimal outstandingBalance;

    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper Methods
    public boolean isPending() {
        return this.status == LoanStatus.PENDING || this.status == LoanStatus.UNDER_REVIEW;
    }

    public boolean isActive() {
        return this.status == LoanStatus.ACTIVE || this.status == LoanStatus.DISBURSED;
    }

    public boolean isCompleted() {
        return this.status == LoanStatus.PAID_OFF ||
               this.status == LoanStatus.REJECTED ||
               this.status == LoanStatus.CANCELLED ||
               this.status == LoanStatus.DEFAULTED;
    }

    public void approve(String approvedBy) {
        this.status = LoanStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(String reviewedBy, String reason) {
        this.status = LoanStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public void disburse(String transferReference) {
        this.status = LoanStatus.DISBURSED;
        this.disbursedAt = LocalDateTime.now();
        this.disbursementReference = transferReference;
        this.startDate = LocalDateTime.now();
        this.outstandingBalance = this.totalAmount;
    }

    public void activate() {
        this.status = LoanStatus.ACTIVE;
    }

    public void recordPayment(BigDecimal paymentAmount) {
        this.paymentsMade++;
        this.amountPaid = this.amountPaid.add(paymentAmount);
        this.outstandingBalance = this.outstandingBalance.subtract(paymentAmount);

        if (this.outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = LoanStatus.PAID_OFF;
        }
    }
}
