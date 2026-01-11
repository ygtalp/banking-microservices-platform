package com.banking.aml.controller;

import com.banking.aml.model.CustomerRiskProfile;
import com.banking.aml.service.CustomerRiskScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer-risk")
@RequiredArgsConstructor
public class CustomerRiskProfileController {

    private final CustomerRiskScoringService customerRiskScoringService;

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerRiskProfile> getCustomerRiskProfile(@PathVariable("customerId") String customerId) {
        CustomerRiskProfile profile = customerRiskScoringService.getCustomerRiskProfile(customerId);
        return profile != null ? ResponseEntity.ok(profile) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<CustomerRiskProfile> createRiskProfile(@RequestBody CustomerRiskProfile profile) {
        CustomerRiskProfile created = customerRiskScoringService.createRiskProfile(profile);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{customerId}/mark-pep")
    public ResponseEntity<CustomerRiskProfile> markAsPep(
            @PathVariable("customerId") String customerId,
            @RequestParam String pepCategory) {
        CustomerRiskProfile profile = customerRiskScoringService.markAsPep(customerId, pepCategory);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/{customerId}/block")
    public ResponseEntity<CustomerRiskProfile> blockCustomer(
            @PathVariable("customerId") String customerId,
            @RequestParam String reason) {
        CustomerRiskProfile profile = customerRiskScoringService.blockCustomer(customerId, reason);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/{customerId}/cdd-review")
    public ResponseEntity<CustomerRiskProfile> performCddReview(
            @PathVariable("customerId") String customerId,
            @RequestParam String reviewedBy) {
        CustomerRiskProfile profile = customerRiskScoringService.performCddReview(customerId, reviewedBy);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/high-risk")
    public ResponseEntity<List<CustomerRiskProfile>> getHighRiskCustomers() {
        return ResponseEntity.ok(customerRiskScoringService.getHighRiskCustomers());
    }

    @GetMapping("/needing-cdd-review")
    public ResponseEntity<List<CustomerRiskProfile>> getCustomersNeedingCddReview() {
        return ResponseEntity.ok(customerRiskScoringService.getCustomersNeedingCddReview());
    }

    @PostMapping("/recalculate-all")
    public ResponseEntity<Integer> recalculateAllRiskScores() {
        int count = customerRiskScoringService.recalculateAllRiskScores();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/statistics")
    public ResponseEntity<CustomerRiskScoringService.RiskStatistics> getStatistics() {
        return ResponseEntity.ok(customerRiskScoringService.getStatistics());
    }
}
