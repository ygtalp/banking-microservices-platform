package com.banking.transfer.exception;

public class AccountNotFoundException extends TransferException {
    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}