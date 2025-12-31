package com.banking.fraud.controller;

import com.banking.fraud.dto.FraudRuleResponse;
import com.banking.fraud.dto.UpdateRuleRequest;
import com.banking.fraud.service.FraudDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
@Slf4j
public class FraudRuleController {

    private final FraudDetectionService fraudDetectionService;

    @GetMapping
    public ResponseEntity<List<FraudRuleResponse>> getAllRules() {
        log.info("GET /rules - Retrieving all fraud rules");
        List<FraudRuleResponse> responses = fraudDetectionService.getAllRules();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<FraudRuleResponse> getRule(
            @PathVariable("ruleId") String ruleId) {
        log.info("GET /rules/{} - Retrieving fraud rule", ruleId);
        FraudRuleResponse response = fraudDetectionService.getRuleById(ruleId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<FraudRuleResponse> updateRule(
            @PathVariable("ruleId") String ruleId,
            @Valid @RequestBody UpdateRuleRequest request) {
        log.info("PUT /rules/{} - Updating fraud rule", ruleId);
        FraudRuleResponse response = fraudDetectionService.updateRule(ruleId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{ruleId}/toggle")
    public ResponseEntity<FraudRuleResponse> toggleRule(
            @PathVariable("ruleId") String ruleId) {
        log.info("POST /rules/{}/toggle - Toggling fraud rule", ruleId);
        FraudRuleResponse response = fraudDetectionService.toggleRule(ruleId);
        return ResponseEntity.ok(response);
    }
}
