package com.banking.account.eventsourcing;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Account Event Entity - Event Store
 * Stores all state changes as immutable events
 */
@Entity
@Table(name = "account_events", indexes = {
        @Index(name = "idx_account_number", columnList = "account_number"),
        @Index(name = "idx_aggregate_version", columnList = "aggregate_version"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Aggregate ID (Account Number)
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    // Event Metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "aggregate_version", nullable = false)
    private Long aggregateVersion;  // Version of the account after this event

    // Event Data (JSON)
    @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
    private String eventData;  // JSON representation of the event

    // Audit Fields
    @Column(name = "user_id", length = 50)
    private String userId;  // Who triggered this event

    @Column(name = "correlation_id", length = 100)
    private String correlationId;  // For tracking related events

    @CreatedDate
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Metadata
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  // Additional context (IP address, source system, etc.)
}
