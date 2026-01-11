package com.banking.loan.controller;

import com.banking.loan.dto.LoanApplicationRequest;
import com.banking.loan.dto.LoanResponse;
import com.banking.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan Management APIs")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Apply for loan")
    public ResponseEntity<LoanResponse> applyForLoan(@Valid @RequestBody LoanApplicationRequest request) {
        LoanResponse response = loanService.applyForLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get loan by ID")
    public ResponseEntity<LoanResponse> getLoan(@PathVariable String loanId) {
        return ResponseEntity.ok(loanService.getLoan(loanId));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer loans")
    public ResponseEntity<List<LoanResponse>> getCustomerLoans(@PathVariable String customerId) {
        return ResponseEntity.ok(loanService.getCustomerLoans(customerId));
    }

    @PostMapping("/{loanId}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Approve loan")
    public ResponseEntity<LoanResponse> approveLoan(
            @PathVariable String loanId,
            @RequestParam String approvedBy) {
        return ResponseEntity.ok(loanService.approveLoan(loanId, approvedBy));
    }

    @PostMapping("/{loanId}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Reject loan")
    public ResponseEntity<LoanResponse> rejectLoan(
            @PathVariable String loanId,
            @RequestParam String reviewedBy,
            @RequestParam String reason) {
        return ResponseEntity.ok(loanService.rejectLoan(loanId, reviewedBy, reason));
    }

    @PostMapping("/{loanId}/disburse")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disburse loan")
    public ResponseEntity<LoanResponse> disburseLoan(
            @PathVariable String loanId,
            @RequestParam String transferReference) {
        return ResponseEntity.ok(loanService.disburseLoan(loanId, transferReference));
    }
}
