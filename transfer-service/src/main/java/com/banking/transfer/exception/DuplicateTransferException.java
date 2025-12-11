package com.banking.transfer.exception;

public class DuplicateTransferException extends TransferException {
    public DuplicateTransferException(String idempotencyKey) {
        super("Transfer with idempotency key already exists: " + idempotencyKey);
    }
}