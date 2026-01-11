package com.banking.aml.controller;

import com.banking.aml.model.*;
import com.banking.aml.service.AmlScreeningService;
import com.banking.aml.service.TransactionMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/aml")
@RequiredArgsConstructor
@Slf4j
public class AmlController {

    private final AmlScreeningService amlScreeningService;
    private final TransactionMonitoringService transactionMonitoringService;

    // Alert endpoints
    @GetMapping("/alerts/{alertId}")
    public ResponseEntity<AmlAlert> getAlert(@PathVariable("alertId") String alertId) {
        return amlScreeningService.getAlert(alertId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/alerts/account/{accountNumber}")
    public ResponseEntity<List<AmlAlert>> getAlertsByAccount(
            @PathVariable("accountNumber") String accountNumber) {
        return ResponseEntity.ok(amlScreeningService.getAlertsByAccount(accountNumber));
    }

    @GetMapping("/alerts/account/{accountNumber}/paged")
    public ResponseEntity<Page<AmlAlert>> getAlertsByAccountPaged(
            @PathVariable("accountNumber") String accountNumber,
            Pageable pageable) {
        return ResponseEntity.ok(amlScreeningService.getAlertsByAccount(accountNumber, pageable));
    }

    @GetMapping("/alerts/status/{status}")
    public ResponseEntity<List<AmlAlert>> getAlertsByStatus(
            @PathVariable("status") AlertStatus status) {
        return ResponseEntity.ok(amlScreeningService.getAlertsByStatus(status));
    }

    @GetMapping("/alerts/pending-review")
    public ResponseEntity<List<AmlAlert>> getPendingReview() {
        return ResponseEntity.ok(amlScreeningService.getPendingReview());
    }

    @GetMapping("/alerts/high-risk")
    public ResponseEntity<List<AmlAlert>> getHighRiskAlerts() {
        return ResponseEntity.ok(amlScreeningService.getHighRiskAlerts());
    }

    @GetMapping("/alerts/recent")
    public ResponseEntity<List<AmlAlert>> getRecentAlerts(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(amlScreeningService.getRecentAlerts(hours));
    }

    @PutMapping("/alerts/{alertId}/review")
    public ResponseEntity<AmlAlert> reviewAlert(
            @PathVariable("alertId") String alertId,
            @RequestParam AlertStatus status,
            @RequestParam String reviewedBy,
            @RequestParam(required = false) String notes) {
        AmlAlert updated = amlScreeningService.updateAlertStatus(alertId, status, reviewedBy, notes);
        return ResponseEntity.ok(updated);
    }

    // Sanction match endpoints
    @GetMapping("/sanctions/account/{accountNumber}")
    public ResponseEntity<List<SanctionMatch>> getSanctionMatchesByAccount(
            @PathVariable("accountNumber") String accountNumber) {
        return ResponseEntity.ok(amlScreeningService.getSanctionMatchesByAccount(accountNumber));
    }

    @GetMapping("/sanctions/potential")
    public ResponseEntity<List<SanctionMatch>> getPotentialMatches(
            @RequestParam(defaultValue = "70") int minScore) {
        return ResponseEntity.ok(amlScreeningService.getPotentialMatches(minScore));
    }

    @GetMapping("/sanctions/unreviewed")
    public ResponseEntity<List<SanctionMatch>> getUnreviewedMatches() {
        return ResponseEntity.ok(amlScreeningService.getUnreviewedMatches());
    }

    @PutMapping("/sanctions/{matchId}/review")
    public ResponseEntity<SanctionMatch> reviewMatch(
            @PathVariable("matchId") String matchId,
            @RequestParam String matchStatus,
            @RequestParam String reviewedBy) {
        SanctionMatch updated = amlScreeningService.reviewMatch(matchId, matchStatus, reviewedBy);
        return ResponseEntity.ok(updated);
    }

    // Transaction monitoring endpoints
    @GetMapping("/monitoring/account/{accountNumber}")
    public ResponseEntity<List<TransactionMonitoring>> getMonitoringByAccount(
            @PathVariable("accountNumber") String accountNumber) {
        return ResponseEntity.ok(transactionMonitoringService.getTransactionsByAccount(accountNumber));
    }

    @GetMapping("/monitoring/flagged")
    public ResponseEntity<List<TransactionMonitoring>> getFlaggedTransactions() {
        return ResponseEntity.ok(transactionMonitoringService.getFlaggedTransactions());
    }

    @GetMapping("/monitoring/high-risk-unalerted")
    public ResponseEntity<List<TransactionMonitoring>> getHighRiskUnalerted(
            @RequestParam(defaultValue = "60") int minScore) {
        return ResponseEntity.ok(transactionMonitoringService.getHighRiskUnalerted(minScore));
    }

    // Statistics endpoints
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("openAlerts", amlScreeningService.getAlertCount(AlertStatus.OPEN));
        stats.put("underReview", amlScreeningService.getAlertCount(AlertStatus.UNDER_REVIEW));
        stats.put("escalated", amlScreeningService.getAlertCount(AlertStatus.ESCALATED));
        stats.put("lowRisk", amlScreeningService.getAlertCountByRisk(RiskLevel.LOW));
        stats.put("mediumRisk", amlScreeningService.getAlertCountByRisk(RiskLevel.MEDIUM));
        stats.put("highRisk", amlScreeningService.getAlertCountByRisk(RiskLevel.HIGH));
        stats.put("criticalRisk", amlScreeningService.getAlertCountByRisk(RiskLevel.CRITICAL));
        return ResponseEntity.ok(stats);
    }
}
