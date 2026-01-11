package com.banking.aml.service;

import com.banking.aml.model.AmlAlert;
import com.banking.aml.model.AmlCase;
import com.banking.aml.model.RegulatoryReport;
import com.banking.aml.repository.AmlAlertRepository;
import com.banking.aml.repository.AmlCaseRepository;
import com.banking.aml.repository.RegulatoryReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegulatoryReportingService {

    private final RegulatoryReportRepository regulatoryReportRepository;
    private final AmlAlertRepository amlAlertRepository;
    private final AmlCaseRepository amlCaseRepository;

    /**
     * Create a new regulatory report from an AML alert
     */
    @Transactional
    public RegulatoryReport createReportFromAlert(String alertId, RegulatoryReport.ReportType reportType,
                                                    String preparedBy) {
        log.info("Creating {} report from alert: {}", reportType, alertId);

        AmlAlert alert = amlAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        RegulatoryReport report = new RegulatoryReport();
        report.setReportType(reportType);
        report.setStatus(RegulatoryReport.ReportStatus.DRAFT);
        report.setCustomerId(alert.getCustomerId());
        report.setCustomerName(alert.getCustomerName());
        report.setAccountNumber(alert.getAccountNumber());
        report.setAlertId(alertId);

        // Set transaction details from alert
        if (alert.getTransferReference() != null) {
            report.setTransactionReferences(alert.getTransferReference());
        }
        report.setSuspiciousAmount(alert.getAmount());
        report.setCurrency(alert.getCurrency());

        // Map alert type to suspicion category
        report.setSuspicionCategory(mapAlertTypeToSuspicionCategory(alert.getAlertType()));

        // Create narrative from alert reasons
        StringBuilder narrative = new StringBuilder();
        narrative.append("Suspicious activity detected based on the following indicators:\\n");
        if (alert.getReasons() != null && !alert.getReasons().isEmpty()) {
            alert.getReasons().forEach(reason -> narrative.append("- ").append(reason).append("\\n"));
        }
        narrative.append("\\nRisk Score: ").append(alert.getRiskScore());
        narrative.append("\\nRisk Level: ").append(alert.getRiskLevel());
        report.setNarrative(narrative.toString());

        // Set suspicion indicators
        if (alert.getReasons() != null) {
            report.setSuspicionIndicators(String.join(", ", alert.getReasons()));
        }

        // Set reporting information
        report.setPreparedBy(preparedBy);
        report.setReportingInstitution("Banking Platform Inc.");

        // Generate report number
        report.setReportNumber(generateReportNumber(reportType));

        RegulatoryReport saved = regulatoryReportRepository.save(report);
        log.info("Created {} report: {}", reportType, saved.getReportNumber());

        return saved;
    }

    /**
     * Create a report from an AML case
     */
    @Transactional
    public RegulatoryReport createReportFromCase(String caseId, RegulatoryReport.ReportType reportType,
                                                  String preparedBy) {
        log.info("Creating {} report from case: {}", reportType, caseId);

        AmlCase amlCase = amlCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        RegulatoryReport report = new RegulatoryReport();
        report.setReportType(reportType);
        report.setStatus(RegulatoryReport.ReportStatus.DRAFT);
        report.setCustomerId(amlCase.getCustomerId());
        report.setCustomerName(amlCase.getCustomerName());
        report.setAccountNumber(amlCase.getAccountNumber());
        report.setCaseId(caseId);

        // Set transaction references from case
        if (amlCase.getTransactionReferences() != null && !amlCase.getTransactionReferences().isEmpty()) {
            report.setTransactionReferences(String.join(", ", amlCase.getTransactionReferences()));
        }

        // Map case type to suspicion category
        report.setSuspicionCategory(mapCaseTypeToSuspicionCategory(amlCase.getCaseType()));

        // Use case description and findings as narrative
        StringBuilder narrative = new StringBuilder();
        narrative.append(amlCase.getDescription()).append("\\n\\n");
        if (amlCase.getFindings() != null) {
            narrative.append("Findings:\\n").append(amlCase.getFindings()).append("\\n\\n");
        }
        if (amlCase.getInvestigationSummary() != null) {
            narrative.append("Investigation Summary:\\n").append(amlCase.getInvestigationSummary());
        }
        report.setNarrative(narrative.toString());

        // Set risk assessment from case
        if (amlCase.getRecommendations() != null) {
            report.setRiskAssessment(amlCase.getRecommendations());
        }

        // Set reporting information
        report.setPreparedBy(preparedBy);
        report.setReportingInstitution("Banking Platform Inc.");
        report.setReportNumber(generateReportNumber(reportType));

        RegulatoryReport saved = regulatoryReportRepository.save(report);
        log.info("Created {} report: {}", reportType, saved.getReportNumber());

        return saved;
    }

    /**
     * Submit report for review
     */
    @Transactional
    public RegulatoryReport submitForReview(String reportId) {
        RegulatoryReport report = regulatoryReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (report.getStatus() != RegulatoryReport.ReportStatus.DRAFT) {
            throw new RuntimeException("Only draft reports can be submitted for review");
        }

        report.setStatus(RegulatoryReport.ReportStatus.PENDING_REVIEW);
        log.info("Report {} submitted for review", report.getReportNumber());

        return regulatoryReportRepository.save(report);
    }

    /**
     * Review report
     */
    @Transactional
    public RegulatoryReport reviewReport(String reportId, boolean approved, String reviewedBy, String comments) {
        RegulatoryReport report = regulatoryReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        report.setReviewedBy(reviewedBy);
        report.setReviewedAt(LocalDateTime.now());

        if (approved) {
            report.setStatus(RegulatoryReport.ReportStatus.PENDING_APPROVAL);
            log.info("Report {} reviewed and approved by {}", report.getReportNumber(), reviewedBy);
        } else {
            report.setStatus(RegulatoryReport.ReportStatus.REJECTED);
            report.setRejectionReason(comments);
            log.info("Report {} rejected by {}: {}", report.getReportNumber(), reviewedBy, comments);
        }

        if (comments != null) {
            report.setRemarks(comments);
        }

        return regulatoryReportRepository.save(report);
    }

    /**
     * Approve report (final approval before filing)
     */
    @Transactional
    public RegulatoryReport approveReport(String reportId, String approvedBy) {
        RegulatoryReport report = regulatoryReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (report.getStatus() != RegulatoryReport.ReportStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Report must be in PENDING_APPROVAL status");
        }

        report.setStatus(RegulatoryReport.ReportStatus.APPROVED);
        report.setApprovedBy(approvedBy);
        report.setApprovedAt(LocalDateTime.now());

        log.info("Report {} approved by {}", report.getReportNumber(), approvedBy);

        return regulatoryReportRepository.save(report);
    }

    /**
     * File report to regulatory authority
     */
    @Transactional
    public RegulatoryReport fileReport(String reportId, String filedTo, String filedBy) {
        RegulatoryReport report = regulatoryReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (report.getStatus() != RegulatoryReport.ReportStatus.APPROVED) {
            throw new RuntimeException("Only approved reports can be filed");
        }

        report.setStatus(RegulatoryReport.ReportStatus.FILED);
        report.setFiledToAuthority(filedTo);
        report.setFiledAt(LocalDateTime.now());

        log.info("Report {} filed to {} by {}", report.getReportNumber(), filedTo, filedBy);

        // Update associated case
        if (report.getCaseId() != null) {
            updateCaseAfterFiling(report.getCaseId(), reportId);
        }

        return regulatoryReportRepository.save(report);
    }

    /**
     * Acknowledge receipt from authority
     */
    @Transactional
    public RegulatoryReport acknowledgeReceipt(String reportId, String authorityReferenceNumber) {
        RegulatoryReport report = regulatoryReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        report.setStatus(RegulatoryReport.ReportStatus.ACKNOWLEDGED);
        report.setAuthorityReferenceNumber(authorityReferenceNumber);
        report.setAcknowledgmentReceivedAt(LocalDateTime.now());

        log.info("Report {} acknowledged by authority. Reference: {}",
                report.getReportNumber(), authorityReferenceNumber);

        return regulatoryReportRepository.save(report);
    }

    /**
     * Get pending reports (for review/approval workflow)
     */
    public List<RegulatoryReport> getPendingReview() {
        return regulatoryReportRepository.findPendingReview();
    }

    /**
     * Get filed reports
     */
    public List<RegulatoryReport> getFiledReports() {
        return regulatoryReportRepository.findFiledReports();
    }

    /**
     * Get reports by customer
     */
    public List<RegulatoryReport> getReportsByCustomer(String customerId) {
        return regulatoryReportRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Get report statistics
     */
    public ReportStatistics getStatistics() {
        ReportStatistics stats = new ReportStatistics();
        stats.setTotalReports(regulatoryReportRepository.count());
        stats.setDraftCount(regulatoryReportRepository.countByStatus(RegulatoryReport.ReportStatus.DRAFT));
        stats.setPendingReviewCount(regulatoryReportRepository.countByStatus(RegulatoryReport.ReportStatus.PENDING_REVIEW));
        stats.setPendingApprovalCount(regulatoryReportRepository.countByStatus(RegulatoryReport.ReportStatus.PENDING_APPROVAL));
        stats.setFiledCount(regulatoryReportRepository.countByStatus(RegulatoryReport.ReportStatus.FILED));
        stats.setAcknowledgedCount(regulatoryReportRepository.countByStatus(RegulatoryReport.ReportStatus.ACKNOWLEDGED));
        return stats;
    }

    /**
     * Helper: Update case after SAR filing
     */
    private void updateCaseAfterFiling(String caseId, String reportId) {
        Optional<AmlCase> caseOpt = amlCaseRepository.findById(caseId);
        if (caseOpt.isPresent()) {
            AmlCase amlCase = caseOpt.get();
            amlCase.setSarFiled(true);
            amlCase.setSarReportId(reportId);
            amlCase.setSarFiledAt(LocalDateTime.now());
            amlCaseRepository.save(amlCase);
            log.info("Updated case {} with SAR filing information", caseId);
        }
    }

    /**
     * Helper: Generate report number
     */
    private String generateReportNumber(RegulatoryReport.ReportType reportType) {
        int year = LocalDateTime.now().getYear();
        long count = regulatoryReportRepository.countByReportType(reportType);
        return String.format("%s-%d-%06d", reportType.name(), year, count + 1);
    }

    /**
     * Helper: Map alert type to suspicion category
     */
    private RegulatoryReport.SuspicionCategory mapAlertTypeToSuspicionCategory(String alertType) {
        return switch (alertType.toUpperCase()) {
            case "TRANSACTION_MONITORING" -> RegulatoryReport.SuspicionCategory.UNUSUAL_TRANSACTION;
            case "SANCTION_SCREENING" -> RegulatoryReport.SuspicionCategory.SANCTIONS_EVASION;
            case "STRUCTURING" -> RegulatoryReport.SuspicionCategory.STRUCTURING;
            case "VELOCITY" -> RegulatoryReport.SuspicionCategory.MONEY_LAUNDERING;
            case "ROUND_AMOUNT" -> RegulatoryReport.SuspicionCategory.STRUCTURING;
            default -> RegulatoryReport.SuspicionCategory.OTHER;
        };
    }

    /**
     * Helper: Map case type to suspicion category
     */
    private RegulatoryReport.SuspicionCategory mapCaseTypeToSuspicionCategory(AmlCase.CaseType caseType) {
        return switch (caseType) {
            case STRUCTURING -> RegulatoryReport.SuspicionCategory.STRUCTURING;
            case MONEY_LAUNDERING -> RegulatoryReport.SuspicionCategory.MONEY_LAUNDERING;
            case TERRORISM_FINANCING -> RegulatoryReport.SuspicionCategory.TERRORISM_FINANCING;
            case FRAUD_INVESTIGATION -> RegulatoryReport.SuspicionCategory.FRAUD;
            case SANCTION_SCREENING -> RegulatoryReport.SuspicionCategory.SANCTIONS_EVASION;
            case PEP_REVIEW -> RegulatoryReport.SuspicionCategory.PEP_RELATED;
            default -> RegulatoryReport.SuspicionCategory.OTHER;
        };
    }

    /**
     * Statistics DTO
     */
    @lombok.Data
    public static class ReportStatistics {
        private Long totalReports;
        private Long draftCount;
        private Long pendingReviewCount;
        private Long pendingApprovalCount;
        private Long filedCount;
        private Long acknowledgedCount;
    }
}
