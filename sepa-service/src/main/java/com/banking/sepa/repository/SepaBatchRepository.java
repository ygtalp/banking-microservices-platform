package com.banking.sepa.repository;

import com.banking.sepa.model.SepaBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SepaBatchRepository extends JpaRepository<SepaBatch, String> {

    // Find by message ID
    Optional<SepaBatch> findByMessageId(String messageId);

    // Find by batch type and status
    List<SepaBatch> findByBatchTypeAndStatusOrderByCreatedAtDesc(SepaBatch.BatchType batchType,
                                                                   SepaBatch.BatchStatus status);

    // Find by status
    List<SepaBatch> findByStatusOrderByCreatedAtDesc(SepaBatch.BatchStatus status);

    // Find pending batches
    @Query("SELECT b FROM SepaBatch b WHERE b.status IN ('PENDING', 'VALIDATING', 'VALIDATED') " +
           "ORDER BY b.createdAt ASC")
    List<SepaBatch> findPendingBatches();

    // Find processing batches
    @Query("SELECT b FROM SepaBatch b WHERE b.status IN ('SUBMITTED', 'PROCESSING', 'PARTIALLY_COMPLETE') " +
           "ORDER BY b.submittedAt ASC")
    List<SepaBatch> findProcessingBatches();

    // Find batches with errors
    List<SepaBatch> findByHasErrorsTrueOrderByCreatedAtDesc();

    // Find batches by date range
    @Query("SELECT b FROM SepaBatch b WHERE b.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY b.createdAt DESC")
    List<SepaBatch> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // Find by initiating party
    List<SepaBatch> findByInitiatingPartyIdOrderByCreatedAtDesc(String initiatingPartyId);

    // Count by status
    Long countByStatus(SepaBatch.BatchStatus status);

    // Count by batch type
    Long countByBatchType(SepaBatch.BatchType batchType);

    // Find stale batches (created but not submitted)
    @Query("SELECT b FROM SepaBatch b WHERE b.status = 'PENDING' AND " +
           "b.createdAt < :cutoffDate")
    List<SepaBatch> findStaleBatches(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Statistics: Sum of amounts by status
    @Query("SELECT SUM(b.totalAmount) FROM SepaBatch b WHERE b.status = :status")
    java.math.BigDecimal sumTotalAmountByStatus(@Param("status") SepaBatch.BatchStatus status);
}
