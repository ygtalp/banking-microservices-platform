package com.banking.aml.repository;

import com.banking.aml.model.TransactionMonitoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionMonitoringRepository extends JpaRepository<TransactionMonitoring, String> {

    // Find by account number
    List<TransactionMonitoring> findByAccountNumberOrderByTransactionDateDesc(String accountNumber);

    // Find by transfer reference
    Optional<TransactionMonitoring> findByTransferReference(String transferReference);

    // Find flagged transactions
    List<TransactionMonitoring> findByFlaggedTrueOrderByRiskScoreDesc();

    // Find by account and date range
    @Query("SELECT t FROM TransactionMonitoring t WHERE t.accountNumber = :accountNumber " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<TransactionMonitoring> findByAccountAndDateRange(
        @Param("accountNumber") String accountNumber,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    // Count transactions in time window (for velocity checks)
    @Query("SELECT COUNT(t) FROM TransactionMonitoring t WHERE t.accountNumber = :accountNumber " +
           "AND t.transactionDate >= :since")
    Long countRecentTransactions(@Param("accountNumber") String accountNumber,
                                  @Param("since") LocalDateTime since);

    // Sum amounts in time window (for daily limit checks)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionMonitoring t " +
           "WHERE t.accountNumber = :accountNumber AND t.transactionDate >= :since AND t.currency = :currency")
    java.math.BigDecimal sumRecentAmounts(@Param("accountNumber") String accountNumber,
                                          @Param("since") LocalDateTime since,
                                          @Param("currency") String currency);

    // Find high risk transactions (not yet alerted)
    @Query("SELECT t FROM TransactionMonitoring t WHERE t.riskScore >= :minScore " +
           "AND t.flagged = true AND t.alertId IS NULL ORDER BY t.riskScore DESC")
    List<TransactionMonitoring> findHighRiskUnalerted(@Param("minScore") Integer minScore);
}
