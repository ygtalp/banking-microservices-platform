# Account Service - Complete Reference

> **Service:** Account Service  
> **Port:** 8081  
> **Database:** banking_accounts (PostgreSQL)  
> **Responsibility:** Account lifecycle management  
> **Last Updated:** 23 December 2025

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model](#domain-model)
3. [API Reference](#api-reference)
4. [Business Rules](#business-rules)
5. [IBAN Generation](#iban-generation)
6. [Caching Strategy](#caching-strategy)
7. [Event Publishing](#event-publishing)
8. [Error Handling](#error-handling)
9. [Testing](#testing)

---

## Overview

Account Service manages the complete lifecycle of bank accounts including creation, updates, balance operations, and status management.

### Key Features

- ✅ Account CRUD operations
- ✅ Turkish IBAN generation (TR format)
- ✅ Multi-currency support (TRY, USD, EUR, GBP)
- ✅ Account types (CHECKING, SAVINGS, BUSINESS)
- ✅ Balance operations (credit/debit)
- ✅ Redis caching for performance
- ✅ Kafka event publishing
- ✅ Complete audit trail

### Technology Stack

```yaml
Framework: Spring Boot 3.2.0
Database: PostgreSQL 16
Cache: Redis 7.2
Messaging: Apache Kafka 3.6
Validation: Spring Validation + Hibernate Validator
```

---

## Domain Model

### Account Entity

```java
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String accountNumber;  // System-generated: ACC-{UUID}
    
    @Column(unique = true, nullable = false, length = 26)
    private String iban;  // TR-format: TR{2 digits}{5 bank}{1 reserved}{16 account}
    
    @Column(nullable = false, length = 100)
    private String customerName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;  // CHECKING, SAVINGS, BUSINESS
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;  // ALWAYS BigDecimal for money!
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;  // TRY, USD, EUR, GBP
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;  // ACTIVE, SUSPENDED, CLOSED
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### Account Enums

```java
public enum AccountType {
    CHECKING,   // Regular checking account
    SAVINGS,    // Savings account with interest
    BUSINESS    // Business account
}

public enum AccountStatus {
    ACTIVE,     // Account is active, all operations allowed
    SUSPENDED,  // Account suspended, only query operations
    CLOSED      // Account closed, no operations allowed
}

public enum Currency {
    TRY("Turkish Lira", "₺"),
    USD("US Dollar", "$"),
    EUR("Euro", "€"),
    GBP("British Pound", "£");
    
    private final String displayName;
    private final String symbol;
}
```

### Account History Entity

```java
@Entity
@Table(name = "account_history")
@Data
@Builder
public class AccountHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String accountNumber;
    
    @Column(nullable = false, length = 50)
    private String operation;  // CREATE, UPDATE_BALANCE, STATUS_CHANGE, UPDATE
    
    @Column(precision = 19, scale = 2)
    private BigDecimal previousBalance;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal newBalance;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

## API Reference

### Base URL

```
http://localhost:8080/accounts  (via API Gateway)
http://localhost:8081/accounts  (direct access)
```

### 1. Create Account

**Endpoint:** `POST /accounts`

**Request:**
```json
{
  "customerName": "John Doe",
  "accountType": "CHECKING",
  "currency": "TRY"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Account created successfully",
  "data": {
    "accountNumber": "ACC-123e4567-e89b-12d3-a456-426614174000",
    "iban": "TR330006200000000006295467",
    "customerName": "John Doe",
    "accountType": "CHECKING",
    "balance": 0.00,
    "currency": "TRY",
    "status": "ACTIVE",
    "createdAt": "2025-12-23T10:30:00Z"
  }
}
```

**Business Logic:**
1. Generate unique account number (ACC-{UUID})
2. Generate valid Turkish IBAN (26 characters)
3. Initialize balance to 0.00
4. Set status to ACTIVE
5. Save to database
6. Cache in Redis (5 min TTL)
7. Publish AccountCreatedEvent to Kafka

**Validation Rules:**
- Customer name: Required, 3-100 characters
- Account type: Required, must be valid enum
- Currency: Required, must be valid enum

**Error Responses:**
```json
// 400 Bad Request - Validation error
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "customerName": "Customer name is required"
  }
}

// 409 Conflict - Account already exists
{
  "success": false,
  "message": "Account with this IBAN already exists"
}
```

---

### 2. Get Account by Account Number

**Endpoint:** `GET /accounts/{accountNumber}`

**Request:**
```
GET /accounts/ACC-123e4567-e89b-12d3-a456-426614174000
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accountNumber": "ACC-123e4567-e89b-12d3-a456-426614174000",
    "iban": "TR330006200000000006295467",
    "customerName": "John Doe",
    "accountType": "CHECKING",
    "balance": 1500.50,
    "currency": "TRY",
    "status": "ACTIVE",
    "createdAt": "2025-12-23T10:30:00Z",
    "updatedAt": "2025-12-23T14:45:00Z"
  }
}
```

**Caching:**
- Cache key: `account:{accountNumber}`
- TTL: 5 minutes (300 seconds)
- Invalidated on: Updates, balance changes, status changes

**Error Responses:**
```json
// 404 Not Found
{
  "success": false,
  "message": "Account not found: ACC-xxx"
}
```

---

### 3. Get Account by IBAN

**Endpoint:** `GET /accounts/iban/{iban}`

**Request:**
```
GET /accounts/iban/TR330006200000000006295467
```

**Response:** Same as Get Account by Account Number

**Caching:**
- Primary cache key: `account:iban:{iban}`
- Secondary cache: `account:{accountNumber}` (after lookup)

---

### 4. Update Balance

**Endpoint:** `PUT /accounts/{accountNumber}/balance`

**Request:**
```json
{
  "amount": 100.50,
  "description": "Deposit from Transfer Service"
}
```

**Parameters:**
- `amount`: Can be positive (credit) or negative (debit)
- `description`: Optional operation description

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Balance updated successfully",
  "data": {
    "accountNumber": "ACC-123...",
    "previousBalance": 1500.50,
    "newBalance": 1601.00,
    "amount": 100.50,
    "operation": "CREDIT",
    "transactionId": "TXN-abc123",
    "timestamp": "2025-12-23T15:00:00Z"
  }
}
```

**Business Logic:**
1. Validate account exists and is ACTIVE
2. For debit: Check sufficient balance
3. Update balance atomically (@Transactional)
4. Create account history record
5. Invalidate cache
6. Publish BalanceChangedEvent

**Validation Rules:**
- Amount: Required, non-zero
- Debit amount: Must not exceed available balance
- Account status: Must be ACTIVE

**Error Responses:**
```json
// 400 Bad Request - Insufficient balance
{
  "success": false,
  "message": "Insufficient balance",
  "data": {
    "accountNumber": "ACC-123...",
    "currentBalance": 100.00,
    "requestedAmount": -150.00,
    "shortfall": 50.00
  }
}

// 400 Bad Request - Account suspended
{
  "success": false,
  "message": "Cannot update balance: Account is SUSPENDED"
}
```

---

### 5. Update Account

**Endpoint:** `PUT /accounts/{accountNumber}`

**Request:**
```json
{
  "customerName": "John Smith",
  "accountType": "SAVINGS"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Account updated successfully",
  "data": {
    "accountNumber": "ACC-123...",
    "customerName": "John Smith",
    "accountType": "SAVINGS",
    "balance": 1500.50,
    "currency": "TRY",
    "status": "ACTIVE"
  }
}
```

**Business Logic:**
1. Validate account exists
2. Update allowed fields only
3. Create account history record
4. Invalidate cache
5. Publish AccountUpdatedEvent (if implemented)

**Restrictions:**
- Cannot change: account number, IBAN, currency
- Can change: customer name, account type

---

### 6. Close Account

**Endpoint:** `DELETE /accounts/{accountNumber}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Account closed successfully",
  "data": {
    "accountNumber": "ACC-123...",
    "status": "CLOSED",
    "finalBalance": 0.00,
    "closedAt": "2025-12-23T16:00:00Z"
  }
}
```

**Business Logic:**
1. Validate account exists
2. Check balance is zero
3. Set status to CLOSED
4. Create account history record
5. Remove from cache
6. Publish AccountStatusChangedEvent

**Validation Rules:**
- Balance must be exactly 0.00
- Cannot close already closed account

**Error Responses:**
```json
// 400 Bad Request - Non-zero balance
{
  "success": false,
  "message": "Cannot close account with non-zero balance",
  "data": {
    "accountNumber": "ACC-123...",
    "currentBalance": 100.50
  }
}
```

---

### 7. Get Account History

**Endpoint:** `GET /accounts/{accountNumber}/history`

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `operation`: Filter by operation type (optional)

**Request:**
```
GET /accounts/ACC-123.../history?page=0&size=10&operation=UPDATE_BALANCE
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "accountNumber": "ACC-123...",
        "operation": "CREATE",
        "previousBalance": null,
        "newBalance": 0.00,
        "description": "Account created",
        "createdAt": "2025-12-23T10:30:00Z"
      },
      {
        "id": 2,
        "accountNumber": "ACC-123...",
        "operation": "UPDATE_BALANCE",
        "previousBalance": 0.00,
        "newBalance": 1000.00,
        "description": "Initial deposit",
        "createdAt": "2025-12-23T10:35:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 15,
    "totalPages": 2
  }
}
```

---

## Business Rules

### Account Creation

1. **Account Number Generation:**
   - Format: `ACC-{UUID}`
   - Example: `ACC-123e4567-e89b-12d3-a456-426614174000`
   - Must be globally unique

2. **IBAN Generation:**
   - Format: TR + 2 check digits + 5 bank code + 1 reserved + 16 account number
   - Example: `TR330006200000000006295467`
   - Must pass MOD-97 validation
   - See [IBAN Generation](#iban-generation) section

3. **Initial State:**
   - Balance: 0.00
   - Status: ACTIVE
   - Created timestamp: Current time

### Balance Operations

1. **Credit (Positive Amount):**
   - No restrictions
   - Always allowed for ACTIVE accounts

2. **Debit (Negative Amount):**
   - Requires sufficient balance
   - Formula: `currentBalance + debitAmount >= 0`
   - Atomic operation (transaction isolation)

3. **Account Status Validation:**
   - ACTIVE: All operations allowed
   - SUSPENDED: Only queries allowed, no balance updates
   - CLOSED: Only queries allowed, no modifications

### Account Closure

1. **Prerequisites:**
   - Balance must be exactly 0.00
   - No pending transactions
   - Status must be ACTIVE or SUSPENDED

2. **Process:**
   - Set status to CLOSED
   - Record closure in history
   - Retain data for audit (soft delete)

---

## IBAN Generation

### Turkish IBAN Format

```
TR{check digits}{bank code}{reserved}{account number}
  └─ 2 digits   └─ 5 digits └─ 1 digit └─ 16 digits

Total: 26 characters
Example: TR330006200000000006295467
```

### Implementation

```java
public class IbanGenerator {
    
    private static final String COUNTRY_CODE = "TR";
    private static final String BANK_CODE = "00062";  // Custom bank code
    private static final String RESERVED = "0";
    
    public static String generateIban(String accountNumber) {
        // Extract numeric part from account number
        String numericPart = extractNumericPart(accountNumber);
        
        // Pad to 16 digits
        String paddedAccountNumber = padLeft(numericPart, 16, '0');
        
        // Calculate check digits
        String checkDigits = calculateCheckDigits(
            BANK_CODE + RESERVED + paddedAccountNumber
        );
        
        // Construct IBAN
        return COUNTRY_CODE + checkDigits + BANK_CODE + 
               RESERVED + paddedAccountNumber;
    }
    
    private static String calculateCheckDigits(String bban) {
        // MOD-97 algorithm (ISO 13616)
        // 1. Move country code to end
        // 2. Replace letters with numbers (A=10, B=11, ..., Z=35)
        // 3. Calculate mod 97
        // 4. Check digit = 98 - mod 97
        
        String rearranged = bban + "TR00";
        String numeric = replaceLettersWithNumbers(rearranged);
        
        BigInteger mod = new BigInteger(numeric).mod(BigInteger.valueOf(97));
        int checkDigit = 98 - mod.intValue();
        
        return String.format("%02d", checkDigit);
    }
    
    private static String replaceLettersWithNumbers(String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                // T=29, R=27
                result.append(Character.getNumericValue(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
```

### IBAN Validation

```java
public static boolean validateIban(String iban) {
    if (iban == null || iban.length() != 26) {
        return false;
    }
    
    if (!iban.startsWith("TR")) {
        return false;
    }
    
    // Move first 4 characters to end
    String rearranged = iban.substring(4) + iban.substring(0, 4);
    
    // Replace letters with numbers
    String numeric = replaceLettersWithNumbers(rearranged);
    
    // Calculate mod 97 - should be 1
    BigInteger mod = new BigInteger(numeric).mod(BigInteger.valueOf(97));
    
    return mod.intValue() == 1;
}
```

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
account:{accountNumber}        → Full account object
account:iban:{iban}           → Account number (then full object)
```

### Cache Operations

1. **Read (GET)**
   ```java
   @Cacheable(value = "accounts", key = "#accountNumber")
   public Account getAccount(String accountNumber) {
       return accountRepository.findByAccountNumber(accountNumber)
           .orElseThrow(() -> new AccountNotFoundException(accountNumber));
   }
   ```

2. **Write (POST)**
   ```java
   @CachePut(value = "accounts", key = "#result.accountNumber")
   public Account createAccount(CreateAccountRequest request) {
       // Create and return account
   }
   ```

3. **Update (PUT)**
   ```java
   @CacheEvict(value = "accounts", key = "#accountNumber")
   public Account updateAccount(String accountNumber, UpdateRequest request) {
       // Update and return account
   }
   ```

4. **Delete (DELETE)**
   ```java
   @CacheEvict(value = "accounts", key = "#accountNumber")
   public void closeAccount(String accountNumber) {
       // Close account
   }
   ```

### Cache Invalidation

**Triggers:**
- Balance update
- Status change
- Account update
- Account closure

**Strategy:**
- Invalidate on write
- Short TTL (5 minutes) for consistency
- No distributed cache invalidation needed (single Redis instance)

---

## Event Publishing

### Kafka Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

### Events Schema

**1. AccountCreatedEvent**
```json
{
  "eventType": "ACCOUNT_CREATED",
  "accountNumber": "ACC-123...",
  "iban": "TR330006200000000006295467",
  "customerName": "John Doe",
  "accountType": "CHECKING",
  "currency": "TRY",
  "initialBalance": 0.00,
  "timestamp": "2025-12-23T10:30:00Z"
}
```

**2. BalanceChangedEvent**
```json
{
  "eventType": "BALANCE_CHANGED",
  "accountNumber": "ACC-123...",
  "previousBalance": 1000.00,
  "newBalance": 1100.00,
  "amount": 100.00,
  "operation": "CREDIT",
  "description": "Transfer credit",
  "timestamp": "2025-12-23T14:30:00Z"
}
```

**3. AccountStatusChangedEvent**
```json
{
  "eventType": "STATUS_CHANGED",
  "accountNumber": "ACC-123...",
  "previousStatus": "ACTIVE",
  "newStatus": "SUSPENDED",
  "reason": "Suspicious activity detected",
  "timestamp": "2025-12-23T15:00:00Z"
}
```

### Publishing Implementation

```java
@Component
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishAccountCreated(Account account) {
        AccountCreatedEvent event = AccountCreatedEvent.builder()
            .eventType("ACCOUNT_CREATED")
            .accountNumber(account.getAccountNumber())
            .iban(account.getIban())
            .customerName(account.getCustomerName())
            .accountType(account.getAccountType())
            .currency(account.getCurrency())
            .initialBalance(account.getBalance())
            .timestamp(LocalDateTime.now())
            .build();
        
        kafkaTemplate.send("account.events", account.getAccountNumber(), event);
    }
    
    public void publishBalanceChanged(
        Account account,
        BigDecimal previousBalance,
        BigDecimal amount,
        String operation
    ) {
        BalanceChangedEvent event = BalanceChangedEvent.builder()
            .eventType("BALANCE_CHANGED")
            .accountNumber(account.getAccountNumber())
            .previousBalance(previousBalance)
            .newBalance(account.getBalance())
            .amount(amount)
            .operation(operation)
            .timestamp(LocalDateTime.now())
            .build();
        
        kafkaTemplate.send("account.events", account.getAccountNumber(), event);
    }
}
```

---

## Error Handling

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse> handleAccountNotFound(
        AccountNotFoundException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse> handleInsufficientBalance(
        InsufficientBalanceException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(
                "Insufficient balance",
                ex.getDetails()
            ));
    }
    
    @ExceptionHandler(InvalidAccountStateException.class)
    public ResponseEntity<ApiResponse> handleInvalidAccountState(
        InvalidAccountStateException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }
}
```

### Custom Exceptions

```java
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}

public class InsufficientBalanceException extends RuntimeException {
    private final Map<String, Object> details;
    
    public InsufficientBalanceException(
        String accountNumber,
        BigDecimal currentBalance,
        BigDecimal requestedAmount
    ) {
        super("Insufficient balance in account: " + accountNumber);
        this.details = Map.of(
            "accountNumber", accountNumber,
            "currentBalance", currentBalance,
            "requestedAmount", requestedAmount,
            "shortfall", requestedAmount.abs().subtract(currentBalance)
        );
    }
}

public class InvalidAccountStateException extends RuntimeException {
    public InvalidAccountStateException(String message) {
        super(message);
    }
}
```

---

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @InjectMocks
    private AccountServiceImpl accountService;
    
    @Test
    @DisplayName("Should create account successfully")
    void shouldCreateAccountSuccessfully() {
        // Given
        CreateAccountRequest request = CreateAccountRequest.builder()
            .customerName("John Doe")
            .accountType(AccountType.CHECKING)
            .currency(Currency.TRY)
            .build();
        
        Account savedAccount = Account.builder()
            .id(1L)
            .accountNumber("ACC-123")
            .iban("TR330006200000000006295467")
            .customerName("John Doe")
            .accountType(AccountType.CHECKING)
            .balance(BigDecimal.ZERO)
            .currency(Currency.TRY)
            .status(AccountStatus.ACTIVE)
            .build();
        
        when(accountRepository.save(any(Account.class)))
            .thenReturn(savedAccount);
        
        // When
        AccountResponse response = accountService.createAccount(request);
        
        // Then
        assertNotNull(response);
        assertEquals("ACC-123", response.getAccountNumber());
        assertEquals("John Doe", response.getCustomerName());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        
        verify(accountRepository).save(any(Account.class));
        verify(eventPublisher).publishAccountCreated(any(Account.class));
    }
    
    @Test
    @DisplayName("Should throw exception when account not found")
    void shouldThrowExceptionWhenAccountNotFound() {
        // Given
        String accountNumber = "NONEXISTENT";
        when(accountRepository.findByAccountNumber(accountNumber))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(AccountNotFoundException.class, () -> 
            accountService.getAccount(accountNumber)
        );
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AccountControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:16-alpine"
    );
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Test
    void shouldCreateAndRetrieveAccount() {
        // Create account
        CreateAccountRequest request = new CreateAccountRequest(
            "John Doe",
            AccountType.CHECKING,
            Currency.TRY
        );
        
        ResponseEntity<ApiResponse> createResponse = restTemplate.postForEntity(
            "/accounts",
            request,
            ApiResponse.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        // Extract account number
        String accountNumber = extractAccountNumber(createResponse);
        
        // Retrieve account
        ResponseEntity<AccountResponse> getResponse = restTemplate.getForEntity(
            "/accounts/" + accountNumber,
            AccountResponse.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals("John Doe", getResponse.getBody().getCustomerName());
    }
}
```

---

**Last Updated:** 23 December 2025  
**API Version:** 1.0  
**Service Status:** ✅ Production Ready
