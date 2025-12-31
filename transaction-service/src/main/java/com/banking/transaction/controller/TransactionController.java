package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionHistoryRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransactionSummaryResponse;
import com.banking.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable("transactionId") String transactionId) {
        log.info("GET /transactions/{}", transactionId);
        TransactionResponse response = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/history")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @RequestBody TransactionHistoryRequest request) {
        log.info("POST /transactions/history for account: {}", request.getAccountNumber());
        Page<TransactionResponse> response = transactionService.getTransactionHistory(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Page<TransactionResponse>> getAccountTransactions(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /transactions/account/{} (page={}, size={})", accountNumber, page, size);
        Page<TransactionResponse> response = transactionService.getAccountTransactions(accountNumber, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}/date-range")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByDateRange(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("GET /transactions/account/{}/date-range (start={}, end={})",
            accountNumber, startDate, endDate);
        List<TransactionResponse> response = transactionService
            .getTransactionsByDateRange(accountNumber, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}/summary")
    public ResponseEntity<TransactionSummaryResponse> getTransactionSummary(
            @PathVariable("accountNumber") String accountNumber) {
        log.info("GET /transactions/account/{}/summary", accountNumber);
        TransactionSummaryResponse response = transactionService.getTransactionSummary(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByReference(
            @PathVariable("reference") String reference) {
        log.info("GET /transactions/reference/{}", reference);
        List<TransactionResponse> response = transactionService.getTransactionsByReference(reference);
        return ResponseEntity.ok(response);
    }
}
