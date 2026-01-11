package com.banking.auth.model;

/**
 * Multi-Factor Authentication Methods
 * Supported authentication methods for second factor
 */
public enum MfaMethod {
    /**
     * Time-based One-Time Password (Google Authenticator, Authy, etc.)
     */
    TOTP,

    /**
     * SMS-based One-Time Password
     */
    SMS,

    /**
     * Email-based One-Time Password
     */
    EMAIL,

    /**
     * Backup codes (recovery codes)
     */
    BACKUP_CODE
}
