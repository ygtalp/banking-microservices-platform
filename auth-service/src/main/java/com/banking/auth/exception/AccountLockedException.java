package com.banking.auth.exception;

/**
 * Exception thrown when attempting to authenticate with a locked account
 */
public class AccountLockedException extends AuthException {

    public AccountLockedException(String message) {
        super(message, "ACCOUNT_LOCKED");
    }

    public AccountLockedException() {
        super("Account is locked due to too many failed login attempts", "ACCOUNT_LOCKED");
    }
}
