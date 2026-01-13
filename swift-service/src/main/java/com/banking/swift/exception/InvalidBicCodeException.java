package com.banking.swift.exception;

/**
 * Exception thrown when an invalid BIC code is provided
 */
public class InvalidBicCodeException extends RuntimeException {
    
    public InvalidBicCodeException(String message) {
        super(message);
    }
    
    public InvalidBicCodeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static InvalidBicCodeException forBic(String bic) {
        return new InvalidBicCodeException("Invalid BIC code: " + bic);
    }
    
    public static InvalidBicCodeException forBic(String bic, String reason) {
        return new InvalidBicCodeException("Invalid BIC code: " + bic + ". Reason: " + reason);
    }
}
