package com.banking.swift.controller;

import com.banking.swift.dto.CreateSwiftTransferRequest;
import com.banking.swift.dto.SwiftTransferResponse;
import com.banking.swift.dto.SwiftTransferStatisticsResponse;
import com.banking.swift.model.SwiftTransfer;
import com.banking.swift.model.SwiftTransferStatus;
import com.banking.swift.service.SwiftTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for SWIFT transfer operations
 */
@RestController
@RequestMapping("/swift/transfers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SWIFT Transfers", description = "SWIFT international transfer operations")
@SecurityRequirement(name = "bearer-jwt")
public class SwiftController {

    private final SwiftTransferService swiftTransferService;

    @PostMapping
    @Operation(summary = "Create a new SWIFT transfer", description = "Initiates a new SWIFT MT103 transfer")
    public ResponseEntity<SwiftTransferResponse> createTransfer(
            @Valid @RequestBody CreateSwiftTransferRequest request) {

        log.info("Creating SWIFT transfer: amount={} {}, beneficiary={}",
                request.getAmount(), request.getCurrency(), request.getBeneficiaryName());

        // Map DTO to Entity
        SwiftTransfer transfer = mapToEntity(request);

        // Create transfer
        SwiftTransfer created = swiftTransferService.createSwiftTransfer(transfer);

        log.info("SWIFT transfer created successfully: reference={}", created.getTransactionReference());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToResponse(created));
    }

    @PostMapping("/{reference}/process")
    @Operation(summary = "Process a SWIFT transfer", description = "Processes a pending SWIFT transfer (compliance checks, MT103 generation)")
    public ResponseEntity<SwiftTransferResponse> processTransfer(
            @PathVariable("reference") String reference) {

        log.info("Processing SWIFT transfer: reference={}", reference);

        SwiftTransfer processed = swiftTransferService.processTransfer(reference);

        log.info("SWIFT transfer processed successfully: reference={}, status={}",
                reference, processed.getStatus());

        return ResponseEntity.ok(mapToResponse(processed));
    }

    @PostMapping("/{reference}/complete")
    @Operation(summary = "Complete a SWIFT transfer", description = "Marks a transfer as completed with settlement confirmation")
    public ResponseEntity<SwiftTransferResponse> completeTransfer(
            @PathVariable("reference") String reference,
            @RequestParam("settlementReference") String settlementReference) {

        log.info("Completing SWIFT transfer: reference={}, settlementReference={}",
                reference, settlementReference);

        SwiftTransfer completed = swiftTransferService.completeTransfer(reference, settlementReference);

        log.info("SWIFT transfer completed successfully: reference={}", reference);

        return ResponseEntity.ok(mapToResponse(completed));
    }

    @PostMapping("/{reference}/fail")
    @Operation(summary = "Mark a SWIFT transfer as failed", description = "Marks a transfer as failed with reason")
    public ResponseEntity<SwiftTransferResponse> failTransfer(
            @PathVariable("reference") String reference,
            @RequestParam("reason") String reason) {

        log.info("Failing SWIFT transfer: reference={}, reason={}", reference, reason);

        SwiftTransfer failed = swiftTransferService.failTransfer(reference, reason);

        log.info("SWIFT transfer marked as failed: reference={}", reference);

        return ResponseEntity.ok(mapToResponse(failed));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get SWIFT transfer by reference", description = "Retrieves a SWIFT transfer by transaction reference")
    public ResponseEntity<SwiftTransferResponse> getTransferByReference(
            @PathVariable("reference") String reference) {

        log.debug("Fetching SWIFT transfer: reference={}", reference);

        SwiftTransfer transfer = swiftTransferService.getTransferByReference(reference);

        return ResponseEntity.ok(mapToResponse(transfer));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get SWIFT transfers by account", description = "Retrieves all SWIFT transfers for a specific account")
    public ResponseEntity<List<SwiftTransferResponse>> getTransfersByAccount(
            @PathVariable("accountId") String accountId) {

        log.debug("Fetching SWIFT transfers for account: accountId={}", accountId);

        List<SwiftTransfer> transfers = swiftTransferService.getTransfersByAccount(accountId);

        List<SwiftTransferResponse> responses = transfers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get SWIFT transfers by status", description = "Retrieves SWIFT transfers filtered by status with pagination")
    public ResponseEntity<Page<SwiftTransferResponse>> getTransfersByStatus(
            @PathVariable("status") SwiftTransferStatus status,
            Pageable pageable) {

        log.debug("Fetching SWIFT transfers by status: status={}, page={}, size={}",
                status, pageable.getPageNumber(), pageable.getPageSize());

        Page<SwiftTransfer> transfers = swiftTransferService.getTransfersByStatus(status, pageable);

        Page<SwiftTransferResponse> responses = transfers.map(this::mapToResponse);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get SWIFT transfer statistics", description = "Retrieves comprehensive statistics about SWIFT transfers")
    public ResponseEntity<SwiftTransferStatisticsResponse> getStatistics() {

        log.debug("Fetching SWIFT transfer statistics");

        // Note: Need to implement proper statistics mapping
        SwiftTransferStatisticsResponse stats = SwiftTransferStatisticsResponse.builder()
                .totalTransfers(0L)
                .pendingCount(0L)
                .processingCount(0L)
                .completedCount(0L)
                .failedCount(0L)
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Maps CreateSwiftTransferRequest DTO to SwiftTransfer entity
     */
    private SwiftTransfer mapToEntity(CreateSwiftTransferRequest request) {
        return SwiftTransfer.builder()
                .valueDate(request.getValueDate())
                .currency(request.getCurrency())
                .amount(request.getAmount())
                .orderingCustomerName(request.getOrderingCustomerName())
                .orderingCustomerAddress(request.getOrderingCustomerAddress())
                .orderingCustomerAccount(request.getOrderingCustomerAccount())
                .senderBic(request.getSenderBic())
                .senderName(request.getSenderName())
                .beneficiaryBankBic(request.getBeneficiaryBankBic())
                .beneficiaryBankName(request.getBeneficiaryBankName())
                .beneficiaryName(request.getBeneficiaryName())
                .beneficiaryAddress(request.getBeneficiaryAddress())
                .beneficiaryAccount(request.getBeneficiaryAccount())
                .remittanceInfo(request.getRemittanceInfo())
                .chargeType(request.getChargeType())
                .build();
    }

    /**
     * Maps SwiftTransfer entity to SwiftTransferResponse DTO
     */
    private SwiftTransferResponse mapToResponse(SwiftTransfer transfer) {
        return SwiftTransferResponse.builder()
                .id(transfer.getId())
                .transactionReference(transfer.getTransactionReference())
                .messageType(transfer.getMessageType())
                .valueDate(transfer.getValueDate())
                .currency(transfer.getCurrency())
                .amount(transfer.getAmount())
                .orderingCustomerName(transfer.getOrderingCustomerName())
                .orderingCustomerAddress(transfer.getOrderingCustomerAddress())
                .orderingCustomerAccount(transfer.getOrderingCustomerAccount())
                .senderBic(transfer.getSenderBic())
                .senderName(transfer.getSenderName())
                .correspondentBic(transfer.getCorrespondentBic())
                .correspondentName(transfer.getCorrespondentName())
                .correspondentAccount(transfer.getCorrespondentAccount())
                .beneficiaryBankBic(transfer.getBeneficiaryBankBic())
                .beneficiaryBankName(transfer.getBeneficiaryBankName())
                .beneficiaryName(transfer.getBeneficiaryName())
                .beneficiaryAddress(transfer.getBeneficiaryAddress())
                .beneficiaryAccount(transfer.getBeneficiaryAccount())
                .remittanceInfo(transfer.getRemittanceInfo())
                .chargeType(transfer.getChargeType())
                .fixedFee(transfer.getFixedFee())
                .percentageFee(transfer.getPercentageFee())
                .totalFee(transfer.getTotalFee())
                .status(transfer.getStatus())
                .statusReason(transfer.getStatusReason())
                .ofacChecked(transfer.getOfacChecked())
                .sanctionsChecked(transfer.getSanctionsChecked())
                .complianceCleared(transfer.getComplianceCleared())
                .complianceNotes(transfer.getComplianceNotes())
                .settlementDate(transfer.getSettlementDate())
                .settlementReference(transfer.getSettlementReference())
                .createdAt(transfer.getCreatedAt())
                .updatedAt(transfer.getUpdatedAt())
                .build();
    }
}
