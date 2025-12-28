# Testing Agents

> **Category:** Test Generation & Maintenance
> **Agent Count:** 4
> **Automation Level:** High (85-90%)
> **Last Updated:** 28 December 2025

---

## Overview

Testing agents generate comprehensive test suites following the platform's 80%+ coverage standard. They create unit tests, integration tests with TestContainers, API tests in PowerShell, and fix test failures.

---

## 1. UnitTestAgent 🧪

### Specification

**ID:** `AGENT-TEST-001`
**Name:** UnitTestAgent
**Category:** Testing
**Scope:** Service layer unit tests
**Automation Level:** High (90%)

### Objective

Generate unit tests for service classes using JUnit 5 and Mockito, following Given-When-Then pattern with 80%+ coverage.

### Usage Example

**Prompt:**
```
Generate unit tests for NotificationService with methods:
- sendNotification(SendNotificationRequest)
- getNotification(String notificationId)
- retryNotification(String notificationId)
- cancelNotification(String notificationId)

Cover success cases, validation failures, and error scenarios.
```

**Output:**
```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailChannelHandler emailChannelHandler;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("Should send email notification successfully")
    void shouldSendEmailNotificationSuccessfully() {
        // Given
        SendNotificationRequest request = SendNotificationRequest.builder()
            .userId("USR-123")
            .channel(NotificationChannel.EMAIL)
            .subject("Test Notification")
            .content("Test content")
            .recipientEmail("user@example.com")
            .build();

        Notification savedNotification = Notification.builder()
            .id(1L)
            .notificationId("NOT-ABC123")
            .userId("USR-123")
            .status(NotificationStatus.SENT)
            .build();

        when(notificationRepository.save(any(Notification.class)))
            .thenReturn(savedNotification);

        // When
        NotificationResponse response = notificationService.sendNotification(request);

        // Then
        assertNotNull(response);
        assertEquals("NOT-ABC123", response.getNotificationId());
        assertEquals(NotificationStatus.SENT, response.getStatus());

        verify(notificationRepository).save(any(Notification.class));
        verify(emailChannelHandler).send(any(Notification.class));
        verify(kafkaTemplate).send(
            eq("notification.events"),
            anyString(),
            any(NotificationSentEvent.class)
        );
    }

    @Test
    @DisplayName("Should throw exception when notification not found")
    void shouldThrowExceptionWhenNotificationNotFound() {
        // Given
        String notificationId = "NOT-NOTFOUND";
        when(notificationRepository.findByNotificationId(notificationId))
            .thenReturn(Optional.empty());

        // When/Then
        assertThrows(
            NotificationNotFoundException.class,
            () -> notificationService.getNotification(notificationId)
        );

        verify(notificationRepository).findByNotificationId(notificationId);
    }

    @Test
    @DisplayName("Should handle email sending failure and mark as failed")
    void shouldHandleEmailSendingFailure() {
        // Given
        SendNotificationRequest request = createEmailRequest();

        doThrow(new EmailSendException("SMTP error"))
            .when(emailChannelHandler).send(any(Notification.class));

        // When
        NotificationResponse response = notificationService.sendNotification(request);

        // Then
        assertEquals(NotificationStatus.FAILED, response.getStatus());
        assertNotNull(response.getFailedReason());
        assertTrue(response.getFailedReason().contains("SMTP error"));
    }

    // ... more tests for all methods and edge cases
}
```

**Coverage:** 90%+ (all branches, exceptions, edge cases)

---

## 2. IntegrationTestAgent 🐳

### Specification

**ID:** `AGENT-TEST-002`
**Name:** IntegrationTestAgent
**Category:** Testing
**Scope:** Integration tests with TestContainers
**Automation Level:** High (85%)

### Objective

Generate integration tests using TestContainers (PostgreSQL, Redis, Kafka) for end-to-end flow validation.

### Usage Example

**Prompt:**
```
Generate integration test for Customer Service:
- Full KYC workflow (register → upload docs → verify → approve)
- Database persistence verification
- Event publishing verification
- Feign client integration

Use TestContainers for PostgreSQL and Redis.
```

**Output:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.bootstrap-servers=localhost:9093"
})
class CustomerServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("banking_customers_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private KycDocumentRepository kycDocumentRepository;

    @Test
    void shouldCompleteFullKYCWorkflow() {
        // 1. Register customer
        RegisterCustomerRequest registerRequest = RegisterCustomerRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@test.com")
            .nationalId("12345678901")
            .build();

        CustomerResponse customer = customerService.registerCustomer(registerRequest);
        assertEquals(CustomerStatus.PENDING_VERIFICATION, customer.getStatus());

        // 2. Upload KYC documents
        UploadDocumentRequest docRequest = UploadDocumentRequest.builder()
            .documentType(DocumentType.PASSPORT)
            .documentNumber("P12345")
            .build();

        KycDocumentResponse doc = customerService.uploadDocument(
            customer.getCustomerId(),
            docRequest
        );
        assertEquals(DocumentStatus.PENDING, doc.getStatus());

        // 3. Verify document
        customerService.verifyDocument(doc.getId(), "admin@bank.com");

        // 4. Verify customer
        customerService.verifyCustomer(
            customer.getCustomerId(),
            new VerifyCustomerRequest("admin@bank.com", "KYC verified")
        );

        // 5. Approve customer
        customerService.approveCustomer(
            customer.getCustomerId(),
            new ApproveCustomerRequest("manager@bank.com", RiskLevel.LOW)
        );

        // Verify final state
        Customer finalCustomer = customerRepository
            .findByCustomerId(customer.getCustomerId())
            .orElseThrow();

        assertEquals(CustomerStatus.APPROVED, finalCustomer.getStatus());
        assertEquals(RiskLevel.LOW, finalCustomer.getRiskLevel());
        assertNotNull(finalCustomer.getApprovedAt());
        assertNotNull(finalCustomer.getApprovedBy());
    }

    @Test
    void shouldEnforceUniqueEmailConstraint() {
        RegisterCustomerRequest request = createCustomerRequest("duplicate@test.com");
        customerService.registerCustomer(request);

        assertThrows(
            EmailAlreadyExistsException.class,
            () -> customerService.registerCustomer(request)
        );
    }
}
```

---

## 3. APITestAgent 📝

### Specification

**ID:** `AGENT-TEST-003`
**Name:** APITestAgent
**Category:** Testing
**Scope:** End-to-end API tests (PowerShell)
**Automation Level:** High (85%)

### Objective

Generate PowerShell scripts for end-to-end API testing with request/response validation, error handling, and colored output.

### Usage Example

**Prompt:**
```
Generate PowerShell API test script for Auth Service:
- Health check
- Register user
- Login
- Get current user profile
- Change password
- Logout
- Verify token blacklisted

Include error scenarios (invalid credentials, weak password).
```

**Output:**
```powershell
# test-auth-service.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AUTH SERVICE - API TESTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8084"
$testsPassed = 0
$testsFailed = 0

# Helper function
function Test-Endpoint {
    param($TestName, $ScriptBlock)
    Write-Host "`n[$TestName]" -ForegroundColor Yellow
    try {
        & $ScriptBlock
        Write-Host "  ✅ PASSED" -ForegroundColor Green
        $script:testsPassed++
    } catch {
        Write-Host "  ❌ FAILED: $_" -ForegroundColor Red
        $script:testsFailed++
    }
}

# Test 1: Health Check
Test-Endpoint "Health Check" {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/health" -Method GET
    if ($response.status -ne "UP") { throw "Service not healthy" }
}

# Test 2: Register User
$randomEmail = "test$(Get-Random)@example.com"
$password = "SecureP@ss123"

Test-Endpoint "Register New User" {
    $body = @{
        email = $randomEmail
        password = $password
        firstName = "Test"
        lastName = "User"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/auth/register" `
        -Method POST -Body $body -ContentType "application/json"

    if (-not $response.success) { throw "Registration failed" }
    $script:userId = $response.data.userId
}

# Test 3: Login
Test-Endpoint "Login with Valid Credentials" {
    $body = @{
        email = $randomEmail
        password = $password
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" `
        -Method POST -Body $body -ContentType "application/json"

    if (-not $response.data.accessToken) { throw "No access token received" }
    $script:accessToken = $response.data.accessToken
    $script:refreshToken = $response.data.refreshToken
}

# Test 4: Get Current User
Test-Endpoint "Get Current User Profile" {
    $headers = @{ Authorization = "Bearer $accessToken" }
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/me" `
        -Method GET -Headers $headers

    if ($response.data.email -ne $randomEmail) { throw "Email mismatch" }
}

# Test 5: Logout
Test-Endpoint "Logout" {
    $headers = @{ Authorization = "Bearer $accessToken" }
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/logout" `
        -Method POST -Headers $headers

    if (-not $response.success) { throw "Logout failed" }
}

# Test 6: Verify Token Blacklisted
Test-Endpoint "Verify Token is Blacklisted After Logout" {
    $headers = @{ Authorization = "Bearer $accessToken" }
    try {
        Invoke-RestMethod -Uri "$baseUrl/auth/me" -Method GET -Headers $headers
        throw "Token should be blacklisted"
    } catch {
        # Expected 401 Unauthorized
        if ($_.Exception.Response.StatusCode -ne 401) { throw }
    }
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TESTS COMPLETED" -ForegroundColor Cyan
Write-Host "  Passed: $testsPassed" -ForegroundColor Green
Write-Host "  Failed: $testsFailed" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Cyan

if ($testsFailed -gt 0) { exit 1 }
```

---

## 4. TestFixAgent 🔧

### Specification

**ID:** `AGENT-TEST-004`
**Name:** TestFixAgent
**Category:** Testing
**Scope:** Test failure diagnosis and fixes
**Automation Level:** High (80%)

### Objective

Analyze test failures, identify root causes, and fix compilation errors, mock mismatches, and assertion failures.

### Usage Example

**Prompt:**
```
Fix test failures in CustomerServiceTest:
- 12 compilation errors (method signature mismatches)
- Mock setup issues (constructor parameters)
- Field name inconsistencies (action vs operation)
```

**Output:**
```markdown
# Test Fix Report

## Issues Found

### 1. Method Signature Mismatch (4 instances)
**Problem:** uploadDocument() takes 2 parameters, tests only pass 1

**Files Affected:**
- KycDocumentServiceTest.java:95
- KycDocumentServiceTest.java:122
- KycDocumentServiceTest.java:147

**Fix:**
```java
// ❌ BEFORE
kycDocumentService.uploadDocument(request)

// ✅ AFTER
kycDocumentService.uploadDocument(customerId, request)
```

### 2. Constructor Parameter Count (6 instances)
**Problem:** VerifyDocumentRequest requires 2 params (verifiedBy, notes)

**Fix:**
```java
// ❌ BEFORE
new VerifyDocumentRequest("admin@bank.com")

// ✅ AFTER
new VerifyDocumentRequest("admin@bank.com", null)
```

### 3. Field Name Inconsistency (2 instances)
**Problem:** CustomerHistory field is "operation" not "action"

**Fix:**
```java
// ❌ BEFORE
history.getAction()

// ✅ AFTER
history.getOperation()
```

## All Tests Fixed ✅

Total errors fixed: 12
Compilation: ✅ SUCCESS
Tests passing: 45/45 (100%)
```

---

## Best Practices

### Coverage Targets
- **Unit Tests:** 85%+ line coverage, 80%+ branch coverage
- **Integration Tests:** All critical flows
- **API Tests:** All endpoints + error scenarios

### Test Organization
```
src/test/java/
├── unit/           # Fast, isolated tests
├── integration/    # TestContainers tests
└── api/            # (PowerShell scripts in /scripts/test/)
```

### Naming Conventions
```java
// Test class
class AccountServiceTest

// Test method
@Test
void shouldCreateAccountSuccessfullyWhenValidRequest()
void shouldThrowExceptionWhenInsufficientBalance()
```

---

**Next:** [Documentation Agents →](./04-documentation.md)
