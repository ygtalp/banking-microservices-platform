# Testing Guide

> **Purpose:** Comprehensive testing strategy for banking microservices platform  
> **Coverage Target:** 80%+  
> **Last Updated:** 23 December 2025

---

## Test Pyramid

```
           E2E Tests (PowerShell Scripts)
                    /\
                   /  \
                  /    \
                 /      \
       Integration Tests (TestContainers)
              /            \
             /              \
            /                \
           /                  \
    Unit Tests (JUnit + Mockito)
```

### Coverage Distribution

```
Unit Tests:         70% (fast, isolated)
Integration Tests:  20% (database, containers)
E2E Tests:          10% (full flow, API)
```

---

## Unit Testing

### Framework

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Example: Service Layer

```java
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private AccountServiceImpl accountService;
    
    @Test
    @DisplayName("Should create account with valid request")
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
            .customerName("John Doe")
            .balance(BigDecimal.ZERO)
            .status(AccountStatus.ACTIVE)
            .build();
        
        when(accountRepository.save(any(Account.class)))
            .thenReturn(savedAccount);
        
        // When
        AccountResponse response = accountService.createAccount(request);
        
        // Then
        assertNotNull(response);
        assertEquals("ACC-123", response.getAccountNumber());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        
        verify(accountRepository).save(any(Account.class));
        verify(eventPublisher).publishAccountCreated(any(Account.class));
    }
}
```

---

## Integration Testing

### TestContainers Setup

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AccountServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:16-alpine"
    )
        .withDatabaseName("banking_accounts_test")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
        "redis:7.2-alpine"
    )
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
    private AccountService accountService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Test
    void shouldPersistAccountToDatabase() {
        // Given
        CreateAccountRequest request = new CreateAccountRequest(
            "John Doe",
            AccountType.CHECKING,
            Currency.TRY
        );
        
        // When
        AccountResponse response = accountService.createAccount(request);
        
        // Then
        Optional<Account> savedAccount = accountRepository
            .findByAccountNumber(response.getAccountNumber());
        
        assertTrue(savedAccount.isPresent());
        assertEquals("John Doe", savedAccount.get().getCustomerName());
    }
}
```

---

## API Testing (PowerShell)

### Test Script Structure

```powershell
# scripts/test/test-services-fixed.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BANKING PLATFORM - API TESTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Test 1: Create Account
Write-Host "`n[1/5] Testing Account Creation..." -ForegroundColor Yellow

$createAccountBody = @{
    customerName = "Test User"
    accountType = "CHECKING"
    currency = "TRY"
} | ConvertTo-Json

$account1 = Invoke-RestMethod -Uri "http://localhost:8080/accounts" `
    -Method POST `
    -Body $createAccountBody `
    -ContentType "application/json"

Write-Host "  ✅ Account created: $($account1.data.accountNumber)" -ForegroundColor Green

# Test 2: Get Account
Write-Host "`n[2/5] Testing Get Account..." -ForegroundColor Yellow

$retrievedAccount = Invoke-RestMethod `
    -Uri "http://localhost:8080/accounts/$($account1.data.accountNumber)" `
    -Method GET

Write-Host "  ✅ Account retrieved: $($retrievedAccount.data.customerName)" -ForegroundColor Green
```

---

## Running Tests

### Maven Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AccountServiceImplTest

# Run tests with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Coverage Report

```
Target/site/jacoco/index.html
├── Package Coverage
├── Class Coverage
├── Method Coverage
└── Line Coverage

Target: All metrics > 80%
```

---

**Last Updated:** 23 December 2025  
**Test Status:** ✅ 80%+ Coverage
