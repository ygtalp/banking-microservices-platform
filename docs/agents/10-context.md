# Context & Pattern Agents

> **Category:** Consistency & Pattern Enforcement
> **Agent Count:** 3
> **Automation Level:** High (80%)
> **Last Updated:** 28 December 2025

---

## 1. ProjectContextAgent 🧠

**Objective:** Provide AI agents with comprehensive project context from documentation.

**Context Sources:**
- CLAUDE.md (project standards)
- CODING_STANDARDS.md
- ARCHITECTURE_DECISIONS.md
- Session logs
- Existing service code

**Context Extraction:**

```markdown
# Project Context Summary

## Tech Stack
- Language: Java 17 LTS
- Framework: Spring Boot 3.2.0
- Database: PostgreSQL 16
- Cache: Redis 7.2
- Messaging: Apache Kafka 3.6
- Service Discovery: Eureka
- Containerization: Docker

## Architectural Patterns
1. **SAGA Pattern** (Orchestration-based)
   - Used in: Transfer Service
   - Pattern: ValidationStep → DebitStep → CreditStep
   - Compensation: Reverse order on failure

2. **Event-Driven Architecture**
   - Kafka topics: {service}.events
   - Events: {Action}Event (e.g., AccountCreatedEvent)
   - Async communication between services

3. **Circuit Breaker**
   - Library: Resilience4j
   - Configuration: 50% failure threshold, 60s wait
   - Applied to: Feign clients

4. **Domain-Driven Design**
   - Entities: @Entity with helper methods
   - Services: Business logic only
   - Repositories: Data access only

## Coding Standards (CRITICAL!)

### Money Handling
```java
// ❌ NEVER
float amount = 100.50f;
double balance = 1000.00;

// ✅ ALWAYS
BigDecimal amount = new BigDecimal("100.50");
BigDecimal balance = new BigDecimal("1000.00");
```

### PathVariable Names
```java
// ❌ WRONG
@PathVariable String accountNumber

// ✅ CORRECT
@PathVariable("accountNumber") String accountNumber
```

### Transaction Management
```java
// ✅ On all data modifications
@Transactional
public void updateAccount(...) { }

// ✅ Read-only optimization
@Transactional(readOnly = true)
public Account getAccount(...) { }
```

## Service Patterns

### Existing Services
1. **Account Service (8081)** - Account management, IBAN generation
2. **Transfer Service (8082)** - Money transfers with SAGA
3. **Customer Service (8083)** - Customer management, KYC
4. **Auth Service (8084)** - JWT authentication, RBAC (80% complete)

### Common Structure
```
{service}-service/
├── src/main/java/com/banking/{service}/
│   ├── config/          Configuration classes
│   ├── controller/      REST endpoints
│   ├── dto/            Request/Response objects
│   ├── model/          JPA entities
│   ├── repository/     Data access
│   ├── service/        Business logic
│   ├── event/          Kafka events
│   └── exception/      Custom exceptions
```

## Test Standards
- Coverage: 80%+ (unit + integration)
- Framework: JUnit 5, Mockito, TestContainers
- Pattern: Given-When-Then
- Naming: shouldDoSomethingWhenCondition()

## Documentation
- Session logs: /session_logs
- API docs: /docs/{SERVICE}_SERVICE.md
- ADRs: /docs/ARCHITECTURE_DECISIONS.md
```

---

## 2. PatternMatcherAgent 🎨

**Objective:** Detect patterns in existing code and enforce them in new code.

**Pattern Detection:**

```markdown
# Detected Patterns: Account Service

## Pattern 1: Entity ID Generation
**Found in:** Account.java, Customer.java, Transfer.java

**Pattern:**
```java
@PrePersist
protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    if (this.{entity}Id == null) {
        this.{entity}Id = generate{Entity}Id();
    }
}

private String generate{Entity}Id() {
    return "{PREFIX}-" + UUID.randomUUID().toString()
        .replace("-", "").substring(0, 12).toUpperCase();
}
```

**Instances:**
- Account: ACC-XXXXXXXXXXXX
- Customer: CUS-XXXXXXXXXXXX
- Transfer: TRF-XXXXXXXXXXXX
- Notification: NOT-XXXXXXXXXXXX

**Recommendation:** Extract to utility class `EntityIdGenerator`

---

## Pattern 2: ApiResponse Wrapper
**Found in:** All controllers

**Pattern:**
```java
@PostMapping
public ResponseEntity<ApiResponse<{Entity}Response>> create{Entity}(
    @Valid @RequestBody Create{Entity}Request request
) {
    {Entity}Response response = {entity}Service.create{Entity}(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "{Entity} created successfully"));
}
```

**Consistency Check:**
- ✅ Account Service: Using pattern
- ✅ Transfer Service: Using pattern
- ✅ Customer Service: Using pattern
- ⏳ Auth Service: Using pattern

---

## Pattern 3: Exception Handling
**Found in:** All services

**Pattern:**
```java
// Custom exception
public class {Entity}NotFoundException extends RuntimeException {
    public {Entity}NotFoundException(String {entity}Id) {
        super("{Entity} not found: " + {entity}Id);
    }
}

// GlobalExceptionHandler
@ExceptionHandler({Entity}NotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handle{Entity}NotFound(
    {Entity}NotFoundException ex
) {
    log.error("{Entity} not found: {}", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
}
```

---

## Pattern 4: Feign Client with Circuit Breaker
**Found in:** Transfer Service, Customer Service

**Pattern:**
```java
@FeignClient(
    name = "{service}-service",
    fallbackFactory = {Service}ClientFallbackFactory.class
)
public interface {Service}Client {
    @GetMapping("/api/v1/{resource}/{id}")
    ApiResponse<{Resource}Response> get{Resource}(
        @PathVariable("id") String id
    );
}

@Component
public class {Service}ClientFallbackFactory
    implements FallbackFactory<{Service}Client> {

    @Override
    public {Service}Client create(Throwable cause) {
        return new {Service}Client() {
            @Override
            public ApiResponse<{Resource}Response> get{Resource}(String id) {
                log.error("{Service} unavailable: {}", cause.getMessage());
                throw new ServiceUnavailableException(
                    "{Service} temporarily unavailable"
                );
            }
        };
    }
}
```

**Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      {service}Client:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
```

---

## Pattern 5: Kafka Event Publishing
**Found in:** All services

**Pattern:**
```java
// Event class
@Data
@Builder
public class {Entity}{Action}Event {
    private String eventId;
    private String {entity}Id;
    private LocalDateTime occurredAt;
    // ... entity-specific fields
}

// Publisher method in service
private void publish{Entity}{Action}({Entity} {entity}) {
    {Entity}{Action}Event event = {Entity}{Action}Event.builder()
        .eventId(UUID.randomUUID().toString())
        .{entity}Id({entity}.get{Entity}Id())
        .occurredAt(LocalDateTime.now())
        .build();

    kafkaTemplate.send(
        "{service}.events",
        {entity}.get{Entity}Id(),
        event
    );

    log.info("Published {Entity}{Action}Event: {}", {entity}.get{Entity}Id());
}
```

## Pattern Application

When creating a new entity "Notification":

1. ✅ ID generation: `NOT-XXXXXXXXXXXX`
2. ✅ ApiResponse wrapper in controller
3. ✅ Custom exceptions: NotificationNotFoundException
4. ✅ GlobalExceptionHandler entry
5. ✅ Events: NotificationSentEvent, NotificationFailedEvent
6. ✅ Kafka publisher in service
7. ✅ Feign client with circuit breaker (if needed)
```

---

## 3. ConsistencyAgent ✅

**Objective:** Enforce naming conventions, package structure, and code formatting.

**Consistency Checks:**

```markdown
# Consistency Audit: Notification Service

## Package Structure
✅ COMPLIANT

```
com.banking.notification/
├── config/          ✅ (4 files)
├── controller/      ✅ (2 files)
├── dto/            ✅ (8 files)
│   ├── request/    ✅ (3 files)
│   └── response/   ✅ (2 files)
├── model/          ✅ (3 files)
│   └── enums/      ✅ (2 files)
├── repository/     ✅ (2 files)
├── service/        ✅ (4 files)
│   └── impl/       ✅ (3 files)
├── event/          ✅ (2 files)
└── exception/      ✅ (5 files)
```

## Naming Conventions

### Classes (PascalCase)
✅ NotificationService
✅ NotificationController
✅ NotificationRepository
❌ notificationService (should be NotificationService)

### Methods (camelCase)
✅ sendNotification()
✅ getNotification()
✅ retryNotification()
❌ SendNotification() (should be sendNotification)

### Constants (UPPER_SNAKE_CASE)
✅ MAX_RETRY_COUNT
✅ DEFAULT_CHANNEL
❌ maxRetryCount (should be MAX_RETRY_COUNT)

### Variables (camelCase)
✅ notificationId
✅ sentAt
❌ NotificationId (should be notificationId)

## Import Organization

### Required Order:
1. java.*
2. javax.*
3. org.springframework.*
4. Third-party libraries
5. com.banking.*

**Current Status:** ✅ COMPLIANT

## Code Formatting

### Indentation
✅ 4 spaces (not tabs)

### Line Length
⚠️ 3 violations (> 120 characters)
- NotificationServiceImpl.java:67
- NotificationController.java:89
- KafkaConfig.java:45

### Blank Lines
✅ Between methods
✅ Between logical blocks
✅ Before return statements

## Lombok Usage

### Recommended Annotations
✅ @Data (entity classes)
✅ @Builder (DTOs, entities)
✅ @Slf4j (all classes needing logging)
✅ @NoArgsConstructor, @AllArgsConstructor

### Avoid
❌ @ToString on entities with circular references
✅ Use @ToString(exclude = {"relationship"})

## Auto-Fix Suggestions

```java
// File: NotificationServiceImpl.java:67
// Issue: Line too long (145 characters)

// ❌ BEFORE
NotificationResponse response = notificationMapper.toResponse(notificationRepository.findByNotificationId(notificationId).orElseThrow(() -> new NotificationNotFoundException(notificationId)));

// ✅ AFTER
Notification notification = notificationRepository
    .findByNotificationId(notificationId)
    .orElseThrow(() -> new NotificationNotFoundException(notificationId));
NotificationResponse response = notificationMapper.toResponse(notification);
```

## Documentation

### Javadoc Required On:
✅ Public methods (interfaces)
⚠️ Missing on 3 public methods
- NotificationService.retryNotification()
- TemplateService.validateTemplate()
- EmailChannelHandler.send()

**Recommendation:**
```java
/**
 * Retries sending a failed notification
 *
 * @param notificationId The notification ID to retry
 * @return Updated notification response
 * @throws NotificationNotFoundException if notification not found
 * @throws MaxRetriesExceededException if max retries exceeded
 */
public NotificationResponse retryNotification(String notificationId) {
    // ...
}
```

## Summary

| Category | Compliant | Violations | Score |
|----------|-----------|------------|-------|
| Package Structure | ✅ | 0 | 100% |
| Naming Conventions | ✅ | 2 | 98% |
| Import Organization | ✅ | 0 | 100% |
| Code Formatting | ⚠️ | 3 | 97% |
| Lombok Usage | ✅ | 0 | 100% |
| Documentation | ⚠️ | 3 | 95% |

**Overall Consistency Score: 98%** ✅ EXCELLENT
```

---

## Integration Example

**How agents work together:**

```
User: "Create Notification Service"
    ↓
ServicePlannerAgent: Create 5-day plan
    ↓
ProjectContextAgent: Load project context (patterns, standards)
    ↓
EntityDesignAgent: Create Notification entity (following pattern)
    ↓
PatternMatcherAgent: Verify ID generation pattern used
    ↓
APIDesignAgent: Create REST API (following ApiResponse pattern)
    ↓
EventDrivenAgent: Create Kafka events (following event pattern)
    ↓
UnitTestAgent: Generate tests (80%+ coverage)
    ↓
ConsistencyAgent: Check naming, formatting, package structure
    ↓
CodeReviewAgent: Final quality check
    ↓
✅ Ready for deployment
```

---

## Summary

Context & Pattern agents ensure consistency:

- **ProjectContextAgent:** Provides comprehensive project context
- **PatternMatcherAgent:** Detects and enforces code patterns
- **ConsistencyAgent:** Maintains naming and formatting standards

Together, they ensure all generated code follows Banking Platform standards and patterns established in existing services.

---

**End of Agent Documentation**

**Back to:** [Main Agent Catalog →](../AGENTS.md)
