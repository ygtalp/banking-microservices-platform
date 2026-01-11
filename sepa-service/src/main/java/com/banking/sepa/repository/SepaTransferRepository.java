package com.banking.sepa.repository;

import com.banking.sepa.model.SepaTransfer;
import com.banking.sepa.model.SepaTransfer.SepaTransferStatus;
import com.banking.sepa.model.SepaTransfer.SepaTransferType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SepaTransferRepository extends JpaRepository<SepaTransfer, String> {

    // Find by IBAN
    List<SepaTransfer> findByDebtorIbanOrderByCreatedAtDesc(String debtorIban);
    List<SepaTransfer> findByCreditorIbanOrderByCreatedAtDesc(String creditorIban);

    // Find by internal account number
    List<SepaTransfer> findByDebtorAccountNumberOrderByCreatedAtDesc(String accountNumber);

    // Find by status
    List<SepaTransfer> findByStatusOrderByCreatedAtDesc(SepaTransferStatus status);

    // Find by type
    List<SepaTransfer> findByTransferTypeOrderByCreatedAtDesc(SepaTransferType transferType);

    // Find by end-to-end ID
    Optional<SepaTransfer> findByEndToEndId(String endToEndId);

    // Find pending transfers
    @Query("SELECT s FROM SepaTransfer s WHERE s.status IN ('PENDING', 'VALIDATING') ORDER BY s.createdAt ASC")
    List<SepaTransfer> findPendingTransfers();

    // Find by date range
    @Query("SELECT s FROM SepaTransfer s WHERE s.createdAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    List<SepaTransfer> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // Count by status
    Long countByStatus(SepaTransferStatus status);

    // Find failed transfers
    List<SepaTransfer> findByStatusInOrderByCreatedAtDesc(List<SepaTransferStatus> statuses);
}
