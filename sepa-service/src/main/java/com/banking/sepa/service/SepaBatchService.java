package com.banking.sepa.service;

import com.banking.sepa.model.SepaBatch;
import com.banking.sepa.repository.SepaBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing SEPA batch processing.
 * Handles batch creation, validation, submission, and tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SepaBatchService {

    private final SepaBatchRepository batchRepository;
    private final IsoXmlGeneratorService isoXmlGeneratorService;

    /**
     * Creates a new SEPA batch.
     *
     * @param batch The batch to create
     * @return The created batch
     */
    @Transactional
    @CacheEvict(value = "batches", allEntries = true)
    public SepaBatch createBatch(SepaBatch batch) {
        log.info("Creating new SEPA batch: type={}, transfers={}", batch.getBatchType(), batch.getNumberOfTransactions());

        // Set default status
        if (batch.getStatus() == null) {
            batch.setStatus(SepaBatch.BatchStatus.PENDING);
        }

        // Set creation time
        if (batch.getCreationDateTime() == null) {
            batch.setCreationDateTime(LocalDateTime.now());
        }

        // Initialize statistics
        if (batch.getNumberOfTransactions() == null) {
            batch.setNumberOfTransactions(0);
        }
        if (batch.getTotalAmount() == null) {
            batch.setTotalAmount(BigDecimal.ZERO);
        }

        SepaBatch savedBatch = batchRepository.save(batch);
        log.info("Created SEPA batch: {}", savedBatch.getBatchId());

        return savedBatch;
    }

    /**
     * Adds transfers to a batch.
     *
     * @param batchId The batch ID
     * @param transferReferences List of transfer references to add
     * @return The updated batch
     */
    @Transactional
    @CacheEvict(value = "batches", allEntries = true)
    public SepaBatch addTransfersToBatch(String batchId, List<String> transferReferences, BigDecimal totalAmount) {
        log.info("Adding {} transfers to batch: {}", transferReferences.size(), batchId);

        SepaBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (batch.getStatus() != SepaBatch.BatchStatus.PENDING) {
            throw new IllegalStateException("Can only add transfers to PENDING batches. Current status: " + batch.getStatus());
        }

        // Add transfer references
        batch.getTransferReferences().addAll(transferReferences);

        // Update statistics
        batch.setNumberOfTransactions(batch.getTransferReferences().size());
        batch.setTotalAmount(batch.getTotalAmount().add(totalAmount));
        batch.setPendingTransactions(batch.getNumberOfTransactions());

        SepaBatch updatedBatch = batchRepository.save(batch);
        log.info("Added transfers to batch: {}, total transfers: {}", batchId, updatedBatch.getNumberOfTransactions());

        return updatedBatch;
    }

    /**
     * Validates a batch.
     *
     * @param batchId The batch ID
     * @return The validated batch
     */
    @Transactional
    @CacheEvict(value = "batches", allEntries = true)
    public SepaBatch validateBatch(String batchId) {
        log.info("Validating batch: {}", batchId);

        SepaBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (batch.getStatus() != SepaBatch.BatchStatus.PENDING) {
            throw new IllegalStateException("Only PENDING batches can be validated. Current status: " + batch.getStatus());
        }

        batch.setStatus(SepaBatch.BatchStatus.VALIDATING);
        batchRepository.save(batch);

        try {
            // Validate batch content
            if (batch.getNumberOfTransactions() == 0) {
                throw new IllegalArgumentException("Batch contains no transfers");
            }

            if (batch.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Batch total amount must be positive");
            }

            // Generate ISO 20022 XML
            String painXml = isoXmlGeneratorService.generatePainXml(batch);
            batch.setPainXml(painXml);

            // Mark as validated
            batch.setStatus(SepaBatch.BatchStatus.VALIDATED);
            batch.setHasErrors(false);

            log.info("Batch validation successful: {}", batchId);
        } catch (Exception e) {
            log.error("Batch validation failed: {}", batchId, e);
            batch.setStatus(SepaBatch.BatchStatus.FAILED);
            batch.setHasErrors(true);
            batch.setErrorCount(1);
            batch.setErrorSummary(e.getMessage());
        }

        return batchRepository.save(batch);
    }

    /**
     * Submits a validated batch.
     *
     * @param batchId The batch ID
     * @param submittedBy Who submitted the batch
     * @return The submitted batch
     */
    @Transactional
    @CacheEvict(value = "batches", allEntries = true)
    public SepaBatch submitBatch(String batchId, String submittedBy) {
        log.info("Submitting batch: {}", batchId);

        SepaBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (batch.getStatus() != SepaBatch.BatchStatus.VALIDATED) {
            throw new IllegalStateException("Only VALIDATED batches can be submitted. Current status: " + batch.getStatus());
        }

        batch.setStatus(SepaBatch.BatchStatus.SUBMITTED);
        batch.setSubmittedAt(LocalDateTime.now());
        batch.setSubmittedBy(submittedBy);

        // In real implementation, this would submit to SEPA network
        // For now, we just mark as submitted
        batch.setSubmissionReference("SUB-" + System.currentTimeMillis());

        SepaBatch updatedBatch = batchRepository.save(batch);
        log.info("Submitted batch: {}, submission reference: {}", batchId, updatedBatch.getSubmissionReference());

        return updatedBatch;
    }

    /**
     * Starts processing a submitted batch.
     *
     * @param batchId The batch ID
     * @return The batch in processing
     */
    @Transactional
    @CacheEvict(value = "batches", allEntries = true)
    public SepaBatch startProcessing(String batchId) {
        log.info("Starting batch processing: {}", batchId);

        SepaBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (batch.getStatus() != SepaBatch.BatchStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED batches can be processed. Current status: " + batch.getStatus());
        }

        batch.setStatus(SepaBatch.BatchStatus.PROCESSING);
        batch.setProcessingStartedAt(LocalDateTime.now());

        return batchRepository.save(batch);
    }

    /**
     * Records a transfer result in the batch.
     *
     * @param batchId The batch ID
     * @param transferReference The transfer reference
     * @param successful Whether the transfer was successful
     * @param amount The transfer amount
     * @return The updated batch
     */
    @Transactional
    @CacheEvict(value = "batches", allEntries = true)
    public SepaBatch recordTransferResult(String batchId, String transferReference, boolean successful, BigDecimal amount) {
        log.info("Recording transfer result for batch: {}, transfer: {}, successful: {}", batchId, transferReference, successful);

        SepaBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (batch.getStatus() != SepaBatch.BatchStatus.PROCESSING &&
            batch.getStatus() != SepaBatch.BatchStatus.PARTIALLY_COMPLETE) {
            throw new IllegalStateException("Can only record results for PROCESSING/PARTIALLY_COMPLETE batches");
        }

        // Update statistics
        if (successful) {
            batch.setSuccessfulTransactions(batch.getSuccessfulTransactions() + 1);
            if (batch.getSuccessfulAmount() == null) {
                batch.setSuccessfulAmount(BigDecimal.ZERO);
            }
            batch.setSuccessfulAmount(batch.getSuccessfulAmount().add(amount));
        } else {
            batch.setFailedTransactions(batch.getFailedTransactions() + 1);
            batch.setErrorCount(batch.getErrorCount() + 1);
        }

        // Update pending count
        batch.setPendingTransactions(batch.getNumberOfTransactions() - batch.getSuccessfulTransactions() - batch.getFailedTransactions());

        // Check if batch is complete
        if (batch.getPendingTransactions() == 0) {
            if (batch.getFailedTransactions() == 0) {
                batch.setStatus(SepaBatch.BatchStatus.COMPLETED);
            } else if (batch.getSuccessfulTransactions() > 0) {
                batch.setStatus(SepaBatch.BatchStatus.PARTIALLY_COMPLETE);
            } else {
                batch.setStatus(SepaBatch.BatchStatus.FAILED);
            }
            batch.setProcessingCompletedAt(LocalDateTime.now());
        } else if (batch.getSuccessfulTransactions() > 0 || batch.getFailedTransactions() > 0) {
            batch.setStatus(SepaBatch.BatchStatus.PARTIALLY_COMPLETE);
        }

        return batchRepository.save(batch);
    }

    /**
     * Cancels a batch.
     *
     * @param batchId The batch ID
     * @return The cancelled batch
     */
    @Transactional
    @CacheEvict(value = "batches", allEntries = true)
    public SepaBatch cancelBatch(String batchId) {
        log.info("Cancelling batch: {}", batchId);

        SepaBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        if (batch.getStatus() == SepaBatch.BatchStatus.COMPLETED ||
            batch.getStatus() == SepaBatch.BatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel batch with status: " + batch.getStatus());
        }

        batch.setStatus(SepaBatch.BatchStatus.CANCELLED);

        return batchRepository.save(batch);
    }

    /**
     * Retrieves a batch by ID.
     *
     * @param batchId The batch ID
     * @return The batch
     */
    @Cacheable(value = "batches", key = "#batchId")
    public SepaBatch getBatchById(String batchId) {
        return batchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
    }

    /**
     * Retrieves pending batches.
     *
     * @return List of pending batches
     */
    public List<SepaBatch> getPendingBatches() {
        return batchRepository.findPendingBatches();
    }

    /**
     * Retrieves processing batches.
     *
     * @return List of processing batches
     */
    public List<SepaBatch> getProcessingBatches() {
        return batchRepository.findProcessingBatches();
    }

    /**
     * Retrieves batches with errors.
     *
     * @return List of batches with errors
     */
    public List<SepaBatch> getBatchesWithErrors() {
        return batchRepository.findByHasErrorsTrueOrderByCreatedAtDesc();
    }

    /**
     * Retrieves stale batches (created but not submitted within specified hours).
     *
     * @param hours Number of hours
     * @return List of stale batches
     */
    public List<SepaBatch> getStaleBatches(int hours) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(hours);
        return batchRepository.findStaleBatches(cutoffDate);
    }

    /**
     * Retrieves batches by initiating party.
     *
     * @param initiatingPartyId The initiating party ID
     * @return List of batches
     */
    @Cacheable(value = "batches", key = "'party_' + #initiatingPartyId")
    public List<SepaBatch> getBatchesByInitiatingParty(String initiatingPartyId) {
        return batchRepository.findByInitiatingPartyIdOrderByCreatedAtDesc(initiatingPartyId);
    }
}
