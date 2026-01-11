package com.banking.aml.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sanction_lists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SanctionList implements Serializable {

    @Id
    @Column(name = "sanction_id", length = 50)
    private String sanctionId;

    @Column(name = "list_name", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SanctionListName listName; // OFAC, EU, UN, UK, etc.

    @Column(name = "entity_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SanctionEntityType entityType; // INDIVIDUAL, ENTITY, VESSEL, AIRCRAFT

    @Column(name = "sanctioned_name", nullable = false, length = 200)
    private String sanctionedName;

    @Column(name = "alias_names", columnDefinition = "TEXT")
    private String aliasNames; // Comma-separated aliases

    @Column(name = "entity_id", length = 50)
    private String entityId; // Official ID from sanction list

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "passport_number", length = 50)
    private String passportNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "place_of_birth", length = 100)
    private String placeOfBirth;

    @Column(name = "nationality", length = 50)
    private String nationality;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "sanction_type", length = 50)
    private String sanctionType; // e.g., "Financial Sanctions", "Travel Ban"

    @Column(name = "sanction_reason", columnDefinition = "TEXT")
    private String sanctionReason;

    @Column(name = "listed_date")
    private LocalDate listedDate;

    @Column(name = "delisted_date")
    private LocalDate delistedDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "program", length = 100)
    private String program; // e.g., "IRAN", "SYRIA", "TERRORISM"

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "last_refreshed_at")
    private LocalDateTime lastRefreshedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (sanctionId == null) {
            sanctionId = "SANC-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    public enum SanctionListName {
        OFAC,      // US Office of Foreign Assets Control
        EU,        // European Union Sanctions
        UN,        // United Nations Security Council
        UK_HMT,    // UK Her Majesty's Treasury
        INTERPOL,  // Interpol Red Notices
        WORLD_BANK // World Bank Debarred Entities
    }

    public enum SanctionEntityType {
        INDIVIDUAL,
        ENTITY,      // Company, organization
        VESSEL,      // Ship
        AIRCRAFT
    }
}
