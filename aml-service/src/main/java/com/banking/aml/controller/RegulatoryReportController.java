package com.banking.aml.controller;

import com.banking.aml.model.RegulatoryReport;
import com.banking.aml.service.RegulatoryReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class RegulatoryReportController {

    private final RegulatoryReportingService regulatoryReportingService;

    @PostMapping("/from-alert")
    public ResponseEntity<RegulatoryReport> createFromAlert(
            @RequestParam String alertId,
            @RequestParam RegulatoryReport.ReportType reportType,
            @RequestParam String preparedBy) {
        RegulatoryReport report = regulatoryReportingService.createReportFromAlert(alertId, reportType, preparedBy);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/from-case")
    public ResponseEntity<RegulatoryReport> createFromCase(
            @RequestParam String caseId,
            @RequestParam RegulatoryReport.ReportType reportType,
            @RequestParam String preparedBy) {
        RegulatoryReport report = regulatoryReportingService.createReportFromCase(caseId, reportType, preparedBy);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{reportId}/submit")
    public ResponseEntity<RegulatoryReport> submitForReview(@PathVariable("reportId") String reportId) {
        RegulatoryReport report = regulatoryReportingService.submitForReview(reportId);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{reportId}/review")
    public ResponseEntity<RegulatoryReport> reviewReport(
            @PathVariable("reportId") String reportId,
            @RequestParam boolean approved,
            @RequestParam String reviewedBy,
            @RequestParam(required = false) String comments) {
        RegulatoryReport report = regulatoryReportingService.reviewReport(reportId, approved, reviewedBy, comments);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{reportId}/approve")
    public ResponseEntity<RegulatoryReport> approveReport(
            @PathVariable("reportId") String reportId,
            @RequestParam String approvedBy) {
        RegulatoryReport report = regulatoryReportingService.approveReport(reportId, approvedBy);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{reportId}/file")
    public ResponseEntity<RegulatoryReport> fileReport(
            @PathVariable("reportId") String reportId,
            @RequestParam String filedTo,
            @RequestParam String filedBy) {
        RegulatoryReport report = regulatoryReportingService.fileReport(reportId, filedTo, filedBy);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{reportId}/acknowledge")
    public ResponseEntity<RegulatoryReport> acknowledgeReceipt(
            @PathVariable("reportId") String reportId,
            @RequestParam String authorityReferenceNumber) {
        RegulatoryReport report = regulatoryReportingService.acknowledgeReceipt(reportId, authorityReferenceNumber);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/pending-review")
    public ResponseEntity<List<RegulatoryReport>> getPendingReview() {
        return ResponseEntity.ok(regulatoryReportingService.getPendingReview());
    }

    @GetMapping("/filed")
    public ResponseEntity<List<RegulatoryReport>> getFiledReports() {
        return ResponseEntity.ok(regulatoryReportingService.getFiledReports());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<RegulatoryReport>> getReportsByCustomer(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(regulatoryReportingService.getReportsByCustomer(customerId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<RegulatoryReportingService.ReportStatistics> getStatistics() {
        return ResponseEntity.ok(regulatoryReportingService.getStatistics());
    }
}
