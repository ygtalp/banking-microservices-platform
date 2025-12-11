package com.banking.transfer.exception;

public class SagaCompensationException extends TransferException {
    public SagaCompensationException(String message) {
        super(message);
    }

    public SagaCompensationException(String message, Throwable cause) {
        super(message, cause);
    }
}