package com.banking.transfer.model;

public enum TransferStatus {
    PENDING,              // Initial state
    VALIDATING,          // Validating accounts
    DEBIT_PENDING,       // Debit initiated
    DEBIT_COMPLETED,     // Debit successful
    CREDIT_PENDING,      // Credit initiated
    COMPLETED,           // Transfer successful
    FAILED,              // Transfer failed
    COMPENSATING,        // Rollback in progress
    COMPENSATED          // Rollback completed
}