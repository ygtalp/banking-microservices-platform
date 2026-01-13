package com.banking.account.repository;

import com.banking.account.model.AccountHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Account History Repository Database Tests")
class AccountHistoryRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AccountHistoryRepository accountHistoryRepository;

    private AccountHistory history1;
    private AccountHistory history2;
    private AccountHistory history3;
    private AccountHistory history4;

    @BeforeEach
    void setUp() {
        accountHistoryRepository.deleteAll();

        LocalDateTime now = LocalDateTime.now();

        history1 = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("ACCOUNT_CREATED")
                .previousBalance(BigDecimal.ZERO)
                .newBalance(new BigDecimal("1000.00"))
                .amount(new BigDecimal("1000.00"))
                .description("Account created with initial balance")
                .referenceId("REF-001")
                .timestamp(now.minusDays(5))
                .build();

        history2 = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("CREDIT")
                .previousBalance(new BigDecimal("1000.00"))
                .newBalance(new BigDecimal("1500.00"))
                .amount(new BigDecimal("500.00"))
                .description("Account credited")
                .referenceId("REF-002")
                .timestamp(now.minusDays(3))
                .build();

        history3 = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("DEBIT")
                .previousBalance(new BigDecimal("1500.00"))
                .newBalance(new BigDecimal("1200.00"))
                .amount(new BigDecimal("300.00"))
                .description("Account debited")
                .referenceId("REF-003")
                .timestamp(now.minusDays(1))
                .build();

        history4 = AccountHistory.builder()
                .accountId(2L)
                .accountNumber("TR330006100519786457841327")
                .operation("ACCOUNT_CREATED")
                .previousBalance(BigDecimal.ZERO)
                .newBalance(new BigDecimal("2000.00"))
                .amount(new BigDecimal("2000.00"))
                .description("Second account created")
                .referenceId("REF-004")
                .timestamp(now.minusDays(2))
                .build();
    }

    // BASIC CRUD TESTS

    @Test
    @DisplayName("Should save account history successfully")
    void shouldSaveAccountHistorySuccessfully() {
        AccountHistory savedHistory = accountHistoryRepository.save(history1);

        assertThat(savedHistory).isNotNull();
        assertThat(savedHistory.getId()).isNotNull();
        assertThat(savedHistory.getAccountId()).isEqualTo(1L);
        assertThat(savedHistory.getAccountNumber()).isEqualTo("TR330006100519786457841326");
        assertThat(savedHistory.getOperation()).isEqualTo("ACCOUNT_CREATED");
        assertThat(savedHistory.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(savedHistory.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should find account history by ID")
    void shouldFindAccountHistoryById() {
        AccountHistory savedHistory = accountHistoryRepository.save(history1);

        AccountHistory foundHistory = accountHistoryRepository.findById(savedHistory.getId()).orElseThrow();

        assertThat(foundHistory.getAccountNumber()).isEqualTo("TR330006100519786457841326");
        assertThat(foundHistory.getOperation()).isEqualTo("ACCOUNT_CREATED");
    }

    @Test
    @DisplayName("Should save multiple history records")
    void shouldSaveMultipleHistoryRecords() {
        accountHistoryRepository.save(history1);
        accountHistoryRepository.save(history2);
        accountHistoryRepository.save(history3);

        List<AccountHistory> allHistory = accountHistoryRepository.findAll();

        assertThat(allHistory).hasSize(3);
    }

    // FIND BY ACCOUNT ID ORDER BY TIMESTAMP DESC TESTS

    @Test
    @DisplayName("Should find history by account ID ordered by timestamp descending")
    void shouldFindHistoryByAccountIdOrderedByTimestampDesc() {
        accountHistoryRepository.save(history1);
        accountHistoryRepository.save(history2);
        accountHistoryRepository.save(history3);
        accountHistoryRepository.save(history4);

        List<AccountHistory> accountHistory = accountHistoryRepository.findByAccountIdOrderByTimestampDesc(1L);

        assertThat(accountHistory).hasSize(3);
        assertThat(accountHistory.get(0).getOperation()).isEqualTo("DEBIT");  // Most recent
        assertThat(accountHistory.get(1).getOperation()).isEqualTo("CREDIT");
        assertThat(accountHistory.get(2).getOperation()).isEqualTo("ACCOUNT_CREATED");  // Oldest
    }

    @Test
    @DisplayName("Should return empty list when account ID has no history")
    void shouldReturnEmptyListWhenAccountIdHasNoHistory() {
        List<AccountHistory> accountHistory = accountHistoryRepository.findByAccountIdOrderByTimestampDesc(999L);

        assertThat(accountHistory).isEmpty();
    }

    @Test
    @DisplayName("Should verify descending order with precise timestamps")
    void shouldVerifyDescendingOrderWithPreciseTimestamps() {
        accountHistoryRepository.save(history1);
        accountHistoryRepository.save(history2);
        accountHistoryRepository.save(history3);

        List<AccountHistory> accountHistory = accountHistoryRepository.findByAccountIdOrderByTimestampDesc(1L);

        assertThat(accountHistory).hasSize(3);
        for (int i = 0; i < accountHistory.size() - 1; i++) {
            assertThat(accountHistory.get(i).getTimestamp())
                    .isAfterOrEqualTo(accountHistory.get(i + 1).getTimestamp());
        }
    }

    // FIND BY ACCOUNT ID WITH PAGINATION TESTS

    @Test
    @DisplayName("Should find history by account ID with pagination")
    void shouldFindHistoryByAccountIdWithPagination() {
        accountHistoryRepository.save(history1);
        accountHistoryRepository.save(history2);
        accountHistoryRepository.save(history3);

        Pageable pageable = PageRequest.of(0, 2);
        Page<AccountHistory> page = accountHistoryRepository.findByAccountId(1L, pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should get second page of history")
    void shouldGetSecondPageOfHistory() {
        accountHistoryRepository.save(history1);
        accountHistoryRepository.save(history2);
        accountHistoryRepository.save(history3);

        Pageable pageable = PageRequest.of(1, 2);
        Page<AccountHistory> page = accountHistoryRepository.findByAccountId(1L, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.isLast()).isTrue();
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("Should return empty page when no history exists for pagination")
    void shouldReturnEmptyPageWhenNoHistoryExistsForPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AccountHistory> page = accountHistoryRepository.findByAccountId(999L, pageable);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Should handle custom page sizes")
    void shouldHandleCustomPageSizes() {
        for (int i = 0; i < 10; i++) {
            AccountHistory history = AccountHistory.builder()
                    .accountId(1L)
                    .accountNumber("TR330006100519786457841326")
                    .operation("CREDIT")
                    .amount(new BigDecimal("100.00"))
                    .build();
            accountHistoryRepository.save(history);
        }

        Pageable smallPage = PageRequest.of(0, 5);
        Page<AccountHistory> page = accountHistoryRepository.findByAccountId(1L, smallPage);

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    // FIND BY ACCOUNT NUMBER ORDER BY TIMESTAMP DESC TESTS

    @Test
    @DisplayName("Should find history by account number ordered by timestamp descending")
    void shouldFindHistoryByAccountNumberOrderedByTimestampDesc() {
        accountHistoryRepository.save(history1);
        accountHistoryRepository.save(history2);
        accountHistoryRepository.save(history3);
        accountHistoryRepository.save(history4);

        List<AccountHistory> accountHistory = accountHistoryRepository
                .findByAccountNumberOrderByTimestampDesc("TR330006100519786457841326");

        assertThat(accountHistory).hasSize(3);
        assertThat(accountHistory.get(0).getOperation()).isEqualTo("DEBIT");
        assertThat(accountHistory.get(1).getOperation()).isEqualTo("CREDIT");
        assertThat(accountHistory.get(2).getOperation()).isEqualTo("ACCOUNT_CREATED");
    }

    @Test
    @DisplayName("Should return empty list when account number has no history")
    void shouldReturnEmptyListWhenAccountNumberHasNoHistory() {
        List<AccountHistory> accountHistory = accountHistoryRepository
                .findByAccountNumberOrderByTimestampDesc("TR000000000000000000000000");

        assertThat(accountHistory).isEmpty();
    }

    @Test
    @DisplayName("Should find single history record by account number")
    void shouldFindSingleHistoryRecordByAccountNumber() {
        accountHistoryRepository.save(history4);

        List<AccountHistory> accountHistory = accountHistoryRepository
                .findByAccountNumberOrderByTimestampDesc("TR330006100519786457841327");

        assertThat(accountHistory).hasSize(1);
        assertThat(accountHistory.get(0).getAccountId()).isEqualTo(2L);
    }

    // FIND BY ACCOUNT ID AND TIMESTAMP BETWEEN TESTS

    @Test
    @DisplayName("Should find history by account ID and timestamp range")
    void shouldFindHistoryByAccountIdAndTimestampRange() {
        accountHistoryRepository.save(history1);
        accountHistoryRepository.save(history2);
        accountHistoryRepository.save(history3);

        LocalDateTime startDate = LocalDateTime.now().minusDays(4);
        LocalDateTime endDate = LocalDateTime.now().minusDays(2);

        List<AccountHistory> filteredHistory = accountHistoryRepository
                .findByAccountIdAndTimestampBetween(1L, startDate, endDate);

        assertThat(filteredHistory).hasSize(1);
        assertThat(filteredHistory.get(0).getOperation()).isEqualTo("CREDIT");
    }

    @Test
    @DisplayName("Should return all history when date range is wide")
    void shouldReturnAllHistoryWhenDateRangeIsWide() {
        accountHistoryRepository.save(history1);
        accountHistoryRepository.save(history2);
        accountHistoryRepository.save(history3);

        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        List<AccountHistory> filteredHistory = accountHistoryRepository
                .findByAccountIdAndTimestampBetween(1L, startDate, endDate);

        assertThat(filteredHistory).hasSize(3);
    }

    @Test
    @DisplayName("Should return empty list when no history in date range")
    void shouldReturnEmptyListWhenNoHistoryInDateRange() {
        accountHistoryRepository.save(history1);

        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now().minusDays(8);

        List<AccountHistory> filteredHistory = accountHistoryRepository
                .findByAccountIdAndTimestampBetween(1L, startDate, endDate);

        assertThat(filteredHistory).isEmpty();
    }

    @Test
    @DisplayName("Should handle single day date range")
    void shouldHandleSingleDayDateRange() {
        LocalDateTime now = LocalDateTime.now();
        AccountHistory todayHistory = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("CREDIT")
                .amount(new BigDecimal("100.00"))
                .timestamp(now)
                .build();
        accountHistoryRepository.save(todayHistory);

        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);

        List<AccountHistory> todayHistoryList = accountHistoryRepository
                .findByAccountIdAndTimestampBetween(1L, startOfDay, endOfDay);

        assertThat(todayHistoryList).hasSize(1);
        assertThat(todayHistoryList.get(0).getOperation()).isEqualTo("CREDIT");
    }

    // COMPREHENSIVE OPERATION TYPE TESTS

    @Test
    @DisplayName("Should store all operation types correctly")
    void shouldStoreAllOperationTypesCorrectly() {
        AccountHistory creditHistory = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("CREDIT")
                .amount(new BigDecimal("100.00"))
                .build();

        AccountHistory debitHistory = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("DEBIT")
                .amount(new BigDecimal("50.00"))
                .build();

        AccountHistory freezeHistory = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("FREEZE")
                .description("Account frozen")
                .build();

        AccountHistory activateHistory = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("ACTIVATE")
                .description("Account activated")
                .build();

        AccountHistory closeHistory = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("CLOSE")
                .description("Account closed")
                .build();

        accountHistoryRepository.save(creditHistory);
        accountHistoryRepository.save(debitHistory);
        accountHistoryRepository.save(freezeHistory);
        accountHistoryRepository.save(activateHistory);
        accountHistoryRepository.save(closeHistory);

        List<AccountHistory> allHistory = accountHistoryRepository.findByAccountIdOrderByTimestampDesc(1L);

        assertThat(allHistory).hasSize(5);
        assertThat(allHistory)
                .extracting(AccountHistory::getOperation)
                .contains("CREDIT", "DEBIT", "FREEZE", "ACTIVATE", "CLOSE");
    }

    @Test
    @DisplayName("Should preserve BigDecimal precision in history")
    void shouldPreserveBigDecimalPrecisionInHistory() {
        history1.setPreviousBalance(new BigDecimal("1234.56789"));
        history1.setNewBalance(new BigDecimal("2345.67890"));
        history1.setAmount(new BigDecimal("1111.11101"));

        AccountHistory savedHistory = accountHistoryRepository.save(history1);
        AccountHistory foundHistory = accountHistoryRepository.findById(savedHistory.getId()).orElseThrow();

        assertThat(foundHistory.getPreviousBalance()).isEqualByComparingTo(new BigDecimal("1234.56789"));
        assertThat(foundHistory.getNewBalance()).isEqualByComparingTo(new BigDecimal("2345.67890"));
        assertThat(foundHistory.getAmount()).isEqualByComparingTo(new BigDecimal("1111.11101"));
    }

    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
        AccountHistory minimalHistory = AccountHistory.builder()
                .accountId(1L)
                .accountNumber("TR330006100519786457841326")
                .operation("FREEZE")
                .build();

        AccountHistory savedHistory = accountHistoryRepository.save(minimalHistory);
        AccountHistory foundHistory = accountHistoryRepository.findById(savedHistory.getId()).orElseThrow();

        assertThat(foundHistory.getPreviousBalance()).isNull();
        assertThat(foundHistory.getNewBalance()).isNull();
        assertThat(foundHistory.getAmount()).isNull();
        assertThat(foundHistory.getDescription()).isNull();
        assertThat(foundHistory.getReferenceId()).isNull();
    }

    @Test
    @DisplayName("Should store reference IDs correctly")
    void shouldStoreReferenceIdsCorrectly() {
        history1.setReferenceId("TRANSFER-123456");
        history2.setReferenceId("TRANSACTION-789012");
        history3.setReferenceId("PAYMENT-345678");

        accountHistoryRepository.save(history1);
        accountHistoryRepository.save(history2);
        accountHistoryRepository.save(history3);

        List<AccountHistory> allHistory = accountHistoryRepository.findByAccountIdOrderByTimestampDesc(1L);

        assertThat(allHistory)
                .extracting(AccountHistory::getReferenceId)
                .containsExactlyInAnyOrder("PAYMENT-345678", "TRANSACTION-789012", "TRANSFER-123456");
    }

    @Test
    @DisplayName("Should store long descriptions correctly")
    void shouldStoreLongDescriptionsCorrectly() {
        String longDescription = "This is a very long description that exceeds normal length. " +
                "It contains detailed information about the transaction including source, " +
                "destination, purpose, and other metadata that might be relevant for audit purposes.";

        history1.setDescription(longDescription);
        AccountHistory savedHistory = accountHistoryRepository.save(history1);
        AccountHistory foundHistory = accountHistoryRepository.findById(savedHistory.getId()).orElseThrow();

        assertThat(foundHistory.getDescription()).isEqualTo(longDescription);
        assertThat(foundHistory.getDescription().length()).isGreaterThan(100);
    }
}
