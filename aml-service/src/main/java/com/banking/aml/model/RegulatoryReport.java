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
import java.time.LocalDateTime;

@Entity
@Table(name = "regulatory_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RegulatoryReport implements Serializable {

    @Id
    @Column(name = "report_id", length = 50)
    private String reportId;

    @Column(name = "report_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReportType reportType; // STR, SAR, CTR, GOAML

    @Column(name = "report_number", unique = true, length = 50)
    private String reportNumber; // Official report number (e.g., SAR-2026-001234)

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.DRAFT;

    // Related Entities
    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "alert_id", length = 50)
    private String alertId; // Link to AML alert

    @Column(name = "case_id", length = 50)
    private String caseId; // Link to AML case

    // Transaction Details
    @Column(name = "transaction_references", columnDefinition = "TEXT")
    private String transactionReferences; // Comma-separated list of transfer references

    @Column(name = "suspicious_amount", precision = 19, scale = 2)
    private BigDecimal suspiciousAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "transaction_date_from")
    private LocalDateTime transactionDateFrom;

    @Column(name = "transaction_date_to")
    private LocalDateTime transactionDateTo;

    // Suspicion Details
    @Column(name = "suspicion_category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SuspicionCategory suspicionCategory;

    @Column(name = "suspicion_indicators", columnDefinition = "TEXT")
    private String suspicionIndicators; // Comma-separated list

    @Column(name = "narrative", columnDefinition = "TEXT", nullable = false)
    private String narrative; // Detailed description of suspicious activity

    @Column(name = "risk_assessment", columnDefinition = "TEXT")
    private String riskAssessment;

    // Reporting Information
    @Column(name = "reporting_institution", length = 200)
    private String reportingInstitution;

    @Column(name = "reporting_officer", length = 100)
    private String reportingOfficer;

    @Column(name = "reporting_officer_email", length = 100)
    private String reportingOfficerEmail;

    @Column(name = "compliance_officer", length = 100)
    private String complianceOfficer;

    // Regulatory Authority
    @Column(name = "filed_to_authority", length = 100)
    private String filedToAuthority; // e.g., "FinCEN", "FIU Netherlands", "OFAC"

    @Column(name = "authority_reference_number", length = 50)
    private String authorityReferenceNumber; // Reference from authority

    @Column(name = "filed_at")
    private LocalDateTime filedAt;

    @Column(name = "acknowledgment_received_at")
    private LocalDateTime acknowledgmentReceivedAt;

    // Supporting Documents
    @Column(name = "has_supporting_documents", nullable = false)
    private Boolean hasSupportingDocuments = false;

    @Column(name = "document_count")
    private Integer documentCount = 0;

    @Column(name = "document_paths", columnDefinition = "TEXT")
    private String documentPaths; // JSON or comma-separated file paths

    // Export Information
    @Column(name = "exported_format", length = 20)
    @Enumerated(EnumType.STRING)
    private ExportFormat exportedFormat; // PDF, XML, GOAML

    @Column(name = "exported_file_path", length = 500)
    private String exportedFilePath;

    @Column(name = "exported_at")
    private LocalDateTime exportedAt;

    // Audit Trail
    @Column(name = "prepared_by", length = 100)
    private String preparedBy;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (reportId == null) {
            reportId = generateReportId();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private String generateReportId() {
        String prefix = reportType != null ? reportType.name() : "REP";
        return prefix + "-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    public enum ReportType {
        STR,    // Suspicious Transaction Report
        SAR,    // Suspicious Activity Report
        CTR,    // Currency Transaction Report (>$10,000)
        GOAML   // goAML format (UN standard)
    }

    public enum ReportStatus {
        DRAFT,
        PENDING_REVIEW,
        REVIEWED,
        PENDING_APPROVAL,
        APPROVED,
        FILED,
        ACKNOWLEDGED,
        REJECTED,
        CANCELLED
    }

    public enum SuspicionCategory {
        STRUCTURING,              // Breaking up transactions to avoid reporting
        MONEY_LAUNDERING,         // Suspected money laundering
        TERRORISM_FINANCING,      // Terrorist financing
        FRAUD,                    // Fraudulent activity
        SANCTIONS_EVASION,        // Evading sanctions
        TAX_EVASION,             // Tax evasion
        CORRUPTION,              // Bribery, corruption
        HUMAN_TRAFFICKING,       // Human trafficking proceeds
        DRUG_TRAFFICKING,        // Drug trafficking proceeds
        ARMS_TRAFFICKING,        // Arms trafficking
        CYBERCRIME,              // Cybercrime proceeds
        IDENTITY_THEFT,          // Identity theft
        UNUSUAL_TRANSACTION,     // Unusual transaction pattern
        HIGH_RISK_COUNTRY,       // Transactions with high-risk countries
        PEP_RELATED,            // Related to Politically Exposed Person
        OTHER                    // Other suspicious activity
    }

    public enum ExportFormat {
        PDF,
        XML,
        GOAML,
        JSON
    }
}
