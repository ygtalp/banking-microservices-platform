package com.banking.customer.event;

import com.banking.customer.model.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreatedEvent {

    private String eventType = "CUSTOMER_CREATED";
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String nationalId;
    private CustomerStatus status;
    private LocalDateTime timestamp;
}
