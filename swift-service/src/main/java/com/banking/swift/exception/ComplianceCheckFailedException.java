package com.banking.swift.exception;

/**
 * Exception thrown when a compliance check fails
 */
public class ComplianceCheckFailedException extends RuntimeException {
    
    public ComplianceCheckFailedException(String message) {
        super(message);
    }
    
    public ComplianceCheckFailedException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static ComplianceCheckFailedException ofacFailed(String reference) {
        return new ComplianceCheckFailedException(
            "OFAC compliance check failed for transfer: " + reference
        );
    }
    
    public static ComplianceCheckFailedException sanctionsFailed(String reference) {
        return new ComplianceCheckFailedException(
            "Sanctions screening failed for transfer: " + reference
        );
    }
    
    public static ComplianceCheckFailedException forReason(String reference, String reason) {
        return new ComplianceCheckFailedException(
            "Compliance check failed for transfer " + reference + ": " + reason
        );
    }
}
