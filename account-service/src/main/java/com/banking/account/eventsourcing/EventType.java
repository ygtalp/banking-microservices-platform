package com.banking.account.eventsourcing;

/**
 * Event Types for Event Sourcing
 */
public enum EventType {
    // Account Lifecycle Events
    ACCOUNT_CREATED,
    ACCOUNT_ACTIVATED,
    ACCOUNT_SUSPENDED,
    ACCOUNT_CLOSED,

    // Balance Events
    BALANCE_CREDITED,
    BALANCE_DEBITED,
    BALANCE_UPDATED,

    // Configuration Events
    ACCOUNT_TYPE_CHANGED,
    CURRENCY_CHANGED,

    // Metadata Events
    ACCOUNT_METADATA_UPDATED
}
