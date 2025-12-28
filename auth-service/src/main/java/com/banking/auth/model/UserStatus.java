package com.banking.auth.model;

public enum UserStatus {
    ACTIVE,      // User can login and use the system
    SUSPENDED,   // Temporarily suspended (can be reactivated)
    LOCKED,      // Locked due to failed login attempts
    INACTIVE     // Deactivated account
}
