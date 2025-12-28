package com.banking.customer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVerifiedEvent {

    private String eventType = "CUSTOMER_VERIFIED";
    private String customerId;
    private String email;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime timestamp;
}
