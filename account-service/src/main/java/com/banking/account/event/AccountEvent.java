package com.banking.account.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AccountEvent {

    private String eventId;
    private String accountNumber;
    private String customerId;
    private LocalDateTime timestamp;

    public AccountEvent(String accountNumber, String customerId) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.timestamp = LocalDateTime.now();
    }
}