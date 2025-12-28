package com.banking.customer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_history", indexes = {
        @Index(name = "idx_history_customer_id", columnList = "customer_id"),
        @Index(name = "idx_history_timestamp", columnList = "timestamp"),
        @Index(name = "idx_history_operation", columnList = "operation")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 50)
    private String operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private CustomerStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private CustomerStatus newStatus;

    @Column(length = 1000)
    private String description;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
