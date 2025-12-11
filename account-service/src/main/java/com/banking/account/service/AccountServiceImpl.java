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
import com.banking.account.repository.AccountHistoryRepository;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountHistoryRepository accountHistoryRepository;
    private final EventPublisher eventPublisher;
    private final IbanGenerator ibanGenerator;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for customer: {}", request.getCustomerId());

        // Generate unique IBAN
        String iban = ibanGenerator.generateIban();

        // Check if account already exists
        if (accountRepository.existsByAccountNumber(iban)) {
            throw new AccountAlreadyExistsException("Account with this number already exists");
        }

        // Create account
        Account account = Account.builder()
                .accountNumber(iban)
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .currency(request.getCurrency())
                .accountType(request.getAccountType())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);

        // Record history
        recordHistory(savedAccount, "CREATE", null, savedAccount.getBalance(),
                savedAccount.getBalance(), "Account created", null);

        // Publish event
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountNumber(savedAccount.getAccountNumber())
                .customerId(savedAccount.getCustomerId())
                .customerName(savedAccount.getCustomerName())
                .initialBalance(savedAccount.getBalance())
                .currency(savedAccount.getCurrency())
                .accountType(savedAccount.getAccountType())
                .build();

        eventPublisher.publishAccountCreated(event);

        log.info("Account created successfully: {}", savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(String customerId) {
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponse creditAccount(String accountNumber, BalanceUpdateRequest request) {
        log.info("Crediting account: {} with amount: {}", accountNumber, request.getAmount());

        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException("Account is not active");
        }

        BigDecimal previousBalance = account.getBalance();
        account.credit(request.getAmount());
        Account savedAccount = accountRepository.save(account);

        // Record history
        recordHistory(savedAccount, "CREDIT", previousBalance, savedAccount.getBalance(),
                request.getAmount(), request.getDescription(), request.getReferenceId());

        // Publish event
        BalanceChangedEvent event = BalanceChangedEvent.builder()
                .accountNumber(accountNumber)
                .customerId(account.getCustomerId())
                .operation("CREDIT")
                .amount(request.getAmount())
                .previousBalance(previousBalance)
                .newBalance(savedAccount.getBalance())
                .referenceId(request.getReferenceId())
                .build();

        eventPublisher.publishBalanceChanged(event);

        log.info("Account credited successfully: {}", accountNumber);
        return mapToResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse debitAccount(String accountNumber, BalanceUpdateRequest request) {
        log.info("Debiting account: {} with amount: {}", accountNumber, request.getAmount());

        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException("Account is not active");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in account: " + accountNumber);
        }

        BigDecimal previousBalance = account.getBalance();
        account.debit(request.getAmount());
        Account savedAccount = accountRepository.save(account);

        // Record history
        recordHistory(savedAccount, "DEBIT", previousBalance, savedAccount.getBalance(),
                request.getAmount(), request.getDescription(), request.getReferenceId());

        // Publish event
        BalanceChangedEvent event = BalanceChangedEvent.builder()
                .accountNumber(accountNumber)
                .customerId(account.getCustomerId())
                .operation("DEBIT")
                .amount(request.getAmount())
                .previousBalance(previousBalance)
                .newBalance(savedAccount.getBalance())
                .referenceId(request.getReferenceId())
                .build();

        eventPublisher.publishBalanceChanged(event);

        log.info("Account debited successfully: {}", accountNumber);
        return mapToResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse freezeAccount(String accountNumber) {
        log.info("Freezing account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        AccountStatus previousStatus = account.getStatus();
        account.freeze();
        Account savedAccount = accountRepository.save(account);

        // Record history
        recordHistory(savedAccount, "FREEZE", savedAccount.getBalance(), savedAccount.getBalance(),
                null, "Account frozen", null);

        // Publish event
        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountNumber(accountNumber)
                .customerId(account.getCustomerId())
                .previousStatus(previousStatus)
                .newStatus(AccountStatus.FROZEN)
                .reason("Account frozen by request")
                .build();

        eventPublisher.publishAccountStatusChanged(event);

        log.info("Account frozen successfully: {}", accountNumber);
        return mapToResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse activateAccount(String accountNumber) {
        log.info("Activating account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        AccountStatus previousStatus = account.getStatus();
        account.activate();
        Account savedAccount = accountRepository.save(account);

        // Record history
        recordHistory(savedAccount, "ACTIVATE", savedAccount.getBalance(), savedAccount.getBalance(),
                null, "Account activated", null);

        // Publish event
        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountNumber(accountNumber)
                .customerId(account.getCustomerId())
                .previousStatus(previousStatus)
                .newStatus(AccountStatus.ACTIVE)
                .reason("Account activated by request")
                .build();

        eventPublisher.publishAccountStatusChanged(event);

        log.info("Account activated successfully: {}", accountNumber);
        return mapToResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse closeAccount(String accountNumber) {
        log.info("Closing account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidAccountStateException("Cannot close account with non-zero balance");
        }

        AccountStatus previousStatus = account.getStatus();
        account.close();
        Account savedAccount = accountRepository.save(account);

        // Record history
        recordHistory(savedAccount, "CLOSE", savedAccount.getBalance(), savedAccount.getBalance(),
                null, "Account closed", null);

        // Publish event
        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountNumber(accountNumber)
                .customerId(account.getCustomerId())
                .previousStatus(previousStatus)
                .newStatus(AccountStatus.CLOSED)
                .reason("Account closed by request")
                .build();

        eventPublisher.publishAccountStatusChanged(event);

        log.info("Account closed successfully: {}", accountNumber);
        return mapToResponse(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountHistory> getAccountHistory(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        return accountHistoryRepository.findByAccountIdOrderByTimestampDesc(account.getId());
    }

    private void recordHistory(Account account, String operation, BigDecimal previousBalance,
                               BigDecimal newBalance, BigDecimal amount, String description, String referenceId) {
        AccountHistory history = AccountHistory.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .operation(operation)
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .amount(amount)
                .description(description)
                .referenceId(referenceId)
                .build();

        accountHistoryRepository.save(history);
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .customerName(account.getCustomerName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .accountType(account.getAccountType())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}