package com.banking.fraud.controller;

import com.banking.fraud.dto.*;
import com.banking.fraud.service.FraudDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fraud-checks")
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    @PostMapping
    public ResponseEntity<FraudCheckResponse> performFraudCheck(
            @Valid @RequestBody FraudCheckRequest request) {
        log.info("POST /fraud-checks - Performing fraud check for transfer: {}",
                request.getTransferReference());
        FraudCheckResponse response = fraudDetectionService.performFraudCheck(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{checkId}")
    public ResponseEntity<FraudCheckResponse> getFraudCheck(
            @PathVariable("checkId") String checkId) {
        log.info("GET /fraud-checks/{} - Retrieving fraud check", checkId);
        FraudCheckResponse response = fraudDetectionService.getFraudCheckById(checkId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transfer/{transferReference}")
    public ResponseEntity<List<FraudCheckResponse>> getFraudChecksByTransfer(
            @PathVariable("transferReference") String transferReference) {
        log.info("GET /fraud-checks/transfer/{} - Retrieving checks for transfer", transferReference);
        List<FraudCheckResponse> responses = fraudDetectionService.getFraudChecksByTransfer(transferReference);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<FraudCheckResponse>> getFraudChecksByAccount(
            @PathVariable("accountNumber") String accountNumber) {
        log.info("GET /fraud-checks/account/{} - Retrieving checks for account", accountNumber);
        List<FraudCheckResponse> responses = fraudDetectionService.getFraudChecksByAccount(accountNumber);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/pending-review")
    public ResponseEntity<List<FraudCheckResponse>> getPendingReviews() {
        log.info("GET /fraud-checks/pending-review - Retrieving pending reviews");
        List<FraudCheckResponse> responses = fraudDetectionService.getPendingReviews();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{checkId}/review")
    public ResponseEntity<FraudCheckResponse> reviewFraudCheck(
            @PathVariable("checkId") String checkId,
            @Valid @RequestBody ReviewRequest request) {
        log.info("POST /fraud-checks/{}/review - Reviewing fraud check", checkId);
        FraudCheckResponse response = fraudDetectionService.reviewFraudCheck(checkId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/risk-score/{accountNumber}")
    public ResponseEntity<RiskScoreResponse> getRiskScore(
            @PathVariable("accountNumber") String accountNumber) {
        log.info("GET /fraud-checks/risk-score/{} - Retrieving risk score", accountNumber);
        RiskScoreResponse response = fraudDetectionService.getRiskScore(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/high-risk-accounts")
    public ResponseEntity<List<RiskScoreResponse>> getHighRiskAccounts() {
        log.info("GET /fraud-checks/high-risk-accounts - Retrieving high risk accounts");
        List<RiskScoreResponse> responses = fraudDetectionService.getHighRiskAccounts();
        return ResponseEntity.ok(responses);
    }
}
