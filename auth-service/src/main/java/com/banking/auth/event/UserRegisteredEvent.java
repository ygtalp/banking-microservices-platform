package com.banking.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a new user registers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private LocalDateTime registeredAt;
    private String eventId;
    private String eventType = "USER_REGISTERED";
}
