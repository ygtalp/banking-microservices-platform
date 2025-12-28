package com.banking.auth.exception;

/**
 * Exception thrown when attempting to register with an email that already exists
 */
public class EmailAlreadyExistsException extends AuthException {

    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email, "EMAIL_ALREADY_EXISTS");
    }
}
