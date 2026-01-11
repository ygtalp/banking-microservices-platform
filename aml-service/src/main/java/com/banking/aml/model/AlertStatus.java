package com.banking.aml.model;

public enum AlertStatus {
    OPEN,           // Alert created, awaiting review
    UNDER_REVIEW,   // Alert being investigated
    ESCALATED,      // Alert escalated to compliance team
    CLEARED,        // Alert cleared after review
    FILED_SAR,      // Suspicious Activity Report filed
    CLOSED          // Alert closed (not suspicious)
}
