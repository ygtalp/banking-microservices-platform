package com.banking.transaction.model;

public enum TransactionStatus {
    COMPLETED,      // Transaction completed successfully
    PENDING,        // Transaction in progress
    FAILED,         // Transaction failed
    REVERSED        // Transaction reversed/compensated
}
