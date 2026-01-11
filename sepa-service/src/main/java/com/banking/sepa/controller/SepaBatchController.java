package com.banking.sepa.controller;

import com.banking.sepa.model.SepaBatch;
import com.banking.sepa.service.SepaBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for SEPA batch processing.
 * Provides endpoints for creating, validating, submitting, and tracking batch payments.
 */
@RestController
@RequestMapping("/sepa/batches")
@RequiredArgsConstructor
@Slf4j
public class SepaBatchController {

    private final SepaBatchService batchService;

    /**
     * Creates a new SEPA batch.
     *
     * @param batch The batch to create
     * @return The created batch
     */
    @PostMapping
    public ResponseEntity<SepaBatch> createBatch(@RequestBody SepaBatch batch) {
        log.info("REST request to create SEPA batch");
        SepaBatch createdBatch = batchService.createBatch(batch);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBatch);
    }

    /**
     * Retrieves a batch by ID.
     *
     * @param batchId The batch ID
     * @return The batch
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<SepaBatch> getBatchById(@PathVariable("batchId") String batchId) {
        log.info("REST request to get batch: {}", batchId);
        SepaBatch batch = batchService.getBatchById(batchId);
        return ResponseEntity.ok(batch);
    }

    /**
     * Adds transfers to a batch.
     *
     * @param batchId The batch ID
     * @param request The add transfers request
     * @return The updated batch
     */
    @PostMapping("/{batchId}/transfers")
    public ResponseEntity<SepaBatch> addTransfersToBatch(
            @PathVariable("batchId") String batchId,
            @RequestBody AddTransfersRequest request) {
        log.info("REST request to add {} transfers to batch: {}", request.getTransferReferences().size(), batchId);
        SepaBatch updatedBatch = batchService.addTransfersToBatch(
                batchId,
                request.getTransferReferences(),
                request.getTotalAmount()
        );
        return ResponseEntity.ok(updatedBatch);
    }

    /**
     * Validates a batch.
     *
     * @param batchId The batch ID
     * @return The validated batch
     */
    @PostMapping("/{batchId}/validate")
    public ResponseEntity<SepaBatch> validateBatch(@PathVariable("batchId") String batchId) {
        log.info("REST request to validate batch: {}", batchId);
        SepaBatch validatedBatch = batchService.validateBatch(batchId);
        return ResponseEntity.ok(validatedBatch);
    }

    /**
     * Submits a validated batch.
     *
     * @param batchId The batch ID
     * @param request The submit request
     * @return The submitted batch
     */
    @PostMapping("/{batchId}/submit")
    public ResponseEntity<SepaBatch> submitBatch(
            @PathVariable("batchId") String batchId,
            @RequestBody SubmitRequest request) {
        log.info("REST request to submit batch: {}", batchId);
        SepaBatch submittedBatch = batchService.submitBatch(batchId, request.getSubmittedBy());
        return ResponseEntity.ok(submittedBatch);
    }

    /**
     * Starts processing a submitted batch.
     *
     * @param batchId The batch ID
     * @return The batch in processing
     */
    @PostMapping("/{batchId}/process")
    public ResponseEntity<SepaBatch> startProcessing(@PathVariable("batchId") String batchId) {
        log.info("REST request to start processing batch: {}", batchId);
        SepaBatch processingBatch = batchService.startProcessing(batchId);
        return ResponseEntity.ok(processingBatch);
    }

    /**
     * Records a transfer result in the batch.
     *
     * @param batchId The batch ID
     * @param request The transfer result request
     * @return The updated batch
     */
    @PostMapping("/{batchId}/results")
    public ResponseEntity<SepaBatch> recordTransferResult(
            @PathVariable("batchId") String batchId,
            @RequestBody TransferResultRequest request) {
        log.info("REST request to record transfer result for batch: {}", batchId);
        SepaBatch updatedBatch = batchService.recordTransferResult(
                batchId,
                request.getTransferReference(),
                request.isSuccessful(),
                request.getAmount()
        );
        return ResponseEntity.ok(updatedBatch);
    }

    /**
     * Cancels a batch.
     *
     * @param batchId The batch ID
     * @return The cancelled batch
     */
    @PostMapping("/{batchId}/cancel")
    public ResponseEntity<SepaBatch> cancelBatch(@PathVariable("batchId") String batchId) {
        log.info("REST request to cancel batch: {}", batchId);
        SepaBatch cancelledBatch = batchService.cancelBatch(batchId);
        return ResponseEntity.ok(cancelledBatch);
    }

    /**
     * Retrieves pending batches.
     *
     * @return List of pending batches
     */
    @GetMapping("/pending")
    public ResponseEntity<List<SepaBatch>> getPendingBatches() {
        log.info("REST request to get pending batches");
        List<SepaBatch> batches = batchService.getPendingBatches();
        return ResponseEntity.ok(batches);
    }

    /**
     * Retrieves processing batches.
     *
     * @return List of processing batches
     */
    @GetMapping("/processing")
    public ResponseEntity<List<SepaBatch>> getProcessingBatches() {
        log.info("REST request to get processing batches");
        List<SepaBatch> batches = batchService.getProcessingBatches();
        return ResponseEntity.ok(batches);
    }

    /**
     * Retrieves batches with errors.
     *
     * @return List of batches with errors
     */
    @GetMapping("/errors")
    public ResponseEntity<List<SepaBatch>> getBatchesWithErrors() {
        log.info("REST request to get batches with errors");
        List<SepaBatch> batches = batchService.getBatchesWithErrors();
        return ResponseEntity.ok(batches);
    }

    /**
     * Retrieves stale batches.
     *
     * @param hours Number of hours
     * @return List of stale batches
     */
    @GetMapping("/stale")
    public ResponseEntity<List<SepaBatch>> getStaleBatches(
            @RequestParam(value = "hours", defaultValue = "24") int hours) {
        log.info("REST request to get batches stale for {} hours", hours);
        List<SepaBatch> batches = batchService.getStaleBatches(hours);
        return ResponseEntity.ok(batches);
    }

    /**
     * Retrieves batches by initiating party.
     *
     * @param initiatingPartyId The initiating party ID
     * @return List of batches
     */
    @GetMapping("/party/{initiatingPartyId}")
    public ResponseEntity<List<SepaBatch>> getBatchesByInitiatingParty(
            @PathVariable("initiatingPartyId") String initiatingPartyId) {
        log.info("REST request to get batches for party: {}", initiatingPartyId);
        List<SepaBatch> batches = batchService.getBatchesByInitiatingParty(initiatingPartyId);
        return ResponseEntity.ok(batches);
    }

    // DTOs

    public static class AddTransfersRequest {
        private List<String> transferReferences;
        private BigDecimal totalAmount;

        public AddTransfersRequest() {
        }

        public List<String> getTransferReferences() {
            return transferReferences;
        }

        public void setTransferReferences(List<String> transferReferences) {
            this.transferReferences = transferReferences;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }
    }

    public static class SubmitRequest {
        private String submittedBy;

        public SubmitRequest() {
        }

        public String getSubmittedBy() {
            return submittedBy;
        }

        public void setSubmittedBy(String submittedBy) {
            this.submittedBy = submittedBy;
        }
    }

    public static class TransferResultRequest {
        private String transferReference;
        private boolean successful;
        private BigDecimal amount;

        public TransferResultRequest() {
        }

        public String getTransferReference() {
            return transferReference;
        }

        public void setTransferReference(String transferReference) {
            this.transferReference = transferReference;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
