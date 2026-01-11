package com.banking.aml.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "aml_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AmlCase implements Serializable {

    @Id
    @Column(name = "case_id", length = 50)
    private String caseId;

    @Column(name = "case_number", unique = true, nullable = false, length = 50)
    private String caseNumber; // CASE-2026-001234

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CaseStatus status = CaseStatus.OPEN;

    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CasePriority priority = CasePriority.MEDIUM;

    // Related Entities
    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @ElementCollection
    @CollectionTable(name = "aml_case_alerts", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "alert_id")
    private List<String> alertIds = new ArrayList<>(); // Multiple alerts can be grouped

    @ElementCollection
    @CollectionTable(name = "aml_case_transactions", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "transaction_reference")
    private List<String> transactionReferences = new ArrayList<>();

    // Case Classification
    @Column(name = "case_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private CaseType caseType;

    @Column(name = "risk_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    // Case Details
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "investigation_summary", columnDefinition = "TEXT")
    private String investigationSummary;

    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    // Assignment
    @Column(name = "assigned_to", length = 100)
    private String assignedTo; // Compliance officer username/email

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    // Investigation Timeline
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "opened_by", nullable = false, length = 100)
    private String openedBy;

    @Column(name = "investigation_started_at")
    private LocalDateTime investigationStartedAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    // Resolution
    @Column(name = "resolution", length = 50)
    @Enumerated(EnumType.STRING)
    private CaseResolution resolution;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken;

    // Regulatory Reporting
    @Column(name = "requires_sar_filing", nullable = false)
    private Boolean requiresSarFiling = false;

    @Column(name = "sar_filed", nullable = false)
    private Boolean sarFiled = false;

    @Column(name = "sar_report_id", length = 50)
    private String sarReportId; // Link to RegulatoryReport

    @Column(name = "sar_filed_at")
    private LocalDateTime sarFiledAt;

    // Customer Action
    @Column(name = "customer_blocked", nullable = false)
    private Boolean customerBlocked = false;

    @Column(name = "customer_blocked_at")
    private LocalDateTime customerBlockedAt;

    @Column(name = "customer_blocked_reason", columnDefinition = "TEXT")
    private String customerBlockedReason;

    @Column(name = "relationship_terminated", nullable = false)
    private Boolean relationshipTerminated = false;

    // Escalation
    @Column(name = "escalated", nullable = false)
    private Boolean escalated = false;

    @Column(name = "escalated_to", length = 100)
    private String escalatedTo; // Senior compliance officer, MLRO

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;

    // Notes and Comments
    @ElementCollection
    @CollectionTable(name = "aml_case_notes", joinColumns = @JoinColumn(name = "case_id"))
    @OrderBy("created_at DESC")
    private List<CaseNote> notes = new ArrayList<>();

    // SLA Tracking
    @Column(name = "sla_status", length = 20)
    @Enumerated(EnumType.STRING)
    private SlaStatus slaStatus = SlaStatus.ON_TRACK;

    @Column(name = "days_open")
    private Integer daysOpen = 0;

    @Column(name = "is_overdue", nullable = false)
    private Boolean isOverdue = false;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (caseId == null) {
            caseId = "CASE-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        }
        if (caseNumber == null) {
            caseNumber = generateCaseNumber();
        }
        if (openedAt == null) {
            openedAt = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        calculateDaysOpen();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateDaysOpen();
        checkSla();
    }

    private String generateCaseNumber() {
        int year = LocalDateTime.now().getYear();
        int random = (int)(Math.random() * 999999);
        return String.format("CASE-%d-%06d", year, random);
    }

    private void calculateDaysOpen() {
        if (openedAt != null) {
            LocalDateTime endTime = closedAt != null ? closedAt : LocalDateTime.now();
            this.daysOpen = (int) java.time.Duration.between(openedAt, endTime).toDays();
        }
    }

    private void checkSla() {
        if (dueDate != null && LocalDateTime.now().isAfter(dueDate) && status != CaseStatus.CLOSED) {
            this.isOverdue = true;
            this.slaStatus = SlaStatus.OVERDUE;
        }
    }

    public void addNote(String content, String author) {
        CaseNote note = new CaseNote();
        note.setContent(content);
        note.setAuthor(author);
        note.setCreatedAt(LocalDateTime.now());
        this.notes.add(note);
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaseNote implements Serializable {
        @Column(name = "content", columnDefinition = "TEXT")
        private String content;

        @Column(name = "author", length = 100)
        private String author;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "note_type", length = 20)
        @Enumerated(EnumType.STRING)
        private NoteType noteType = NoteType.GENERAL;

        public enum NoteType {
            GENERAL,
            INVESTIGATION,
            ESCALATION,
            RESOLUTION,
            CUSTOMER_CONTACT,
            INTERNAL_REVIEW
        }
    }

    public enum CaseStatus {
        OPEN,
        INVESTIGATING,
        PENDING_REVIEW,
        ESCALATED,
        PENDING_CLOSURE,
        CLOSED,
        REOPENED
    }

    public enum CasePriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum CaseType {
        TRANSACTION_MONITORING,
        SANCTION_SCREENING,
        PEP_REVIEW,
        CUSTOMER_DUE_DILIGENCE,
        STRUCTURING,
        MONEY_LAUNDERING,
        TERRORISM_FINANCING,
        FRAUD_INVESTIGATION,
        HIGH_RISK_CUSTOMER,
        REGULATORY_INQUIRY,
        OTHER
    }

    public enum CaseResolution {
        FALSE_POSITIVE,
        JUSTIFIED_ACTIVITY,
        SUSPICIOUS_CONFIRMED,
        SAR_FILED,
        CUSTOMER_BLOCKED,
        RELATIONSHIP_TERMINATED,
        ESCALATED_TO_AUTHORITY,
        PENDING_FURTHER_INVESTIGATION,
        NO_ACTION_REQUIRED
    }

    public enum SlaStatus {
        ON_TRACK,
        AT_RISK,
        OVERDUE
    }
}
