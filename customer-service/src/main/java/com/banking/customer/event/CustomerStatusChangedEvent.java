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
public class CustomerStatusChangedEvent {

    private String eventType = "CUSTOMER_STATUS_CHANGED";
    private String customerId;
    private CustomerStatus previousStatus;
    private CustomerStatus newStatus;
    private String reason;
    private LocalDateTime timestamp;
}
