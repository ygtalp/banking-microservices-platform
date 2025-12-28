package com.banking.customer.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents", indexes = {
        @Index(name = "idx_kyc_customer_id", columnList = "customer_id"),
        @Index(name = "idx_kyc_status", columnList = "status"),
        @Index(name = "idx_kyc_document_type", columnList = "document_type"),
        @Index(name = "idx_kyc_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "issuing_authority", length = 100)
    private String issuingAuthority;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = DocumentStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Business Logic Methods

    /**
     * Verify the document
     */
    public void verify(String verifiedBy) {
        if (this.status == DocumentStatus.VERIFIED) {
            throw new IllegalStateException("Document is already verified");
        }
        this.status = DocumentStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = verifiedBy;
        this.rejectionReason = null;
    }

    /**
     * Reject the document
     */
    public void reject(String reason) {
        if (this.status == DocumentStatus.VERIFIED) {
            throw new IllegalStateException("Cannot reject a verified document");
        }
        this.status = DocumentStatus.REJECTED;
        this.rejectionReason = reason;
    }

    /**
     * Check if document is expired
     */
    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Check if document is verified
     */
    public boolean isVerified() {
        return this.status == DocumentStatus.VERIFIED;
    }

    /**
     * Check if document is pending
     */
    public boolean isPending() {
        return this.status == DocumentStatus.PENDING;
    }
}
