package com.banking.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a user logs out
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoggedOutEvent {

    private String userId;
    private String email;
    private LocalDateTime logoutAt;
    private String eventId;
    private String eventType = "USER_LOGGED_OUT";
}
