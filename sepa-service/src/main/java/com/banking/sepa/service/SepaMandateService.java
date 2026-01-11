package com.banking.sepa.service;

import com.banking.sepa.model.SepaMandate;
import com.banking.sepa.repository.SepaMandateRepository;
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
 * Service for managing SEPA Direct Debit mandates.
 * Handles mandate lifecycle: creation, activation, suspension, cancellation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SepaMandateService {

    private final SepaMandateRepository mandateRepository;
    private final IbanValidationService ibanValidationService;
    private final BicValidationService bicValidationService;

    /**
     * Creates a new SEPA mandate.
     *
     * @param mandate The mandate to create
     * @return The created mandate
     */
    @Transactional
    @CacheEvict(value = "mandates", allEntries = true)
    public SepaMandate createMandate(SepaMandate mandate) {
        log.info("Creating new SEPA mandate for debtor: {}", mandate.getDebtorIban());

        // Validate IBAN
        if (!ibanValidationService.validateIban(mandate.getDebtorIban())) {
            throw new IllegalArgumentException("Invalid debtor IBAN: " + mandate.getDebtorIban());
        }
        if (!ibanValidationService.validateIban(mandate.getCreditorIban())) {
            throw new IllegalArgumentException("Invalid creditor IBAN: " + mandate.getCreditorIban());
        }

        // Validate BIC if provided
        if (mandate.getDebtorBic() != null && !mandate.getDebtorBic().isEmpty()) {
            BicValidationService.ValidationResult bicResult = bicValidationService.validateForSepa(mandate.getDebtorBic());
            if (!bicResult.isValid()) {
                throw new IllegalArgumentException("Invalid debtor BIC: " + bicResult.getErrorMessage());
            }
        }
        if (mandate.getCreditorBic() != null && !mandate.getCreditorBic().isEmpty()) {
            BicValidationService.ValidationResult bicResult = bicValidationService.validateForSepa(mandate.getCreditorBic());
            if (!bicResult.isValid()) {
                throw new IllegalArgumentException("Invalid creditor BIC: " + bicResult.getErrorMessage());
            }
        }

        // Set default status
        if (mandate.getStatus() == null) {
            mandate.setStatus(SepaMandate.MandateStatus.PENDING);
        }

        // Save mandate
        SepaMandate savedMandate = mandateRepository.save(mandate);
        log.info("Created SEPA mandate: {}", savedMandate.getMandateId());

        return savedMandate;
    }

    /**
     * Activates a pending mandate.
     *
     * @param mandateId The mandate ID
     * @return The activated mandate
     */
    @Transactional
    @CacheEvict(value = "mandates", allEntries = true)
    public SepaMandate activateMandate(String mandateId) {
        log.info("Activating mandate: {}", mandateId);

        SepaMandate mandate = mandateRepository.findById(mandateId)
            .orElseThrow(() -> new IllegalArgumentException("Mandate not found: " + mandateId));

        if (mandate.getStatus() != SepaMandate.MandateStatus.PENDING) {
            throw new IllegalStateException("Only PENDING mandates can be activated. Current status: " + mandate.getStatus());
        }

        mandate.setStatus(SepaMandate.MandateStatus.ACTIVE);
        mandate.setActivationDate(LocalDate.now());

        SepaMandate updatedMandate = mandateRepository.save(mandate);
        log.info("Activated mandate: {}", mandateId);

        return updatedMandate;
    }

    /**
     * Suspends an active mandate.
     *
     * @param mandateId The mandate ID
     * @return The suspended mandate
     */
    @Transactional
    @CacheEvict(value = "mandates", allEntries = true)
    public SepaMandate suspendMandate(String mandateId) {
        log.info("Suspending mandate: {}", mandateId);

        SepaMandate mandate = mandateRepository.findById(mandateId)
            .orElseThrow(() -> new IllegalArgumentException("Mandate not found: " + mandateId));

        if (mandate.getStatus() != SepaMandate.MandateStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE mandates can be suspended. Current status: " + mandate.getStatus());
        }

        mandate.setStatus(SepaMandate.MandateStatus.SUSPENDED);

        SepaMandate updatedMandate = mandateRepository.save(mandate);
        log.info("Suspended mandate: {}", mandateId);

        return updatedMandate;
    }

    /**
     * Cancels a mandate.
     *
     * @param mandateId The mandate ID
     * @param cancelledBy Who cancelled the mandate
     * @param reason Cancellation reason
     * @return The cancelled mandate
     */
    @Transactional
    @CacheEvict(value = "mandates", allEntries = true)
    public SepaMandate cancelMandate(String mandateId, String cancelledBy, String reason) {
        log.info("Cancelling mandate: {}", mandateId);

        SepaMandate mandate = mandateRepository.findById(mandateId)
            .orElseThrow(() -> new IllegalArgumentException("Mandate not found: " + mandateId));

        if (mandate.getStatus() == SepaMandate.MandateStatus.CANCELLED) {
            throw new IllegalStateException("Mandate is already cancelled");
        }

        mandate.setStatus(SepaMandate.MandateStatus.CANCELLED);
        mandate.setCancelledAt(LocalDateTime.now());
        mandate.setCancelledBy(cancelledBy);
        mandate.setCancellationReason(reason);

        SepaMandate updatedMandate = mandateRepository.save(mandate);
        log.info("Cancelled mandate: {}", mandateId);

        return updatedMandate;
    }

    /**
     * Records a collection against a mandate.
     *
     * @param mandateId The mandate ID
     * @param amount The collection amount
     * @param successful Whether the collection was successful
     * @return The updated mandate
     */
    @Transactional
    @CacheEvict(value = "mandates", allEntries = true)
    public SepaMandate recordCollection(String mandateId, BigDecimal amount, boolean successful) {
        log.info("Recording collection for mandate: {}, amount: {}, successful: {}", mandateId, amount, successful);

        SepaMandate mandate = mandateRepository.findById(mandateId)
            .orElseThrow(() -> new IllegalArgumentException("Mandate not found: " + mandateId));

        if (mandate.getStatus() != SepaMandate.MandateStatus.ACTIVE) {
            throw new IllegalStateException("Collections can only be made against ACTIVE mandates. Current status: " + mandate.getStatus());
        }

        // Update statistics
        mandate.setTotalCollections(mandate.getTotalCollections() + 1);
        if (successful) {
            mandate.setSuccessfulCollections(mandate.getSuccessfulCollections() + 1);
            if (mandate.getTotalAmountCollected() == null) {
                mandate.setTotalAmountCollected(BigDecimal.ZERO);
            }
            mandate.setTotalAmountCollected(mandate.getTotalAmountCollected().add(amount));
        } else {
            mandate.setFailedCollections(mandate.getFailedCollections() + 1);
        }

        // Update collection dates
        if (mandate.getFirstCollectionDate() == null) {
            mandate.setFirstCollectionDate(LocalDate.now());
        }
        mandate.setLastCollectionDate(LocalDate.now());

        SepaMandate updatedMandate = mandateRepository.save(mandate);
        log.info("Recorded collection for mandate: {}", mandateId);

        return updatedMandate;
    }

    /**
     * Retrieves a mandate by ID.
     *
     * @param mandateId The mandate ID
     * @return The mandate
     */
    @Cacheable(value = "mandates", key = "#mandateId")
    public SepaMandate getMandateById(String mandateId) {
        return mandateRepository.findById(mandateId)
            .orElseThrow(() -> new IllegalArgumentException("Mandate not found: " + mandateId));
    }

    /**
     * Retrieves active mandates for a debtor.
     *
     * @param debtorIban The debtor IBAN
     * @return List of active mandates
     */
    @Cacheable(value = "mandates", key = "'debtor_' + #debtorIban")
    public List<SepaMandate> getActiveMandatesForDebtor(String debtorIban) {
        return mandateRepository.findByDebtorIbanAndStatus(debtorIban, SepaMandate.MandateStatus.ACTIVE);
    }

    /**
     * Retrieves active mandates for a creditor.
     *
     * @param creditorId The creditor ID
     * @return List of active mandates
     */
    @Cacheable(value = "mandates", key = "'creditor_' + #creditorId")
    public List<SepaMandate> getActiveMandatesForCreditor(String creditorId) {
        return mandateRepository.findByCreditorIdAndStatus(creditorId, SepaMandate.MandateStatus.ACTIVE);
    }

    /**
     * Retrieves mandates expiring soon (within specified days).
     *
     * @param days Number of days
     * @return List of expiring mandates
     */
    public List<SepaMandate> getExpiringSoonMandates(int days) {
        LocalDate cutoffDate = LocalDate.now().plusDays(days);
        return mandateRepository.findExpiringSoon(cutoffDate);
    }

    /**
     * Retrieves inactive mandates (no collections in specified days).
     *
     * @param days Number of days
     * @return List of inactive mandates
     */
    public List<SepaMandate> getInactiveMandates(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return mandateRepository.findInactiveMandates(cutoffDate);
    }

    /**
     * Checks if a mandate is valid for collection.
     *
     * @param mandateId The mandate ID
     * @param amount The collection amount
     * @return true if valid, false otherwise
     */
    public boolean isValidForCollection(String mandateId, BigDecimal amount) {
        try {
            SepaMandate mandate = getMandateById(mandateId);

            // Check status
            if (mandate.getStatus() != SepaMandate.MandateStatus.ACTIVE) {
                log.warn("Mandate {} is not active: {}", mandateId, mandate.getStatus());
                return false;
            }

            // Check activation date
            if (mandate.getActivationDate() != null && mandate.getActivationDate().isAfter(LocalDate.now())) {
                log.warn("Mandate {} is not yet activated", mandateId);
                return false;
            }

            // Check max amount if specified
            if (mandate.getMaxAmount() != null && amount.compareTo(mandate.getMaxAmount()) > 0) {
                log.warn("Collection amount {} exceeds mandate max amount {}", amount, mandate.getMaxAmount());
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating mandate for collection: {}", mandateId, e);
            return false;
        }
    }
}
