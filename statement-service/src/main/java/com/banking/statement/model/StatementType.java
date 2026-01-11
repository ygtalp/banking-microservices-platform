package com.banking.statement.model;

public enum StatementType {
    MONTHLY,        // Monthly account statement
    QUARTERLY,      // Quarterly summary
    ANNUAL,         // Annual summary
    CUSTOM,         // Custom date range
    TAX,            // Tax statement
    TRANSACTION     // Transaction-specific statement
}
