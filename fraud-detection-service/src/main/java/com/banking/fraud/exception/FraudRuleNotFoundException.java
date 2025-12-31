package com.banking.fraud.exception;

public class FraudRuleNotFoundException extends RuntimeException {
    public FraudRuleNotFoundException(String message) {
        super(message);
    }
}
