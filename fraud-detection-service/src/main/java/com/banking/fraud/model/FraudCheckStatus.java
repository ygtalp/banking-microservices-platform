package com.banking.fraud.model;

public enum FraudCheckStatus {
    PASSED,         // Transaction passed all checks
    FLAGGED,        // Transaction flagged for review
    BLOCKED,        // Transaction blocked due to high risk
    UNDER_REVIEW,   // Manual review in progress
    CLEARED         // Reviewed and cleared by admin
}
