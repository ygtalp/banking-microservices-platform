package com.banking.transfer.exception;

public class TransferNotFoundException extends TransferException {
    public TransferNotFoundException(String transferReference) {
        super("Transfer not found: " + transferReference);
    }
}