package com.banking.transaction.repository;

import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByAccountNumberOrderByTransactionDateDesc(
        String accountNumber,
        Pageable pageable
    );

    List<Transaction> findByAccountNumberAndTransactionDateBetweenOrderByTransactionDateDesc(
        String accountNumber,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    Page<Transaction> findByAccountNumberAndTransactionType(
        String accountNumber,
        TransactionType transactionType,
        Pageable pageable
    );

    Page<Transaction> findByAccountNumberAndStatus(
        String accountNumber,
        TransactionStatus status,
        Pageable pageable
    );

    List<Transaction> findByReference(String reference);

    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "AND (:type IS NULL OR t.transactionType = :type)")
    Page<Transaction> findByAccountAndDateRangeAndType(
        @Param("accountNumber") String accountNumber,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("type") TransactionType type,
        Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountNumber = :accountNumber")
    long countByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.accountNumber = :accountNumber " +
           "AND t.transactionType = :type " +
           "AND t.status = 'COMPLETED'")
    BigDecimal sumAmountByAccountAndType(
        @Param("accountNumber") String accountNumber,
        @Param("type") TransactionType type
    );
}
