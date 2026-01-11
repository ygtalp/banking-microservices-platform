package com.banking.sepa.repository;

import com.banking.sepa.model.SepaReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SepaReturnRepository extends JpaRepository<SepaReturn, String> {

    // Find by original SEPA reference
    List<SepaReturn> findByOriginalSepaReferenceOrderByCreatedAtDesc(String originalSepaReference);

    // Find by return type and status
    List<SepaReturn> findByReturnTypeAndStatusOrderByCreatedAtDesc(SepaReturn.ReturnType returnType,
                                                                     SepaReturn.ReturnStatus status);

    // Find by status
    List<SepaReturn> findByStatusOrderByCreatedAtDesc(SepaReturn.ReturnStatus status);

    // Find by return reason code
    List<SepaReturn> findByReturnReasonCodeOrderByCreatedAtDesc(String reasonCode);

    // Find by debtor IBAN
    List<SepaReturn> findByDebtorIbanOrderByCreatedAtDesc(String debtorIban);

    // Find by creditor IBAN
    List<SepaReturn> findByCreditorIbanOrderByCreatedAtDesc(String creditorIban);

    // Find pending returns
    @Query("SELECT r FROM SepaReturn r WHERE r.status IN ('INITIATED', 'VALIDATED', 'PROCESSING') " +
           "ORDER BY r.initiatedAt ASC")
    List<SepaReturn> findPendingReturns();

    // Find returns with errors
    List<SepaReturn> findByHasErrorsTrueOrderByCreatedAtDesc();

    // Find returns by date range
    @Query("SELECT r FROM SepaReturn r WHERE r.initiatedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY r.initiatedAt DESC")
    List<SepaReturn> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // Count by return type
    Long countByReturnType(SepaReturn.ReturnType returnType);

    // Count by status
    Long countByStatus(SepaReturn.ReturnStatus status);

    // Count by reason code
    Long countByReturnReasonCode(String reasonCode);

    // Find recent returns for a SEPA reference
    Optional<SepaReturn> findTopByOriginalSepaReferenceOrderByCreatedAtDesc(String originalSepaReference);

    // Statistics: Sum of return amounts by reason code
    @Query("SELECT SUM(r.returnAmount) FROM SepaReturn r WHERE r.returnReasonCode = :reasonCode " +
           "AND r.status = 'COMPLETED'")
    java.math.BigDecimal sumReturnAmountByReasonCode(@Param("reasonCode") String reasonCode);
}
