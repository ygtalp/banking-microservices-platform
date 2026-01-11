# Transaction Service - Complete Reference

> **Service:** Transaction Service
> **Port:** 8086
> **Database:** transaction_db (PostgreSQL)
> **Responsibility:** Transaction history, audit trail, and event-driven transaction recording
> **Last Updated:** 1 January 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model](#domain-model)
3. [API Reference](#api-reference)
4. [Event-Driven Architecture](#event-driven-architecture)
5. [Caching Strategy](#caching-strategy)
6. [Query Capabilities](#query-capabilities)
7. [Audit Trail](#audit-trail)
8. [Testing](#testing)

---

## Overview

Transaction Service provides comprehensive transaction history tracking and audit trail capabilities for all financial operations in the banking platform. It operates in an event-driven manner, automatically recording transactions from Kafka events.

### Key Features

- ✅ Event-driven transaction recording (zero manual recording required)
- ✅ Immutable audit trail (transactions never deleted)
- ✅ Multi-type transaction support (6 transaction types)
- ✅ Balance snapshot tracking (before/after for compliance)
- ✅ Advanced query capabilities (by account, date range, type, reference)
- ✅ Transaction summary and analytics
- ✅ Redis caching for performance
- ✅ JWT authentication & authorization
- ✅ Complete metadata support for extensibility

### Technology Stack

```yaml
Framework: Spring Boot 3.2.0
Database: PostgreSQL 16
Cache: Redis 7.2
Messaging: Apache Kafka 3.6 (Consumer)
Security: JWT (HS512)
Validation: Spring Validation + Hibernate Validator
```

---

## Domain Model

### Transaction Entity

```java
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_account_number", columnList = "account_number"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_reference", columnList = "reference")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction implements Serializable {

    @Id
    private String transactionId;  // TXN-XXXXXXXXXXXX

    @Column(nullable = false, length = 50)
    private String accountNumber;  // Account reference

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;  // ALWAYS BigDecimal!

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;  // TRY, USD, EUR, GBP

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;  // Account balance before transaction

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;  // Account balance after transaction

    @Column(length = 100)
    private String reference;  // External reference (transfer ID, account number, etc.)

    @Column(length = 255)
    private String description;  // Human-readable description

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;  // COMPLETED, PENDING, FAILED, REVERSED

    @Column(length = 50)
    private String sourceAccount;  // For transfers

    @Column(length = 50)
    private String destinationAccount;  // For transfers

    @Column(columnDefinition = "TEXT")
    private String metadata;  // JSON metadata for additional info

    @Column(nullable = false)
    private LocalDateTime transactionDate;  // When transaction occurred

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (transactionId == null) {
            transactionId = "TXN-" + UUID.randomUUID().toString();
        }
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
    }
}
```

### Transaction Enums

```java
public enum TransactionType {
    CREDIT,              // Money added to account
    DEBIT,              // Money removed from account
    TRANSFER_DEBIT,     // Money debited for transfer (outgoing)
    TRANSFER_CREDIT,    // Money credited from transfer (incoming)
    OPENING_BALANCE,    // Initial account balance
    ADJUSTMENT          // Manual adjustment/correction
}

public enum TransactionStatus {
    COMPLETED,   // Transaction completed successfully
    PENDING,     // Transaction pending (for future-dated transactions)
    FAILED,      // Transaction failed
    REVERSED     // Transaction reversed (compensation)
}
```

---

## API Reference

### Base URL

```
http://localhost:8080/transactions  (via API Gateway)
http://localhost:8086/transactions  (direct access)
```

### 1. Get Transaction by ID

**Endpoint:** `GET /transactions/{transactionId}`

**Request:**
```
GET /transactions/TXN-123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "transactionId": "TXN-123e4567-e89b-12d3-a456-426614174000",
  "accountNumber": "ACC-789...",
  "transactionType": "TRANSFER_CREDIT",
  "amount": 500.00,
  "currency": "TRY",
  "balanceBefore": 1000.00,
  "balanceAfter": 1500.00,
  "reference": "TRF-456...",
  "description": "Transfer from John Doe",
  "status": "COMPLETED",
  "sourceAccount": "ACC-123...",
  "destinationAccount": "ACC-789...",
  "transactionDate": "2026-01-01T14:30:00Z",
  "createdAt": "2026-01-01T14:30:01Z"
}
```

**Error Responses:**
```json
// 404 Not Found
{
  "error": "Transaction not found: TXN-xxx"
}

// 401 Unauthorized
{
  "error": "Invalid or missing JWT token"
}
```

---

### 2. Get Transaction History

**Endpoint:** `POST /transactions/history`

**Request:**
```json
{
  "accountNumber": "ACC-789...",
  "startDate": "2025-12-01T00:00:00Z",
  "endDate": "2026-01-01T23:59:59Z",
  "transactionType": "TRANSFER_CREDIT",
  "page": 0,
  "size": 20
}
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "transactionId": "TXN-123...",
      "accountNumber": "ACC-789...",
      "transactionType": "TRANSFER_CREDIT",
      "amount": 500.00,
      "currency": "TRY",
      "balanceBefore": 1000.00,
      "balanceAfter": 1500.00,
      "description": "Transfer from John Doe",
      "transactionDate": "2026-01-01T14:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

### 3. Get Account Transactions

**Endpoint:** `GET /transactions/account/{accountNumber}`

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**Request:**
```
GET /transactions/account/ACC-789...?page=0&size=10
Authorization: Bearer <JWT_TOKEN>
```

**Response:** Same as Transaction History (paginated)

---

### 4. Get Transactions by Date Range

**Endpoint:** `GET /transactions/account/{accountNumber}/date-range`

**Query Parameters:**
- `startDate`: Start date (ISO 8601)
- `endDate`: End date (ISO 8601)
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**Request:**
```
GET /transactions/account/ACC-789.../date-range?startDate=2025-12-01T00:00:00Z&endDate=2026-01-01T23:59:59Z
Authorization: Bearer <JWT_TOKEN>
```

**Response:** Same as Transaction History (paginated)

---

### 5. Get Transaction Summary

**Endpoint:** `GET /transactions/account/{accountNumber}/summary`

**Request:**
```
GET /transactions/account/ACC-789.../summary
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "accountNumber": "ACC-789...",
  "totalTransactions": 150,
  "totalCredits": 75,
  "totalDebits": 75,
  "totalCreditAmount": 50000.00,
  "totalDebitAmount": 30000.00,
  "netAmount": 20000.00,
  "currency": "TRY",
  "periodStart": "2025-01-01T00:00:00Z",
  "periodEnd": "2026-01-01T23:59:59Z",
  "averageTransactionAmount": 533.33,
  "largestTransaction": 5000.00,
  "smallestTransaction": 10.00
}
```

---

### 6. Get Transactions by Reference

**Endpoint:** `GET /transactions/reference/{reference}`

**Request:**
```
GET /transactions/reference/TRF-456...
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "transactions": [
    {
      "transactionId": "TXN-123...",
      "accountNumber": "ACC-789...",
      "transactionType": "TRANSFER_DEBIT",
      "amount": -500.00,
      "reference": "TRF-456...",
      "description": "Transfer to Jane Smith",
      "transactionDate": "2026-01-01T14:30:00Z"
    },
    {
      "transactionId": "TXN-124...",
      "accountNumber": "ACC-456...",
      "transactionType": "TRANSFER_CREDIT",
      "amount": 500.00,
      "reference": "TRF-456...",
      "description": "Transfer from John Doe",
      "transactionDate": "2026-01-01T14:30:00Z"
    }
  ],
  "totalTransactions": 2
}
```

---

## Event-Driven Architecture

### Kafka Consumers

Transaction Service consumes 3 event types:

**1. account.created Event**
```java
@KafkaListener(topics = "account.created")
public void handleAccountCreated(AccountCreatedEvent event) {
    // Create OPENING_BALANCE transaction
    Transaction transaction = Transaction.builder()
        .accountNumber(event.getAccountNumber())
        .transactionType(TransactionType.OPENING_BALANCE)
        .amount(event.getInitialBalance())
        .currency(event.getCurrency())
        .balanceBefore(BigDecimal.ZERO)
        .balanceAfter(event.getInitialBalance())
        .reference(event.getAccountNumber())
        .description("Account opening balance")
        .status(TransactionStatus.COMPLETED)
        .build();

    transactionRepository.save(transaction);
}
```

**2. transfer.completed Event**
```java
@KafkaListener(topics = "transfer.completed")
public void handleTransferCompleted(TransferCompletedEvent event) {
    // Create TWO transactions:

    // 1. TRANSFER_DEBIT for source account
    Transaction debit = Transaction.builder()
        .accountNumber(event.getFromAccountNumber())
        .transactionType(TransactionType.TRANSFER_DEBIT)
        .amount(event.getAmount().negate())  // Negative amount
        .currency(event.getCurrency())
        .balanceBefore(event.getSourceBalanceBefore())
        .balanceAfter(event.getSourceBalanceAfter())
        .reference(event.getTransferReference())
        .description("Transfer to " + event.getToAccountNumber())
        .status(TransactionStatus.COMPLETED)
        .sourceAccount(event.getFromAccountNumber())
        .destinationAccount(event.getToAccountNumber())
        .build();

    // 2. TRANSFER_CREDIT for destination account
    Transaction credit = Transaction.builder()
        .accountNumber(event.getToAccountNumber())
        .transactionType(TransactionType.TRANSFER_CREDIT)
        .amount(event.getAmount())
        .currency(event.getCurrency())
        .balanceBefore(event.getDestinationBalanceBefore())
        .balanceAfter(event.getDestinationBalanceAfter())
        .reference(event.getTransferReference())
        .description("Transfer from " + event.getFromAccountNumber())
        .status(TransactionStatus.COMPLETED)
        .sourceAccount(event.getFromAccountNumber())
        .destinationAccount(event.getToAccountNumber())
        .build();

    transactionRepository.saveAll(Arrays.asList(debit, credit));
}
```

**3. account.balance.updated Event**
```java
@KafkaListener(topics = "account.balance.updated")
public void handleBalanceUpdated(BalanceUpdatedEvent event) {
    // Create ADJUSTMENT transaction
    Transaction transaction = Transaction.builder()
        .accountNumber(event.getAccountNumber())
        .transactionType(TransactionType.ADJUSTMENT)
        .amount(event.getAmount())
        .currency(event.getCurrency())
        .balanceBefore(event.getBalanceBefore())
        .balanceAfter(event.getBalanceAfter())
        .reference(event.getReference())
        .description(event.getDescription())
        .status(TransactionStatus.COMPLETED)
        .build();

    transactionRepository.save(transaction);
}
```

### Zero Manual Recording

**Key Principle:** Transactions are NEVER created manually via REST API. All transactions are automatically recorded from Kafka events.

**Benefits:**
- Single source of truth (events)
- Guaranteed consistency
- Complete audit trail
- No manual errors
- Scalable event processing

---

## Caching Strategy

### Cache Configuration

```yaml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
    ttl: 300  # 5 minutes
```

### Cache Keys

```
transaction:{transactionId}        → Full transaction object
transaction:account:{accountNumber}:page:{page}  → Paginated transactions
```

### Caching Implementation

```java
@Cacheable(value = "transactions", key = "#transactionId")
public Optional<Transaction> getTransactionById(String transactionId) {
    return transactionRepository.findById(transactionId);
}

@CacheEvict(value = "transactions", allEntries = true)
public void clearCache() {
    // Invoked when new transactions are recorded
}
```

---

## Query Capabilities

### Advanced Repository Methods

```java
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // Find by account with pagination
    Page<Transaction> findByAccountNumberOrderByTransactionDateDesc(
        String accountNumber,
        Pageable pageable
    );

    // Find by date range
    Page<Transaction> findByAccountNumberAndTransactionDateBetweenOrderByTransactionDateDesc(
        String accountNumber,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    // Find by account, date range, and type
    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "AND t.transactionType = :type " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByAccountAndDateRangeAndType(
        @Param("accountNumber") String accountNumber,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("type") TransactionType type,
        Pageable pageable
    );

    // Count transactions
    Long countByAccountNumber(String accountNumber);

    // Sum amount by account and type
    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.accountNumber = :accountNumber AND t.transactionType = :type")
    BigDecimal sumAmountByAccountAndType(
        @Param("accountNumber") String accountNumber,
        @Param("type") TransactionType type
    );

    // Find by reference (for transfer tracking)
    List<Transaction> findByReference(String reference);
}
```

### Transaction Summary Calculation

```java
@Service
public class TransactionService {

    public TransactionSummary getAccountSummary(String accountNumber) {
        List<Transaction> transactions = transactionRepository
            .findByAccountNumberOrderByTransactionDateDesc(accountNumber);

        BigDecimal totalCredits = transactions.stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = transactions.stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TransactionSummary.builder()
            .accountNumber(accountNumber)
            .totalTransactions(transactions.size())
            .totalCreditAmount(totalCredits)
            .totalDebitAmount(totalDebits.abs())
            .netAmount(totalCredits.add(totalDebits))
            .build();
    }
}
```

---

## Audit Trail

### Immutability Principle

**Transactions are NEVER deleted or modified.** This ensures:
- Complete audit trail for compliance
- Historical accuracy
- Forensic analysis capability
- Regulatory reporting support

### Balance Snapshot Tracking

Every transaction records:
- `balanceBefore`: Account balance before transaction
- `balanceAfter`: Account balance after transaction

**Benefits:**
- Point-in-time balance reconstruction
- Audit compliance (e.g., SOX, Basel III)
- Discrepancy detection
- Historical analysis

### Transaction Reference Linking

The `reference` field links transactions to their source:
- Transfer ID for transfer transactions
- Account number for account events
- External system reference for integrations

**Enables:**
- End-to-end transaction tracking
- Cross-account transfer verification
- External system reconciliation

### Metadata Extensibility

The `metadata` field (JSON) allows storing additional information without schema changes:

```json
{
  "metadata": {
    "channel": "MOBILE_APP",
    "deviceId": "iPhone-12-Pro",
    "ipAddress": "192.168.1.100",
    "userAgent": "BankingApp/2.0",
    "geolocation": {
      "latitude": 41.0082,
      "longitude": 28.9784
    }
  }
}
```

---

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("Should record opening balance transaction from event")
    void shouldRecordOpeningBalanceFromEvent() {
        // Given
        AccountCreatedEvent event = AccountCreatedEvent.builder()
            .accountNumber("ACC-123")
            .initialBalance(new BigDecimal("1000.00"))
            .currency(Currency.TRY)
            .build();

        // When
        transactionService.handleAccountCreated(event);

        // Then
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction transaction = captor.getValue();
        assertEquals("ACC-123", transaction.getAccountNumber());
        assertEquals(TransactionType.OPENING_BALANCE, transaction.getTransactionType());
        assertEquals(new BigDecimal("1000.00"), transaction.getAmount());
        assertEquals(BigDecimal.ZERO, transaction.getBalanceBefore());
        assertEquals(new BigDecimal("1000.00"), transaction.getBalanceAfter());
    }

    @Test
    @DisplayName("Should record two transactions for completed transfer")
    void shouldRecordTwoTransactionsForTransfer() {
        // Given
        TransferCompletedEvent event = TransferCompletedEvent.builder()
            .transferReference("TRF-456")
            .fromAccountNumber("ACC-123")
            .toAccountNumber("ACC-789")
            .amount(new BigDecimal("500.00"))
            .currency(Currency.TRY)
            .sourceBalanceBefore(new BigDecimal("1000.00"))
            .sourceBalanceAfter(new BigDecimal("500.00"))
            .destinationBalanceBefore(new BigDecimal("2000.00"))
            .destinationBalanceAfter(new BigDecimal("2500.00"))
            .build();

        // When
        transactionService.handleTransferCompleted(event);

        // Then
        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());

        List<Transaction> transactions = captor.getValue();
        assertEquals(2, transactions.size());

        // Verify debit transaction
        Transaction debit = transactions.get(0);
        assertEquals("ACC-123", debit.getAccountNumber());
        assertEquals(TransactionType.TRANSFER_DEBIT, debit.getTransactionType());
        assertEquals(new BigDecimal("-500.00"), debit.getAmount());

        // Verify credit transaction
        Transaction credit = transactions.get(1);
        assertEquals("ACC-789", credit.getAccountNumber());
        assertEquals(TransactionType.TRANSFER_CREDIT, credit.getTransactionType());
        assertEquals(new BigDecimal("500.00"), credit.getAmount());
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TransactionControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void shouldRetrieveTransactionHistory() {
        // Create test transactions
        createTestTransactions();

        // Get transaction history
        ResponseEntity<TransactionHistoryResponse> response = restTemplate
            .exchange(
                "/transactions/account/ACC-123",
                HttpMethod.GET,
                createAuthenticatedRequest(),
                TransactionHistoryResponse.class
            );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getTotalElements() > 0);
    }

    private HttpEntity<Void> createAuthenticatedRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + generateTestJwt());
        return new HttpEntity<>(headers);
    }
}
```

---

**Last Updated:** 1 January 2026
**API Version:** 1.0
**Service Status:** ✅ Production Ready (Deployed)
