package com.banking.fraud.exception;

public class FraudCheckNotFoundException extends RuntimeException {
    public FraudCheckNotFoundException(String message) {
        super(message);
    }
}
