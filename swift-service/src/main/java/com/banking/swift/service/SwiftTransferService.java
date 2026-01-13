package com.banking.swift.service;

import com.banking.swift.model.ChargeType;
import com.banking.swift.model.SwiftTransfer;
import com.banking.swift.model.SwiftTransferStatus;
import com.banking.swift.repository.SwiftTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SwiftTransferService {

    private final SwiftTransferRepository swiftTransferRepository;
    private final Mt103MessageGenerator mt103MessageGenerator;
    private final BicValidationService bicValidationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${swift.fees.fixed-fee:25.00}")
    private BigDecimal fixedFee;

    @Value("${swift.fees.percentage-fee:0.001}")
    private BigDecimal percentageFee;

    @Value("${swift.correspondent-bank.bic}")
    private String correspondentBic;

    @Value("${swift.correspondent-bank.name}")
    private String correspondentName;

    /**
     * Create and initiate SWIFT transfer
     */
    @CacheEvict(value = "swift-transfers", allEntries = true)
    public SwiftTransfer createSwiftTransfer(SwiftTransfer transfer) {
        log.info("Creating SWIFT transfer for amount {} {}", transfer.getAmount(), transfer.getCurrency());

        // Generate transaction reference
        transfer.setTransactionReference(generateTransactionReference());

        // Validate BIC codes
        validateBicCodes(transfer);

        // Set correspondent bank (our intermediary)
        transfer.setCorrespondentBic(correspondentBic);
        transfer.setCorrespondentName(correspondentName);

        // Calculate fees
        transfer.calculateFees(fixedFee, percentageFee);

        // Set initial status
        transfer.setStatus(SwiftTransferStatus.PENDING);

        // Set value date if not provided (next business day)
        if (transfer.getValueDate() == null) {
            transfer.setValueDate(getNextBusinessDay());
        }

        // Save transfer
        SwiftTransfer savedTransfer = swiftTransferRepository.save(transfer);

        log.info("SWIFT transfer created: {}", savedTransfer.getTransactionReference());

        return savedTransfer;
    }

    /**
     * Process SWIFT transfer - Generate MT103 and submit
     */
    @CacheEvict(value = "swift-transfers", allEntries = true)
    public SwiftTransfer processTransfer(String transactionReference) {
        log.info("Processing SWIFT transfer: {}", transactionReference);

        SwiftTransfer transfer = swiftTransferRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transactionReference));

        if (transfer.getStatus() != SwiftTransferStatus.PENDING) {
            throw new RuntimeException("Transfer is not in PENDING status");
        }

        // Validate transfer
        transfer.setStatus(SwiftTransferStatus.VALIDATING);
        swiftTransferRepository.save(transfer);

        // Compliance checks
        transfer.setStatus(SwiftTransferStatus.COMPLIANCE_CHECK);
        performComplianceChecks(transfer);

        // Generate MT103 message
        transfer.setStatus(SwiftTransferStatus.PROCESSING);
        String mt103Message = mt103MessageGenerator.generateMt103Message(transfer);
        transfer.setMt103Message(mt103Message);
        transfer.markAsProcessing();

        // Submit to SWIFT network (simulated)
        transfer.setStatus(SwiftTransferStatus.SUBMITTED);
        transfer.setSettlementDate(transfer.getValueDate());

        SwiftTransfer processedTransfer = swiftTransferRepository.save(transfer);

        // Publish event
        publishTransferSubmittedEvent(processedTransfer);

        log.info("SWIFT transfer processed and submitted: {}", transactionReference);

        return processedTransfer;
    }

    /**
     * Complete SWIFT transfer (settlement confirmed)
     */
    @CacheEvict(value = "swift-transfers", allEntries = true)
    public SwiftTransfer completeTransfer(String transactionReference, String settlementReference) {
        log.info("Completing SWIFT transfer: {}", transactionReference);

        SwiftTransfer transfer = swiftTransferRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transactionReference));

        transfer.markAsCompleted(settlementReference);
        SwiftTransfer completedTransfer = swiftTransferRepository.save(transfer);

        // Publish event
        publishTransferCompletedEvent(completedTransfer);

        log.info("SWIFT transfer completed: {}", transactionReference);

        return completedTransfer;
    }

    /**
     * Fail SWIFT transfer
     */
    @CacheEvict(value = "swift-transfers", allEntries = true)
    public SwiftTransfer failTransfer(String transactionReference, String reason) {
        log.info("Failing SWIFT transfer: {} - Reason: {}", transactionReference, reason);

        SwiftTransfer transfer = swiftTransferRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transactionReference));

        transfer.markAsFailed(reason);
        SwiftTransfer failedTransfer = swiftTransferRepository.save(transfer);

        // Publish event
        publishTransferFailedEvent(failedTransfer);

        return failedTransfer;
    }

    /**
     * Get transfer by reference
     */
    @Cacheable(value = "swift-transfers", key = "#transactionReference")
    public SwiftTransfer getTransferByReference(String transactionReference) {
        return swiftTransferRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transactionReference));
    }

    /**
     * Get transfers by account
     */
    public List<SwiftTransfer> getTransfersByAccount(String accountId) {
        return swiftTransferRepository.findByInternalAccountId(accountId);
    }

    /**
     * Get transfers by status
     */
    public Page<SwiftTransfer> getTransfersByStatus(SwiftTransferStatus status, Pageable pageable) {
        return swiftTransferRepository.findByStatus(status, pageable);
    }

    /**
     * Get statistics
     */
    public SwiftTransferStatistics getStatistics() {
        long pending = swiftTransferRepository.countByStatus(SwiftTransferStatus.PENDING);
        long processing = swiftTransferRepository.countByStatus(SwiftTransferStatus.PROCESSING);
        long completed = swiftTransferRepository.countByStatus(SwiftTransferStatus.COMPLETED);
        long failed = swiftTransferRepository.countByStatus(SwiftTransferStatus.FAILED);

        BigDecimal totalVolume = swiftTransferRepository.sumCompletedTransfersAfterDate(
                LocalDateTime.now().minusDays(30)
        );

        return SwiftTransferStatistics.builder()
                .pendingCount(pending)
                .processingCount(processing)
                .completedCount(completed)
                .failedCount(failed)
                .totalVolumeL30D(totalVolume != null ? totalVolume : BigDecimal.ZERO)
                .build();
    }

    // Helper methods

    private void validateBicCodes(SwiftTransfer transfer) {
        if (!bicValidationService.isValidBic(transfer.getSenderBic())) {
            throw new RuntimeException("Invalid sender BIC: " + transfer.getSenderBic());
        }
        if (!bicValidationService.isValidBic(transfer.getBeneficiaryBankBic())) {
            throw new RuntimeException("Invalid beneficiary bank BIC: " + transfer.getBeneficiaryBankBic());
        }
    }

    private void performComplianceChecks(SwiftTransfer transfer) {
        // Simulate OFAC and sanctions screening
        transfer.markComplianceCleared();
        log.info("Compliance checks passed for transfer: {}", transfer.getTransactionReference());
    }

    private String generateTransactionReference() {
        // Format: SWFT + 12 digit unique number
        return "SWFT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private LocalDate getNextBusinessDay() {
        LocalDate date = LocalDate.now().plusDays(1);
        // Skip weekends (simplified - real implementation would check holidays)
        while (date.getDayOfWeek().getValue() > 5) {
            date = date.plusDays(1);
        }
        return date;
    }

    // Kafka event publishing

    private void publishTransferSubmittedEvent(SwiftTransfer transfer) {
        kafkaTemplate.send("swift.transfer.submitted", transfer.getTransactionReference(), transfer);
    }

    private void publishTransferCompletedEvent(SwiftTransfer transfer) {
        kafkaTemplate.send("swift.transfer.completed", transfer.getTransactionReference(), transfer);
    }

    private void publishTransferFailedEvent(SwiftTransfer transfer) {
        kafkaTemplate.send("swift.transfer.failed", transfer.getTransactionReference(), transfer);
    }

    // Statistics DTO
    @lombok.Builder
    @lombok.Data
    public static class SwiftTransferStatistics {
        private long pendingCount;
        private long processingCount;
        private long completedCount;
        private long failedCount;
        private BigDecimal totalVolumeL30D;
    }
}