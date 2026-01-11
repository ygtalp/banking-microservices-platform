package com.banking.statement.service;

import com.banking.statement.client.TransactionServiceClient;
import com.banking.statement.dto.StatementGenerationRequest;
import com.banking.statement.dto.StatementResponse;
import com.banking.statement.dto.TransactionDTO;
import com.banking.statement.model.Statement;
import com.banking.statement.model.StatementType;
import com.banking.statement.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository statementRepository;
    private final TransactionServiceClient transactionServiceClient;
    private final PdfGenerationService pdfGenerationService;

    @Transactional
    @CacheEvict(value = "statements", key = "#request.accountNumber")
    public StatementResponse generateStatement(StatementGenerationRequest request, String userId) {
        log.info("Generating statement for account: {}, type: {}",
                request.getAccountNumber(), request.getStatementType());

        // Calculate period based on statement type
        LocalDate[] period = calculatePeriod(request);
        LocalDate periodStart = period[0];
        LocalDate periodEnd = period[1];

        // Fetch transactions from Transaction Service
        List<TransactionDTO> transactions = fetchTransactions(
                request.getAccountNumber(),
                periodStart.atStartOfDay(),
                periodEnd.atTime(LocalTime.MAX)
        );

        // Calculate summary
        BigDecimal openingBalance = calculateOpeningBalance(transactions);
        BigDecimal closingBalance = calculateClosingBalance(transactions);
        BigDecimal totalCredits = calculateTotalCredits(transactions);
        BigDecimal totalDebits = calculateTotalDebits(transactions);

        // Create statement entity
        Statement statement = Statement.builder()
                .statementId(generateStatementId())
                .accountNumber(request.getAccountNumber())
                .customerId(getUserCustomerId(userId))
                .statementType(request.getStatementType())
                .statementDate(LocalDate.now())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .transactionCount(transactions.size())
                .requestedBy(userId)
                .build();

        // Save initial state
        Statement savedStatement = statementRepository.save(statement);

        try {
            // Generate PDF
            String pdfFilePath = pdfGenerationService.generateStatementPdf(savedStatement, transactions);
            long fileSize = Files.size(Paths.get(pdfFilePath));

            // Update statement with PDF info
            savedStatement.markAsGenerated(pdfFilePath, fileSize);
            savedStatement = statementRepository.save(savedStatement);

            log.info("Statement generated successfully: {}", savedStatement.getStatementId());

        } catch (Exception e) {
            log.error("Failed to generate PDF for statement: {}", savedStatement.getStatementId(), e);
            savedStatement.markAsFailed(e.getMessage());
            statementRepository.save(savedStatement);
            throw new RuntimeException("Failed to generate statement PDF", e);
        }

        return mapToResponse(savedStatement);
    }

    @Cacheable(value = "statements", key = "#statementId")
    public StatementResponse getStatement(String statementId) {
        Statement statement = statementRepository.findByStatementId(statementId)
                .orElseThrow(() -> new RuntimeException("Statement not found: " + statementId));
        return mapToResponse(statement);
    }

    public List<StatementResponse> getAccountStatements(String accountNumber) {
        return statementRepository.findByAccountNumberOrderByStatementDateDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<StatementResponse> getCustomerStatements(String customerId) {
        return statementRepository.findByCustomerIdOrderByStatementDateDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public byte[] downloadStatement(String statementId) {
        Statement statement = statementRepository.findByStatementId(statementId)
                .orElseThrow(() -> new RuntimeException("Statement not found: " + statementId));

        if (!statement.isAvailableForDownload()) {
            throw new RuntimeException("Statement PDF not available");
        }

        try {
            byte[] pdfBytes = Files.readAllBytes(Paths.get(statement.getPdfFilePath()));

            // Mark as downloaded
            statement.markAsDownloaded();
            statementRepository.save(statement);

            return pdfBytes;

        } catch (Exception e) {
            log.error("Failed to read PDF file: {}", statement.getPdfFilePath(), e);
            throw new RuntimeException("Failed to download statement", e);
        }
    }

    private LocalDate[] calculatePeriod(StatementGenerationRequest request) {
        LocalDate start, end;

        switch (request.getStatementType()) {
            case MONTHLY:
                int month = request.getMonth() != null ? request.getMonth() : LocalDate.now().getMonthValue();
                int year = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
                YearMonth yearMonth = YearMonth.of(year, month);
                start = yearMonth.atDay(1);
                end = yearMonth.atEndOfMonth();
                break;

            case QUARTERLY:
                int quarter = request.getQuarter() != null ? request.getQuarter() : getCurrentQuarter();
                int qYear = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
                start = LocalDate.of(qYear, (quarter - 1) * 3 + 1, 1);
                end = start.plusMonths(3).minusDays(1);
                break;

            case ANNUAL:
                int annualYear = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
                start = LocalDate.of(annualYear, 1, 1);
                end = LocalDate.of(annualYear, 12, 31);
                break;

            case CUSTOM:
                if (request.getPeriodStart() == null || request.getPeriodEnd() == null) {
                    throw new RuntimeException("Custom statement requires periodStart and periodEnd");
                }
                start = request.getPeriodStart();
                end = request.getPeriodEnd();
                break;

            default:
                start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
                end = LocalDate.now().minusMonths(1).withDayOfMonth(
                        LocalDate.now().minusMonths(1).lengthOfMonth());
                break;
        }

        return new LocalDate[]{start, end};
    }

    private int getCurrentQuarter() {
        int month = LocalDate.now().getMonthValue();
        return (month - 1) / 3 + 1;
    }

    private List<TransactionDTO> fetchTransactions(String accountNumber,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate) {
        try {
            return transactionServiceClient.getTransactionsByDateRange(accountNumber, startDate, endDate);
        } catch (Exception e) {
            log.error("Failed to fetch transactions from Transaction Service", e);
            throw new RuntimeException("Failed to fetch transaction data", e);
        }
    }

    private BigDecimal calculateOpeningBalance(List<TransactionDTO> transactions) {
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return transactions.get(0).getBalanceBefore();
    }

    private BigDecimal calculateClosingBalance(List<TransactionDTO> transactions) {
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return transactions.get(transactions.size() - 1).getBalanceAfter();
    }

    private BigDecimal calculateTotalCredits(List<TransactionDTO> transactions) {
        return transactions.stream()
                .filter(t -> t.getTransactionType().contains("CREDIT"))
                .map(TransactionDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalDebits(List<TransactionDTO> transactions) {
        return transactions.stream()
                .filter(t -> t.getTransactionType().contains("DEBIT"))
                .map(TransactionDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateStatementId() {
        return "STM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String getUserCustomerId(String userId) {
        // In production: fetch customer ID from User Service
        return "CUS-" + userId;
    }

    private StatementResponse mapToResponse(Statement statement) {
        return StatementResponse.builder()
                .statementId(statement.getStatementId())
                .accountNumber(statement.getAccountNumber())
                .customerId(statement.getCustomerId())
                .statementType(statement.getStatementType())
                .statementDate(statement.getStatementDate())
                .periodStart(statement.getPeriodStart())
                .periodEnd(statement.getPeriodEnd())
                .openingBalance(statement.getOpeningBalance())
                .closingBalance(statement.getClosingBalance())
                .totalCredits(statement.getTotalCredits())
                .totalDebits(statement.getTotalDebits())
                .transactionCount(statement.getTransactionCount())
                .status(statement.getStatus())
                .pdfFileSize(statement.getPdfFileSize())
                .generatedAt(statement.getGeneratedAt())
                .sentAt(statement.getSentAt())
                .downloadedAt(statement.getDownloadedAt())
                .notificationSent(statement.getNotificationSent())
                .downloadUrl(statement.isAvailableForDownload() ?
                        "/statements/download/" + statement.getStatementId() : null)
                .createdAt(statement.getCreatedAt())
                .build();
    }
}
