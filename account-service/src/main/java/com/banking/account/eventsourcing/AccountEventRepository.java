package com.banking.account.eventsourcing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Account Event Repository
 * Event Store persistence layer
 */
@Repository
public interface AccountEventRepository extends JpaRepository<AccountEvent, Long> {

    /**
     * Find all events for an account (ordered by version)
     */
    List<AccountEvent> findByAccountNumberOrderByAggregateVersionAsc(String accountNumber);

    /**
     * Find events after a specific version (for incremental replay)
     */
    List<AccountEvent> findByAccountNumberAndAggregateVersionGreaterThanOrderByAggregateVersionAsc(
            String accountNumber, Long fromVersion);

    /**
     * Get latest version for an account
     */
    @Query("SELECT MAX(e.aggregateVersion) FROM AccountEvent e WHERE e.accountNumber = :accountNumber")
    Long findLatestVersion(@Param("accountNumber") String accountNumber);

    /**
     * Count events for an account
     */
    Long countByAccountNumber(String accountNumber);

    /**
     * Find events by type
     */
    List<AccountEvent> findByEventTypeOrderByTimestampDesc(EventType eventType);

    /**
     * Find events by correlation ID (for distributed tracing)
     */
    List<AccountEvent> findByCorrelationIdOrderByTimestampAsc(String correlationId);
}
