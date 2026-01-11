package com.banking.aml.controller;

import com.banking.aml.model.AmlCase;
import com.banking.aml.service.AmlCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
public class AmlCaseController {

    private final AmlCaseService amlCaseService;

    @PostMapping("/from-alert")
    public ResponseEntity<AmlCase> createFromAlert(
            @RequestParam String alertId,
            @RequestParam AmlCase.CasePriority priority,
            @RequestParam String openedBy) {
        AmlCase amlCase = amlCaseService.createCaseFromAlert(alertId, priority, openedBy);
        return ResponseEntity.ok(amlCase);
    }

    @PostMapping("/{caseId}/assign")
    public ResponseEntity<AmlCase> assignCase(
            @PathVariable("caseId") String caseId,
            @RequestParam String assignedTo,
            @RequestParam String assignedBy) {
        AmlCase amlCase = amlCaseService.assignCase(caseId, assignedTo, assignedBy);
        return ResponseEntity.ok(amlCase);
    }

    @PostMapping("/{caseId}/start-investigation")
    public ResponseEntity<AmlCase> startInvestigation(
            @PathVariable("caseId") String caseId,
            @RequestParam String investigator) {
        AmlCase amlCase = amlCaseService.startInvestigation(caseId, investigator);
        return ResponseEntity.ok(amlCase);
    }

    @PostMapping("/{caseId}/add-note")
    public ResponseEntity<AmlCase> addNote(
            @PathVariable("caseId") String caseId,
            @RequestParam String content,
            @RequestParam String author,
            @RequestParam(required = false, defaultValue = "GENERAL") AmlCase.CaseNote.NoteType noteType) {
        AmlCase amlCase = amlCaseService.addNote(caseId, content, author, noteType);
        return ResponseEntity.ok(amlCase);
    }

    @PostMapping("/{caseId}/escalate")
    public ResponseEntity<AmlCase> escalateCase(
            @PathVariable("caseId") String caseId,
            @RequestParam String escalatedTo,
            @RequestParam String escalationReason,
            @RequestParam String escalatedBy) {
        AmlCase amlCase = amlCaseService.escalateCase(caseId, escalatedTo, escalationReason, escalatedBy);
        return ResponseEntity.ok(amlCase);
    }

    @PostMapping("/{caseId}/block-customer")
    public ResponseEntity<AmlCase> blockCustomer(
            @PathVariable("caseId") String caseId,
            @RequestParam String reason,
            @RequestParam String blockedBy) {
        AmlCase amlCase = amlCaseService.blockCustomer(caseId, reason, blockedBy);
        return ResponseEntity.ok(amlCase);
    }

    @PostMapping("/{caseId}/terminate-relationship")
    public ResponseEntity<AmlCase> terminateRelationship(
            @PathVariable("caseId") String caseId,
            @RequestParam String reason,
            @RequestParam String terminatedBy) {
        AmlCase amlCase = amlCaseService.terminateRelationship(caseId, reason, terminatedBy);
        return ResponseEntity.ok(amlCase);
    }

    @PostMapping("/{caseId}/close")
    public ResponseEntity<AmlCase> closeCase(
            @PathVariable("caseId") String caseId,
            @RequestParam AmlCase.CaseResolution resolution,
            @RequestParam String resolutionNotes,
            @RequestParam String closedBy) {
        AmlCase amlCase = amlCaseService.closeCase(caseId, resolution, resolutionNotes, closedBy);
        return ResponseEntity.ok(amlCase);
    }

    @PostMapping("/{caseId}/reopen")
    public ResponseEntity<AmlCase> reopenCase(
            @PathVariable("caseId") String caseId,
            @RequestParam String reason,
            @RequestParam String reopenedBy) {
        AmlCase amlCase = amlCaseService.reopenCase(caseId, reason, reopenedBy);
        return ResponseEntity.ok(amlCase);
    }

    @GetMapping("/open")
    public ResponseEntity<List<AmlCase>> getOpenCases() {
        return ResponseEntity.ok(amlCaseService.getOpenCases());
    }

    @GetMapping("/assigned/{assignedTo}")
    public ResponseEntity<List<AmlCase>> getAssignedCases(@PathVariable("assignedTo") String assignedTo) {
        return ResponseEntity.ok(amlCaseService.getAssignedCases(assignedTo));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<AmlCase>> getOverdueCases() {
        return ResponseEntity.ok(amlCaseService.getOverdueCases());
    }

    @GetMapping("/high-priority")
    public ResponseEntity<List<AmlCase>> getHighPriorityCases() {
        return ResponseEntity.ok(amlCaseService.getHighPriorityCases());
    }

    @GetMapping("/requiring-sar")
    public ResponseEntity<List<AmlCase>> getCasesRequiringSar() {
        return ResponseEntity.ok(amlCaseService.getCasesRequiringSar());
    }

    @GetMapping("/statistics")
    public ResponseEntity<AmlCaseService.CaseStatistics> getStatistics() {
        return ResponseEntity.ok(amlCaseService.getStatistics());
    }
}
