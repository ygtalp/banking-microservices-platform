package com.banking.swift.repository;

import com.banking.swift.model.SwiftTransfer;
import com.banking.swift.model.SwiftTransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SwiftTransferRepository extends JpaRepository<SwiftTransfer, Long> {

    Optional<SwiftTransfer> findByTransactionReference(String transactionReference);

    List<SwiftTransfer> findBySenderBic(String senderBic);

    List<SwiftTransfer> findByBeneficiaryBankBic(String beneficiaryBankBic);

    Page<SwiftTransfer> findByStatus(SwiftTransferStatus status, Pageable pageable);

    List<SwiftTransfer> findByValueDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT s FROM SwiftTransfer s WHERE s.internalAccountId = :accountId ORDER BY s.createdAt DESC")
    List<SwiftTransfer> findByInternalAccountId(@Param("accountId") String accountId);

    @Query("SELECT s FROM SwiftTransfer s WHERE s.status = :status AND s.valueDate <= :date")
    List<SwiftTransfer> findPendingTransfersByValueDate(
            @Param("status") SwiftTransferStatus status,
            @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(s) FROM SwiftTransfer s WHERE s.status = :status")
    long countByStatus(@Param("status") SwiftTransferStatus status);

    @Query("SELECT SUM(s.amount) FROM SwiftTransfer s WHERE s.status = 'COMPLETED' AND s.completedAt >= :startDate")
    BigDecimal sumCompletedTransfersAfterDate(@Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT s FROM SwiftTransfer s WHERE s.beneficiaryAccount = :account OR s.orderingCustomerAccount = :account")
    List<SwiftTransfer> findByAccountNumber(@Param("account") String account);
}