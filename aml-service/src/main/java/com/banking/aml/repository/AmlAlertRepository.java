package com.banking.aml.repository;

import com.banking.aml.model.AmlAlert;
import com.banking.aml.model.AlertStatus;
import com.banking.aml.model.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmlAlertRepository extends JpaRepository<AmlAlert, String> {

    // Find by account number
    List<AmlAlert> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    Page<AmlAlert> findByAccountNumber(String accountNumber, Pageable pageable);

    // Find by status
    List<AmlAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    Page<AmlAlert> findByStatus(AlertStatus status, Pageable pageable);

    // Find by risk level
    List<AmlAlert> findByRiskLevelOrderByCreatedAtDesc(RiskLevel riskLevel);

    // Find by customer
    List<AmlAlert> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    // Find by transfer reference
    Optional<AmlAlert> findByTransferReference(String transferReference);

    // Find pending review (OPEN or UNDER_REVIEW)
    @Query("SELECT a FROM AmlAlert a WHERE a.status IN ('OPEN', 'UNDER_REVIEW') ORDER BY a.riskScore DESC, a.createdAt DESC")
    List<AmlAlert> findPendingReview();

    // Find high risk alerts (HIGH or CRITICAL)
    @Query("SELECT a FROM AmlAlert a WHERE a.riskLevel IN ('HIGH', 'CRITICAL') AND a.status = 'OPEN' ORDER BY a.riskScore DESC")
    List<AmlAlert> findHighRiskOpenAlerts();

    // Find alerts by date range
    @Query("SELECT a FROM AmlAlert a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AmlAlert> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // Count by status
    Long countByStatus(AlertStatus status);

    // Count by risk level
    Long countByRiskLevel(RiskLevel riskLevel);

    // Count alerts for account
    Long countByAccountNumber(String accountNumber);

    // Find recent alerts (last 24 hours)
    @Query("SELECT a FROM AmlAlert a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AmlAlert> findRecentAlerts(@Param("since") LocalDateTime since);
}
