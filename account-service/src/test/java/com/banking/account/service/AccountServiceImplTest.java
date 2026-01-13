package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.BalanceUpdateRequest;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.event.AccountCreatedEvent;
import com.banking.account.event.AccountStatusChangedEvent;
import com.banking.account.event.BalanceChangedEvent;
import com.banking.account.exception.AccountAlreadyExistsException;
import com.banking.account.exception.AccountNotFoundException;
import com.banking.account.exception.InsufficientBalanceException;
import com.banking.account.exception.InvalidAccountStateException;
import com.banking.account.model.Account;
import com.banking.account.model.AccountHistory;
import com.banking.account.model.AccountStatus;
import com.banking.account.model.AccountType;
import com.banking.account.model.Currency;
import com.banking.account.repository.AccountHistoryRepository;
import com.banking.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Service Implementation Tests")
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountHistoryRepository accountHistoryRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private IbanGenerator ibanGenerator;

    @InjectMocks
    private AccountServiceImpl accountService;

    private CreateAccountRequest createAccountRequest;
    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        createAccountRequest = CreateAccountRequest.builder()
                .customerId("CUS-123456")
                .customerName("John Doe")
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("1000.00"))
                .build();

        sampleAccount = Account.builder()
                .id(1L)
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    // ==================== CREATE ACCOUNT TESTS ====================

    @Test
    @DisplayName("Should create account successfully")
    void shouldCreateAccountSuccessfully() {
        // Given
        String generatedIban = "TR330006100519786457841326";
        when(ibanGenerator.generateIban()).thenReturn(generatedIban);
        when(accountRepository.existsByAccountNumber(generatedIban)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(new AccountHistory());

        // When
        AccountResponse response = accountService.createAccount(createAccountRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo(generatedIban);
        assertThat(response.getCustomerId()).isEqualTo("CUS-123456");
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));

        verify(ibanGenerator).generateIban();
        verify(accountRepository).existsByAccountNumber(generatedIban);
        verify(accountRepository).save(any(Account.class));
        verify(accountHistoryRepository).save(any(AccountHistory.class));
        verify(eventPublisher).publishAccountCreated(any(AccountCreatedEvent.class));
    }

    @Test
    @DisplayName("Should create account with zero initial balance when not provided")
    void shouldCreateAccountWithZeroInitialBalanceWhenNotProvided() {
        // Given
        CreateAccountRequest requestWithoutBalance = CreateAccountRequest.builder()
                .customerId("CUS-123456")
                .customerName("John Doe")
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .build();

        String generatedIban = "TR330006100519786457841326";
        when(ibanGenerator.generateIban()).thenReturn(generatedIban);
        when(accountRepository.existsByAccountNumber(generatedIban)).thenReturn(false);

        Account accountWithZeroBalance = Account.builder()
                .id(1L)
                .accountNumber(generatedIban)
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(BigDecimal.ZERO)
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(accountWithZeroBalance);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(new AccountHistory());

        // When
        AccountResponse response = accountService.createAccount(requestWithoutBalance);

        // Then
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(accountRepository).save(argThat(account ->
            account.getBalance().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    @DisplayName("Should throw exception when account already exists")
    void shouldThrowExceptionWhenAccountAlreadyExists() {
        // Given
        String generatedIban = "TR330006100519786457841326";
        when(ibanGenerator.generateIban()).thenReturn(generatedIban);
        when(accountRepository.existsByAccountNumber(generatedIban)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(createAccountRequest))
                .isInstanceOf(AccountAlreadyExistsException.class)
                .hasMessageContaining("Account with this number already exists");

        verify(accountRepository, never()).save(any(Account.class));
        verify(eventPublisher, never()).publishAccountCreated(any());
    }

    // ==================== GET ACCOUNT TESTS ====================

    @Test
    @DisplayName("Should get account by ID successfully")
    void shouldGetAccountByIdSuccessfully() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        // When
        AccountResponse response = accountService.getAccountById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAccountNumber()).isEqualTo(sampleAccount.getAccountNumber());
        verify(accountRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when account not found by ID")
    void shouldThrowExceptionWhenAccountNotFoundById() {
        // Given
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountById(999L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found with id: 999");
    }

    @Test
    @DisplayName("Should get account by account number successfully")
    void shouldGetAccountByAccountNumberSuccessfully() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(sampleAccount));

        // When
        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo(accountNumber);
        verify(accountRepository).findByAccountNumber(accountNumber);
    }

    @Test
    @DisplayName("Should throw exception when account not found by account number")
    void shouldThrowExceptionWhenAccountNotFoundByAccountNumber() {
        // Given
        String accountNumber = "TR999999999999999999999999";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountByAccountNumber(accountNumber))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found: " + accountNumber);
    }

    @Test
    @DisplayName("Should get accounts by customer ID successfully")
    void shouldGetAccountsByCustomerIdSuccessfully() {
        // Given
        String customerId = "CUS-123456";
        Account account2 = Account.builder()
                .id(2L)
                .accountNumber("TR440006100519786457841327")
                .customerId(customerId)
                .customerName("John Doe")
                .balance(new BigDecimal("2000.00"))
                .currency(Currency.USD)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build();

        List<Account> accounts = Arrays.asList(sampleAccount, account2);
        when(accountRepository.findByCustomerId(customerId)).thenReturn(accounts);

        // When
        List<AccountResponse> responses = accountService.getAccountsByCustomerId(customerId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(AccountResponse::getCustomerId).containsOnly(customerId);
        verify(accountRepository).findByCustomerId(customerId);
    }

    // ==================== CREDIT ACCOUNT TESTS ====================

    @Test
    @DisplayName("Should credit account successfully")
    void shouldCreditAccountSuccessfully() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        BalanceUpdateRequest request = BalanceUpdateRequest.builder()
                .amount(new BigDecimal("500.00"))
                .description("Salary payment")
                .referenceId("REF-123")
                .build();

        Account accountBeforeUpdate = Account.builder()
                .id(1L)
                .accountNumber(accountNumber)
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        Account accountAfterUpdate = Account.builder()
                .id(1L)
                .accountNumber(accountNumber)
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(new BigDecimal("1500.00"))
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(accountBeforeUpdate));
        when(accountRepository.save(any(Account.class))).thenReturn(accountAfterUpdate);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(new AccountHistory());

        // When
        AccountResponse response = accountService.creditAccount(accountNumber, request);

        // Then
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        verify(accountRepository).findByAccountNumberForUpdate(accountNumber);
        verify(accountRepository).save(any(Account.class));
        verify(accountHistoryRepository).save(any(AccountHistory.class));
        verify(eventPublisher).publishBalanceChanged(any(BalanceChangedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when crediting inactive account")
    void shouldThrowExceptionWhenCreditingInactiveAccount() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        sampleAccount.setStatus(AccountStatus.CLOSED);
        BalanceUpdateRequest request = BalanceUpdateRequest.builder()
                .amount(new BigDecimal("500.00"))
                .build();

        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(sampleAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.creditAccount(accountNumber, request))
                .isInstanceOf(InvalidAccountStateException.class)
                .hasMessageContaining("Account is not active");

        verify(accountRepository, never()).save(any(Account.class));
        verify(eventPublisher, never()).publishBalanceChanged(any());
    }

    // ==================== DEBIT ACCOUNT TESTS ====================

    @Test
    @DisplayName("Should debit account successfully")
    void shouldDebitAccountSuccessfully() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        BalanceUpdateRequest request = BalanceUpdateRequest.builder()
                .amount(new BigDecimal("300.00"))
                .description("ATM withdrawal")
                .referenceId("REF-456")
                .build();

        Account accountBeforeUpdate = Account.builder()
                .id(1L)
                .accountNumber(accountNumber)
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        Account accountAfterUpdate = Account.builder()
                .id(1L)
                .accountNumber(accountNumber)
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(new BigDecimal("700.00"))
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(accountBeforeUpdate));
        when(accountRepository.save(any(Account.class))).thenReturn(accountAfterUpdate);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(new AccountHistory());

        // When
        AccountResponse response = accountService.debitAccount(accountNumber, request);

        // Then
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        verify(accountRepository).findByAccountNumberForUpdate(accountNumber);
        verify(accountRepository).save(any(Account.class));
        verify(accountHistoryRepository).save(any(AccountHistory.class));
        verify(eventPublisher).publishBalanceChanged(any(BalanceChangedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when debit amount exceeds balance")
    void shouldThrowExceptionWhenDebitAmountExceedsBalance() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        BalanceUpdateRequest request = BalanceUpdateRequest.builder()
                .amount(new BigDecimal("2000.00")) // More than balance
                .build();

        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(sampleAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.debitAccount(accountNumber, request))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance in account");

        verify(accountRepository, never()).save(any(Account.class));
        verify(eventPublisher, never()).publishBalanceChanged(any());
    }

    @Test
    @DisplayName("Should throw exception when debiting inactive account")
    void shouldThrowExceptionWhenDebitingInactiveAccount() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        sampleAccount.setStatus(AccountStatus.FROZEN);
        BalanceUpdateRequest request = BalanceUpdateRequest.builder()
                .amount(new BigDecimal("100.00"))
                .build();

        when(accountRepository.findByAccountNumberForUpdate(accountNumber)).thenReturn(Optional.of(sampleAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.debitAccount(accountNumber, request))
                .isInstanceOf(InvalidAccountStateException.class)
                .hasMessageContaining("Account is not active");

        verify(accountRepository, never()).save(any(Account.class));
    }

    // ==================== FREEZE ACCOUNT TESTS ====================

    @Test
    @DisplayName("Should freeze account successfully")
    void shouldFreezeAccountSuccessfully() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(new AccountHistory());

        // When
        AccountResponse response = accountService.freezeAccount(accountNumber);

        // Then
        assertThat(response).isNotNull();
        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(accountRepository).save(argThat(account ->
            account.getStatus() == AccountStatus.FROZEN
        ));
        verify(accountHistoryRepository).save(any(AccountHistory.class));
        verify(eventPublisher).publishAccountStatusChanged(any(AccountStatusChangedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when freezing non-existent account")
    void shouldThrowExceptionWhenFreezingNonExistentAccount() {
        // Given
        String accountNumber = "TR999999999999999999999999";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.freezeAccount(accountNumber))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository, never()).save(any(Account.class));
    }

    // ==================== ACTIVATE ACCOUNT TESTS ====================

    @Test
    @DisplayName("Should activate account successfully")
    void shouldActivateAccountSuccessfully() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        sampleAccount.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(new AccountHistory());

        // When
        AccountResponse response = accountService.activateAccount(accountNumber);

        // Then
        assertThat(response).isNotNull();
        verify(accountRepository).save(argThat(account ->
            account.getStatus() == AccountStatus.ACTIVE
        ));
        verify(eventPublisher).publishAccountStatusChanged(any(AccountStatusChangedEvent.class));
    }

    // ==================== CLOSE ACCOUNT TESTS ====================

    @Test
    @DisplayName("Should close account successfully when balance is zero")
    void shouldCloseAccountSuccessfullyWhenBalanceIsZero() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        sampleAccount.setBalance(BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(new AccountHistory());

        // When
        AccountResponse response = accountService.closeAccount(accountNumber);

        // Then
        assertThat(response).isNotNull();
        verify(accountRepository).save(argThat(account ->
            account.getStatus() == AccountStatus.CLOSED
        ));
        verify(eventPublisher).publishAccountStatusChanged(any(AccountStatusChangedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when closing account with non-zero balance")
    void shouldThrowExceptionWhenClosingAccountWithNonZeroBalance() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(sampleAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.closeAccount(accountNumber))
                .isInstanceOf(InvalidAccountStateException.class)
                .hasMessageContaining("Cannot close account with non-zero balance");

        verify(accountRepository, never()).save(any(Account.class));
    }

    // ==================== GET ACCOUNT HISTORY TESTS ====================

    @Test
    @DisplayName("Should get account history successfully")
    void shouldGetAccountHistorySuccessfully() {
        // Given
        String accountNumber = "TR330006100519786457841326";
        AccountHistory history1 = AccountHistory.builder()
                .id(1L)
                .accountId(1L)
                .accountNumber(accountNumber)
                .operation("CREATE")
                .build();
        AccountHistory history2 = AccountHistory.builder()
                .id(2L)
                .accountId(1L)
                .accountNumber(accountNumber)
                .operation("CREDIT")
                .build();

        List<AccountHistory> historyList = Arrays.asList(history1, history2);
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(sampleAccount));
        when(accountHistoryRepository.findByAccountIdOrderByTimestampDesc(1L)).thenReturn(historyList);

        // When
        List<AccountHistory> result = accountService.getAccountHistory(accountNumber);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AccountHistory::getOperation).containsExactly("CREATE", "CREDIT");
        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(accountHistoryRepository).findByAccountIdOrderByTimestampDesc(1L);
    }

    @Test
    @DisplayName("Should throw exception when getting history for non-existent account")
    void shouldThrowExceptionWhenGettingHistoryForNonExistentAccount() {
        // Given
        String accountNumber = "TR999999999999999999999999";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountHistory(accountNumber))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountHistoryRepository, never()).findByAccountIdOrderByTimestampDesc(anyLong());
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("Should handle multiple currency types")
    void shouldHandleMultipleCurrencyTypes() {
        // Given
        CreateAccountRequest usdRequest = CreateAccountRequest.builder()
                .customerId("CUS-123")
                .customerName("Test")
                .currency(Currency.USD)
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("100.00"))
                .build();

        when(ibanGenerator.generateIban()).thenReturn("TR330006100519786457841326");
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        Account usdAccount = Account.builder()
                .id(1L)
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123")
                .customerName("Test")
                .balance(new BigDecimal("100.00"))
                .currency(Currency.USD)
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(usdAccount);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(new AccountHistory());

        // When
        AccountResponse response = accountService.createAccount(usdRequest);

        // Then
        assertThat(response.getCurrency()).isEqualTo(Currency.USD);
    }

    @Test
    @DisplayName("Should handle different account types")
    void shouldHandleDifferentAccountTypes() {
        // Given
        CreateAccountRequest savingsRequest = CreateAccountRequest.builder()
                .customerId("CUS-123")
                .customerName("Test")
                .currency(Currency.TRY)
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("5000.00"))
                .build();

        when(ibanGenerator.generateIban()).thenReturn("TR330006100519786457841326");
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        Account savingsAccount = Account.builder()
                .id(1L)
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123")
                .customerName("Test")
                .balance(new BigDecimal("5000.00"))
                .currency(Currency.TRY)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(savingsAccount);
        when(accountHistoryRepository.save(any(AccountHistory.class))).thenReturn(new AccountHistory());

        // When
        AccountResponse response = accountService.createAccount(savingsRequest);

        // Then
        assertThat(response.getAccountType()).isEqualTo(AccountType.SAVINGS);
    }
}
