package com.banking.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a user changes their password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPasswordChangedEvent {

    private String userId;
    private String email;
    private LocalDateTime changedAt;
    private String eventId;
    private String eventType = "USER_PASSWORD_CHANGED";
}
