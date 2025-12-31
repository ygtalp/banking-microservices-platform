package com.banking.fraud.model;

public enum RuleType {
    VELOCITY,       // Multiple transactions in short time
    AMOUNT,         // High amount threshold
    PATTERN,        // Unusual pattern detection
    TIME,           // Time-based rules (night hours)
    DAILY_LIMIT     // Daily transaction limit
}
