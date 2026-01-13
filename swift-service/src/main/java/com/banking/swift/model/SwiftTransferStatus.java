package com.banking.swift.model;

public enum SwiftTransferStatus {
    PENDING,           // Initial state
    VALIDATING,        // Validating BIC codes, amounts, etc.
    COMPLIANCE_CHECK,  // Running OFAC/sanctions checks
    PROCESSING,        // Generating MT103 message
    SUBMITTED,         // Submitted to SWIFT network
    ACKNOWLEDGED,      // Acknowledged by correspondent bank
    SETTLED,           // Settlement completed
    COMPLETED,         // Final success state
    FAILED,            // Failed (various reasons)
    REJECTED,          // Rejected by correspondent or beneficiary bank
    CANCELLED          // Cancelled by user/system
}