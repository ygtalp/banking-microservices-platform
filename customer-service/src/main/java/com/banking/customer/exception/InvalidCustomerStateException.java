package com.banking.customer.exception;

public class InvalidCustomerStateException extends RuntimeException {
    public InvalidCustomerStateException(String message) {
        super(message);
    }
}
