package com.banking.auth.exception;

/**
 * Exception thrown when password validation fails
 */
public class InvalidPasswordException extends AuthException {

    public InvalidPasswordException(String message) {
        super(message, "INVALID_PASSWORD");
    }
}
