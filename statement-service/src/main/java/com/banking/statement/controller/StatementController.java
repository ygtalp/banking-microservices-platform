package com.banking.statement.controller;

import com.banking.statement.dto.StatementGenerationRequest;
import com.banking.statement.dto.StatementResponse;
import com.banking.statement.service.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/statements")
@RequiredArgsConstructor
@Tag(name = "Statements", description = "Statement Generation and Management APIs")
public class StatementController {

    private final StatementService statementService;

    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate account statement")
    public ResponseEntity<StatementResponse> generateStatement(
            @Valid @RequestBody StatementGenerationRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        StatementResponse response = statementService.generateStatement(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{statementId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get statement by ID")
    public ResponseEntity<StatementResponse> getStatement(@PathVariable String statementId) {
        return ResponseEntity.ok(statementService.getStatement(statementId));
    }

    @GetMapping("/account/{accountNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all statements for account")
    public ResponseEntity<List<StatementResponse>> getAccountStatements(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(statementService.getAccountStatements(accountNumber));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all statements for customer")
    public ResponseEntity<List<StatementResponse>> getCustomerStatements(
            @PathVariable String customerId) {
        return ResponseEntity.ok(statementService.getCustomerStatements(customerId));
    }

    @GetMapping("/download/{statementId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download statement PDF")
    public ResponseEntity<byte[]> downloadStatement(@PathVariable String statementId) {
        byte[] pdfBytes = statementService.downloadStatement(statementId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", statementId + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
