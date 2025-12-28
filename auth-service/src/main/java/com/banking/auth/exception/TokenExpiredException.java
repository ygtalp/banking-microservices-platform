package com.banking.auth.exception;

/**
 * Exception thrown when JWT token has expired
 */
public class TokenExpiredException extends AuthException {

    public TokenExpiredException(String message) {
        super(message, "TOKEN_EXPIRED");
    }

    public TokenExpiredException() {
        super("Token has expired", "TOKEN_EXPIRED");
    }
}
