package com.banking.transaction.event;

import com.banking.transaction.model.Transaction;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import com.banking.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "account.created", groupId = "transaction-service")
    public void handleAccountCreated(String message) {
        try {
            log.info("Received account.created event: {}", message);
            AccountCreatedEvent event = objectMapper.readValue(message, AccountCreatedEvent.class);

            // Record opening balance transaction
            if (event.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                Transaction transaction = Transaction.builder()
                    .accountNumber(event.getAccountNumber())
                    .transactionType(TransactionType.OPENING_BALANCE)
                    .amount(event.getBalance())
                    .currency(event.getCurrency())
                    .balanceBefore(BigDecimal.ZERO)
                    .balanceAfter(event.getBalance())
                    .description("Opening balance for new account")
                    .status(TransactionStatus.COMPLETED)
                    .build();

                transactionService.recordTransaction(transaction);
                log.info("Recorded opening balance transaction for account: {}", event.getAccountNumber());
            }
        } catch (Exception e) {
            log.error("Error processing account.created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "transfer.completed", groupId = "transaction-service")
    public void handleTransferCompleted(String message) {
        try {
            log.info("Received transfer.completed event: {}", message);
            TransferCompletedEvent event = objectMapper.readValue(message, TransferCompletedEvent.class);

            // Record debit transaction for sender
            Transaction debitTransaction = Transaction.builder()
                .accountNumber(event.getFromAccountNumber())
                .transactionType(TransactionType.TRANSFER_DEBIT)
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .reference(event.getTransferReference())
                .sourceAccount(event.getFromAccountNumber())
                .destinationAccount(event.getToAccountNumber())
                .description("Transfer to " + event.getToAccountNumber())
                .status(TransactionStatus.COMPLETED)
                .build();

            transactionService.recordTransaction(debitTransaction);

            // Record credit transaction for receiver
            Transaction creditTransaction = Transaction.builder()
                .accountNumber(event.getToAccountNumber())
                .transactionType(TransactionType.TRANSFER_CREDIT)
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .reference(event.getTransferReference())
                .sourceAccount(event.getFromAccountNumber())
                .destinationAccount(event.getToAccountNumber())
                .description("Transfer from " + event.getFromAccountNumber())
                .status(TransactionStatus.COMPLETED)
                .build();

            transactionService.recordTransaction(creditTransaction);

            log.info("Recorded transfer transactions for reference: {}", event.getTransferReference());
        } catch (Exception e) {
            log.error("Error processing transfer.completed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "account.balance.updated", groupId = "transaction-service")
    public void handleAccountBalanceUpdated(String message) {
        try {
            log.info("Received account.balance.updated event: {}", message);
            AccountBalanceUpdatedEvent event = objectMapper.readValue(message, AccountBalanceUpdatedEvent.class);

            TransactionType type = "CREDIT".equals(event.getTransactionType()) ?
                TransactionType.CREDIT : TransactionType.DEBIT;

            Transaction transaction = Transaction.builder()
                .accountNumber(event.getAccountNumber())
                .transactionType(type)
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .balanceBefore(event.getOldBalance())
                .balanceAfter(event.getNewBalance())
                .reference(event.getReference())
                .description("Balance update - " + event.getTransactionType())
                .status(TransactionStatus.COMPLETED)
                .build();

            transactionService.recordTransaction(transaction);
            log.info("Recorded balance update transaction for account: {}", event.getAccountNumber());
        } catch (Exception e) {
            log.error("Error processing account.balance.updated event: {}", e.getMessage(), e);
        }
    }
}
