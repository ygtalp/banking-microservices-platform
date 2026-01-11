package com.banking.aml.service;

import com.banking.aml.model.CustomerRiskProfile;
import com.banking.aml.model.RiskLevel;
import com.banking.aml.repository.AmlAlertRepository;
import com.banking.aml.repository.CustomerRiskProfileRepository;
import com.banking.aml.repository.SanctionMatchRepository;
import com.banking.aml.repository.TransactionMonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerRiskScoringService {

    private final CustomerRiskProfileRepository customerRiskProfileRepository;
    private final TransactionMonitoringRepository transactionMonitoringRepository;
    private final AmlAlertRepository amlAlertRepository;
    private final SanctionMatchRepository sanctionMatchRepository;

    /**
     * Get or create customer risk profile
     */
    @Cacheable(value = "customerRisk", key = "#customerId")
    public CustomerRiskProfile getCustomerRiskProfile(String customerId) {
        return customerRiskProfileRepository.findByCustomerId(customerId)
                .orElse(null);
    }

    /**
     * Create initial risk profile for new customer
     */
    @Transactional
    @CacheEvict(value = "customerRisk", key = "#profile.customerId")
    public CustomerRiskProfile createRiskProfile(CustomerRiskProfile profile) {
        log.info("Creating risk profile for customer: {}", profile.getCustomerId());

        // Set initial values
        profile.setTotalTransactions(0L);
        profile.setFlaggedTransactions(0L);
        profile.setBlockedTransactions(0L);
        profile.setTotalAlerts(0L);
        profile.setOpenAlerts(0L);
        profile.setSarFiledCount(0L);
        profile.setSanctionMatches(0L);
        profile.setStatus(CustomerRiskProfile.ProfileStatus.ACTIVE);

        // Calculate initial risk based on customer attributes
        profile.calculateRiskScore();

        // Set CDD review schedule based on risk level
        setCddReviewSchedule(profile);

        return customerRiskProfileRepository.save(profile);
    }

    /**
     * Update risk profile after transaction monitoring
     */
    @Transactional
    @CacheEvict(value = "customerRisk", key = "#customerId")
    public CustomerRiskProfile updateAfterTransaction(String customerId, String accountNumber,
                                                       BigDecimal amount, boolean flagged) {
        CustomerRiskProfile profile = getOrCreateProfile(customerId, accountNumber);

        // Update transaction stats
        profile.setTotalTransactions(profile.getTotalTransactions() + 1);
        if (flagged) {
            profile.setFlaggedTransactions(profile.getFlaggedTransactions() + 1);
        }

        // Update amounts
        BigDecimal currentTotal = profile.getTotalTransactionAmount() != null ?
                profile.getTotalTransactionAmount() : BigDecimal.ZERO;
        profile.setTotalTransactionAmount(currentTotal.add(amount));

        // Recalculate average
        BigDecimal average = profile.getTotalTransactionAmount()
                .divide(BigDecimal.valueOf(profile.getTotalTransactions()), 2, RoundingMode.HALF_UP);
        profile.setAverageTransactionAmount(average);

        // Update max amount
        if (profile.getMaxTransactionAmount() == null ||
                amount.compareTo(profile.getMaxTransactionAmount()) > 0) {
            profile.setMaxTransactionAmount(amount);
        }

        profile.setLastTransactionAt(LocalDateTime.now());

        return customerRiskProfileRepository.save(profile);
    }

    /**
     * Update risk profile after AML alert
     */
    @Transactional
    @CacheEvict(value = "customerRisk", key = "#customerId")
    public CustomerRiskProfile updateAfterAlert(String customerId, String accountNumber, String alertStatus) {
        CustomerRiskProfile profile = getOrCreateProfile(customerId, accountNumber);

        profile.setTotalAlerts(profile.getTotalAlerts() + 1);

        if ("OPEN".equals(alertStatus) || "UNDER_REVIEW".equals(alertStatus)) {
            profile.setOpenAlerts(profile.getOpenAlerts() + 1);
        } else if ("CLEARED".equals(alertStatus)) {
            profile.setClearedAlerts(profile.getClearedAlerts() + 1);
            if (profile.getOpenAlerts() > 0) {
                profile.setOpenAlerts(profile.getOpenAlerts() - 1);
            }
        }

        profile.setLastAlertAt(LocalDateTime.now());

        return customerRiskProfileRepository.save(profile);
    }

    /**
     * Update risk profile after sanction screening
     */
    @Transactional
    @CacheEvict(value = "customerRisk", key = "#customerId")
    public CustomerRiskProfile updateAfterSanctionScreening(String customerId, String accountNumber,
                                                             boolean matchFound, boolean highConfidence) {
        CustomerRiskProfile profile = getOrCreateProfile(customerId, accountNumber);

        if (matchFound) {
            profile.setSanctionMatches(profile.getSanctionMatches() + 1);
            profile.setLastSanctionCheckAt(LocalDateTime.now());

            if (highConfidence) {
                profile.setIsSanctioned(true);
                profile.setStatus(CustomerRiskProfile.ProfileStatus.BLOCKED);
                log.warn("Customer {} marked as sanctioned", customerId);
            }
        }

        return customerRiskProfileRepository.save(profile);
    }

    /**
     * Mark customer as PEP (Politically Exposed Person)
     */
    @Transactional
    @CacheEvict(value = "customerRisk", key = "#customerId")
    public CustomerRiskProfile markAsPep(String customerId, String pepCategory) {
        CustomerRiskProfile profile = customerRiskProfileRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer risk profile not found"));

        profile.setIsPep(true);
        profile.setPepCategory(pepCategory);
        profile.setCddLevel(CustomerRiskProfile.CddLevel.ENHANCED);

        log.info("Customer {} marked as PEP: {}", customerId, pepCategory);

        return customerRiskProfileRepository.save(profile);
    }

    /**
     * Update after SAR filing
     */
    @Transactional
    @CacheEvict(value = "customerRisk", key = "#customerId")
    public CustomerRiskProfile updateAfterSarFiling(String customerId) {
        CustomerRiskProfile profile = customerRiskProfileRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer risk profile not found"));

        profile.setSarFiledCount(profile.getSarFiledCount() + 1);

        return customerRiskProfileRepository.save(profile);
    }

    /**
     * Block customer
     */
    @Transactional
    @CacheEvict(value = "customerRisk", key = "#customerId")
    public CustomerRiskProfile blockCustomer(String customerId, String reason) {
        CustomerRiskProfile profile = customerRiskProfileRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer risk profile not found"));

        profile.setStatus(CustomerRiskProfile.ProfileStatus.BLOCKED);
        profile.setBlockedReason(reason);

        log.warn("Customer {} blocked: {}", customerId, reason);

        return customerRiskProfileRepository.save(profile);
    }

    /**
     * Recalculate risk score for all customers (batch job)
     */
    @Transactional
    public int recalculateAllRiskScores() {
        log.info("Starting batch risk score recalculation");

        List<CustomerRiskProfile> allProfiles = customerRiskProfileRepository.findAll();
        allProfiles.forEach(profile -> profile.calculateRiskScore());
        customerRiskProfileRepository.saveAll(allProfiles);

        log.info("Recalculated risk scores for {} customers", allProfiles.size());
        return allProfiles.size();
    }

    /**
     * Get high-risk customers
     */
    public List<CustomerRiskProfile> getHighRiskCustomers() {
        return customerRiskProfileRepository.findHighRiskCustomers();
    }

    /**
     * Get customers needing CDD review
     */
    public List<CustomerRiskProfile> getCustomersNeedingCddReview() {
        return customerRiskProfileRepository.findCustomersNeedingCddReview(LocalDateTime.now());
    }

    /**
     * Perform CDD review
     */
    @Transactional
    @CacheEvict(value = "customerRisk", key = "#customerId")
    public CustomerRiskProfile performCddReview(String customerId, String reviewedBy) {
        CustomerRiskProfile profile = customerRiskProfileRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Customer risk profile not found"));

        profile.setLastCddReviewAt(LocalDateTime.now());
        setCddReviewSchedule(profile);

        log.info("CDD review performed for customer {} by {}", customerId, reviewedBy);

        return customerRiskProfileRepository.save(profile);
    }

    /**
     * Get statistics
     */
    public RiskStatistics getStatistics() {
        RiskStatistics stats = new RiskStatistics();
        stats.setTotalCustomers(customerRiskProfileRepository.count());
        stats.setLowRisk(customerRiskProfileRepository.countByRiskLevelAndStatus(
                RiskLevel.LOW, CustomerRiskProfile.ProfileStatus.ACTIVE));
        stats.setMediumRisk(customerRiskProfileRepository.countByRiskLevelAndStatus(
                RiskLevel.MEDIUM, CustomerRiskProfile.ProfileStatus.ACTIVE));
        stats.setHighRisk(customerRiskProfileRepository.countByRiskLevelAndStatus(
                RiskLevel.HIGH, CustomerRiskProfile.ProfileStatus.ACTIVE));
        stats.setCriticalRisk(customerRiskProfileRepository.countByRiskLevelAndStatus(
                RiskLevel.CRITICAL, CustomerRiskProfile.ProfileStatus.ACTIVE));
        stats.setPepCount(customerRiskProfileRepository.countByIsPepTrueAndStatus(
                CustomerRiskProfile.ProfileStatus.ACTIVE));
        stats.setSanctionedCount(customerRiskProfileRepository.countByIsSanctionedTrueAndStatus(
                CustomerRiskProfile.ProfileStatus.ACTIVE));
        return stats;
    }

    /**
     * Helper: Get or create profile
     */
    private CustomerRiskProfile getOrCreateProfile(String customerId, String accountNumber) {
        return customerRiskProfileRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    CustomerRiskProfile newProfile = new CustomerRiskProfile();
                    newProfile.setCustomerId(customerId);
                    newProfile.setAccountNumber(accountNumber);
                    newProfile.setCustomerName("Customer-" + customerId);
                    newProfile.setCustomerType(CustomerRiskProfile.CustomerType.INDIVIDUAL);
                    return createRiskProfile(newProfile);
                });
    }

    /**
     * Helper: Set CDD review schedule based on risk level
     */
    private void setCddReviewSchedule(CustomerRiskProfile profile) {
        LocalDateTime nextReview;
        switch (profile.getCddLevel()) {
            case SIMPLIFIED:
                nextReview = LocalDateTime.now().plusYears(3);
                break;
            case ENHANCED:
                nextReview = LocalDateTime.now().plusMonths(6);
                break;
            case STANDARD:
            default:
                nextReview = LocalDateTime.now().plusYears(1);
                break;
        }
        profile.setNextCddReviewAt(nextReview);
    }

    /**
     * Statistics DTO
     */
    @lombok.Data
    public static class RiskStatistics {
        private Long totalCustomers;
        private Long lowRisk;
        private Long mediumRisk;
        private Long highRisk;
        private Long criticalRisk;
        private Long pepCount;
        private Long sanctionedCount;
    }
}
