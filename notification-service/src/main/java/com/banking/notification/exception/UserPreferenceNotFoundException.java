package com.banking.notification.exception;

public class UserPreferenceNotFoundException extends RuntimeException {
    public UserPreferenceNotFoundException(String message) {
        super(message);
    }

    public UserPreferenceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
