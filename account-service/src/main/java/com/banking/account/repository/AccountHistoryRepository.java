package com.banking.account.repository;

import com.banking.account.model.AccountHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountHistoryRepository extends JpaRepository<AccountHistory, Long> {

    List<AccountHistory> findByAccountIdOrderByTimestampDesc(Long accountId);

    Page<AccountHistory> findByAccountId(Long accountId, Pageable pageable);

    List<AccountHistory> findByAccountNumberOrderByTimestampDesc(String accountNumber);

    List<AccountHistory> findByAccountIdAndTimestampBetween(
            Long accountId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}