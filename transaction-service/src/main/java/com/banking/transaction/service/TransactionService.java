package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionHistoryRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransactionSummaryResponse;
import com.banking.transaction.model.Transaction;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    Transaction recordTransaction(Transaction transaction);

    TransactionResponse getTransactionById(String transactionId);

    Page<TransactionResponse> getTransactionHistory(TransactionHistoryRequest request);

    Page<TransactionResponse> getAccountTransactions(String accountNumber, int page, int size);

    List<TransactionResponse> getTransactionsByDateRange(
        String accountNumber,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    TransactionSummaryResponse getTransactionSummary(String accountNumber);

    List<TransactionResponse> getTransactionsByReference(String reference);
}
