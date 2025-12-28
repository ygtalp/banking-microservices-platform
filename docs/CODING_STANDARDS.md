# Coding Standards

> **Purpose:** Establish consistent coding practices across the banking platform  
> **Scope:** All microservices  
> **Last Updated:** 23 December 2025

---

## Table of Contents

1. [General Principles](#general-principles)
2. [Java Conventions](#java-conventions)
3. [Spring Boot Patterns](#spring-boot-patterns)
4. [Database Standards](#database-standards)
5. [API Design](#api-design)
6. [Error Handling](#error-handling)
7. [Testing Standards](#testing-standards)
8. [Security Guidelines](#security-guidelines)
9. [Code Review Checklist](#code-review-checklist)

---

## General Principles

### Clean Code

```java
// ✅ GOOD - Self-documenting
public BigDecimal calculateTotalTransferAmount(Transfer transfer) {
    return transfer.getAmount()
        .add(transfer.getFeeAmount())
        .setScale(2, RoundingMode.HALF_UP);
}

// ❌ BAD - Unclear naming
public BigDecimal calc(Transfer t) {
    return t.getAmt().add(t.getFee()).setScale(2, RoundingMode.HALF_UP);
}
```

### SOLID Principles

1. **Single Responsibility:** One class, one purpose
2. **Open/Closed:** Open for extension, closed for modification
3. **Liskov Substitution:** Subtypes must be substitutable for their base types
4. **Interface Segregation:** Many specific interfaces over one general
5. **Dependency Inversion:** Depend on abstractions, not concretions

### DRY (Don't Repeat Yourself)

```java
// ✅ GOOD - Extracted common logic
private void validateAccount(Account account, String operation) {
    if (account == null) {
        throw new AccountNotFoundException("Account not found");
    }
    if (account.getStatus() != AccountStatus.ACTIVE) {
        throw new InvalidAccountStateException(
            "Cannot " + operation + ": Account is " + account.getStatus()
        );
    }
}

// ❌ BAD - Repeated validation
public void debit(Account account, BigDecimal amount) {
    if (account == null) throw new AccountNotFoundException(...);
    if (account.getStatus() != AccountStatus.ACTIVE) throw new...;
    // ...
}

public void credit(Account account, BigDecimal amount) {
    if (account == null) throw new AccountNotFoundException(...);
    if (account.getStatus() != AccountStatus.ACTIVE) throw new...;
    // ...
}
```

---

## Java Conventions

### Package Structure

```
com.banking.{service}
├── config          // Spring configuration (@Configuration)
├── controller      // REST controllers (@RestController)
├── dto             // Data Transfer Objects (Request/Response)
│   ├── request     // API request objects
│   └── response    // API response objects
├── model           // Domain entities (@Entity)
│   └── enums       // Enums (Status, Type, etc.)
├── repository      // Data access (@Repository)
├── service         // Business logic
│   └── impl        // Service implementations
├── event           // Domain events (Kafka)
├── exception       // Custom exceptions
├── client          // External service clients (Feign)
├── saga            // SAGA orchestration (Transfer Service only)
└── util            // Utility classes
```

### Naming Conventions

```java
// Classes: PascalCase
public class AccountService { }
public class TransferController { }

// Interfaces: PascalCase (no "I" prefix)
public interface AccountRepository { }  // ✅ GOOD
public interface IAccountRepository { }  // ❌ BAD

// Methods: camelCase
public void executeTransfer() { }
public Account getAccountByNumber(String accountNumber) { }

// Variables: camelCase
String accountNumber;
BigDecimal transferAmount;
LocalDateTime createdAt;

// Constants: UPPER_SNAKE_CASE
public static final String DEFAULT_CURRENCY = "TRY";
public static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("100000.00");

// Packages: lowercase
package com.banking.account.service;
```

### Money Handling (CRITICAL!)

```java
// ✅ ALWAYS use BigDecimal for money
BigDecimal amount = new BigDecimal("100.00");      // String constructor
BigDecimal price = BigDecimal.valueOf(99.95);      // valueOf method

// ❌ NEVER use double constructor
BigDecimal wrong = new BigDecimal(0.1);  // Rounding error!

// ✅ Arithmetic operations
BigDecimal sum = amount.add(price);
BigDecimal difference = amount.subtract(price);
BigDecimal product = amount.multiply(BigDecimal.valueOf(1.1));
BigDecimal quotient = amount.divide(price, 2, RoundingMode.HALF_UP);

// ✅ Comparison (use compareTo, not equals!)
if (amount.compareTo(BigDecimal.ZERO) > 0) {
    // amount is positive
}

if (amount.compareTo(price) >= 0) {
    // amount is greater than or equal to price
}

// ❌ NEVER use equals for BigDecimal comparison
if (amount.equals(price)) {  // WRONG - scale difference!
    // This can fail due to scale differences
    // Example: new BigDecimal("100.0") != new BigDecimal("100.00")
}

// ✅ Set scale and rounding mode explicitly
BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
```

### Lombok Usage

```java
@Entity
@Table(name = "accounts")
@Data                    // ✅ Generates getters, setters, toString, equals, hashCode
@Builder                 // ✅ Builder pattern
@NoArgsConstructor       // ✅ Required by JPA
@AllArgsConstructor      // ✅ Required by @Builder
@Slf4j                   // ✅ Logger (log.info(...))
public class Account {
    // Fields
}

// Avoid on entities that may cause issues:
@ToString(exclude = {"largeCollection"})     // Exclude large collections
@EqualsAndHashCode(exclude = {"id"})        // Exclude auto-generated IDs
```

### Optional Usage

```java
// ✅ GOOD - Return Optional for methods that may not find a result
public Optional<Account> findByAccountNumber(String accountNumber) {
    return accountRepository.findByAccountNumber(accountNumber);
}

// ✅ GOOD - Use Optional properly
accountService.findByAccountNumber(accountNumber)
    .ifPresentOrElse(
        account -> log.info("Found: {}", account),
        () -> log.warn("Account not found: {}", accountNumber)
    );

// ❌ BAD - Don't use Optional as method parameter
public void updateAccount(Optional<Account> account) {  // WRONG!
    // ...
}

// ✅ GOOD - Use null check or overloading instead
public void updateAccount(Account account) {
    if (account == null) {
        throw new IllegalArgumentException("Account cannot be null");
    }
    // ...
}
```

---

## Spring Boot Patterns

### Controller Layer

```java
@RestController
@RequestMapping("/accounts")
@Slf4j
@Validated
public class AccountController {
    
    private final AccountService accountService;
    
    // ✅ Constructor injection (preferred over @Autowired)
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
        @Valid @RequestBody CreateAccountRequest request  // ✅ Validate request
    ) {
        log.info("Creating account for customer: {}", request.getCustomerName());
        
        AccountResponse response = accountService.createAccount(request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }
    
    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
        @PathVariable("accountNumber") String accountNumber  // ✅ Explicit name!
    ) {
        AccountResponse response = accountService.getAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### Service Layer

```java
@Service
@Slf4j
@Transactional  // ✅ Default transactional behavior for all methods
public class AccountServiceImpl implements AccountService {
    
    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;
    
    // ✅ Constructor injection
    public AccountServiceImpl(
        AccountRepository accountRepository,
        EventPublisher eventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    @Transactional  // ✅ Explicit for critical operations
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account: {}", request);
        
        // Business logic
        Account account = buildAccount(request);
        account = accountRepository.save(account);
        
        // Publish event
        eventPublisher.publishAccountCreated(account);
        
        return mapToResponse(account);
    }
    
    @Override
    @Transactional(readOnly = true)  // ✅ Read-only optimization
    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository
            .findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        
        return mapToResponse(account);
    }
}
```

### Repository Layer

```java
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByAccountNumber(String accountNumber);
    
    Optional<Account> findByIban(String iban);
    
    boolean existsByAccountNumber(String accountNumber);
    
    @Query("SELECT a FROM Account a WHERE a.status = :status AND a.balance > :minBalance")
    List<Account> findActiveAccountsWithMinimumBalance(
        @Param("status") AccountStatus status,
        @Param("minBalance") BigDecimal minBalance
    );
}
```

---

## Database Standards

### Entity Annotations

```java
@Entity
@Table(
    name = "accounts",
    indexes = {
        @Index(name = "idx_account_number", columnList = "account_number"),
        @Index(name = "idx_iban", columnList = "iban")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String accountNumber;
    
    @Column(unique = true, nullable = false, length = 26)
    private String iban;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;  // ✅ precision and scale for money!
    
    @Enumerated(EnumType.STRING)  // ✅ STRING not ORDINAL!
    @Column(nullable = false, length = 20)
    private AccountStatus status;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### Naming Conventions

```sql
-- Tables: lowercase, plural
accounts
transfers
customers

-- Columns: lowercase, snake_case
account_number
created_at
transfer_reference

-- Indexes: idx_{table}_{column}
idx_accounts_account_number
idx_transfers_status

-- Foreign Keys: fk_{table}_{referenced_table}
fk_transfers_accounts
```

---

## API Design

### REST Principles

```
POST   /accounts              Create new account
GET    /accounts/{id}         Get account by ID
GET    /accounts?status=ACTIVE  List accounts (with filter)
PUT    /accounts/{id}         Update entire account
PATCH  /accounts/{id}         Update partial account
DELETE /accounts/{id}         Delete account

// Sub-resources
PUT    /accounts/{id}/balance    Update balance
GET    /accounts/{id}/history    Get history
```

### Request/Response Format

```java
// ✅ Consistent response wrapper
@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Map<String, String> errors;  // For validation errors
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .build();
    }
}
```

### Validation

```java
@Data
@Builder
public class CreateAccountRequest {
    
    @NotBlank(message = "Customer name is required")
    @Size(min = 3, max = 100, message = "Customer name must be between 3 and 100 characters")
    private String customerName;
    
    @NotNull(message = "Account type is required")
    private AccountType accountType;
    
    @NotNull(message = "Currency is required")
    private Currency currency;
}
```

---

## Error Handling

### Custom Exceptions

```java
// Business exceptions
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
            "requestedAmount", requestedAmount
        );
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse> handleAccountNotFound(
        AccountNotFoundException ex
    ) {
        log.error("Account not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.builder()
                .success(false)
                .message("Validation failed")
                .errors(errors)
                .build()
            );
    }
}
```

---

## Testing Standards

### Test Structure

```java
// Given-When-Then pattern
@Test
@DisplayName("Should create account successfully with valid request")
void shouldCreateAccountSuccessfully() {
    // Given - Setup test data
    CreateAccountRequest request = CreateAccountRequest.builder()
        .customerName("John Doe")
        .accountType(AccountType.CHECKING)
        .currency(Currency.TRY)
        .build();
    
    // When - Execute the operation
    AccountResponse response = accountService.createAccount(request);
    
    // Then - Verify the results
    assertNotNull(response);
    assertEquals("John Doe", response.getCustomerName());
    assertEquals(AccountType.CHECKING, response.getAccountType());
    assertEquals(BigDecimal.ZERO, response.getBalance());
}
```

### Test Coverage

```
Target: 80%+ overall coverage

Priority:
1. Business logic (service layer): 90%+
2. Controllers: 80%+
3. Utilities: 90%+
4. Configuration: Not required
```

---

## Security Guidelines

### Logging

```java
// ✅ SAFE - No sensitive data
log.info("Processing transfer: reference={}, amount={}",
    transfer.getTransferReference(), transfer.getAmount());

// ✅ SAFE - Masked account number
log.info("Account created: {}", maskAccountNumber(accountNumber));

// ❌ DANGEROUS - Full account number
log.info("Account: {}", accountNumber);  // DON'T!

// ❌ DANGEROUS - Sensitive data
log.info("Transfer from {} to {}", fromAccount, toAccount);  // DON'T!
```

### Input Validation

```java
// ✅ Always validate inputs
@PostMapping
public ResponseEntity<ApiResponse> createTransfer(
    @Valid @RequestBody TransferRequest request  // Spring validation
) {
    // Additional business validation
    validateTransferRequest(request);
    
    // Process
    TransferResponse response = transferService.executeTransfer(request);
    
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

---

## Code Review Checklist

### Before Committing

```
□ All tests pass (mvn test)
□ Code follows naming conventions
□ BigDecimal used for money
□ @PathVariable names explicit
□ @Transactional on data modifications
□ No sensitive data in logs
□ Proper error handling
□ Javadoc on public methods
□ No TODO/FIXME left
□ Git commit message follows convention
```

### Review Guidelines

```
✅ Check for:
- Correct use of BigDecimal
- Explicit @PathVariable names
- Transaction management
- Error handling
- Test coverage
- Security issues
- Performance concerns

❌ Reject if:
- float/double used for money
- Sensitive data logged
- No tests for new code
- Unclear naming
- Missing error handling
```

---

**Last Updated:** 23 December 2025  
**Version:** 1.0  
**Compliance:** Banking Sector Standards
