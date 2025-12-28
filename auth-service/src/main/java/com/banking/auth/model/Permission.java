package com.banking.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "permissions",
       uniqueConstraints = @UniqueConstraint(name = "uk_resource_action", columnNames = {"resource", "action"}),
       indexes = {
               @Index(name = "idx_resource", columnList = "resource"),
               @Index(name = "idx_action", columnList = "action")
       })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String resource;  // e.g., "accounts", "transfers", "customers"

    @Column(nullable = false, length = 50)
    private String action;    // e.g., "read", "write", "delete", "approve"

    @Column(length = 255)
    private String description;

    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Returns permission in format "resource:action" (e.g., "accounts:read")
     */
    public String getPermissionString() {
        return resource + ":" + action;
    }
}
