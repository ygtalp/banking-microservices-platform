# Transfer Service - SAGA Pattern Deep Dive

> **Service:** Transfer Service  
> **Port:** 8082  
> **Database:** banking_transfers (PostgreSQL)  
> **Responsibility:** Distributed money transfers  
> **Pattern:** Orchestration-based SAGA  
> **Last Updated:** 23 December 2025

---

## Table of Contents

1. [Overview](#overview)
2. [SAGA Pattern](#saga-pattern)
3. [Domain Model](#domain-model)
4. [SAGA Implementation](#saga-implementation)
5. [API Reference](#api-reference)
6. [Circuit Breaker](#circuit-breaker)
7. [Idempotency](#idempotency)
8. [Event Publishing](#event-publishing)
9. [Testing](#testing)

---

## Overview

Transfer Service orchestrates money transfers between accounts using the SAGA pattern to ensure distributed transaction consistency without distributed locks.

### Key Features

- ✅ Orchestration-based SAGA pattern
- ✅ Automatic compensation on failure
- ✅ Idempotency key support (Redis, 24h TTL)
- ✅ Circuit breaker (Resilience4j)
- ✅ Feign client for Account Service integration
- ✅ Event-driven notifications (Kafka)
- ✅ Transfer types (INTERNAL, EXTERNAL, INTERNATIONAL)
- ✅ Multi-currency support

### Technology Stack

```yaml
Framework: Spring Boot 3.2.0
Database: PostgreSQL 16
Cache: Redis 7.2 (idempotency)
Messaging: Apache Kafka 3.6
Service Client: OpenFeign
Resilience: Resilience4j (Circuit Breaker)
```

---

## SAGA Pattern

### What is SAGA?

SAGA is a pattern for managing distributed transactions across microservices. Instead of traditional ACID transactions, SAGA breaks a transaction into a sequence of local transactions, each with a compensating transaction for rollback.

### Why SAGA?

**Problem:** Account Service and Transfer Service are separate with their own databases. Traditional distributed transactions (2PC) are:
- ❌ Locking (poor performance)
- ❌ Synchronous (low availability)
- ❌ Complex (coordinator overhead)

**Solution:** SAGA pattern provides:
- ✅ Eventual consistency
- ✅ No distributed locks
- ✅ Automatic compensation
- ✅ High availability

### Orchestration vs Choreography

**Choreography (Event-Driven):**
```
Service A → Event → Service B → Event → Service C
  (decoupled but hard to track)
```

**Orchestration (Central Coordinator):**
```
Orchestrator → Service A
            → Service B
            → Service C
  (centralized control, easier to debug)
```

**We chose Orchestration because:**
1. Clear transaction flow
2. Easier debugging
3. Centralized compensation logic
4. Better for 2-10 services

---

## Domain Model

### Transfer Entity

```java
@Entity
@Table(name = "transfers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String transferReference;  // TRF-{UUID}
    
    @Column(nullable = false)
    private String fromAccountNumber;
    
    @Column(nullable = false)
    private String toAccountNumber;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferType transferType;
    
    @Column(unique = true, length = 100)
    private String idempotencyKey;  // Client-provided
    
    @Column(length = 50)
    private String debitTransactionId;   // From Account Service
    
    @Column(length = 50)
    private String creditTransactionId;  // From Account Service
    
    @Column(length = 1000)
    private String failureReason;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### Transfer Status Flow

```
PENDING
  ↓
VALIDATING (ValidationStep executing)
  ↓
VALIDATED (All validations passed)
  ↓
DEBIT_PENDING (DebitStep executing)
  ↓
DEBIT_COMPLETED (Debit successful)
  ↓
CREDIT_PENDING (CreditStep executing)
  ↓
COMPLETED ✅ (All steps successful)

OR

FAILED ❌ (Step failed, compensation failed/not possible)

OR

COMPENSATING ↩️ (Rolling back)
  ↓
COMPENSATED ✅ (Rollback successful)
```

### Transfer Enums

```java
public enum TransferStatus {
    PENDING,
    VALIDATING,
    VALIDATED,
    DEBIT_PENDING,
    DEBIT_COMPLETED,
    CREDIT_PENDING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}

public enum TransferType {
    INTERNAL,       // Same bank, same currency
    EXTERNAL,       // Different bank, same currency
    INTERNATIONAL   // Different currency
}
```

---

## SAGA Implementation

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│             Transfer Service (Orchestrator)             │
│                                                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │          TransferSagaOrchestrator                 │ │
│  │                                                   │ │
│  │  Step 1: ValidationStep                          │ │
│  │    ├─ Validate source account exists             │ │
│  │    ├─ Validate source account active             │ │
│  │    ├─ Validate destination account exists        │ │
│  │    ├─ Validate destination account active        │ │
│  │    ├─ Check sufficient balance                   │ │
│  │    └─ Validate currency match                    │ │
│  │                                                   │ │
│  │  Step 2: DebitStep                               │ │
│  │    ├─ Call Account Service (Feign)               │ │
│  │    ├─ Debit source account                       │ │
│  │    ├─ Store debit transaction ID                 │ │
│  │    └─ Update status: DEBIT_COMPLETED             │ │
│  │                                                   │ │
│  │  Step 3: CreditStep                              │ │
│  │    ├─ Call Account Service (Feign)               │ │
│  │    ├─ Credit destination account                 │ │
│  │    ├─ Store credit transaction ID                │ │
│  │    └─ Update status: COMPLETED                   │ │
│  │                                                   │ │
│  │  On Failure:                                     │ │
│  │    ├─ Set status: COMPENSATING                   │ │
│  │    ├─ Reverse steps (in reverse order)           │ │
│  │    ├─ CreditStep.compensate()                    │ │
│  │    ├─ DebitStep.compensate()                     │ │
│  │    ├─ ValidationStep.compensate() (no-op)        │ │
│  │    └─ Set status: COMPENSATED or FAILED          │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │   Account Service     │
              │   (via Feign Client)  │
              └───────────────────────┘
```

### Orchestrator Implementation

```java
@Component
@Slf4j
public class TransferSagaOrchestrator {
    
    private final TransferRepository transferRepository;
    private final List<SagaStep> steps;
    
    public TransferSagaOrchestrator(
        ValidationStep validationStep,
        DebitStep debitStep,
        CreditStep creditStep,
        TransferRepository transferRepository
    ) {
        this.steps = Arrays.asList(validationStep, debitStep, creditStep);
        this.transferRepository = transferRepository;
    }
    
    public Transfer executeTransfer(Transfer transfer) {
        List<SagaStep> executedSteps = new ArrayList<>();
        
        try {
            // Execute steps sequentially
            for (SagaStep step : steps) {
                log.info("Executing SAGA step: {} for transfer: {}",
                    step.getStepName(), transfer.getTransferReference());
                
                if (!step.execute(transfer)) {
                    log.error("SAGA step failed: {} for transfer: {}",
                        step.getStepName(), transfer.getTransferReference());
                    
                    // Compensate executed steps
                    compensate(executedSteps, transfer);
                    return transfer;
                }
                
                executedSteps.add(step);
                transferRepository.save(transfer);
            }
            
            // All steps successful
            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setCompletedAt(LocalDateTime.now());
            transferRepository.save(transfer);
            
            log.info("SAGA orchestration completed successfully for transfer: {}",
                transfer.getTransferReference());
            
            return transfer;
            
        } catch (Exception e) {
            log.error("Unexpected error during SAGA execution: {}", 
                e.getMessage(), e);
            
            transfer.setFailureReason("Unexpected error: " + e.getMessage());
            compensate(executedSteps, transfer);
            return transfer;
        }
    }
    
    private void compensate(List<SagaStep> executedSteps, Transfer transfer) {
        log.warn("Starting compensation for transfer: {}",
            transfer.getTransferReference());
        
        transfer.setStatus(TransferStatus.COMPENSATING);
        transferRepository.save(transfer);
        
        // Reverse the steps in reverse order
        Collections.reverse(executedSteps);
        
        boolean compensationSuccessful = true;
        
        for (SagaStep step : executedSteps) {
            try {
                log.info("Compensating step: {} for transfer: {}",
                    step.getStepName(), transfer.getTransferReference());
                
                if (!step.compensate(transfer)) {
                    log.error("Compensation failed for step: {}",
                        step.getStepName());
                    compensationSuccessful = false;
                }
            } catch (Exception e) {
                log.error("Compensation error for step {}: {}",
                    step.getStepName(), e.getMessage(), e);
                compensationSuccessful = false;
            }
        }
        
        if (compensationSuccessful) {
            transfer.setStatus(TransferStatus.COMPENSATED);
            log.info("Compensation completed successfully for transfer: {}",
                transfer.getTransferReference());
        } else {
            transfer.setStatus(TransferStatus.FAILED);
            String errorMsg = "Compensation partially failed - " +
                "Manual intervention required for transfer: " +
                transfer.getTransferReference();
            log.error(errorMsg);
            transfer.setFailureReason(
                transfer.getFailureReason() + " | " + errorMsg
            );
        }
        
        transferRepository.save(transfer);
    }
}
```

### SAGA Step Interface

```java
public interface SagaStep {
    
    /**
     * Get step name for logging
     */
    String getStepName();
    
    /**
     * Execute the step
     * @return true if successful, false otherwise
     */
    boolean execute(Transfer transfer);
    
    /**
     * Compensate (rollback) the step
     * @return true if compensation successful, false otherwise
     */
    boolean compensate(Transfer transfer);
}
```

### Step 1: ValidationStep

```java
@Component
@Slf4j
public class ValidationStep implements SagaStep {
    
    private final AccountServiceClient accountServiceClient;
    
    @Override
    public String getStepName() {
        return "ValidationStep";
    }
    
    @Override
    public boolean execute(Transfer transfer) {
        transfer.setStatus(TransferStatus.VALIDATING);
        
        try {
            // 1. Validate source account
            log.info("Validating source account: {}",
                transfer.getFromAccountNumber());
            
            AccountBalanceResponse sourceAccount = 
                accountServiceClient.getAccount(transfer.getFromAccountNumber());
            
            if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
                transfer.setFailureReason(
                    "Source account is not active: " + 
                    sourceAccount.getStatus()
                );
                return false;
            }
            
            // 2. Validate destination account
            log.info("Validating destination account: {}",
                transfer.getToAccountNumber());
            
            AccountBalanceResponse destAccount = 
                accountServiceClient.getAccount(transfer.getToAccountNumber());
            
            if (destAccount.getStatus() != AccountStatus.ACTIVE) {
                transfer.setFailureReason(
                    "Destination account is not active: " + 
                    destAccount.getStatus()
                );
                return false;
            }
            
            // 3. Validate sufficient balance
            if (sourceAccount.getBalance().compareTo(transfer.getAmount()) < 0) {
                transfer.setFailureReason(
                    String.format("Insufficient balance. Required: %s, Available: %s",
                        transfer.getAmount(), sourceAccount.getBalance())
                );
                return false;
            }
            
            // 4. Validate currency match
            if (!sourceAccount.getCurrency().equals(transfer.getCurrency())) {
                transfer.setFailureReason(
                    "Currency mismatch. Account: " + sourceAccount.getCurrency() +
                    ", Transfer: " + transfer.getCurrency()
                );
                return false;
            }
            
            // All validations passed
            transfer.setStatus(TransferStatus.VALIDATED);
            log.info("Validation successful for transfer: {}",
                transfer.getTransferReference());
            return true;
            
        } catch (FeignException e) {
            transfer.setFailureReason(
                "Failed to validate accounts: " + e.getMessage()
            );
            log.error("Validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean compensate(Transfer transfer) {
        // No compensation needed for validation
        log.info("No compensation needed for ValidationStep");
        return true;
    }
}
```

### Step 2: DebitStep

```java
@Component
@Slf4j
public class DebitStep implements SagaStep {
    
    private final AccountServiceClient accountServiceClient;
    
    @Override
    public String getStepName() {
        return "DebitStep";
    }
    
    @Override
    public boolean execute(Transfer transfer) {
        transfer.setStatus(TransferStatus.DEBIT_PENDING);
        
        try {
            log.info("Debiting {} from account: {}",
                transfer.getAmount(), transfer.getFromAccountNumber());
            
            // Call Account Service to debit amount
            TransactionRequest debitRequest = TransactionRequest.builder()
                .accountNumber(transfer.getFromAccountNumber())
                .amount(transfer.getAmount().negate())  // Negative for debit
                .description("Transfer debit: " + transfer.getTransferReference())
                .build();
            
            TransactionResponse response = accountServiceClient.updateBalance(
                transfer.getFromAccountNumber(),
                debitRequest
            );
            
            // Store transaction ID for compensation
            transfer.setDebitTransactionId(response.getTransactionId());
            transfer.setStatus(TransferStatus.DEBIT_COMPLETED);
            
            log.info("Debit successful. Transaction ID: {}",
                response.getTransactionId());
            
            return true;
            
        } catch (FeignException e) {
            transfer.setFailureReason("Debit failed: " + e.getMessage());
            log.error("Debit failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean compensate(Transfer transfer) {
        try {
            log.info("Compensating debit by crediting {} back to account: {}",
                transfer.getAmount(), transfer.getFromAccountNumber());
            
            // Reverse the debit by crediting back
            TransactionRequest creditRequest = TransactionRequest.builder()
                .accountNumber(transfer.getFromAccountNumber())
                .amount(transfer.getAmount())  // Positive for credit
                .description("Transfer rollback: " + transfer.getTransferReference())
                .build();
            
            accountServiceClient.updateBalance(
                transfer.getFromAccountNumber(),
                creditRequest
            );
            
            log.info("Debit compensation successful for transfer: {}",
                transfer.getTransferReference());
            
            return true;
            
        } catch (Exception e) {
            log.error("Debit compensation failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
```

### Step 3: CreditStep

```java
@Component
@Slf4j
public class CreditStep implements SagaStep {
    
    private final AccountServiceClient accountServiceClient;
    
    @Override
    public String getStepName() {
        return "CreditStep";
    }
    
    @Override
    public boolean execute(Transfer transfer) {
        transfer.setStatus(TransferStatus.CREDIT_PENDING);
        
        try {
            log.info("Crediting {} to account: {}",
                transfer.getAmount(), transfer.getToAccountNumber());
            
            // Call Account Service to credit amount
            TransactionRequest creditRequest = TransactionRequest.builder()
                .accountNumber(transfer.getToAccountNumber())
                .amount(transfer.getAmount())  // Positive for credit
                .description("Transfer credit: " + transfer.getTransferReference())
                .build();
            
            TransactionResponse response = accountServiceClient.updateBalance(
                transfer.getToAccountNumber(),
                creditRequest
            );
            
            // Store transaction ID
            transfer.setCreditTransactionId(response.getTransactionId());
            
            log.info("Credit successful. Transaction ID: {}",
                response.getTransactionId());
            
            return true;
            
        } catch (FeignException e) {
            transfer.setFailureReason("Credit failed: " + e.getMessage());
            log.error("Credit failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean compensate(Transfer transfer) {
        try {
            log.info("Compensating credit by debiting {} from account: {}",
                transfer.getAmount(), transfer.getToAccountNumber());
            
            // Reverse the credit by debiting back
            TransactionRequest debitRequest = TransactionRequest.builder()
                .accountNumber(transfer.getToAccountNumber())
                .amount(transfer.getAmount().negate())  // Negative for debit
                .description("Transfer rollback: " + transfer.getTransferReference())
                .build();
            
            accountServiceClient.updateBalance(
                transfer.getToAccountNumber(),
                debitRequest
            );
            
            log.info("Credit compensation successful for transfer: {}",
                transfer.getTransferReference());
            
            return true;
            
        } catch (Exception e) {
            log.error("Credit compensation failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
```

---

## API Reference

### Base URL

```
http://localhost:8080/transfers  (via API Gateway)
http://localhost:8082/transfers  (direct access)
```

### 1. Execute Transfer

**Endpoint:** `POST /transfers`

**Request:**
```json
{
  "fromAccountNumber": "ACC-source-123",
  "toAccountNumber": "ACC-dest-456",
  "amount": 100.50,
  "currency": "TRY",
  "description": "Payment for services",
  "transferType": "INTERNAL",
  "idempotencyKey": "UNIQUE-KEY-20251223-001"
}
```

**Response (200 OK - Completed):**
```json
{
  "success": true,
  "message": "Transfer completed successfully",
  "data": {
    "transferReference": "TRF-789abc",
    "status": "COMPLETED",
    "fromAccountNumber": "ACC-source-123",
    "toAccountNumber": "ACC-dest-456",
    "amount": 100.50,
    "currency": "TRY",
    "description": "Payment for services",
    "transferType": "INTERNAL",
    "debitTransactionId": "TXN-DEBIT-001",
    "creditTransactionId": "TXN-CREDIT-002",
    "createdAt": "2025-12-23T10:30:00Z",
    "completedAt": "2025-12-23T10:30:03Z"
  }
}
```

**Response (200 OK - Compensated):**
```json
{
  "success": false,
  "message": "Transfer failed and was rolled back",
  "data": {
    "transferReference": "TRF-789abc",
    "status": "COMPENSATED",
    "failureReason": "Credit failed: Destination account suspended",
    "fromAccountNumber": "ACC-source-123",
    "toAccountNumber": "ACC-dest-456",
    "amount": 100.50,
    "currency": "TRY"
  }
}
```

### 2. Get Transfer

**Endpoint:** `GET /transfers/{transferReference}`

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "transferReference": "TRF-789abc",
    "status": "COMPLETED",
    "fromAccountNumber": "ACC-source-123",
    "toAccountNumber": "ACC-dest-456",
    "amount": 100.50,
    "currency": "TRY",
    "description": "Payment for services",
    "transferType": "INTERNAL",
    "debitTransactionId": "TXN-DEBIT-001",
    "creditTransactionId": "TXN-CREDIT-002",
    "createdAt": "2025-12-23T10:30:00Z",
    "completedAt": "2025-12-23T10:30:03Z"
  }
}
```

---

## Circuit Breaker

### Configuration

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        failureRateThreshold: 50        # Open after 50% failures
        waitDurationInOpenState: 60s    # Wait before half-open
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
  
  timelimiter:
    configs:
      default:
        timeoutDuration: 5s
```

### Feign Client with Circuit Breaker

```java
@FeignClient(
    name = "account-service",
    fallbackFactory = AccountServiceClientFallbackFactory.class
)
public interface AccountServiceClient {
    
    @GetMapping("/accounts/{accountNumber}")
    AccountBalanceResponse getAccount(
        @PathVariable("accountNumber") String accountNumber
    );
    
    @PutMapping("/accounts/{accountNumber}/balance")
    TransactionResponse updateBalance(
        @PathVariable("accountNumber") String accountNumber,
        @RequestBody TransactionRequest request
    );
}
```

### Fallback Implementation

```java
@Component
public class AccountServiceClientFallbackFactory 
    implements FallbackFactory<AccountServiceClient> {
    
    @Override
    public AccountServiceClient create(Throwable cause) {
        return new AccountServiceClient() {
            
            @Override
            public AccountBalanceResponse getAccount(String accountNumber) {
                throw new ServiceUnavailableException(
                    "Account Service is currently unavailable. Please try again later."
                );
            }
            
            @Override
            public TransactionResponse updateBalance(
                String accountNumber,
                TransactionRequest request
            ) {
                throw new ServiceUnavailableException(
                    "Account Service is currently unavailable. Transfer cannot proceed."
                );
            }
        };
    }
}
```

---

## Idempotency

### Implementation

```java
@Service
public class TransferService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final TransferRepository transferRepository;
    private final TransferSagaOrchestrator sagaOrchestrator;
    
    public TransferResponse executeTransfer(TransferRequest request) {
        // Check idempotency key if provided
        if (request.getIdempotencyKey() != null) {
            String existingReference = redisTemplate.opsForValue().get(
                "idempotency:" + request.getIdempotencyKey()
            );
            
            if (existingReference != null) {
                // Duplicate request - return existing transfer
                log.warn("Duplicate transfer detected with idempotency key: {}",
                    request.getIdempotencyKey());
                
                Transfer existingTransfer = transferRepository
                    .findByTransferReference(existingReference)
                    .orElseThrow(() -> new TransferNotFoundException(existingReference));
                
                return mapToResponse(existingTransfer);
            }
        }
        
        // Create new transfer
        Transfer transfer = createTransfer(request);
        
        // Store idempotency key in Redis
        if (request.getIdempotencyKey() != null) {
            redisTemplate.opsForValue().set(
                "idempotency:" + request.getIdempotencyKey(),
                transfer.getTransferReference(),
                Duration.ofHours(24)  // TTL: 24 hours
            );
        }
        
        // Execute SAGA orchestration
        transfer = sagaOrchestrator.executeTransfer(transfer);
        
        // Publish events
        publishTransferEvents(transfer);
        
        return mapToResponse(transfer);
    }
}
```

### Redis Configuration

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

---

## Event Publishing

### Kafka Topics

```
transfer.events:
  - TransferInitiatedEvent
  - TransferCompletedEvent
  - TransferFailedEvent
  - TransferCompensatedEvent
```

### Event Schemas

```java
@Data
@Builder
public class TransferInitiatedEvent {
    private String eventType = "TRANSFER_INITIATED";
    private String transferReference;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private Currency currency;
    private LocalDateTime timestamp;
}

@Data
@Builder
public class TransferCompletedEvent {
    private String eventType = "TRANSFER_COMPLETED";
    private String transferReference;
    private String debitTransactionId;
    private String creditTransactionId;
    private LocalDateTime completedAt;
}

@Data
@Builder
public class TransferFailedEvent {
    private String eventType = "TRANSFER_FAILED";
    private String transferReference;
    private String failureReason;
    private TransferStatus status;  // FAILED or COMPENSATED
    private LocalDateTime timestamp;
}
```

---

## Testing

### SAGA Flow Tests

```java
@ExtendWith(MockitoExtension.class)
class TransferSagaOrchestratorTest {
    
    @Mock private ValidationStep validationStep;
    @Mock private DebitStep debitStep;
    @Mock private CreditStep creditStep;
    @Mock private TransferRepository transferRepository;
    
    @InjectMocks
    private TransferSagaOrchestrator orchestrator;
    
    @Test
    @DisplayName("Should complete transfer when all steps succeed")
    void shouldCompleteTransferSuccessfully() {
        // Given
        Transfer transfer = createTestTransfer();
        when(validationStep.execute(transfer)).thenReturn(true);
        when(debitStep.execute(transfer)).thenReturn(true);
        when(creditStep.execute(transfer)).thenReturn(true);
        
        // When
        Transfer result = orchestrator.executeTransfer(transfer);
        
        // Then
        assertEquals(TransferStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        
        InOrder inOrder = inOrder(validationStep, debitStep, creditStep);
        inOrder.verify(validationStep).execute(transfer);
        inOrder.verify(debitStep).execute(transfer);
        inOrder.verify(creditStep).execute(transfer);
    }
    
    @Test
    @DisplayName("Should compensate when credit step fails")
    void shouldCompensateOnCreditFailure() {
        // Given
        Transfer transfer = createTestTransfer();
        when(validationStep.execute(transfer)).thenReturn(true);
        when(debitStep.execute(transfer)).thenReturn(true);
        when(creditStep.execute(transfer)).thenReturn(false);
        when(debitStep.compensate(transfer)).thenReturn(true);
        when(validationStep.compensate(transfer)).thenReturn(true);
        
        // When
        Transfer result = orchestrator.executeTransfer(transfer);
        
        // Then
        assertEquals(TransferStatus.COMPENSATED, result.getStatus());
        
        // Verify compensation in reverse order
        InOrder inOrder = inOrder(debitStep, validationStep);
        inOrder.verify(debitStep).compensate(transfer);
        inOrder.verify(validationStep).compensate(transfer);
    }
}
```

---

**Last Updated:** 23 December 2025  
**Pattern:** Orchestration-based SAGA  
**Service Status:** ✅ Production Ready
