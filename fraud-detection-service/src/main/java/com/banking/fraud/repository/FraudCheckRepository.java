package com.banking.fraud.repository;

import com.banking.fraud.model.FraudCheck;
import com.banking.fraud.model.FraudCheckStatus;
import com.banking.fraud.model.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, Long> {

    Optional<FraudCheck> findByCheckId(String checkId);

    List<FraudCheck> findByTransferReference(String transferReference);

    List<FraudCheck> findByAccountNumberOrderByCheckedAtDesc(String accountNumber);

    List<FraudCheck> findByStatus(FraudCheckStatus status);

    List<FraudCheck> findByRiskLevel(RiskLevel riskLevel);

    @Query("SELECT fc FROM FraudCheck fc WHERE fc.status IN :statuses ORDER BY fc.checkedAt DESC")
    List<FraudCheck> findByStatusIn(@Param("statuses") List<FraudCheckStatus> statuses);

    @Query("SELECT fc FROM FraudCheck fc WHERE fc.accountNumber = :accountNumber " +
           "AND fc.checkedAt >= :since ORDER BY fc.checkedAt DESC")
    List<FraudCheck> findRecentChecksByAccount(
        @Param("accountNumber") String accountNumber,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(fc) FROM FraudCheck fc WHERE fc.accountNumber = :accountNumber " +
           "AND fc.checkedAt >= :since")
    Long countRecentChecksByAccount(
        @Param("accountNumber") String accountNumber,
        @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(fc) FROM FraudCheck fc WHERE fc.accountNumber = :accountNumber " +
           "AND fc.status = :status AND fc.checkedAt >= :since")
    Long countByAccountAndStatusSince(
        @Param("accountNumber") String accountNumber,
        @Param("status") FraudCheckStatus status,
        @Param("since") LocalDateTime since
    );

    boolean existsByCheckId(String checkId);
}
