package com.banking.swift.exception;

/**
 * Exception thrown when a SWIFT transfer is not found
 */
public class SwiftTransferNotFoundException extends RuntimeException {
    
    public SwiftTransferNotFoundException(String message) {
        super(message);
    }
    
    public SwiftTransferNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static SwiftTransferNotFoundException byReference(String reference) {
        return new SwiftTransferNotFoundException("SWIFT transfer not found with reference: " + reference);
    }
    
    public static SwiftTransferNotFoundException byId(Long id) {
        return new SwiftTransferNotFoundException("SWIFT transfer not found with ID: " + id);
    }
}
