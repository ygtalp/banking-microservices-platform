package com.banking.sepa.repository;

import com.banking.sepa.model.SepaMandate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SepaMandateRepository extends JpaRepository<SepaMandate, String> {

    // Find by debtor IBAN
    List<SepaMandate> findByDebtorIbanAndStatusOrderByCreatedAtDesc(String debtorIban, SepaMandate.MandateStatus status);

    // Find by debtor account number
    List<SepaMandate> findByDebtorAccountNumberAndStatusOrderByCreatedAtDesc(String accountNumber, SepaMandate.MandateStatus status);

    // Find by creditor ID
    List<SepaMandate> findByCreditorIdAndStatusOrderByCreatedAtDesc(String creditorId, SepaMandate.MandateStatus status);

    // Find by mandate type
    List<SepaMandate> findByMandateTypeAndStatusOrderByCreatedAtDesc(SepaMandate.MandateType mandateType, SepaMandate.MandateStatus status);

    // Find active mandates
    List<SepaMandate> findByStatusOrderByCreatedAtDesc(SepaMandate.MandateStatus status);

    // Find mandates expiring soon
    @Query("SELECT m FROM SepaMandate m WHERE m.status = 'ACTIVE' AND " +
           "m.lastCollectionDate BETWEEN :startDate AND :endDate")
    List<SepaMandate> findMandatesExpiringSoon(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    // Find by debtor and creditor
    Optional<SepaMandate> findByDebtorIbanAndCreditorIdAndStatus(String debtorIban, String creditorId,
                                                                   SepaMandate.MandateStatus status);

    // Count by status
    Long countByStatus(SepaMandate.MandateStatus status);

    // Count by mandate type
    Long countByMandateTypeAndStatus(SepaMandate.MandateType mandateType, SepaMandate.MandateStatus status);

    // Find mandates needing first collection
    @Query("SELECT m FROM SepaMandate m WHERE m.status = 'ACTIVE' AND " +
           "m.sequenceType = 'FRST' AND m.totalCollections = 0 AND " +
           "m.firstCollectionDate <= :today")
    List<SepaMandate> findMandatesNeedingFirstCollection(@Param("today") LocalDate today);

    // Find inactive mandates (no collections in X days)
    @Query("SELECT m FROM SepaMandate m WHERE m.status = 'ACTIVE' AND " +
           "m.lastCollectionDate < :cutoffDate")
    List<SepaMandate> findInactiveMandates(@Param("cutoffDate") LocalDate cutoffDate);
}
