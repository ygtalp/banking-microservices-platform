# Task Breakdown Agents

> **Category:** Task Decomposition & Planning
> **Agent Count:** 3
> **Automation Level:** Medium (60-80%)
> **Last Updated:** 28 December 2025

---

## Overview

Task Breakdown agents decompose complex development tasks into manageable, actionable steps. They analyze requirements, identify dependencies, and create structured implementation plans following the Banking Platform's established patterns.

---

## 1. ServiceImplementationAgent 🏗️

### Specification

**ID:** `AGENT-TB-001`
**Name:** ServiceImplementationAgent
**Category:** Task Breakdown
**Scope:** End-to-end microservice implementation
**Automation Level:** Medium (70%)

### Objective

Create comprehensive implementation plans for new microservices following the 12-phase pattern established by Customer Service and the 5-day pattern used for Auth Service.

### Capabilities

#### Input Parameters
- Service name (e.g., "Notification Service", "Transaction History Service")
- Service purpose/description
- Key requirements
- Integration points (which services to integrate with)
- Timeline constraints (optional)

#### Processing Logic

1. **Analyze Project Context**
   - Read CLAUDE.md for project standards
   - Review existing services (Account, Transfer, Customer, Auth)
   - Extract common patterns
   - Identify reusable components

2. **Create Domain Model**
   - Entity identification
   - Relationship mapping
   - Event identification
   - API endpoint planning

3. **Generate Phase Breakdown**
   - 12 phases for complex services
   - 5-day breakdown for standard services
   - Each phase with:
     - Objectives
     - Files to create
     - Dependencies
     - Estimated time
     - Success criteria

4. **Dependency Mapping**
   - Database dependencies
   - Service dependencies
   - Infrastructure dependencies
   - External integrations

5. **Timeline Estimation**
   - Per-phase time estimates
   - Critical path analysis
   - Buffer allocation
   - Milestone identification

#### Output Artifacts

1. **Implementation Plan Document** (Markdown)
   ```markdown
   # {Service Name} Implementation Plan

   ## Overview
   - Purpose
   - Scope
   - Timeline

   ## Architecture
   - Domain Model Diagram
   - Integration Points
   - Event Flow

   ## Implementation Phases
   ### Phase 1: Foundation
   - Database Schema
   - Entity Classes
   - Repository Interfaces

   ### Phase 2: Core Business Logic
   ...

   ## Timeline
   - Day-by-day breakdown
   - Milestones
   - Dependencies
   ```

2. **Task Checklist**
   ```
   □ Database schema design
   □ Entity classes
   □ Repository interfaces
   □ Service layer
   □ Controller layer
   ...
   ```

3. **File Structure Plan**
   ```
   {service}-service/
   ├── src/main/java/com/banking/{service}/
   │   ├── config/
   │   ├── controller/
   │   ├── dto/
   │   ├── model/
   │   ├── repository/
   │   ├── service/
   │   ├── event/
   │   └── exception/
   └── src/test/java/...
   ```

4. **Integration Points Document**
   - Feign clients needed
   - Kafka events to produce/consume
   - Circuit breaker configuration
   - API Gateway routes

### Usage Examples

#### Example 1: Notification Service

**Prompt:**
```
Create a complete implementation plan for Notification Service with the following requirements:
- Multi-channel notifications (Email, SMS, Push)
- Template management
- Event-driven triggers from other services
- Delivery status tracking
- User preferences

Follow the Banking Platform patterns. Create a 5-day implementation plan.
```

**Expected Output:**
```markdown
# Notification Service - 5-Day Implementation Plan

## Service Overview
**Port:** 8085
**Database:** banking_notifications
**Pattern:** Event-Driven, Template Engine

## Day 1: Foundation & Database (8 hours)

### Objectives
- Database schema design
- Entity classes
- Repository layer
- Basic configuration

### Files to Create (15 files)
1. Database Schema:
   - notifications table
   - templates table
   - user_preferences table
   - delivery_log table

2. Entities:
   - Notification.java
   - NotificationTemplate.java
   - UserPreference.java
   - DeliveryLog.java

3. Repositories:
   - NotificationRepository.java
   - TemplateRepository.java
   - UserPreferenceRepository.java
   - DeliveryLogRepository.java

### Dependencies
- PostgreSQL (banking_notifications database)
- Liquibase
- Spring Data JPA

### Success Criteria
- Database schema created
- All entities mapped correctly
- Repository tests passing

## Day 2: Core Business Logic (8 hours)

### Objectives
- Service layer implementation
- Channel handlers (Email, SMS, Push)
- Template engine integration

### Files to Create (12 files)
1. Services:
   - NotificationService.java
   - TemplateService.java
   - PreferenceService.java

2. Channel Handlers:
   - EmailChannelHandler.java (SendGrid)
   - SmsChannelHandler.java (Twilio)
   - PushChannelHandler.java (Firebase)

3. Template Engine:
   - TemplateRenderer.java (Thymeleaf)
   - TemplateValidator.java

### Integration Points
- SendGrid API (email)
- Twilio API (SMS)
- Firebase Cloud Messaging (push)

### Success Criteria
- All channels working
- Template rendering functional
- Service layer tests 80%+

## Day 3: API & Event Integration (8 hours)

### Objectives
- REST API endpoints
- Kafka event consumers
- DTOs and validation

### Files to Create (10 files)
1. Controllers:
   - NotificationController.java
   - TemplateController.java
   - PreferenceController.java

2. DTOs:
   - SendNotificationRequest.java
   - NotificationResponse.java
   - TemplateRequest.java
   - PreferenceRequest.java

3. Kafka Consumers:
   - AccountEventConsumer.java
   - TransferEventConsumer.java
   - CustomerEventConsumer.java

### Event Subscriptions
- account.events (AccountCreatedEvent)
- transfer.events (TransferCompletedEvent)
- customer.events (CustomerApprovedEvent)

### API Endpoints
- POST /notifications/send
- GET /notifications/{id}
- POST /templates
- GET /templates/{id}
- PUT /preferences/{userId}

### Success Criteria
- All endpoints working
- Event consumers processing correctly
- API tests passing

## Day 4: Testing & Quality (6 hours)

### Objectives
- Comprehensive testing
- Code review
- Security audit

### Files to Create (8 files)
1. Unit Tests:
   - NotificationServiceTest.java
   - TemplateServiceTest.java
   - EmailChannelHandlerTest.java

2. Integration Tests:
   - NotificationIntegrationTest.java (TestContainers)
   - EventConsumerTest.java

3. API Tests:
   - test-notification-service.ps1

### Success Criteria
- 80%+ test coverage
- All tests passing
- No security vulnerabilities

## Day 5: Docker & Deployment (4 hours)

### Objectives
- Docker containerization
- Deployment configuration
- Documentation

### Files to Create (5 files)
1. Docker:
   - Dockerfile
   - docker-compose service definition

2. Scripts:
   - build-notification-service.ps1
   - deploy-notification-service.ps1

3. Documentation:
   - docs/NOTIFICATION_SERVICE.md

### Success Criteria
- Docker image built
- Service deployed and healthy
- Documentation complete

## Total Estimation
- **Time:** 34 hours (~5 days)
- **Files:** 50+ files
- **Tests:** 30+ tests
- **Endpoints:** 10+ API endpoints
- **Events:** 3+ event consumers

## Dependencies
- PostgreSQL 16
- Redis 7.2
- Kafka 3.6
- Eureka Server
- SendGrid API key
- Twilio API key
- Firebase credentials

## Risks & Mitigation
1. **Risk:** Third-party API rate limits
   **Mitigation:** Implement retry with exponential backoff

2. **Risk:** Template rendering performance
   **Mitigation:** Cache compiled templates (Redis)

3. **Risk:** High notification volume
   **Mitigation:** Async processing with Kafka
```

#### Example 2: Transaction History Service

**Prompt:**
```
Create implementation plan for Transaction History Service:
- Complete audit trail for all transactions
- Event sourcing pattern
- Advanced filtering and search
- Export capabilities (CSV, PDF)
- Real-time balance calculation

Use 12-phase pattern like Customer Service.
```

**Expected Output:** (Similar detailed 12-phase plan)

### Best Practices

#### When to Use ServiceImplementationAgent

✅ **DO Use When:**
- Starting a new microservice from scratch
- Need comprehensive planning
- Multiple developers working on service
- Complex domain model
- Many integration points

❌ **DON'T Use When:**
- Simple utility service (< 5 files)
- Prototype/POC development
- One-off script
- Modifying existing service (use FeatureImplementationAgent)

#### Tips for Better Results

1. **Provide Clear Requirements**
   - List all functional requirements
   - Specify integration points
   - Mention special constraints

2. **Reference Existing Services**
   - "Follow Account Service pattern"
   - "Similar to Customer Service structure"

3. **Specify Complexity Level**
   - "Simple CRUD service" → 5-day plan
   - "Complex workflow service" → 12-phase plan

4. **Include Non-Functional Requirements**
   - Performance (TPS, latency)
   - Security level
   - Compliance needs

### Integration with Other Agents

**Workflow:**
```
ServiceImplementationAgent (Planning)
    ↓ [Implementation Plan]
EntityDesignAgent (Domain Model)
    ↓ [Entity Classes]
APIDesignAgent (REST Layer)
    ↓ [Controllers, DTOs]
EventDrivenAgent (Kafka Integration)
    ↓ [Event Classes]
UnitTestAgent (Testing)
    ↓ [Test Classes]
DockerAgent (Deployment)
    ↓ [Dockerfile]
SessionLogAgent (Documentation)
```

---

## 2. FeatureImplementationAgent ✨

### Specification

**ID:** `AGENT-TB-002`
**Name:** FeatureImplementationAgent
**Category:** Task Breakdown
**Scope:** Feature addition to existing microservices
**Automation Level:** Medium (65%)

### Objective

Decompose feature requests into implementation tasks for existing services, identifying affected files, required changes, and test updates.

### Capabilities

#### Input Parameters
- Target service name
- Feature description
- Acceptance criteria
- Priority level (optional)

#### Processing Logic

1. **Feature Analysis**
   - Parse feature requirements
   - Identify affected layers (controller, service, repository)
   - Determine scope (localized vs cross-cutting)
   - Assess complexity

2. **Impact Assessment**
   - Identify files to modify
   - Detect breaking changes
   - Check backward compatibility
   - Estimate effort

3. **Task Breakdown**
   - Database changes (if needed)
   - Business logic changes
   - API changes
   - Event changes
   - Test updates

4. **Test Strategy**
   - Unit test updates
   - Integration test updates
   - API test updates
   - Regression test identification

5. **Migration Planning**
   - Database migration scripts
   - Data migration (if needed)
   - Feature flag strategy
   - Rollback plan

#### Output Artifacts

1. **Feature Implementation Plan**
   ```markdown
   # Feature: {Feature Name}

   ## Overview
   - Description
   - Acceptance Criteria
   - Priority

   ## Impact Analysis
   ### Files to Modify
   - AccountService.java (lines 45-67)
   - AccountController.java (new endpoint)
   - Account.java (new field: lastAccessedAt)

   ### Files to Create
   - AccountActivityRequest.java (DTO)
   - AccountActivityResponse.java (DTO)

   ## Implementation Tasks
   1. Database Migration
      - Add last_accessed_at column
      - Create account_activity table

   2. Domain Model Update
      - Add lastAccessedAt field to Account entity
      - Create AccountActivity entity

   3. Service Layer
      - Add trackActivity() method
      - Update getAccount() to record access

   4. Controller Layer
      - Add GET /accounts/{id}/activity endpoint

   5. Events
      - Publish AccountAccessedEvent

   ## Test Updates
   - AccountServiceTest: Add testTrackActivity()
   - AccountControllerTest: Add testGetActivity()
   - Integration: Full activity tracking flow

   ## Estimated Effort
   - Database: 1 hour
   - Implementation: 4 hours
   - Testing: 2 hours
   - Total: 7 hours
   ```

2. **Database Migration Script**
   ```xml
   <!-- Liquibase changelog -->
   <changeSet id="add-account-activity-tracking" author="system">
     <addColumn tableName="accounts">
       <column name="last_accessed_at" type="timestamp"/>
     </addColumn>
     <createTable tableName="account_activity">
       ...
     </createTable>
   </changeSet>
   ```

3. **Event Schema**
   ```java
   @Data
   @Builder
   public class AccountAccessedEvent {
       private String accountNumber;
       private String userId;
       private LocalDateTime accessedAt;
       private String accessType; // VIEW, UPDATE, etc.
   }
   ```

4. **Test Scenarios**
   ```
   Scenario 1: Track account view
   GIVEN account exists
   WHEN user views account
   THEN activity is recorded
   AND lastAccessedAt is updated

   Scenario 2: Retrieve activity history
   GIVEN account has 10 activities
   WHEN user requests activity
   THEN activities are returned in descending order
   ```

### Usage Examples

#### Example 1: Add Multi-Currency Support to Account Service

**Prompt:**
```
Add multi-currency support to Account Service with the following features:
- Account can have multiple currency balances
- Currency conversion on the fly
- Exchange rate integration
- Transaction history per currency

Analyze impact and create implementation plan.
```

**Expected Output:**
```markdown
# Feature: Multi-Currency Account Support

## Impact Analysis

### High Impact (Breaking Changes)
- Account.java: Replace single balance with Map<Currency, BigDecimal>
- AccountResponse.java: Add balances field (Map)
- Database schema: New account_balances table

### Medium Impact (Non-Breaking)
- AccountService.java: Add currency conversion logic
- TransferService.java: Update to handle currency conversion
- New ExchangeRateService.java

### Low Impact (Additions Only)
- New ExchangeRateController.java
- New CurrencyConversionRequest.java

## Implementation Plan

### Phase 1: Database Schema (2 hours)
**Files to Create:**
1. Migration: 005-add-multi-currency-support.xml

**Schema Changes:**
```sql
-- Keep accounts.balance for backward compatibility (default currency)
-- Add new table for multi-currency balances
CREATE TABLE account_balances (
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT NOT NULL REFERENCES accounts(id),
  currency VARCHAR(3) NOT NULL,
  balance DECIMAL(19,2) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_account_currency UNIQUE (account_id, currency)
);
```

### Phase 2: Domain Model (3 hours)
**Files to Modify:**
1. Account.java
   - Add: @OneToMany Set<AccountBalance> balances
   - Add: getBalance(Currency currency) method
   - Keep: balance field (for backward compatibility)

**Files to Create:**
1. AccountBalance.java (new entity)
```java
@Entity
@Table(name = "account_balances")
@Data
public class AccountBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    // timestamps, etc.
}
```

2. ExchangeRate.java (new entity)
3. AccountBalanceRepository.java

### Phase 3: Exchange Rate Service (4 hours)
**Files to Create:**
1. ExchangeRateService.java
   - Integration with external API (e.g., fixer.io)
   - Caching (Redis, 1-hour TTL)
   - Fallback rates

2. ExchangeRateProvider.java (interface)
3. FixerExchangeRateProvider.java (implementation)
4. CachedExchangeRateProvider.java (decorator)

**Configuration:**
```yaml
exchange-rate:
  provider: fixer
  api-key: ${FIXER_API_KEY}
  cache-ttl: 3600
  base-currency: EUR
```

### Phase 4: Account Service Update (5 hours)
**Files to Modify:**
1. AccountService.java
   - Add: credit(accountNumber, amount, currency)
   - Add: debit(accountNumber, amount, currency)
   - Add: convertBalance(from, to, amount)
   - Update: createAccount() to initialize default currency balance

**Backward Compatibility:**
- Keep existing methods
- Add overloaded methods with currency parameter
- Default to TRY if currency not specified

### Phase 5: API Layer (3 hours)
**Files to Create:**
1. DTOs:
   - CurrencyBalanceResponse.java
   - ConvertCurrencyRequest.java
   - ConvertCurrencyResponse.java

**Files to Modify:**
1. AccountController.java
   - Add: GET /accounts/{id}/balances (all currencies)
   - Add: GET /accounts/{id}/balance/{currency}
   - Add: POST /accounts/{id}/convert (currency conversion)

**New Endpoints:**
```java
@GetMapping("/{accountNumber}/balances")
public ResponseEntity<ApiResponse<Map<Currency, BigDecimal>>> getBalances(
    @PathVariable("accountNumber") String accountNumber
);

@PostMapping("/{accountNumber}/convert")
public ResponseEntity<ApiResponse<ConvertCurrencyResponse>> convertCurrency(
    @PathVariable("accountNumber") String accountNumber,
    @Valid @RequestBody ConvertCurrencyRequest request
);
```

### Phase 6: Event Updates (2 hours)
**Files to Create:**
1. CurrencyConvertedEvent.java
```java
public class CurrencyConvertedEvent {
    private String accountNumber;
    private Currency fromCurrency;
    private Currency toCurrency;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private BigDecimal exchangeRate;
    private LocalDateTime convertedAt;
}
```

**Files to Modify:**
1. BalanceChangedEvent.java
   - Add: Currency currency field

### Phase 7: Testing (6 hours)
**Unit Tests:**
1. AccountServiceTest.java
   - testCreditWithCurrency()
   - testDebitWithCurrency()
   - testConvertCurrency()
   - testGetBalanceInCurrency()

2. ExchangeRateServiceTest.java
   - testFetchExchangeRate()
   - testCacheHit()
   - testCacheMiss()
   - testFallbackRate()

**Integration Tests:**
1. MultiCurrencyIntegrationTest.java
   - testCreateAccountWithMultipleCurrencies()
   - testTransferWithCurrencyConversion()
   - testBalanceRetrievalAllCurrencies()

**API Tests:**
1. test-multi-currency.ps1

### Phase 8: Documentation (2 hours)
**Files to Update:**
1. docs/ACCOUNT_SERVICE.md
   - Add multi-currency section
   - Document new endpoints
   - Add currency conversion examples

## Migration Strategy

### Database Migration
```sql
-- Step 1: Add new table (non-breaking)
CREATE TABLE account_balances ...

-- Step 2: Migrate existing balances
INSERT INTO account_balances (account_id, currency, balance)
SELECT id, 'TRY', balance FROM accounts;

-- Step 3: (Future) Remove accounts.balance column
-- Only after all services migrated
```

### Feature Flag
```yaml
features:
  multi-currency: true  # Enable/disable feature
```

### Rollback Plan
1. Disable feature flag
2. Keep old API endpoints functional
3. account_balances table can be dropped
4. No data loss (accounts.balance still populated)

## Testing Strategy

### Unit Tests (15 tests)
- Currency conversion logic
- Balance operations per currency
- Exchange rate caching
- Backward compatibility

### Integration Tests (8 tests)
- Multi-currency account creation
- Cross-currency transfers
- Balance retrieval
- Exchange rate updates

### API Tests (10 scenarios)
- Create account with EUR
- Credit in USD
- Debit in GBP
- Convert EUR to USD
- Get all balances
- Error handling (unsupported currency)

## Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Exchange rate API down | High | Medium | Cache + fallback rates |
| Precision loss in conversion | High | Low | Use BigDecimal, 6 decimal places |
| Performance (multiple DB queries) | Medium | Medium | Eager loading, caching |
| Backward compatibility break | High | Low | Keep old API, feature flag |

## Estimated Effort
- Database: 2 hours
- Domain Model: 3 hours
- Exchange Rate Service: 4 hours
- Account Service: 5 hours
- API Layer: 3 hours
- Events: 2 hours
- Testing: 6 hours
- Documentation: 2 hours
- **Total: 27 hours (~3.5 days)**

## Dependencies
- External Exchange Rate API (fixer.io)
- Redis (for caching rates)
- No changes to other services (backward compatible)

## Acceptance Criteria
- [ ] Account can have balances in TRY, USD, EUR, GBP
- [ ] Currency conversion works with live exchange rates
- [ ] All existing tests still pass
- [ ] 80%+ test coverage for new code
- [ ] API documentation updated
- [ ] No breaking changes to existing API
```

#### Example 2: Add Scheduled Transfers to Transfer Service

**Prompt:**
```
Add scheduled/recurring transfers to Transfer Service:
- One-time scheduled transfer (future date)
- Recurring transfers (daily, weekly, monthly)
- Cancel scheduled transfer
- Modify scheduled transfer
- Execution tracking
```

**Expected Output:** (Similar detailed feature plan)

### Best Practices

#### When to Use FeatureImplementationAgent

✅ **DO Use When:**
- Adding significant feature to existing service
- Need impact analysis
- Multiple files affected
- Database changes required
- Want structured implementation plan

❌ **DON'T Use When:**
- Simple bug fix (1-2 files)
- Configuration change only
- Documentation update
- Refactoring without new functionality

#### Tips for Better Results

1. **Be Specific About Scope**
   - Clearly define what's in/out of scope
   - Provide acceptance criteria
   - Mention constraints

2. **Request Impact Analysis**
   - "Analyze impact on existing code"
   - "List all files to be modified"
   - "Identify breaking changes"

3. **Ask for Migration Strategy**
   - "How to migrate existing data?"
   - "Backward compatibility plan?"
   - "Feature flag strategy?"

### Integration with Other Agents

**After FeatureImplementationAgent:**
```
FeatureImplementationAgent (Planning)
    ↓
PatternMatcherAgent (Extract similar patterns)
    ↓
[Code Generation Agents] (Parallel execution)
    ├─ EntityDesignAgent (if DB changes)
    ├─ APIDesignAgent (if new endpoints)
    └─ EventDrivenAgent (if new events)
    ↓
TestFixAgent (Update tests)
    ↓
CodeReviewAgent (Review changes)
    ↓
DeploymentAgent (Deploy with feature flag)
```

---

## 3. IntegrationAgent 🔗

### Specification

**ID:** `AGENT-TB-003`
**Name:** IntegrationAgent
**Category:** Task Breakdown
**Scope:** Service-to-service integration planning
**Automation Level:** Medium (65%)

### Objective

Plan and document integration between microservices, covering synchronous (Feign) and asynchronous (Kafka) communication patterns.

### Capabilities

#### Input Parameters
- Source service
- Target service
- Integration type (Feign, Kafka, or both)
- Use cases (what operations need integration)

#### Processing Logic

1. **Integration Point Analysis**
   - Identify required data exchanges
   - Determine communication pattern (sync vs async)
   - Map use cases to integration points
   - Check existing integrations

2. **Feign Client Design**
   - Define Feign interface
   - Circuit breaker configuration
   - Fallback strategy
   - Timeout settings
   - Retry policy

3. **Event Flow Design**
   - Identify events to produce
   - Identify events to consume
   - Define event schemas
   - Determine topic structure
   - Handle event versioning

4. **Error Handling Strategy**
   - Timeout scenarios
   - Circuit breaker open
   - Event processing failures
   - Dead letter queue
   - Retry logic

5. **Testing Strategy**
   - Integration test scenarios
   - WireMock for Feign testing
   - Embedded Kafka for event testing
   - Contract testing

#### Output Artifacts

1. **Integration Architecture Document**
   ```markdown
   # Customer Service ↔ Account Service Integration

   ## Overview
   - Source: Customer Service
   - Target: Account Service
   - Patterns: Feign (sync), Kafka (async)

   ## Use Cases
   1. Get customer accounts (Feign)
   2. React to customer approval (Kafka)
   3. Suspend customer accounts (Kafka)

   ## Integration Points
   ### Synchronous (Feign)
   - GET /accounts/customer/{customerId}
   - Circuit breaker: 50% threshold
   - Timeout: 5s

   ### Asynchronous (Kafka)
   - Produce: CustomerApprovedEvent
   - Produce: CustomerSuspendedEvent
   - Consume: (none from Account Service)

   ## Error Handling
   - Feign timeout → Return empty list
   - Circuit open → Cached data (5min)
   - Event failure → DLQ + alert
   ```

2. **Feign Client Code**
   ```java
   @FeignClient(
       name = "account-service",
       fallbackFactory = AccountServiceFallbackFactory.class
   )
   public interface AccountServiceClient {

       @GetMapping("/api/v1/accounts/customer/{customerId}")
       ApiResponse<List<AccountResponse>> getAccountsByCustomerId(
           @PathVariable("customerId") String customerId
       );
   }

   @Component
   public class AccountServiceFallbackFactory
       implements FallbackFactory<AccountServiceClient> {

       @Override
       public AccountServiceClient create(Throwable cause) {
           return customerId -> {
               log.error("Account Service unavailable: {}", cause.getMessage());
               return ApiResponse.error("Account Service temporarily unavailable");
           };
       }
   }
   ```

3. **Circuit Breaker Configuration**
   ```yaml
   resilience4j:
     circuitbreaker:
       instances:
         accountService:
           sliding-window-size: 10
           failure-rate-threshold: 50
           wait-duration-in-open-state: 60s
           permitted-number-of-calls-in-half-open-state: 3
           automatic-transition-from-open-to-half-open-enabled: true
   ```

4. **Event Producer Code**
   ```java
   @Service
   @Slf4j
   public class CustomerEventPublisher {

       private final KafkaTemplate<String, Object> kafkaTemplate;

       public void publishCustomerApproved(Customer customer) {
           CustomerApprovedEvent event = CustomerApprovedEvent.builder()
               .customerId(customer.getCustomerId())
               .email(customer.getEmail())
               .approvedBy(customer.getApprovedBy())
               .approvedAt(customer.getApprovedAt())
               .riskLevel(customer.getRiskLevel())
               .build();

           kafkaTemplate.send("customer.events", customer.getCustomerId(), event);
           log.info("Published CustomerApprovedEvent: {}", customer.getCustomerId());
       }
   }
   ```

5. **Event Consumer Code** (in Account Service)
   ```java
   @Component
   @Slf4j
   public class CustomerEventConsumer {

       private final AccountService accountService;

       @KafkaListener(
           topics = "customer.events",
           groupId = "account-service-group"
       )
       public void handleCustomerEvent(CustomerApprovedEvent event) {
           log.info("Received CustomerApprovedEvent: {}", event.getCustomerId());

           try {
               // Business logic: Enable account creation for approved customer
               accountService.enableAccountCreationForCustomer(event.getCustomerId());
           } catch (Exception e) {
               log.error("Failed to process CustomerApprovedEvent", e);
               // Will be retried or sent to DLQ
               throw e;
           }
       }
   }
   ```

6. **Integration Test**
   ```java
   @SpringBootTest
   @Testcontainers
   class CustomerAccountIntegrationTest {

       @Container
       static MockServerContainer mockServer = new MockServerContainer();

       @Test
       void shouldFetchCustomerAccountsViaFeign() {
           // Setup WireMock
           stubFor(get(urlEqualTo("/api/v1/accounts/customer/CUS-123"))
               .willReturn(aResponse()
                   .withHeader("Content-Type", "application/json")
                   .withBody("{\"data\": [...]}")));

           // Test Feign client
           List<AccountResponse> accounts =
               customerService.getCustomerAccounts("CUS-123");

           assertThat(accounts).isNotEmpty();
       }

       @Test
       void shouldPublishCustomerApprovedEvent() {
           // Approve customer
           customerService.approveCustomer("CUS-123", "manager@bank.com");

           // Verify event published
           ConsumerRecords<String, CustomerApprovedEvent> records =
               kafkaConsumer.poll(Duration.ofSeconds(5));

           assertThat(records.count()).isEqualTo(1);
       }
   }
   ```

### Usage Examples

#### Example 1: Customer Service → Account Service Integration

**Prompt:**
```
Plan integration between Customer Service and Account Service:

Synchronous needs:
- Customer Service needs to fetch all accounts for a customer
- Display account summary in customer profile

Asynchronous needs:
- When customer is approved → Account Service should allow account creation
- When customer is suspended → Account Service should suspend all accounts

Create complete integration plan.
```

**Expected Output:** (See artifacts above)

#### Example 2: Auth Service → All Services Integration

**Prompt:**
```
Plan integration of Auth Service with Account, Transfer, and Customer services:

Requirements:
- All services must validate JWT tokens
- All services must check permissions (RBAC)
- Services should NOT call Auth Service on every request (stateless)
- Spring Security setup in each service

Create integration plan and sample code for one service.
```

**Expected Output:**
```markdown
# Auth Service → Platform-Wide Integration

## Architecture

### Stateless JWT Validation (No Feign Calls)
```
Request → API Gateway → [Downstream Service]
   │                           │
   JWT ──────────────────────► JWT Filter
                               ├─ Validate signature (shared secret)
                               ├─ Check expiration
                               ├─ Check blacklist (Redis)
                               └─ Extract roles
                                  │
                                  ▼
                              @PreAuthorize check
                                  │
                                  ▼
                              Controller method
```

## Integration Plan

### Phase 1: Add Dependencies (All Services)
**pom.xml additions:**
```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Redis (for token blacklist check) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Phase 2: Shared JWT Configuration
**Each service needs:**

1. **JwtConfig.java**
```java
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    private String secret;  // Must match Auth Service secret
    private Long accessTokenExpiration = 900000L; // 15 min
}
```

2. **application.yml**
```yaml
jwt:
  secret: ${JWT_SECRET:SameSecretAsAuthService}
  access-token-expiration: 900000
```

3. **JwtTokenValidator.java**
```java
@Component
public class JwtTokenValidator {

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtTokenValidator(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(
            jwtConfig.getSecret().getBytes()
        );
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    public List<String> getRolesFromToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("roles", List.class);
    }
}
```

4. **JwtAuthenticationFilter.java**
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator jwtTokenValidator;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtTokenValidator.validateToken(token)) {
            // Check if token is blacklisted
            Boolean isBlacklisted = redisTemplate
                .hasKey("token:blacklist:" + token);

            if (Boolean.FALSE.equals(isBlacklisted)) {
                // Extract user details
                String username = jwtTokenValidator.getUsernameFromToken(token);
                List<String> roles = jwtTokenValidator.getRolesFromToken(token);

                // Create authentication
                List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        username, null, authorities
                    );

                SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

5. **SecurityConfig.java**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
```

### Phase 3: Controller Updates
**Add @PreAuthorize annotations:**

```java
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    // Public (no auth required) - None in banking!

    // Customer role (own accounts only)
    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
        @PathVariable("accountNumber") String accountNumber,
        @AuthenticationPrincipal String username // Current user
    ) {
        // Verify user owns this account
        // ...
    }

    // Admin role
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {
        // Only admins can see all accounts
    }

    // Manager role
    @PostMapping("/{accountNumber}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<AccountResponse>> approveAccount(
        @PathVariable("accountNumber") String accountNumber
    ) {
        // Only managers can approve accounts
    }
}
```

### Phase 4: Testing
**1. Security Test Configuration**
```java
@TestConfiguration
public class SecurityTestConfig {

    @Bean
    @Primary
    public JwtTokenValidator mockJwtTokenValidator() {
        JwtTokenValidator mock = mock(JwtTokenValidator.class);
        when(mock.validateToken(anyString())).thenReturn(true);
        when(mock.getUsernameFromToken(anyString()))
            .thenReturn("test@example.com");
        when(mock.getRolesFromToken(anyString()))
            .thenReturn(List.of("ROLE_CUSTOMER"));
        return mock;
    }
}
```

**2. Controller Test with JWT**
```java
@WebMvcTest(AccountController.class)
@Import(SecurityTestConfig.class)
class AccountControllerSecurityTest {

    @Test
    void shouldAllowAccessWithValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/123")
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/123"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCanAccessOwnAccount() throws Exception {
        // Test customer role access
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAllAccounts() throws Exception {
        // Test admin role access
    }
}
```

### Phase 5: API Gateway Integration
**Gateway should:**
1. Validate JWT (same logic)
2. Pass JWT to downstream services
3. Add user context headers (optional)

```yaml
# API Gateway configuration
spring:
  cloud:
    gateway:
      routes:
        - id: account-service
          uri: lb://account-service
          predicates:
            - Path=/api/v1/accounts/**
          filters:
            - name: JwtAuthenticationFilter
```

## Rollout Plan

### Step 1: Auth Service (✅ Complete)
- JWT generation working
- Token blacklisting working
- Roles configured

### Step 2: Account Service (First Integration)
1. Add dependencies
2. Add JWT validation classes
3. Add SecurityConfig
4. Update controllers with @PreAuthorize
5. Test with real JWT from Auth Service
6. Deploy with feature flag

### Step 3: Transfer Service
- Repeat Step 2
- Additional: Check user owns source account

### Step 4: Customer Service
- Repeat Step 2
- Additional: Customers can only see own profile

### Step 5: API Gateway
- Add JWT validation
- Configure routing with auth

## Testing Strategy

### Integration Test Scenario
```
1. Register user (Auth Service)
2. Login (Auth Service) → Get JWT
3. Create account (Account Service with JWT)
   → Should succeed with CUSTOMER role
4. View all accounts (Account Service with JWT)
   → Should fail (need ADMIN role)
5. Logout (Auth Service) → Token blacklisted
6. Try to create account with same JWT
   → Should fail (token blacklisted)
```

### Security Checklist
- [ ] All endpoints require authentication (except health)
- [ ] Role-based access control working
- [ ] Token blacklisting checked in all services
- [ ] Expired tokens rejected
- [ ] Invalid signatures rejected
- [ ] No sensitive data in logs
- [ ] HTTPS in production

## Performance Considerations

### Redis Connection Pool
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
          min-idle: 2
```

### Token Validation Cache
```java
@Cacheable(value = "jwt-validation", key = "#token")
public boolean validateToken(String token) {
    // Cache validation result for 1 minute
    // Reduce parsing overhead
}
```

## Rollback Plan
1. Disable JWT validation (feature flag)
2. Keep endpoints open temporarily
3. Revert SecurityConfig
4. No data loss

## Dependencies
- All services need Redis access
- JWT_SECRET must be shared (environment variable)
- Clock sync between services (for exp validation)

## Estimated Effort
- Per service: 4-6 hours
- API Gateway: 2 hours
- Integration testing: 4 hours
- **Total: ~20 hours for platform-wide integration**
```

### Best Practices

#### When to Use IntegrationAgent

✅ **DO Use When:**
- Integrating two or more services
- Need both Feign and Kafka integration
- Want comprehensive error handling strategy
- Planning complex integration (e.g., Auth Service)

❌ **DON'T Use When:**
- Single service development
- Simple internal method call
- Configuration change only

#### Tips for Better Results

1. **Specify Communication Pattern**
   - "Use Feign for synchronous calls"
   - "Use Kafka for asynchronous events"
   - "Both patterns needed"

2. **Define Use Cases Clearly**
   - List all integration scenarios
   - Specify error handling requirements
   - Mention performance constraints

3. **Request Error Handling**
   - "What happens if service is down?"
   - "Circuit breaker configuration?"
   - "Dead letter queue strategy?"

### Integration with Other Agents

**Integration Workflow:**
```
IntegrationAgent (Planning)
    ↓
[Analysis Phase]
    ├─ Identify Feign endpoints needed
    ├─ Identify Kafka events needed
    └─ Define error handling
    ↓
[Implementation Phase]
    ├─ EventDrivenAgent (Kafka code)
    ├─ APIDesignAgent (Feign client)
    └─ SecurityImplementationAgent (if auth involved)
    ↓
[Testing Phase]
    ├─ IntegrationTestAgent (WireMock, Embedded Kafka)
    └─ APITestAgent (End-to-end flow)
    ↓
[Documentation Phase]
    └─ APIDocumentationAgent (Integration docs)
```

---

## Summary

Task Breakdown agents provide the foundation for structured development:

- **ServiceImplementationAgent:** Complete service planning (12 phases or 5 days)
- **FeatureImplementationAgent:** Feature planning with impact analysis
- **IntegrationAgent:** Service integration planning (Feign + Kafka)

All agents follow Banking Platform standards and generate actionable, detailed plans that accelerate development while maintaining quality and consistency.

**Next:** [Code Generation Agents →](./02-code-generation.md)
