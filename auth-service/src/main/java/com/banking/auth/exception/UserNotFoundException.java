package com.banking.auth.exception;

/**
 * Exception thrown when user is not found
 */
public class UserNotFoundException extends AuthException {

    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier, "USER_NOT_FOUND");
    }

    public UserNotFoundException() {
        super("User not found", "USER_NOT_FOUND");
    }
}
