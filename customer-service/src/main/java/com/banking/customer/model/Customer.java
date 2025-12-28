package com.banking.customer.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_national_id", columnList = "national_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true, length = 50)
    private String customerId;

    // Personal Information
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    // KYC Information
    @Column(name = "national_id", nullable = false, unique = true, length = 20)
    private String nationalId;

    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String country;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    // Status Management
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerStatus status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    // Risk & Compliance
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    // Audit Fields
    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = CustomerStatus.PENDING_VERIFICATION;
        }
    }

    // Business Logic Methods

    /**
     * Verify customer - marks as VERIFIED after KYC documents are verified
     */
    public void verify(String verifiedBy) {
        if (this.status != CustomerStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException(
                "Customer must be in PENDING_VERIFICATION status to be verified. Current status: " + this.status
            );
        }
        this.status = CustomerStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = verifiedBy;
    }

    /**
     * Approve customer - final approval, can now create bank accounts
     */
    public void approve(String approvedBy, RiskLevel riskLevel) {
        if (this.status != CustomerStatus.VERIFIED) {
            throw new IllegalStateException(
                "Customer must be VERIFIED before approval. Current status: " + this.status
            );
        }
        this.status = CustomerStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = approvedBy;
        this.riskLevel = riskLevel;
    }

    /**
     * Suspend customer - temporarily suspend all operations
     */
    public void suspend(String reason) {
        if (this.status == CustomerStatus.CLOSED) {
            throw new IllegalStateException("Cannot suspend a closed customer");
        }
        this.status = CustomerStatus.SUSPENDED;
        this.statusReason = reason;
    }

    /**
     * Reactivate customer - move back to APPROVED status
     */
    public void reactivate() {
        if (this.status != CustomerStatus.SUSPENDED) {
            throw new IllegalStateException(
                "Only suspended customers can be reactivated. Current status: " + this.status
            );
        }
        // Restore previous status - if they were approved before suspension
        if (this.approvedAt != null) {
            this.status = CustomerStatus.APPROVED;
        } else if (this.verifiedAt != null) {
            this.status = CustomerStatus.VERIFIED;
        } else {
            this.status = CustomerStatus.PENDING_VERIFICATION;
        }
        this.statusReason = null;
    }

    /**
     * Close customer - permanently close the customer account
     */
    public void close() {
        if (this.status == CustomerStatus.CLOSED) {
            throw new IllegalStateException("Customer is already closed");
        }
        this.status = CustomerStatus.CLOSED;
        this.statusReason = "Customer account closed";
    }

    /**
     * Check if customer can create bank accounts
     */
    public boolean canCreateAccount() {
        return this.status == CustomerStatus.APPROVED;
    }

    /**
     * Check if customer is active (not suspended or closed)
     */
    public boolean isActive() {
        return this.status == CustomerStatus.APPROVED ||
               this.status == CustomerStatus.VERIFIED ||
               this.status == CustomerStatus.PENDING_VERIFICATION;
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Mask national ID for display (show first 3 and last 4 digits)
     */
    public String getMaskedNationalId() {
        if (nationalId == null || nationalId.length() < 7) {
            return "***";
        }
        String first = nationalId.substring(0, 3);
        String last = nationalId.substring(nationalId.length() - 4);
        return first + "****" + last;
    }
}
