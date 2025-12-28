package com.banking.auth.exception;

/**
 * Exception thrown when attempting to use a blacklisted token
 */
public class TokenBlacklistedException extends AuthException {

    public TokenBlacklistedException() {
        super("Token has been blacklisted", "TOKEN_BLACKLISTED");
    }

    public TokenBlacklistedException(String message) {
        super(message, "TOKEN_BLACKLISTED");
    }
}
