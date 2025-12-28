package com.banking.customer.model;

public enum CustomerStatus {
    PENDING_VERIFICATION,  // Just registered, waiting for KYC documents
    VERIFIED,              // KYC documents uploaded and verified
    APPROVED,              // Fully approved, can create accounts
    SUSPENDED,             // Temporarily suspended
    CLOSED                 // Permanently closed
}
