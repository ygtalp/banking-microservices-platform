package com.banking.transfer.exception;

public class InvalidTransferException extends TransferException {
    public InvalidTransferException(String message) {
        super(message);
    }
}