package com.banking.aml.repository;

import com.banking.aml.model.CustomerRiskProfile;
import com.banking.aml.model.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRiskProfileRepository extends JpaRepository<CustomerRiskProfile, String> {

    // Find by customer ID
    Optional<CustomerRiskProfile> findByCustomerId(String customerId);

    // Find by account number
    Optional<CustomerRiskProfile> findByAccountNumber(String accountNumber);

    // Find by risk level
    List<CustomerRiskProfile> findByRiskLevelAndStatus(RiskLevel riskLevel, CustomerRiskProfile.ProfileStatus status);

    // Find high-risk customers
    @Query("SELECT c FROM CustomerRiskProfile c WHERE c.status = 'ACTIVE' AND " +
           "(c.riskLevel = 'HIGH' OR c.riskLevel = 'CRITICAL')")
    List<CustomerRiskProfile> findHighRiskCustomers();

    // Find sanctioned customers
    List<CustomerRiskProfile> findByIsSanctionedTrueAndStatus(CustomerRiskProfile.ProfileStatus status);

    // Find PEPs
    List<CustomerRiskProfile> findByIsPepTrueAndStatus(CustomerRiskProfile.ProfileStatus status);

    // Find customers needing CDD review
    @Query("SELECT c FROM CustomerRiskProfile c WHERE c.status = 'ACTIVE' AND " +
           "c.nextCddReviewAt <= :now")
    List<CustomerRiskProfile> findCustomersNeedingCddReview(@Param("now") LocalDateTime now);

    // Find customers with open alerts
    @Query("SELECT c FROM CustomerRiskProfile c WHERE c.status = 'ACTIVE' AND c.openAlerts > 0")
    List<CustomerRiskProfile> findCustomersWithOpenAlerts();

    // Find customers by country
    List<CustomerRiskProfile> findByCountryAndStatus(String country, CustomerRiskProfile.ProfileStatus status);

    // Find high-risk jurisdictions
    List<CustomerRiskProfile> findByHighRiskJurisdictionTrueAndStatus(CustomerRiskProfile.ProfileStatus status);

    // Find by customer type
    List<CustomerRiskProfile> findByCustomerTypeAndStatus(CustomerRiskProfile.CustomerType customerType,
                                                           CustomerRiskProfile.ProfileStatus status);

    // Find by CDD level
    List<CustomerRiskProfile> findByCddLevelAndStatus(CustomerRiskProfile.CddLevel cddLevel,
                                                       CustomerRiskProfile.ProfileStatus status);

    // Find customers with SAR filings
    @Query("SELECT c FROM CustomerRiskProfile c WHERE c.status = 'ACTIVE' AND c.sarFiledCount > 0")
    List<CustomerRiskProfile> findCustomersWithSarFilings();

    // Count by risk level
    Long countByRiskLevelAndStatus(RiskLevel riskLevel, CustomerRiskProfile.ProfileStatus status);

    // Count PEPs
    Long countByIsPepTrueAndStatus(CustomerRiskProfile.ProfileStatus status);

    // Count sanctioned customers
    Long countByIsSanctionedTrueAndStatus(CustomerRiskProfile.ProfileStatus status);

    // Find customers with high alert ratio
    @Query("SELECT c FROM CustomerRiskProfile c WHERE c.status = 'ACTIVE' AND " +
           "c.totalTransactions > 0 AND " +
           "(CAST(c.flaggedTransactions AS double) / CAST(c.totalTransactions AS double)) > :threshold")
    List<CustomerRiskProfile> findCustomersWithHighAlertRatio(@Param("threshold") double threshold);

    // Find inactive customers (no recent transactions)
    @Query("SELECT c FROM CustomerRiskProfile c WHERE c.status = 'ACTIVE' AND " +
           "(c.lastTransactionAt IS NULL OR c.lastTransactionAt < :cutoffDate)")
    List<CustomerRiskProfile> findInactiveCustomers(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Top N highest risk customers
    @Query("SELECT c FROM CustomerRiskProfile c WHERE c.status = 'ACTIVE' " +
           "ORDER BY c.riskScore DESC, c.openAlerts DESC")
    List<CustomerRiskProfile> findTopRiskCustomers();

    // Advanced search
    @Query("SELECT c FROM CustomerRiskProfile c WHERE c.status = 'ACTIVE' AND " +
           "(:riskLevel IS NULL OR c.riskLevel = :riskLevel) AND " +
           "(:isPep IS NULL OR c.isPep = :isPep) AND " +
           "(:isSanctioned IS NULL OR c.isSanctioned = :isSanctioned) AND " +
           "(:country IS NULL OR c.country = :country)")
    List<CustomerRiskProfile> advancedSearch(@Param("riskLevel") RiskLevel riskLevel,
                                             @Param("isPep") Boolean isPep,
                                             @Param("isSanctioned") Boolean isSanctioned,
                                             @Param("country") String country);
}
