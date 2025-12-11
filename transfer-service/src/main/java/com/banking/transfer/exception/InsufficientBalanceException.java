package com.banking.transfer.exception;

public class InsufficientBalanceException extends TransferException {
    public InsufficientBalanceException(String accountNumber) {
        super("Insufficient balance in account: " + accountNumber);
    }
}