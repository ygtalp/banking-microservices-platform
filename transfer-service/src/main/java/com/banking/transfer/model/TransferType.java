package com.banking.transfer.model;

public enum TransferType {
    INTERNAL,     // Same bank transfer
    EXTERNAL,     // Different bank transfer (future)
    INSTANT,      // Instant transfer
    SCHEDULED     // Scheduled transfer (future)
}