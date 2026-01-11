package com.banking.sepa.controller;

import com.banking.sepa.model.SepaTransfer;
import com.banking.sepa.model.SepaTransfer.SepaTransferStatus;
import com.banking.sepa.service.SepaTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sepa")
@RequiredArgsConstructor
@Slf4j
public class SepaController {

    private final SepaTransferService sepaTransferService;

    @PostMapping("/transfers")
    public ResponseEntity<SepaTransfer> initiateTransfer(@RequestBody SepaTransfer transfer) {
        SepaTransfer created = sepaTransferService.initiateTransfer(transfer);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/transfers/{sepaReference}")
    public ResponseEntity<SepaTransfer> getTransfer(@PathVariable("sepaReference") String sepaReference) {
        return sepaTransferService.getTransfer(sepaReference)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/transfers/debtor-iban/{iban}")
    public ResponseEntity<List<SepaTransfer>> getTransfersByDebtorIban(@PathVariable("iban") String iban) {
        return ResponseEntity.ok(sepaTransferService.getTransfersByDebtorIban(iban));
    }

    @GetMapping("/transfers/creditor-iban/{iban}")
    public ResponseEntity<List<SepaTransfer>> getTransfersByCreditorIban(@PathVariable("iban") String iban) {
        return ResponseEntity.ok(sepaTransferService.getTransfersByCreditorIban(iban));
    }

    @GetMapping("/transfers/account/{accountNumber}")
    public ResponseEntity<List<SepaTransfer>> getTransfersByAccount(
            @PathVariable("accountNumber") String accountNumber) {
        return ResponseEntity.ok(sepaTransferService.getTransfersByAccount(accountNumber));
    }

    @GetMapping("/transfers/status/{status}")
    public ResponseEntity<List<SepaTransfer>> getTransfersByStatus(
            @PathVariable("status") SepaTransferStatus status) {
        return ResponseEntity.ok(sepaTransferService.getTransfersByStatus(status));
    }

    @GetMapping("/transfers/pending")
    public ResponseEntity<List<SepaTransfer>> getPendingTransfers() {
        return ResponseEntity.ok(sepaTransferService.getPendingTransfers());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", sepaTransferService.getTransferCount(SepaTransferStatus.PENDING));
        stats.put("submitted", sepaTransferService.getTransferCount(SepaTransferStatus.SUBMITTED));
        stats.put("completed", sepaTransferService.getTransferCount(SepaTransferStatus.COMPLETED));
        stats.put("failed", sepaTransferService.getTransferCount(SepaTransferStatus.FAILED));
        return ResponseEntity.ok(stats);
    }
}
