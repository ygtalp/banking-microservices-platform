package com.banking.transfer.repository;

import com.banking.transfer.model.Transfer;
import com.banking.transfer.model.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByTransferReference(String transferReference);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    List<Transfer> findByFromAccountNumberOrderByCreatedAtDesc(String fromAccountNumber);

    List<Transfer> findByToAccountNumberOrderByCreatedAtDesc(String toAccountNumber);

    @Query("SELECT t FROM Transfer t WHERE t.fromAccountNumber = :accountNumber OR t.toAccountNumber = :accountNumber ORDER BY t.createdAt DESC")
    List<Transfer> findByAccountNumber(@Param("accountNumber") String accountNumber);

    List<Transfer> findByStatusIn(List<TransferStatus> statuses);

    @Query("SELECT t FROM Transfer t WHERE t.status IN :statuses AND t.createdAt < :threshold")
    List<Transfer> findStuckTransfers(
            @Param("statuses") List<TransferStatus> statuses,
            @Param("threshold") LocalDateTime threshold
    );

    boolean existsByIdempotencyKey(String idempotencyKey);
}