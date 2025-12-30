package com.banking.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVerifiedEvent {
    private String customerId;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
}
