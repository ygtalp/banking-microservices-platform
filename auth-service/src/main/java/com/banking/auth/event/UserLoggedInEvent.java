package com.banking.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a user logs in successfully
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoggedInEvent {

    private String userId;
    private String email;
    private LocalDateTime loginAt;
    private String ipAddress;
    private String userAgent;
    private String eventId;
    private String eventType = "USER_LOGGED_IN";
}
