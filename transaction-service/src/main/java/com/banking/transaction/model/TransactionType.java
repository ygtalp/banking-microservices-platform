package com.banking.transaction.model;

public enum TransactionType {
    CREDIT,              // Money added to account
    DEBIT,              // Money removed from account
    TRANSFER_DEBIT,     // Money sent via transfer
    TRANSFER_CREDIT,    // Money received via transfer
    OPENING_BALANCE,    // Initial account balance
    ADJUSTMENT          // Manual adjustment
}
