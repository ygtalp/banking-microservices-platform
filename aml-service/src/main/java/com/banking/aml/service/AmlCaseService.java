package com.banking.aml.service;

import com.banking.aml.event.AmlCaseEscalatedEvent;
import com.banking.aml.model.AmlAlert;
import com.banking.aml.model.AmlCase;
import com.banking.aml.repository.AmlAlertRepository;
import com.banking.aml.repository.AmlCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmlCaseService {

    private final AmlCaseRepository amlCaseRepository;
    private final AmlAlertRepository amlAlertRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CustomerRiskScoringService customerRiskScoringService;

    /**
     * Create case from alert
     */
    @Transactional
    public AmlCase createCaseFromAlert(String alertId, AmlCase.CasePriority priority, String openedBy) {
        log.info("Creating AML case from alert: {}", alertId);

        AmlAlert alert = amlAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        AmlCase amlCase = new AmlCase();
        amlCase.setCustomerId(alert.getCustomerId());
        amlCase.setCustomerName(alert.getCustomerName());
        amlCase.setAccountNumber(alert.getAccountNumber());
        amlCase.setRiskLevel(alert.getRiskLevel());
        amlCase.setPriority(priority);
        amlCase.setOpenedBy(openedBy);
        amlCase.setOpenedAt(LocalDateTime.now());

        // Add alert to case
        amlCase.getAlertIds().add(alertId);

        // Determine case type from alert type
        amlCase.setCaseType(mapAlertTypeToCaseType(alert.getAlertType()));

        // Create title and description
        amlCase.setTitle(String.format("%s - %s", alert.getAlertType(), alert.getCustomerName()));
        StringBuilder description = new StringBuilder();
        description.append("Case opened based on AML alert ").append(alertId).append("\\n");
        description.append("Alert Type: ").append(alert.getAlertType()).append("\\n");
        description.append("Risk Score: ").append(alert.getRiskScore()).append("\\n");
        if (alert.getReasons() != null && !alert.getReasons().isEmpty()) {
            description.append("Reasons:\\n");
            alert.getReasons().forEach(reason -> description.append("- ").append(reason).append("\\n"));
        }
        amlCase.setDescription(description.toString());

        // Set due date based on priority
        amlCase.setDueDate(calculateDueDate(priority));

        // Add transaction reference if available
        if (alert.getTransferReference() != null) {
            amlCase.getTransactionReferences().add(alert.getTransferReference());
        }

        // Determine if SAR filing is required based on risk level
        if (alert.getRiskLevel().name().equals("HIGH") || alert.getRiskLevel().name().equals("CRITICAL")) {
            amlCase.setRequiresSarFiling(true);
        }

        AmlCase saved = amlCaseRepository.save(amlCase);
        log.info("Created AML case: {} from alert: {}", saved.getCaseNumber(), alertId);

        return saved;
    }

    /**
     * Assign case to compliance officer
     */
    @Transactional
    public AmlCase assignCase(String caseId, String assignedTo, String assignedBy) {
        AmlCase amlCase = amlCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        amlCase.setAssignedTo(assignedTo);
        amlCase.setAssignedAt(LocalDateTime.now());
        amlCase.setAssignedBy(assignedBy);

        amlCase.addNote("Case assigned to " + assignedTo, assignedBy);

        log.info("Case {} assigned to {} by {}", amlCase.getCaseNumber(), assignedTo, assignedBy);

        return amlCaseRepository.save(amlCase);
    }

    /**
     * Start investigation
     */
    @Transactional
    public AmlCase startInvestigation(String caseId, String investigator) {
        AmlCase amlCase = amlCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        amlCase.setStatus(AmlCase.CaseStatus.INVESTIGATING);
        amlCase.setInvestigationStartedAt(LocalDateTime.now());

        amlCase.addNote("Investigation started", investigator);

        log.info("Investigation started for case: {}", amlCase.getCaseNumber());

        return amlCaseRepository.save(amlCase);
    }

    /**
     * Add note to case
     */
    @Transactional
    public AmlCase addNote(String caseId, String content, String author, AmlCase.CaseNote.NoteType noteType) {
        AmlCase amlCase = amlCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        AmlCase.CaseNote note = new AmlCase.CaseNote();
        note.setContent(content);
        note.setAuthor(author);
        note.setCreatedAt(LocalDateTime.now());
        note.setNoteType(noteType);

        amlCase.getNotes().add(note);

        return amlCaseRepository.save(amlCase);
    }

    /**
     * Escalate case
     */
    @Transactional
    public AmlCase escalateCase(String caseId, String escalatedTo, String escalationReason, String escalatedBy) {
        AmlCase amlCase = amlCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        amlCase.setEscalated(true);
        amlCase.setEscalatedTo(escalatedTo);
        amlCase.setEscalatedAt(LocalDateTime.now());
        amlCase.setEscalationReason(escalationReason);
        amlCase.setStatus(AmlCase.CaseStatus.ESCALATED);
        amlCase.setPriority(AmlCase.CasePriority.CRITICAL);

        amlCase.addNote("Case escalated to " + escalatedTo + ". Reason: " + escalationReason, escalatedBy);

        // Publish escalation event
        publishEscalationEvent(amlCase);

        log.warn("Case {} escalated to {} by {}", amlCase.getCaseNumber(), escalatedTo, escalatedBy);

        return amlCaseRepository.save(amlCase);
    }

    /**
     * Block customer based on case findings
     */
    @Transactional
    public AmlCase blockCustomer(String caseId, String reason, String blockedBy) {
        AmlCase amlCase = amlCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        amlCase.setCustomerBlocked(true);
        amlCase.setCustomerBlockedAt(LocalDateTime.now());
        amlCase.setCustomerBlockedReason(reason);

        amlCase.addNote("Customer blocked. Reason: " + reason, blockedBy);

        // Update customer risk profile
        customerRiskScoringService.blockCustomer(amlCase.getCustomerId(), reason);

        log.warn("Customer {} blocked by case {}", amlCase.getCustomerId(), amlCase.getCaseNumber());

        return amlCaseRepository.save(amlCase);
    }

    /**
     * Terminate customer relationship
     */
    @Transactional
    public AmlCase terminateRelationship(String caseId, String reason, String terminatedBy) {
        AmlCase amlCase = amlCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        amlCase.setRelationshipTerminated(true);
        amlCase.setCustomerBlocked(true);
        amlCase.setCustomerBlockedAt(LocalDateTime.now());
        amlCase.setCustomerBlockedReason("Relationship terminated: " + reason);

        amlCase.addNote("Customer relationship terminated. Reason: " + reason, terminatedBy);

        log.warn("Customer {} relationship terminated by case {}", amlCase.getCustomerId(), amlCase.getCaseNumber());

        return amlCaseRepository.save(amlCase);
    }

    /**
     * Close case
     */
    @Transactional
    public AmlCase closeCase(String caseId, AmlCase.CaseResolution resolution,
                             String resolutionNotes, String closedBy) {
        AmlCase amlCase = amlCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        amlCase.setStatus(AmlCase.CaseStatus.CLOSED);
        amlCase.setResolution(resolution);
        amlCase.setResolutionNotes(resolutionNotes);
        amlCase.setClosedAt(LocalDateTime.now());
        amlCase.setClosedBy(closedBy);

        amlCase.addNote("Case closed. Resolution: " + resolution + ". Notes: " + resolutionNotes, closedBy);

        log.info("Case {} closed with resolution: {}", amlCase.getCaseNumber(), resolution);

        return amlCaseRepository.save(amlCase);
    }

    /**
     * Reopen case
     */
    @Transactional
    public AmlCase reopenCase(String caseId, String reason, String reopenedBy) {
        AmlCase amlCase = amlCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        if (amlCase.getStatus() != AmlCase.CaseStatus.CLOSED) {
            throw new RuntimeException("Only closed cases can be reopened");
        }

        amlCase.setStatus(AmlCase.CaseStatus.REOPENED);
        amlCase.setClosedAt(null);
        amlCase.setClosedBy(null);

        amlCase.addNote("Case reopened. Reason: " + reason, reopenedBy);

        log.info("Case {} reopened by {}", amlCase.getCaseNumber(), reopenedBy);

        return amlCaseRepository.save(amlCase);
    }

    /**
     * Get open cases
     */
    public List<AmlCase> getOpenCases() {
        return amlCaseRepository.findOpenCases();
    }

    /**
     * Get assigned cases
     */
    public List<AmlCase> getAssignedCases(String assignedTo) {
        List<AmlCase.CaseStatus> openStatuses = List.of(
                AmlCase.CaseStatus.OPEN,
                AmlCase.CaseStatus.INVESTIGATING,
                AmlCase.CaseStatus.PENDING_REVIEW,
                AmlCase.CaseStatus.ESCALATED
        );
        return amlCaseRepository.findByAssignedToAndStatusInOrderByPriorityDescOpenedAtAsc(assignedTo, openStatuses);
    }

    /**
     * Get overdue cases
     */
    public List<AmlCase> getOverdueCases() {
        return amlCaseRepository.findOverdueCases();
    }

    /**
     * Get high priority cases
     */
    public List<AmlCase> getHighPriorityCases() {
        return amlCaseRepository.findHighPriorityOpenCases();
    }

    /**
     * Get cases requiring SAR filing
     */
    public List<AmlCase> getCasesRequiringSar() {
        return amlCaseRepository.findCasesRequiringSarFiling();
    }

    /**
     * Get case statistics
     */
    public CaseStatistics getStatistics() {
        CaseStatistics stats = new CaseStatistics();
        stats.setTotalCases(amlCaseRepository.count());
        stats.setOpenCases(amlCaseRepository.countByStatus(AmlCase.CaseStatus.OPEN));
        stats.setInvestigatingCases(amlCaseRepository.countByStatus(AmlCase.CaseStatus.INVESTIGATING));
        stats.setEscalatedCases(amlCaseRepository.countByStatus(AmlCase.CaseStatus.ESCALATED));
        stats.setClosedCases(amlCaseRepository.countByStatus(AmlCase.CaseStatus.CLOSED));
        stats.setOverdueCases(amlCaseRepository.countByIsOverdueTrueAndStatusNot(AmlCase.CaseStatus.CLOSED));
        stats.setCasesRequiringSar(amlCaseRepository.countCasesRequiringSar());
        return stats;
    }

    /**
     * Helper: Map alert type to case type
     */
    private AmlCase.CaseType mapAlertTypeToCaseType(String alertType) {
        return switch (alertType.toUpperCase()) {
            case "TRANSACTION_MONITORING" -> AmlCase.CaseType.TRANSACTION_MONITORING;
            case "SANCTION_SCREENING" -> AmlCase.CaseType.SANCTION_SCREENING;
            case "STRUCTURING" -> AmlCase.CaseType.STRUCTURING;
            case "VELOCITY" -> AmlCase.CaseType.MONEY_LAUNDERING;
            default -> AmlCase.CaseType.OTHER;
        };
    }

    /**
     * Helper: Calculate due date based on priority
     */
    private LocalDateTime calculateDueDate(AmlCase.CasePriority priority) {
        return switch (priority) {
            case CRITICAL -> LocalDateTime.now().plusDays(1);
            case HIGH -> LocalDateTime.now().plusDays(3);
            case MEDIUM -> LocalDateTime.now().plusDays(7);
            case LOW -> LocalDateTime.now().plusDays(14);
        };
    }

    /**
     * Helper: Publish escalation event
     */
    private void publishEscalationEvent(AmlCase amlCase) {
        AmlCaseEscalatedEvent event = new AmlCaseEscalatedEvent();
        event.setCaseId(amlCase.getCaseId());
        event.setCaseNumber(amlCase.getCaseNumber());
        event.setCustomerId(amlCase.getCustomerId());
        event.setCustomerName(amlCase.getCustomerName());
        event.setCaseType(amlCase.getCaseType().name());
        event.setPriority(amlCase.getPriority().name());
        event.setRiskLevel(amlCase.getRiskLevel().name());
        event.setEscalatedTo(amlCase.getEscalatedTo());
        event.setEscalationReason(amlCase.getEscalationReason());
        event.setEscalatedAt(amlCase.getEscalatedAt());
        event.setTimestamp(LocalDateTime.now());

        kafkaTemplate.send("aml.case.escalated", event);
        log.info("Published case escalation event for case: {}", amlCase.getCaseNumber());
    }

    /**
     * Statistics DTO
     */
    @lombok.Data
    public static class CaseStatistics {
        private Long totalCases;
        private Long openCases;
        private Long investigatingCases;
        private Long escalatedCases;
        private Long closedCases;
        private Long overdueCases;
        private Long casesRequiringSar;
    }
}
