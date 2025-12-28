package com.banking.customer.model;

public enum DocumentStatus {
    PENDING,    // Document uploaded, awaiting verification
    VERIFIED,   // Document verified and approved
    REJECTED    // Document rejected
}
