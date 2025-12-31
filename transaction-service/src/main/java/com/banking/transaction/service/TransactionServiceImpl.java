package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionHistoryRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransactionSummaryResponse;
import com.banking.transaction.exception.TransactionNotFoundException;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction recordTransaction(Transaction transaction) {
        log.info("Recording transaction for account: {}", transaction.getAccountNumber());
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction recorded successfully: {}", savedTransaction.getTransactionId());
        return savedTransaction;
    }

    @Override
    @Cacheable(value = "transactions", key = "#transactionId")
    public TransactionResponse getTransactionById(String transactionId) {
        log.info("Fetching transaction: {}", transactionId);
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));
        return mapToResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getTransactionHistory(TransactionHistoryRequest request) {
        log.info("Fetching transaction history for account: {}", request.getAccountNumber());

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());

        LocalDateTime startDate = request.getStartDate() != null ?
            request.getStartDate() : LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = request.getEndDate() != null ?
            request.getEndDate() : LocalDateTime.now();

        Page<Transaction> transactions = transactionRepository.findByAccountAndDateRangeAndType(
            request.getAccountNumber(),
            startDate,
            endDate,
            request.getTransactionType(),
            pageable
        );

        return transactions.map(this::mapToResponse);
    }

    @Override
    public Page<TransactionResponse> getAccountTransactions(String accountNumber, int page, int size) {
        log.info("Fetching transactions for account: {}, page: {}, size: {}", accountNumber, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionRepository
            .findByAccountNumberOrderByTransactionDateDesc(accountNumber, pageable);
        return transactions.map(this::mapToResponse);
    }

    @Override
    public List<TransactionResponse> getTransactionsByDateRange(
            String accountNumber,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        log.info("Fetching transactions for account: {} between {} and {}",
            accountNumber, startDate, endDate);
        List<Transaction> transactions = transactionRepository
            .findByAccountNumberAndTransactionDateBetweenOrderByTransactionDateDesc(
                accountNumber, startDate, endDate);
        return transactions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "transactionSummary", key = "#accountNumber")
    public TransactionSummaryResponse getTransactionSummary(String accountNumber) {
        log.info("Calculating transaction summary for account: {}", accountNumber);

        long totalTransactions = transactionRepository.countByAccountNumber(accountNumber);

        BigDecimal totalCredits = transactionRepository.sumAmountByAccountAndType(
            accountNumber, TransactionType.CREDIT);
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;

        BigDecimal totalDebits = transactionRepository.sumAmountByAccountAndType(
            accountNumber, TransactionType.DEBIT);
        if (totalDebits == null) totalDebits = BigDecimal.ZERO;

        // Add transfer credits
        BigDecimal transferCredits = transactionRepository.sumAmountByAccountAndType(
            accountNumber, TransactionType.TRANSFER_CREDIT);
        if (transferCredits != null) {
            totalCredits = totalCredits.add(transferCredits);
        }

        // Add transfer debits
        BigDecimal transferDebits = transactionRepository.sumAmountByAccountAndType(
            accountNumber, TransactionType.TRANSFER_DEBIT);
        if (transferDebits != null) {
            totalDebits = totalDebits.add(transferDebits);
        }

        BigDecimal netAmount = totalCredits.subtract(totalDebits);

        return TransactionSummaryResponse.builder()
            .accountNumber(accountNumber)
            .totalTransactions(totalTransactions)
            .totalCredits(totalCredits)
            .totalDebits(totalDebits)
            .netAmount(netAmount)
            .currency("TRY") // TODO: Get from account
            .build();
    }

    @Override
    public List<TransactionResponse> getTransactionsByReference(String reference) {
        log.info("Fetching transactions by reference: {}", reference);
        List<Transaction> transactions = transactionRepository.findByReference(reference);
        return transactions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .transactionId(transaction.getTransactionId())
            .accountNumber(transaction.getAccountNumber())
            .transactionType(transaction.getTransactionType())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .balanceBefore(transaction.getBalanceBefore())
            .balanceAfter(transaction.getBalanceAfter())
            .reference(transaction.getReference())
            .description(transaction.getDescription())
            .status(transaction.getStatus())
            .sourceAccount(transaction.getSourceAccount())
            .destinationAccount(transaction.getDestinationAccount())
            .transactionDate(transaction.getTransactionDate())
            .build();
    }
}
