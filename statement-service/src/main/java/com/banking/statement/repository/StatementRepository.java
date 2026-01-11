package com.banking.statement.repository;

import com.banking.statement.model.Statement;
import com.banking.statement.model.StatementStatus;
import com.banking.statement.model.StatementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {

    Optional<Statement> findByStatementId(String statementId);

    List<Statement> findByAccountNumberOrderByStatementDateDesc(String accountNumber);

    List<Statement> findByCustomerIdOrderByStatementDateDesc(String customerId);

    List<Statement> findByStatus(StatementStatus status);

    @Query("SELECT s FROM Statement s WHERE s.accountNumber = :accountNumber " +
           "AND s.periodStart >= :startDate AND s.periodEnd <= :endDate " +
           "ORDER BY s.statementDate DESC")
    List<Statement> findByAccountAndDateRange(
            @Param("accountNumber") String accountNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT s FROM Statement s WHERE s.accountNumber = :accountNumber " +
           "AND s.statementType = :type ORDER BY s.statementDate DESC")
    List<Statement> findByAccountAndType(
            @Param("accountNumber") String accountNumber,
            @Param("type") StatementType type
    );

    Long countByAccountNumber(String accountNumber);

    Long countByCustomerId(String customerId);

    @Query("SELECT COUNT(s) FROM Statement s WHERE s.accountNumber = :accountNumber " +
           "AND s.status = :status")
    Long countByAccountAndStatus(
            @Param("accountNumber") String accountNumber,
            @Param("status") StatementStatus status
    );
}
