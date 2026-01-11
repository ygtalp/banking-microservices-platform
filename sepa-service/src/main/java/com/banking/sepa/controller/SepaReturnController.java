package com.banking.sepa.controller;

import com.banking.sepa.model.SepaReturn;
import com.banking.sepa.service.SepaReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for SEPA R-Transactions (returns, rejections, refunds, recalls).
 * Provides endpoints for initiating, processing, and tracking return transactions.
 */
@RestController
@RequestMapping("/sepa/returns")
@RequiredArgsConstructor
@Slf4j
public class SepaReturnController {

    private final SepaReturnService returnService;

    /**
     * Initiates a SEPA return transaction.
     *
     * @param sepaReturn The return to initiate
     * @return The initiated return
     */
    @PostMapping
    public ResponseEntity<SepaReturn> initiateReturn(@RequestBody SepaReturn sepaReturn) {
        log.info("REST request to initiate SEPA return");
        SepaReturn initiatedReturn = returnService.initiateReturn(sepaReturn);
        return ResponseEntity.status(HttpStatus.CREATED).body(initiatedReturn);
    }

    /**
     * Retrieves a return by ID.
     *
     * @param returnId The return ID
     * @return The return
     */
    @GetMapping("/{returnId}")
    public ResponseEntity<SepaReturn> getReturnById(@PathVariable("returnId") String returnId) {
        log.info("REST request to get return: {}", returnId);
        SepaReturn sepaReturn = returnService.getReturnById(returnId);
        return ResponseEntity.ok(sepaReturn);
    }

    /**
     * Validates a return transaction.
     *
     * @param returnId The return ID
     * @return The validated return
     */
    @PostMapping("/{returnId}/validate")
    public ResponseEntity<SepaReturn> validateReturn(@PathVariable("returnId") String returnId) {
        log.info("REST request to validate return: {}", returnId);
        SepaReturn validatedReturn = returnService.validateReturn(returnId);
        return ResponseEntity.ok(validatedReturn);
    }

    /**
     * Processes a validated return.
     *
     * @param returnId The return ID
     * @return The processed return
     */
    @PostMapping("/{returnId}/process")
    public ResponseEntity<SepaReturn> processReturn(@PathVariable("returnId") String returnId) {
        log.info("REST request to process return: {}", returnId);
        SepaReturn processedReturn = returnService.processReturn(returnId);
        return ResponseEntity.ok(processedReturn);
    }

    /**
     * Completes a return transaction.
     *
     * @param returnId The return ID
     * @return The completed return
     */
    @PostMapping("/{returnId}/complete")
    public ResponseEntity<SepaReturn> completeReturn(@PathVariable("returnId") String returnId) {
        log.info("REST request to complete return: {}", returnId);
        SepaReturn completedReturn = returnService.completeReturn(returnId);
        return ResponseEntity.ok(completedReturn);
    }

    /**
     * Processes a refund for a completed return.
     *
     * @param returnId The return ID
     * @param request The refund request
     * @return The refunded return
     */
    @PostMapping("/{returnId}/refund")
    public ResponseEntity<SepaReturn> processRefund(
            @PathVariable("returnId") String returnId,
            @RequestBody RefundRequest request) {
        log.info("REST request to process refund for return: {}", returnId);
        SepaReturn refundedReturn = returnService.processRefund(
                returnId,
                request.getRefundSepaReference(),
                request.getRefundTransactionId()
        );
        return ResponseEntity.ok(refundedReturn);
    }

    /**
     * Marks a return as failed.
     *
     * @param returnId The return ID
     * @param request The failure request
     * @return The failed return
     */
    @PostMapping("/{returnId}/fail")
    public ResponseEntity<SepaReturn> failReturn(
            @PathVariable("returnId") String returnId,
            @RequestBody FailureRequest request) {
        log.info("REST request to fail return: {}", returnId);
        SepaReturn failedReturn = returnService.failReturn(returnId, request.getErrorMessage());
        return ResponseEntity.ok(failedReturn);
    }

    /**
     * Retrieves all returns for an original SEPA reference.
     *
     * @param originalSepaReference The original SEPA reference
     * @return List of returns
     */
    @GetMapping("/sepa/{originalSepaReference}")
    public ResponseEntity<List<SepaReturn>> getReturnsByOriginalSepaReference(
            @PathVariable("originalSepaReference") String originalSepaReference) {
        log.info("REST request to get returns for SEPA reference: {}", originalSepaReference);
        List<SepaReturn> returns = returnService.getReturnsByOriginalSepaReference(originalSepaReference);
        return ResponseEntity.ok(returns);
    }

    /**
     * Retrieves pending returns.
     *
     * @return List of pending returns
     */
    @GetMapping("/pending")
    public ResponseEntity<List<SepaReturn>> getPendingReturns() {
        log.info("REST request to get pending returns");
        List<SepaReturn> returns = returnService.getPendingReturns();
        return ResponseEntity.ok(returns);
    }

    /**
     * Retrieves returns with errors.
     *
     * @return List of returns with errors
     */
    @GetMapping("/errors")
    public ResponseEntity<List<SepaReturn>> getReturnsWithErrors() {
        log.info("REST request to get returns with errors");
        List<SepaReturn> returns = returnService.getReturnsWithErrors();
        return ResponseEntity.ok(returns);
    }

    /**
     * Retrieves returns by reason code.
     *
     * @param reasonCode The reason code
     * @return List of returns
     */
    @GetMapping("/reason/{reasonCode}")
    public ResponseEntity<List<SepaReturn>> getReturnsByReasonCode(
            @PathVariable("reasonCode") String reasonCode) {
        log.info("REST request to get returns by reason code: {}", reasonCode);
        List<SepaReturn> returns = returnService.getReturnsByReasonCode(reasonCode);
        return ResponseEntity.ok(returns);
    }

    /**
     * Retrieves returns by debtor IBAN.
     *
     * @param debtorIban The debtor IBAN
     * @return List of returns
     */
    @GetMapping("/debtor/{debtorIban}")
    public ResponseEntity<List<SepaReturn>> getReturnsByDebtorIban(
            @PathVariable("debtorIban") String debtorIban) {
        log.info("REST request to get returns by debtor IBAN: {}", debtorIban);
        List<SepaReturn> returns = returnService.getReturnsByDebtorIban(debtorIban);
        return ResponseEntity.ok(returns);
    }

    /**
     * Retrieves returns by creditor IBAN.
     *
     * @param creditorIban The creditor IBAN
     * @return List of returns
     */
    @GetMapping("/creditor/{creditorIban}")
    public ResponseEntity<List<SepaReturn>> getReturnsByCreditorIban(
            @PathVariable("creditorIban") String creditorIban) {
        log.info("REST request to get returns by creditor IBAN: {}", creditorIban);
        List<SepaReturn> returns = returnService.getReturnsByCreditorIban(creditorIban);
        return ResponseEntity.ok(returns);
    }

    /**
     * Gets return statistics by reason code.
     *
     * @param reasonCode The reason code
     * @return Return statistics
     */
    @GetMapping("/statistics/{reasonCode}")
    public ResponseEntity<SepaReturnService.ReturnStatistics> getReturnStatisticsByReasonCode(
            @PathVariable("reasonCode") String reasonCode) {
        log.info("REST request to get return statistics for reason code: {}", reasonCode);
        SepaReturnService.ReturnStatistics statistics = returnService.getReturnStatisticsByReasonCode(reasonCode);
        return ResponseEntity.ok(statistics);
    }

    // DTOs

    public static class RefundRequest {
        private String refundSepaReference;
        private String refundTransactionId;

        public RefundRequest() {
        }

        public String getRefundSepaReference() {
            return refundSepaReference;
        }

        public void setRefundSepaReference(String refundSepaReference) {
            this.refundSepaReference = refundSepaReference;
        }

        public String getRefundTransactionId() {
            return refundTransactionId;
        }

        public void setRefundTransactionId(String refundTransactionId) {
            this.refundTransactionId = refundTransactionId;
        }
    }

    public static class FailureRequest {
        private String errorMessage;

        public FailureRequest() {
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
