package com.banking.statement.model;

public enum StatementStatus {
    GENERATING,     // PDF generation in progress
    GENERATED,      // PDF generated successfully
    SENT,           // Sent via email/notification
    FAILED,         // Generation failed
    DOWNLOADED      // Downloaded by user
}
