package com.banking.aml.model;

public enum AlertType {
    SANCTIONS_MATCH,        // Match with sanctions list
    VELOCITY_ALERT,         // High transaction velocity
    STRUCTURING,            // Potential structuring (amounts just below threshold)
    ROUND_AMOUNT,           // Unusual round amount pattern
    DAILY_LIMIT_EXCEEDED,   // Daily transaction limit exceeded
    PATTERN_ANOMALY,        // Unusual transaction pattern
    HIGH_RISK_COUNTRY,      // Transaction to/from high-risk country
    PEP_INVOLVEMENT,        // Politically Exposed Person involved
    DORMANT_ACCOUNT,        // Sudden activity on dormant account
    RAPID_MOVEMENT          // Rapid movement of funds
}
