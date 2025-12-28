package com.banking.customer.repository;

import com.banking.customer.model.CustomerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerHistoryRepository extends JpaRepository<CustomerHistory, Long> {

    List<CustomerHistory> findByCustomerIdOrderByTimestampDesc(Long customerId);

    List<CustomerHistory> findByCustomerIdAndOperationOrderByTimestampDesc(Long customerId, String operation);

    // Get history within date range
    @Query("SELECT h FROM CustomerHistory h WHERE h.customerId = :customerId " +
           "AND h.timestamp BETWEEN :startDate AND :endDate ORDER BY h.timestamp DESC")
    List<CustomerHistory> findByCustomerIdAndDateRange(
        @Param("customerId") Long customerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    // Get recent history (last N entries)
    @Query("SELECT h FROM CustomerHistory h WHERE h.customerId = :customerId " +
           "ORDER BY h.timestamp DESC LIMIT :limit")
    List<CustomerHistory> findRecentHistory(@Param("customerId") Long customerId, @Param("limit") int limit);
}
