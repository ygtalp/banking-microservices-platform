package com.banking.transfer.controller;

import com.banking.transfer.dto.ApiResponse;
import com.banking.transfer.dto.TransferRequest;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> initiateTransfer(
            @Valid @RequestBody TransferRequest request) {
        log.info("POST /api/v1/transfers - Initiating transfer");

        TransferResponse response = transferService.initiateTransfer(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Transfer initiated successfully"));
    }

    @GetMapping("/{transferReference}")
    public ResponseEntity<ApiResponse<TransferResponse>> getTransfer(
            @PathVariable("transferReference") String transferReference) {
        log.info("GET /api/v1/transfers/{} - Fetching transfer", transferReference);

        TransferResponse response = transferService.getTransferByReference(transferReference);

        return ResponseEntity.ok(ApiResponse.success(response, "Transfer retrieved successfully"));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getTransfersByAccount(
            @PathVariable("accountNumber") String accountNumber) {
        log.info("GET /api/v1/transfers/account/{} - Fetching transfers", accountNumber);

        List<TransferResponse> transfers = transferService.getTransfersByAccount(accountNumber);

        return ResponseEntity.ok(ApiResponse.success(transfers,
                "Transfers retrieved successfully - Count: " + transfers.size()));
    }

    @GetMapping("/from/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getTransfersFrom(
            @PathVariable("accountNumber") String accountNumber) {
        log.info("GET /api/v1/transfers/from/{} - Fetching outgoing transfers", accountNumber);

        List<TransferResponse> transfers = transferService.getTransfersFrom(accountNumber);

        return ResponseEntity.ok(ApiResponse.success(transfers,
                "Outgoing transfers retrieved - Count: " + transfers.size()));
    }

    @GetMapping("/to/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getTransfersTo(
            @PathVariable("accountNumber") String accountNumber) {
        log.info("GET /api/v1/transfers/to/{} - Fetching incoming transfers", accountNumber);

        List<TransferResponse> transfers = transferService.getTransfersTo(accountNumber);

        return ResponseEntity.ok(ApiResponse.success(transfers,
                "Incoming transfers retrieved - Count: " + transfers.size()));
    }
}