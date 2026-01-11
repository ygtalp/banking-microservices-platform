package com.banking.sepa.service;

import com.banking.sepa.model.SepaReturn;
import com.banking.sepa.repository.SepaReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling SEPA R-Transactions (returns, rejections, refunds, recalls, reversals).
 * Manages the lifecycle of return transactions and refund processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SepaReturnService {

    private final SepaReturnRepository returnRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Initiates a SEPA return transaction.
     *
     * @param sepaReturn The return transaction to initiate
     * @return The initiated return
     */
    @Transactional
    @CacheEvict(value = "returns", allEntries = true)
    public SepaReturn initiateReturn(SepaReturn sepaReturn) {
        log.info("Initiating SEPA return: type={}, originalRef={}, reason={}",
                sepaReturn.getReturnType(), sepaReturn.getOriginalSepaReference(), sepaReturn.getReturnReasonCode());

        // Set default status
        if (sepaReturn.getStatus() == null) {
            sepaReturn.setStatus(SepaReturn.ReturnStatus.INITIATED);
        }

        // Set initiated timestamp
        if (sepaReturn.getInitiatedAt() == null) {
            sepaReturn.setInitiatedAt(LocalDateTime.now());
        }

        // Validate reason code
        validateReasonCode(sepaReturn.getReturnReasonCode());

        // Save return
        SepaReturn savedReturn = returnRepository.save(sepaReturn);
        log.info("Initiated SEPA return: {}", savedReturn.getReturnId());

        // Publish event
        publishReturnInitiatedEvent(savedReturn);

        return savedReturn;
    }

    /**
     * Validates a return transaction.
     *
     * @param returnId The return ID
     * @return The validated return
     */
    @Transactional
    @CacheEvict(value = "returns", allEntries = true)
    public SepaReturn validateReturn(String returnId) {
        log.info("Validating return: {}", returnId);

        SepaReturn sepaReturn = returnRepository.findById(returnId)
            .orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnId));

        if (sepaReturn.getStatus() != SepaReturn.ReturnStatus.INITIATED) {
            throw new IllegalStateException("Only INITIATED returns can be validated. Current status: " + sepaReturn.getStatus());
        }

        try {
            // Validation logic
            if (sepaReturn.getReturnAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Return amount must be positive");
            }

            // Mark as validated
            sepaReturn.setStatus(SepaReturn.ReturnStatus.VALIDATED);
            sepaReturn.setHasErrors(false);

            log.info("Return validation successful: {}", returnId);
        } catch (Exception e) {
            log.error("Return validation failed: {}", returnId, e);
            sepaReturn.setStatus(SepaReturn.ReturnStatus.FAILED);
            sepaReturn.setHasErrors(true);
            sepaReturn.setErrorMessage(e.getMessage());
        }

        return returnRepository.save(sepaReturn);
    }

    /**
     * Processes a validated return.
     *
     * @param returnId The return ID
     * @return The processed return
     */
    @Transactional
    @CacheEvict(value = "returns", allEntries = true)
    public SepaReturn processReturn(String returnId) {
        log.info("Processing return: {}", returnId);

        SepaReturn sepaReturn = returnRepository.findById(returnId)
            .orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnId));

        if (sepaReturn.getStatus() != SepaReturn.ReturnStatus.VALIDATED) {
            throw new IllegalStateException("Only VALIDATED returns can be processed. Current status: " + sepaReturn.getStatus());
        }

        sepaReturn.setStatus(SepaReturn.ReturnStatus.PROCESSING);
        sepaReturn.setProcessedAt(LocalDateTime.now());

        SepaReturn updatedReturn = returnRepository.save(sepaReturn);

        // In real implementation, this would interact with SEPA network
        // For now, we simulate processing
        log.info("Return processing started: {}", returnId);

        return updatedReturn;
    }

    /**
     * Completes a return transaction.
     *
     * @param returnId The return ID
     * @return The completed return
     */
    @Transactional
    @CacheEvict(value = "returns", allEntries = true)
    public SepaReturn completeReturn(String returnId) {
        log.info("Completing return: {}", returnId);

        SepaReturn sepaReturn = returnRepository.findById(returnId)
            .orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnId));

        if (sepaReturn.getStatus() != SepaReturn.ReturnStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING returns can be completed. Current status: " + sepaReturn.getStatus());
        }

        sepaReturn.setStatus(SepaReturn.ReturnStatus.COMPLETED);
        sepaReturn.setCompletedAt(LocalDateTime.now());

        SepaReturn updatedReturn = returnRepository.save(sepaReturn);
        log.info("Return completed: {}", returnId);

        // Publish event
        publishReturnCompletedEvent(updatedReturn);

        return updatedReturn;
    }

    /**
     * Processes a refund for a completed return.
     *
     * @param returnId The return ID
     * @param refundSepaReference The refund SEPA reference
     * @param refundTransactionId The refund transaction ID
     * @return The refunded return
     */
    @Transactional
    @CacheEvict(value = "returns", allEntries = true)
    public SepaReturn processRefund(String returnId, String refundSepaReference, String refundTransactionId) {
        log.info("Processing refund for return: {}, refundRef: {}", returnId, refundSepaReference);

        SepaReturn sepaReturn = returnRepository.findById(returnId)
            .orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnId));

        if (sepaReturn.getStatus() != SepaReturn.ReturnStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED returns can be refunded. Current status: " + sepaReturn.getStatus());
        }

        sepaReturn.setStatus(SepaReturn.ReturnStatus.REFUNDED);
        sepaReturn.setRefundSepaReference(refundSepaReference);
        sepaReturn.setRefundTransactionId(refundTransactionId);
        sepaReturn.setRefundCompletedAt(LocalDateTime.now());

        SepaReturn updatedReturn = returnRepository.save(sepaReturn);
        log.info("Refund processed for return: {}", returnId);

        // Publish event
        publishRefundCompletedEvent(updatedReturn);

        return updatedReturn;
    }

    /**
     * Marks a return as failed.
     *
     * @param returnId The return ID
     * @param errorMessage The error message
     * @return The failed return
     */
    @Transactional
    @CacheEvict(value = "returns", allEntries = true)
    public SepaReturn failReturn(String returnId, String errorMessage) {
        log.info("Marking return as failed: {}", returnId);

        SepaReturn sepaReturn = returnRepository.findById(returnId)
            .orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnId));

        sepaReturn.setStatus(SepaReturn.ReturnStatus.FAILED);
        sepaReturn.setHasErrors(true);
        sepaReturn.setErrorMessage(errorMessage);

        SepaReturn updatedReturn = returnRepository.save(sepaReturn);
        log.error("Return failed: {}, error: {}", returnId, errorMessage);

        // Publish event
        publishReturnFailedEvent(updatedReturn);

        return updatedReturn;
    }

    /**
     * Retrieves a return by ID.
     *
     * @param returnId The return ID
     * @return The return
     */
    @Cacheable(value = "returns", key = "#returnId")
    public SepaReturn getReturnById(String returnId) {
        return returnRepository.findById(returnId)
            .orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnId));
    }

    /**
     * Retrieves all returns for an original SEPA reference.
     *
     * @param originalSepaReference The original SEPA reference
     * @return List of returns
     */
    @Cacheable(value = "returns", key = "'sepa_' + #originalSepaReference")
    public List<SepaReturn> getReturnsByOriginalSepaReference(String originalSepaReference) {
        return returnRepository.findByOriginalSepaReferenceOrderByCreatedAtDesc(originalSepaReference);
    }

    /**
     * Retrieves pending returns.
     *
     * @return List of pending returns
     */
    public List<SepaReturn> getPendingReturns() {
        return returnRepository.findPendingReturns();
    }

    /**
     * Retrieves returns with errors.
     *
     * @return List of returns with errors
     */
    public List<SepaReturn> getReturnsWithErrors() {
        return returnRepository.findByHasErrorsTrueOrderByCreatedAtDesc();
    }

    /**
     * Retrieves returns by reason code.
     *
     * @param reasonCode The reason code
     * @return List of returns
     */
    public List<SepaReturn> getReturnsByReasonCode(String reasonCode) {
        return returnRepository.findByReturnReasonCodeOrderByCreatedAtDesc(reasonCode);
    }

    /**
     * Retrieves returns by debtor IBAN.
     *
     * @param debtorIban The debtor IBAN
     * @return List of returns
     */
    public List<SepaReturn> getReturnsByDebtorIban(String debtorIban) {
        return returnRepository.findByDebtorIbanOrderByCreatedAtDesc(debtorIban);
    }

    /**
     * Retrieves returns by creditor IBAN.
     *
     * @param creditorIban The creditor IBAN
     * @return List of returns
     */
    public List<SepaReturn> getReturnsByCreditorIban(String creditorIban) {
        return returnRepository.findByCreditorIbanOrderByCreatedAtDesc(creditorIban);
    }

    /**
     * Gets return statistics by reason code.
     *
     * @param reasonCode The reason code
     * @return Return statistics
     */
    public ReturnStatistics getReturnStatisticsByReasonCode(String reasonCode) {
        Long count = returnRepository.countByReturnReasonCode(reasonCode);
        BigDecimal totalAmount = returnRepository.sumReturnAmountByReasonCode(reasonCode);

        return new ReturnStatistics(reasonCode, count, totalAmount != null ? totalAmount : BigDecimal.ZERO);
    }

    /**
     * Validates a SEPA return reason code.
     */
    private void validateReasonCode(String reasonCode) {
        if (reasonCode == null || reasonCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Return reason code is required");
        }

        // Basic validation - in production, validate against full list of SEPA reason codes
        if (!reasonCode.matches("^[A-Z]{2}[0-9]{2}$")) {
            throw new IllegalArgumentException("Invalid return reason code format: " + reasonCode);
        }
    }

    /**
     * Publishes return initiated event.
     */
    private void publishReturnInitiatedEvent(SepaReturn sepaReturn) {
        Map<String, Object> event = new HashMap<>();
        event.put("returnId", sepaReturn.getReturnId());
        event.put("returnType", sepaReturn.getReturnType().name());
        event.put("originalSepaReference", sepaReturn.getOriginalSepaReference());
        event.put("amount", sepaReturn.getReturnAmount());
        event.put("currency", sepaReturn.getCurrency());
        event.put("reasonCode", sepaReturn.getReturnReasonCode());
        event.put("timestamp", LocalDateTime.now());

        kafkaTemplate.send("sepa.return.initiated", event);
        log.debug("Published return initiated event: {}", sepaReturn.getReturnId());
    }

    /**
     * Publishes return completed event.
     */
    private void publishReturnCompletedEvent(SepaReturn sepaReturn) {
        Map<String, Object> event = new HashMap<>();
        event.put("returnId", sepaReturn.getReturnId());
        event.put("returnType", sepaReturn.getReturnType().name());
        event.put("originalSepaReference", sepaReturn.getOriginalSepaReference());
        event.put("amount", sepaReturn.getReturnAmount());
        event.put("currency", sepaReturn.getCurrency());
        event.put("timestamp", LocalDateTime.now());

        kafkaTemplate.send("sepa.return.completed", event);
        log.debug("Published return completed event: {}", sepaReturn.getReturnId());
    }

    /**
     * Publishes refund completed event.
     */
    private void publishRefundCompletedEvent(SepaReturn sepaReturn) {
        Map<String, Object> event = new HashMap<>();
        event.put("returnId", sepaReturn.getReturnId());
        event.put("refundSepaReference", sepaReturn.getRefundSepaReference());
        event.put("amount", sepaReturn.getReturnAmount());
        event.put("currency", sepaReturn.getCurrency());
        event.put("timestamp", LocalDateTime.now());

        kafkaTemplate.send("sepa.refund.completed", event);
        log.debug("Published refund completed event: {}", sepaReturn.getReturnId());
    }

    /**
     * Publishes return failed event.
     */
    private void publishReturnFailedEvent(SepaReturn sepaReturn) {
        Map<String, Object> event = new HashMap<>();
        event.put("returnId", sepaReturn.getReturnId());
        event.put("returnType", sepaReturn.getReturnType().name());
        event.put("originalSepaReference", sepaReturn.getOriginalSepaReference());
        event.put("errorMessage", sepaReturn.getErrorMessage());
        event.put("timestamp", LocalDateTime.now());

        kafkaTemplate.send("sepa.return.failed", event);
        log.debug("Published return failed event: {}", sepaReturn.getReturnId());
    }

    /**
     * Return statistics DTO.
     */
    public static class ReturnStatistics {
        private final String reasonCode;
        private final Long count;
        private final BigDecimal totalAmount;

        public ReturnStatistics(String reasonCode, Long count, BigDecimal totalAmount) {
            this.reasonCode = reasonCode;
            this.count = count;
            this.totalAmount = totalAmount;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public Long getCount() {
            return count;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }
}
