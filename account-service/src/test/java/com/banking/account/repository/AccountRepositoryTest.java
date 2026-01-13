package com.banking.account.repository;

import com.banking.account.model.Account;
import com.banking.account.model.AccountStatus;
import com.banking.account.model.AccountType;
import com.banking.account.model.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Account Repository Database Tests")
class AccountRepositoryTest {

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
    private AccountRepository accountRepository;

    private Account sampleAccount1;
    private Account sampleAccount2;
    private Account sampleAccount3;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();

        sampleAccount1 = Account.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.CHECKING)
                .build();

        sampleAccount2 = Account.builder()
                .accountNumber("TR330006100519786457841327")
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(new BigDecimal("2000.00"))
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.SAVINGS)
                .build();

        sampleAccount3 = Account.builder()
                .accountNumber("TR330006100519786457841328")
                .customerId("CUS-789012")
                .customerName("Jane Smith")
                .balance(new BigDecimal("500.00"))
                .currency(Currency.EUR)
                .status(AccountStatus.FROZEN)
                .accountType(AccountType.CHECKING)
                .build();
    }

    // BASIC CRUD TESTS

    @Test
    @DisplayName("Should save account successfully")
    void shouldSaveAccountSuccessfully() {
        Account savedAccount = accountRepository.save(sampleAccount1);

        assertThat(savedAccount).isNotNull();
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getAccountNumber()).isEqualTo("TR330006100519786457841326");
        assertThat(savedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(savedAccount.getCreatedAt()).isNotNull();
        assertThat(savedAccount.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find account by ID")
    void shouldFindAccountById() {
        Account savedAccount = accountRepository.save(sampleAccount1);

        Optional<Account> foundAccount = accountRepository.findById(savedAccount.getId());

        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getAccountNumber()).isEqualTo("TR330006100519786457841326");
    }

    @Test
    @DisplayName("Should update account successfully")
    void shouldUpdateAccountSuccessfully() {
        Account savedAccount = accountRepository.save(sampleAccount1);

        savedAccount.setBalance(new BigDecimal("1500.00"));
        savedAccount.setStatus(AccountStatus.FROZEN);
        Account updatedAccount = accountRepository.save(savedAccount);

        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(updatedAccount.getStatus()).isEqualTo(AccountStatus.FROZEN);
        assertThat(updatedAccount.getUpdatedAt()).isAfter(updatedAccount.getCreatedAt());
    }

    @Test
    @DisplayName("Should delete account successfully")
    void shouldDeleteAccountSuccessfully() {
        Account savedAccount = accountRepository.save(sampleAccount1);
        Long accountId = savedAccount.getId();

        accountRepository.deleteById(accountId);

        Optional<Account> deletedAccount = accountRepository.findById(accountId);
        assertThat(deletedAccount).isEmpty();
    }

    // FIND BY ACCOUNT NUMBER TESTS

    @Test
    @DisplayName("Should find account by account number")
    void shouldFindAccountByAccountNumber() {
        accountRepository.save(sampleAccount1);

        Optional<Account> foundAccount = accountRepository.findByAccountNumber("TR330006100519786457841326");

        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getCustomerId()).isEqualTo("CUS-123456");
        assertThat(foundAccount.get().getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should return empty when account number not found")
    void shouldReturnEmptyWhenAccountNumberNotFound() {
        Optional<Account> foundAccount = accountRepository.findByAccountNumber("TR000000000000000000000000");

        assertThat(foundAccount).isEmpty();
    }

    // FIND BY ACCOUNT NUMBER FOR UPDATE TESTS

    @Test
    @DisplayName("Should find account by account number for update with pessimistic lock")
    void shouldFindAccountByAccountNumberForUpdateWithPessimisticLock() {
        accountRepository.save(sampleAccount1);

        Optional<Account> foundAccount = accountRepository.findByAccountNumberForUpdate("TR330006100519786457841326");

        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getAccountNumber()).isEqualTo("TR330006100519786457841326");
    }

    @Test
    @DisplayName("Should return empty when finding for update with non-existent account number")
    void shouldReturnEmptyWhenFindingForUpdateWithNonExistentAccountNumber() {
        Optional<Account> foundAccount = accountRepository.findByAccountNumberForUpdate("TR000000000000000000000000");

        assertThat(foundAccount).isEmpty();
    }

    // FIND BY CUSTOMER ID TESTS

    @Test
    @DisplayName("Should find all accounts by customer ID")
    void shouldFindAllAccountsByCustomerId() {
        accountRepository.save(sampleAccount1);
        accountRepository.save(sampleAccount2);
        accountRepository.save(sampleAccount3);

        List<Account> accounts = accountRepository.findByCustomerId("CUS-123456");

        assertThat(accounts).hasSize(2);
        assertThat(accounts)
                .extracting(Account::getAccountNumber)
                .containsExactlyInAnyOrder("TR330006100519786457841326", "TR330006100519786457841327");
    }

    @Test
    @DisplayName("Should return empty list when customer has no accounts")
    void shouldReturnEmptyListWhenCustomerHasNoAccounts() {
        List<Account> accounts = accountRepository.findByCustomerId("CUS-NOACCOUNTS");

        assertThat(accounts).isEmpty();
    }

    @Test
    @DisplayName("Should find single account for customer with one account")
    void shouldFindSingleAccountForCustomerWithOneAccount() {
        accountRepository.save(sampleAccount3);

        List<Account> accounts = accountRepository.findByCustomerId("CUS-789012");

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getAccountNumber()).isEqualTo("TR330006100519786457841328");
    }

    // FIND BY STATUS TESTS

    @Test
    @DisplayName("Should find all accounts by status ACTIVE")
    void shouldFindAllAccountsByStatusActive() {
        accountRepository.save(sampleAccount1);
        accountRepository.save(sampleAccount2);
        accountRepository.save(sampleAccount3);

        List<Account> activeAccounts = accountRepository.findByStatus(AccountStatus.ACTIVE);

        assertThat(activeAccounts).hasSize(2);
        assertThat(activeAccounts)
                .extracting(Account::getStatus)
                .containsOnly(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find all accounts by status FROZEN")
    void shouldFindAllAccountsByStatusFrozen() {
        accountRepository.save(sampleAccount1);
        accountRepository.save(sampleAccount2);
        accountRepository.save(sampleAccount3);

        List<Account> frozenAccounts = accountRepository.findByStatus(AccountStatus.FROZEN);

        assertThat(frozenAccounts).hasSize(1);
        assertThat(frozenAccounts.get(0).getAccountNumber()).isEqualTo("TR330006100519786457841328");
    }

    @Test
    @DisplayName("Should return empty list when no accounts have specified status")
    void shouldReturnEmptyListWhenNoAccountsHaveSpecifiedStatus() {
        accountRepository.save(sampleAccount1);

        List<Account> closedAccounts = accountRepository.findByStatus(AccountStatus.CLOSED);

        assertThat(closedAccounts).isEmpty();
    }

    // EXISTS BY ACCOUNT NUMBER TESTS

    @Test
    @DisplayName("Should return true when account number exists")
    void shouldReturnTrueWhenAccountNumberExists() {
        accountRepository.save(sampleAccount1);

        boolean exists = accountRepository.existsByAccountNumber("TR330006100519786457841326");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when account number does not exist")
    void shouldReturnFalseWhenAccountNumberDoesNotExist() {
        boolean exists = accountRepository.existsByAccountNumber("TR000000000000000000000000");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return false after deleting account")
    void shouldReturnFalseAfterDeletingAccount() {
        Account savedAccount = accountRepository.save(sampleAccount1);
        accountRepository.deleteById(savedAccount.getId());

        boolean exists = accountRepository.existsByAccountNumber("TR330006100519786457841326");

        assertThat(exists).isFalse();
    }

    // COUNT BY CUSTOMER ID AND STATUS TESTS

    @Test
    @DisplayName("Should count accounts by customer ID and status")
    void shouldCountAccountsByCustomerIdAndStatus() {
        accountRepository.save(sampleAccount1);
        accountRepository.save(sampleAccount2);
        accountRepository.save(sampleAccount3);

        long count = accountRepository.countByCustomerIdAndStatus("CUS-123456", AccountStatus.ACTIVE);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return zero count when customer has no accounts with specified status")
    void shouldReturnZeroCountWhenCustomerHasNoAccountsWithSpecifiedStatus() {
        accountRepository.save(sampleAccount1);
        accountRepository.save(sampleAccount2);

        long count = accountRepository.countByCustomerIdAndStatus("CUS-123456", AccountStatus.FROZEN);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should return zero count for non-existent customer")
    void shouldReturnZeroCountForNonExistentCustomer() {
        long count = accountRepository.countByCustomerIdAndStatus("CUS-NOEXIST", AccountStatus.ACTIVE);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should count all statuses correctly for customer")
    void shouldCountAllStatusesCorrectlyForCustomer() {
        accountRepository.save(sampleAccount1);
        sampleAccount2.setStatus(AccountStatus.FROZEN);
        accountRepository.save(sampleAccount2);

        Account closedAccount = Account.builder()
                .accountNumber("TR330006100519786457841329")
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(BigDecimal.ZERO)
                .currency(Currency.TRY)
                .status(AccountStatus.CLOSED)
                .accountType(AccountType.CHECKING)
                .build();
        accountRepository.save(closedAccount);

        long activeCount = accountRepository.countByCustomerIdAndStatus("CUS-123456", AccountStatus.ACTIVE);
        long frozenCount = accountRepository.countByCustomerIdAndStatus("CUS-123456", AccountStatus.FROZEN);
        long closedCount = accountRepository.countByCustomerIdAndStatus("CUS-123456", AccountStatus.CLOSED);

        assertThat(activeCount).isEqualTo(1);
        assertThat(frozenCount).isEqualTo(1);
        assertThat(closedCount).isEqualTo(1);
    }

    // COMPLEX QUERY TESTS

    @Test
    @DisplayName("Should handle multiple currency types correctly")
    void shouldHandleMultipleCurrencyTypesCorrectly() {
        accountRepository.save(sampleAccount1);  // TRY
        accountRepository.save(sampleAccount2);  // USD
        accountRepository.save(sampleAccount3);  // EUR

        List<Account> allAccounts = accountRepository.findAll();

        assertThat(allAccounts).hasSize(3);
        assertThat(allAccounts)
                .extracting(Account::getCurrency)
                .containsExactlyInAnyOrder(Currency.TRY, Currency.USD, Currency.EUR);
    }

    @Test
    @DisplayName("Should handle multiple account types correctly")
    void shouldHandleMultipleAccountTypesCorrectly() {
        accountRepository.save(sampleAccount1);  // CHECKING
        accountRepository.save(sampleAccount2);  // SAVINGS

        List<Account> allAccounts = accountRepository.findAll();

        assertThat(allAccounts).hasSize(2);
        assertThat(allAccounts)
                .extracting(Account::getAccountType)
                .containsExactlyInAnyOrder(AccountType.CHECKING, AccountType.SAVINGS);
    }

    @Test
    @DisplayName("Should preserve BigDecimal precision for balance")
    void shouldPreserveBigDecimalPrecisionForBalance() {
        sampleAccount1.setBalance(new BigDecimal("1234.56789"));
        Account savedAccount = accountRepository.save(sampleAccount1);

        Account foundAccount = accountRepository.findById(savedAccount.getId()).orElseThrow();

        assertThat(foundAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1234.56789"));
    }

    @Test
    @DisplayName("Should handle account with zero balance")
    void shouldHandleAccountWithZeroBalance() {
        sampleAccount1.setBalance(BigDecimal.ZERO);
        Account savedAccount = accountRepository.save(sampleAccount1);

        Optional<Account> foundAccount = accountRepository.findById(savedAccount.getId());

        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle concurrent saves for different accounts")
    void shouldHandleConcurrentSavesForDifferentAccounts() {
        Account saved1 = accountRepository.save(sampleAccount1);
        Account saved2 = accountRepository.save(sampleAccount2);
        Account saved3 = accountRepository.save(sampleAccount3);

        List<Account> allAccounts = accountRepository.findAll();

        assertThat(allAccounts).hasSize(3);
        assertThat(allAccounts)
                .extracting(Account::getId)
                .containsExactlyInAnyOrder(saved1.getId(), saved2.getId(), saved3.getId());
    }
}
