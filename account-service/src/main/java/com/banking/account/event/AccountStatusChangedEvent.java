package com.banking.account.event;

import com.banking.account.model.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AccountStatusChangedEvent extends AccountEvent {

    private AccountStatus previousStatus;
    private AccountStatus newStatus;
    private String reason;
}