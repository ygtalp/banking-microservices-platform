package com.banking.loan.model;

public enum LoanStatus {
    PENDING,           // Application submitted, awaiting review
    UNDER_REVIEW,      // Being reviewed
    APPROVED,          // Approved, awaiting disbursement
    REJECTED,          // Application rejected
    DISBURSED,         // Funds disbursed to account
    ACTIVE,            // Loan is active, payments ongoing
    PAID_OFF,          // Loan fully paid
    DEFAULTED,         // Payment defaulted
    CANCELLED          // Cancelled before disbursement
}
