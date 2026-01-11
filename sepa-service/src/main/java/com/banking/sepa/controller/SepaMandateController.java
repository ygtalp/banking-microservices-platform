package com.banking.sepa.controller;

import com.banking.sepa.model.SepaMandate;
import com.banking.sepa.service.SepaMandateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for SEPA Direct Debit mandate management.
 * Provides endpoints for creating, activating, suspending, and cancelling mandates.
 */
@RestController
@RequestMapping("/sepa/mandates")
@RequiredArgsConstructor
@Slf4j
public class SepaMandateController {

    private final SepaMandateService mandateService;

    /**
     * Creates a new SEPA mandate.
     *
     * @param mandate The mandate to create
     * @return The created mandate
     */
    @PostMapping
    public ResponseEntity<SepaMandate> createMandate(@RequestBody SepaMandate mandate) {
        log.info("REST request to create SEPA mandate");
        SepaMandate createdMandate = mandateService.createMandate(mandate);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMandate);
    }

    /**
     * Retrieves a mandate by ID.
     *
     * @param mandateId The mandate ID
     * @return The mandate
     */
    @GetMapping("/{mandateId}")
    public ResponseEntity<SepaMandate> getMandateById(@PathVariable("mandateId") String mandateId) {
        log.info("REST request to get mandate: {}", mandateId);
        SepaMandate mandate = mandateService.getMandateById(mandateId);
        return ResponseEntity.ok(mandate);
    }

    /**
     * Activates a pending mandate.
     *
     * @param mandateId The mandate ID
     * @return The activated mandate
     */
    @PostMapping("/{mandateId}/activate")
    public ResponseEntity<SepaMandate> activateMandate(@PathVariable("mandateId") String mandateId) {
        log.info("REST request to activate mandate: {}", mandateId);
        SepaMandate activatedMandate = mandateService.activateMandate(mandateId);
        return ResponseEntity.ok(activatedMandate);
    }

    /**
     * Suspends an active mandate.
     *
     * @param mandateId The mandate ID
     * @return The suspended mandate
     */
    @PostMapping("/{mandateId}/suspend")
    public ResponseEntity<SepaMandate> suspendMandate(@PathVariable("mandateId") String mandateId) {
        log.info("REST request to suspend mandate: {}", mandateId);
        SepaMandate suspendedMandate = mandateService.suspendMandate(mandateId);
        return ResponseEntity.ok(suspendedMandate);
    }

    /**
     * Cancels a mandate.
     *
     * @param mandateId The mandate ID
     * @param request The cancellation request
     * @return The cancelled mandate
     */
    @PostMapping("/{mandateId}/cancel")
    public ResponseEntity<SepaMandate> cancelMandate(
            @PathVariable("mandateId") String mandateId,
            @RequestBody CancellationRequest request) {
        log.info("REST request to cancel mandate: {}", mandateId);
        SepaMandate cancelledMandate = mandateService.cancelMandate(
                mandateId,
                request.getCancelledBy(),
                request.getReason()
        );
        return ResponseEntity.ok(cancelledMandate);
    }

    /**
     * Records a collection against a mandate.
     *
     * @param mandateId The mandate ID
     * @param request The collection request
     * @return The updated mandate
     */
    @PostMapping("/{mandateId}/collections")
    public ResponseEntity<SepaMandate> recordCollection(
            @PathVariable("mandateId") String mandateId,
            @RequestBody CollectionRequest request) {
        log.info("REST request to record collection for mandate: {}", mandateId);
        SepaMandate updatedMandate = mandateService.recordCollection(
                mandateId,
                request.getAmount(),
                request.isSuccessful()
        );
        return ResponseEntity.ok(updatedMandate);
    }

    /**
     * Retrieves active mandates for a debtor.
     *
     * @param debtorIban The debtor IBAN
     * @return List of active mandates
     */
    @GetMapping("/debtor/{debtorIban}")
    public ResponseEntity<List<SepaMandate>> getActiveMandatesForDebtor(
            @PathVariable("debtorIban") String debtorIban) {
        log.info("REST request to get active mandates for debtor: {}", debtorIban);
        List<SepaMandate> mandates = mandateService.getActiveMandatesForDebtor(debtorIban);
        return ResponseEntity.ok(mandates);
    }

    /**
     * Retrieves active mandates for a creditor.
     *
     * @param creditorId The creditor ID
     * @return List of active mandates
     */
    @GetMapping("/creditor/{creditorId}")
    public ResponseEntity<List<SepaMandate>> getActiveMandatesForCreditor(
            @PathVariable("creditorId") String creditorId) {
        log.info("REST request to get active mandates for creditor: {}", creditorId);
        List<SepaMandate> mandates = mandateService.getActiveMandatesForCreditor(creditorId);
        return ResponseEntity.ok(mandates);
    }

    /**
     * Retrieves mandates expiring soon.
     *
     * @param days Number of days
     * @return List of expiring mandates
     */
    @GetMapping("/expiring")
    public ResponseEntity<List<SepaMandate>> getExpiringSoonMandates(
            @RequestParam(value = "days", defaultValue = "30") int days) {
        log.info("REST request to get mandates expiring in {} days", days);
        List<SepaMandate> mandates = mandateService.getExpiringSoonMandates(days);
        return ResponseEntity.ok(mandates);
    }

    /**
     * Retrieves inactive mandates.
     *
     * @param days Number of days
     * @return List of inactive mandates
     */
    @GetMapping("/inactive")
    public ResponseEntity<List<SepaMandate>> getInactiveMandates(
            @RequestParam(value = "days", defaultValue = "90") int days) {
        log.info("REST request to get mandates inactive for {} days", days);
        List<SepaMandate> mandates = mandateService.getInactiveMandates(days);
        return ResponseEntity.ok(mandates);
    }

    /**
     * Validates if a mandate is valid for collection.
     *
     * @param mandateId The mandate ID
     * @param amount The collection amount
     * @return Validation result
     */
    @GetMapping("/{mandateId}/validate")
    public ResponseEntity<ValidationResponse> validateForCollection(
            @PathVariable("mandateId") String mandateId,
            @RequestParam("amount") BigDecimal amount) {
        log.info("REST request to validate mandate {} for collection amount {}", mandateId, amount);
        boolean isValid = mandateService.isValidForCollection(mandateId, amount);
        return ResponseEntity.ok(new ValidationResponse(isValid));
    }

    // DTOs

    public static class CancellationRequest {
        private String cancelledBy;
        private String reason;

        public CancellationRequest() {
        }

        public String getCancelledBy() {
            return cancelledBy;
        }

        public void setCancelledBy(String cancelledBy) {
            this.cancelledBy = cancelledBy;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class CollectionRequest {
        private BigDecimal amount;
        private boolean successful;

        public CollectionRequest() {
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }
    }

    public static class ValidationResponse {
        private boolean valid;

        public ValidationResponse() {
        }

        public ValidationResponse(boolean valid) {
            this.valid = valid;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }
}
