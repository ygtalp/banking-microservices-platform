package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.ApiResponse;
import com.banking.account.dto.BalanceUpdateRequest;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.model.AccountHistory;
import com.banking.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        log.info("Received request to create account for customer: {}", request.getCustomerId());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Account created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable("id") Long id) {
        log.info("Received request to get account by id: {}", id);
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Account retrieved successfully"));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByNumber(
            @PathVariable("accountNumber") String accountNumber) {
        log.info("Received request to get account by number: {}", accountNumber);
        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Account retrieved successfully"));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByCustomerId(
            @PathVariable("customerId") String customerId) {
        log.info("Received request to get accounts for customer: {}", customerId);
        List<AccountResponse> response = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Accounts retrieved successfully"));
    }

    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<ApiResponse<AccountResponse>> creditAccount(
            @PathVariable("accountNumber") String accountNumber,
            @Valid @RequestBody BalanceUpdateRequest request) {
        log.info("Received request to credit account: {} with amount: {}", accountNumber, request.getAmount());
        AccountResponse response = accountService.creditAccount(accountNumber, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Account credited successfully"));
    }

    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<ApiResponse<AccountResponse>> debitAccount(
            @PathVariable("accountNumber") String accountNumber,
            @Valid @RequestBody BalanceUpdateRequest request) {
        log.info("Received request to debit account: {} with amount: {}", accountNumber, request.getAmount());
        AccountResponse response = accountService.debitAccount(accountNumber, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Account debited successfully"));
    }

    @PostMapping("/{accountNumber}/freeze")
    public ResponseEntity<ApiResponse<AccountResponse>> freezeAccount(@PathVariable("accountNumber") String accountNumber) {
        log.info("Received request to freeze account: {}", accountNumber);
        AccountResponse response = accountService.freezeAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Account frozen successfully"));
    }

    @PostMapping("/{accountNumber}/activate")
    public ResponseEntity<ApiResponse<AccountResponse>> activateAccount(@PathVariable("accountNumber") String accountNumber) {
        log.info("Received request to activate account: {}", accountNumber);
        AccountResponse response = accountService.activateAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Account activated successfully"));
    }

    @PostMapping("/{accountNumber}/close")
    public ResponseEntity<ApiResponse<AccountResponse>> closeAccount(@PathVariable("accountNumber") String accountNumber) {
        log.info("Received request to close account: {}", accountNumber);
        AccountResponse response = accountService.closeAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Account closed successfully"));
    }

    @GetMapping("/{accountNumber}/history")
    public ResponseEntity<ApiResponse<List<AccountHistory>>> getAccountHistory(
            @PathVariable("accountNumber") String accountNumber) {
        log.info("Received request to get history for account: {}", accountNumber);
        List<AccountHistory> response = accountService.getAccountHistory(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Account history retrieved successfully"));
    }
}