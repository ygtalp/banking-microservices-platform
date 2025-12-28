package com.banking.auth.exception;

/**
 * Exception thrown when JWT token is invalid
 */
public class InvalidTokenException extends AuthException {

    public InvalidTokenException(String message) {
        super(message, "INVALID_TOKEN");
    }

    public InvalidTokenException() {
        super("Invalid or malformed token", "INVALID_TOKEN");
    }
}
