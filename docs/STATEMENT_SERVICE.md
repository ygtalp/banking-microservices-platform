# Statement Service - Complete Reference

> **Service:** Statement Service
> **Port:** 8091
> **Database:** statement_db (PostgreSQL)
> **Responsibility:** Account statement generation, PDF creation, and statement management
> **Last Updated:** 01 January 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model](#domain-model)
3. [API Reference](#api-reference)
4. [Statement Types](#statement-types)
5. [PDF Generation](#pdf-generation)
6. [Business Rules](#business-rules)
7. [Caching Strategy](#caching-strategy)
8. [Testing](#testing)

---

## Overview

The Statement Service is responsible for generating, managing, and delivering account statements in PDF format. It integrates with the Transaction Service to fetch transaction data and produces comprehensive financial summaries for various periods.

### Key Features

- ✅ **Statement Generation** - Monthly, quarterly, annual, and custom period statements
- ✅ **PDF Creation** - Professional PDF generation with transaction details
- ✅ **Transaction Summary** - Opening/closing balance, total credits/debits
- ✅ **Statement History** - Complete history of all generated statements
- ✅ **Download Management** - Secure PDF download with tracking
- ✅ **Status Tracking** - Statement lifecycle management (generating → generated → sent → downloaded)
- ✅ **Integration** - Feign client integration with Transaction Service
- ✅ **Caching** - Redis caching for improved performance
- ✅ **JWT Security** - Complete authentication and authorization

### Technology Stack

```yaml
Framework: Spring Boot 3.2.0
Database: PostgreSQL 16 (statement_db)
Cache: Redis 7.2
Authentication: JWT (HS512)
PDF Library: iText 7 or Apache PDFBox
Integration: OpenFeign (Transaction Service)
API Documentation: Swagger/OpenAPI 3.0
Testing: JUnit 5, Mockito, TestContainers
```

---

## Domain Model

### Statement Entity

```java
@Entity
@Table(name = "statements", indexes = {
        @Index(name = "idx_account_number", columnList = "account_number"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_statement_date", columnList = "statement_date"),
        @Index(name = "idx_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class Statement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_id", unique = true, nullable = false, length = 50)
    private String statementId;  // STM-XXXXXXXXXXXX

    @Column(name = "account_number", nullable = false, length = 26)
    private String accountNumber;  // IBAN format

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 20)
    private StatementType statementType;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;  // Generation date

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "opening_balance", precision = 19, scale = 2)
    private BigDecimal openingBalance;  // Balance at period start

    @Column(name = "closing_balance", precision = 19, scale = 2)
    private BigDecimal closingBalance;  // Balance at period end

    @Column(name = "total_credits", precision = 19, scale = 2)
    private BigDecimal totalCredits;  // Sum of all credits in period

    @Column(name = "total_debits", precision = 19, scale = 2)
    private BigDecimal totalDebits;  // Sum of all debits in period

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatementStatus status;  // GENERATING, GENERATED, SENT, FAILED, DOWNLOADED

    @Column(name = "pdf_file_path", length = 500)
    private String pdfFilePath;  // Local file system path

    @Column(name = "pdf_file_size")
    private Long pdfFileSize;  // File size in bytes

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;  // Error details if generation failed

    @Column(name = "requested_by", length = 50)
    private String requestedBy;  // User who requested the statement

    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void markAsGenerated(String filePath, Long fileSize) {
        this.status = StatementStatus.GENERATED;
        this.pdfFilePath = filePath;
        this.pdfFileSize = fileSize;
        this.generatedAt = LocalDateTime.now();
    }

    public void markAsSent() {
        this.status = StatementStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.notificationSent = true;
    }

    public void markAsDownloaded() {
        this.status = StatementStatus.DOWNLOADED;
        this.downloadedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = StatementStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public boolean isGenerated() {
        return this.status == StatementStatus.GENERATED ||
               this.status == StatementStatus.SENT ||
               this.status == StatementStatus.DOWNLOADED;
    }

    public boolean isAvailableForDownload() {
        return this.pdfFilePath != null && !this.pdfFilePath.isEmpty();
    }
}
```

### Statement Type Enum

```java
public enum StatementType {
    MONTHLY,        // Monthly account statement (most common)
    QUARTERLY,      // Quarterly summary (business accounts)
    ANNUAL,         // Annual summary (tax purposes)
    CUSTOM,         // Custom date range (user-specified)
    TAX,            // Tax statement (specialized format)
    TRANSACTION     // Transaction-specific statement (single transaction)
}
```

### Statement Status Enum

```java
public enum StatementStatus {
    GENERATING,     // PDF generation in progress
    GENERATED,      // PDF generated successfully, ready for download
    SENT,           // Sent via email/notification
    FAILED,         // Generation failed (see errorMessage)
    DOWNLOADED      // Downloaded by user at least once
}
```

### Statement Generation Request DTO

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatementGenerationRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Statement type is required")
    private StatementType statementType;

    // For MONTHLY statements
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    // For QUARTERLY statements
    @Min(value = 1, message = "Quarter must be between 1 and 4")
    @Max(value = 4, message = "Quarter must be between 1 and 4")
    private Integer quarter;

    // For MONTHLY, QUARTERLY, ANNUAL statements
    private Integer year;

    // For CUSTOM statements
    private LocalDate periodStart;
    private LocalDate periodEnd;
}
```

### Statement Response DTO

```java
@Data
@Builder
public class StatementResponse {
    private String statementId;
    private String accountNumber;
    private String customerId;
    private StatementType statementType;
    private LocalDate statementDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private Integer transactionCount;
    private StatementStatus status;
    private Long pdfFileSize;
    private LocalDateTime generatedAt;
    private LocalDateTime sentAt;
    private LocalDateTime downloadedAt;
    private Boolean notificationSent;
    private String downloadUrl;  // /statements/download/{statementId}
    private LocalDateTime createdAt;
}
```

---

## API Reference

### Base URL
```
http://localhost:8091/statements
```

### Endpoints

#### 1. Generate Statement

```http
POST /statements/generate
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body (Monthly):**
```json
{
  "accountNumber": "TR330006100519786457841326",
  "statementType": "MONTHLY",
  "month": 12,
  "year": 2025
}
```

**Request Body (Custom):**
```json
{
  "accountNumber": "TR330006100519786457841326",
  "statementType": "CUSTOM",
  "periodStart": "2025-10-01",
  "periodEnd": "2025-12-31"
}
```

**Response (201 Created):**
```json
{
  "statementId": "STM-A1B2C3D4E5F6",
  "accountNumber": "TR330006100519786457841326",
  "customerId": "CUS-123456789012",
  "statementType": "MONTHLY",
  "statementDate": "2026-01-01",
  "periodStart": "2025-12-01",
  "periodEnd": "2025-12-31",
  "openingBalance": 10000.00,
  "closingBalance": 12500.00,
  "totalCredits": 5000.00,
  "totalDebits": 2500.00,
  "transactionCount": 45,
  "status": "GENERATED",
  "pdfFileSize": 245678,
  "generatedAt": "2026-01-01T10:30:00",
  "sentAt": null,
  "downloadedAt": null,
  "notificationSent": false,
  "downloadUrl": "/statements/download/STM-A1B2C3D4E5F6",
  "createdAt": "2026-01-01T10:29:45"
}
```

**Business Logic:**
1. Calculate period based on statement type
2. Fetch transactions from Transaction Service (Feign client)
3. Calculate opening/closing balance
4. Calculate total credits/debits
5. Create statement entity (status: GENERATING)
6. Generate PDF using PdfGenerationService
7. Save PDF to file system
8. Update statement status to GENERATED
9. Return statement response

**Error Handling:**
- If PDF generation fails → status: FAILED, errorMessage populated
- If Transaction Service unavailable → throw RuntimeException
- If invalid period specified → validation error

---

#### 2. Get Statement by ID

```http
GET /statements/{statementId}
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "statementId": "STM-A1B2C3D4E5F6",
  "accountNumber": "TR330006100519786457841326",
  "statementType": "MONTHLY",
  "status": "GENERATED",
  "downloadUrl": "/statements/download/STM-A1B2C3D4E5F6",
  // ... other fields
}
```

**Cache:** Redis (key: `statements::STM-A1B2C3D4E5F6`, TTL: 5 minutes)

---

#### 3. Get Account Statements

```http
GET /statements/account/{accountNumber}
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
[
  {
    "statementId": "STM-A1B2C3D4E5F6",
    "statementType": "MONTHLY",
    "periodStart": "2025-12-01",
    "periodEnd": "2025-12-31",
    "status": "DOWNLOADED",
    "downloadUrl": "/statements/download/STM-A1B2C3D4E5F6"
  },
  {
    "statementId": "STM-X9Y8Z7W6V5U4",
    "statementType": "QUARTERLY",
    "periodStart": "2025-10-01",
    "periodEnd": "2025-12-31",
    "status": "GENERATED",
    "downloadUrl": "/statements/download/STM-X9Y8Z7W6V5U4"
  }
]
```

**Ordering:** Descending by statement_date (most recent first)

---

#### 4. Get Customer Statements

```http
GET /statements/customer/{customerId}
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
[
  {
    "statementId": "STM-A1B2C3D4E5F6",
    "accountNumber": "TR330006100519786457841326",
    "statementType": "MONTHLY",
    "status": "GENERATED"
  },
  {
    "statementId": "STM-B2C3D4E5F6G7",
    "accountNumber": "TR330006100519786457999999",
    "statementType": "ANNUAL",
    "status": "SENT"
  }
]
```

**Use Case:** Customer dashboard showing all statements across all accounts

---

#### 5. Download Statement PDF

```http
GET /statements/download/{statementId}
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="STM-A1B2C3D4E5F6.pdf"`
- Binary PDF data

**Side Effects:**
- Updates `downloadedAt` timestamp
- Changes status to DOWNLOADED (if not already)

**Error Responses:**
- 404: Statement not found
- 400: PDF not available (generation failed or in progress)
- 500: Failed to read PDF file

---

## Statement Types

### Monthly Statement

**Period Calculation:**
```java
// If month and year not specified, use previous month
int month = request.getMonth() != null ? request.getMonth() : LocalDate.now().getMonthValue();
int year = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
YearMonth yearMonth = YearMonth.of(year, month);
LocalDate start = yearMonth.atDay(1);
LocalDate end = yearMonth.atEndOfMonth();
```

**Example:**
- Request: `{ "statementType": "MONTHLY", "month": 11, "year": 2025 }`
- Period: `2025-11-01` to `2025-11-30`

---

### Quarterly Statement

**Period Calculation:**
```java
int quarter = request.getQuarter() != null ? request.getQuarter() : getCurrentQuarter();
int year = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
LocalDate start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
LocalDate end = start.plusMonths(3).minusDays(1);
```

**Quarter Mapping:**
- Q1: January 1 - March 31
- Q2: April 1 - June 30
- Q3: July 1 - September 30
- Q4: October 1 - December 31

**Example:**
- Request: `{ "statementType": "QUARTERLY", "quarter": 4, "year": 2025 }`
- Period: `2025-10-01` to `2025-12-31`

---

### Annual Statement

**Period Calculation:**
```java
int year = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
LocalDate start = LocalDate.of(year, 1, 1);
LocalDate end = LocalDate.of(year, 12, 31);
```

**Example:**
- Request: `{ "statementType": "ANNUAL", "year": 2025 }`
- Period: `2025-01-01` to `2025-12-31`

---

### Custom Statement

**Validation:**
```java
if (request.getPeriodStart() == null || request.getPeriodEnd() == null) {
    throw new RuntimeException("Custom statement requires periodStart and periodEnd");
}
```

**Example:**
- Request: `{ "statementType": "CUSTOM", "periodStart": "2025-06-15", "periodEnd": "2025-08-20" }`
- Period: `2025-06-15` to `2025-08-20`

---

## PDF Generation

### PDF Generation Service

```java
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    public String generateStatementPdf(Statement statement, List<TransactionDTO> transactions) {
        // 1. Create PDF document
        Document document = new Document();

        // 2. Set output path
        String filePath = "/statements/" + statement.getStatementId() + ".pdf";
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));

        // 3. Open document
        document.open();

        // 4. Add header (logo, bank name, statement title)
        addHeader(document, statement);

        // 5. Add account information
        addAccountInfo(document, statement);

        // 6. Add period summary
        addPeriodSummary(document, statement);

        // 7. Add transaction table
        addTransactionTable(document, transactions);

        // 8. Add footer (page numbers, generation timestamp)
        addFooter(document, statement);

        // 9. Close document
        document.close();

        return filePath;
    }

    private void addHeader(Document document, Statement statement) {
        // Bank logo and name
        // Statement type and period
        // Statement ID and generation date
    }

    private void addAccountInfo(Document document, Statement statement) {
        // Account number (IBAN)
        // Customer ID
        // Statement period
    }

    private void addPeriodSummary(Document document, Statement statement) {
        // Opening balance
        // Total credits (+)
        // Total debits (-)
        // Closing balance
        // Transaction count
    }

    private void addTransactionTable(Document document, List<TransactionDTO> transactions) {
        // Table with columns:
        // - Date
        // - Description
        // - Debit
        // - Credit
        // - Balance
    }

    private void addFooter(Document document, Statement statement) {
        // Page X of Y
        // Generated on: {timestamp}
        // Statement ID: {statementId}
    }
}
```

### PDF Features

- ✅ Professional formatting with bank logo
- ✅ Account and customer information
- ✅ Period summary (opening/closing balance, totals)
- ✅ Detailed transaction table
- ✅ Page numbering
- ✅ Generation timestamp
- ✅ Statement ID for reference

---

## Business Rules

### Statement Generation Rules

1. **Period Validation**
   - Custom period: periodEnd must be after periodStart
   - Custom period: maximum 12 months duration
   - Historical statements: can generate for any past period
   - Future statements: cannot generate for future periods

2. **Transaction Data**
   - Fetched from Transaction Service via Feign client
   - If no transactions found → empty statement with zero balances
   - Transactions ordered by date ascending

3. **Balance Calculation**
   ```java
   openingBalance = transactions.isEmpty() ? BigDecimal.ZERO : transactions.get(0).getBalanceBefore();
   closingBalance = transactions.isEmpty() ? BigDecimal.ZERO : transactions.get(transactions.size() - 1).getBalanceAfter();
   totalCredits = sum of all CREDIT transactions
   totalDebits = sum of all DEBIT transactions
   ```

4. **PDF Storage**
   - File path: `/statements/{statementId}.pdf`
   - File naming: `STM-XXXXXXXXXXXX.pdf`
   - Storage: Local file system (production: S3/Azure Blob)

5. **Statement Status Lifecycle**
   ```
   GENERATING → GENERATED → SENT → DOWNLOADED
             ↓
           FAILED (if error occurs)
   ```

6. **Duplicate Prevention**
   - Same account, same type, same period → allow (multiple generations)
   - Each generation creates new statement with unique ID

7. **Access Control**
   - User can only access their own statements
   - Admin can access all statements
   - Customer can access statements for their accounts only

---

## Caching Strategy

### Redis Caching

```java
@Cacheable(value = "statements", key = "#statementId")
public StatementResponse getStatement(String statementId) {
    // Cached for 5 minutes
}

@CacheEvict(value = "statements", key = "#request.accountNumber")
public StatementResponse generateStatement(StatementGenerationRequest request, String userId) {
    // Evict cache after new statement generated
}
```

### Cache Configuration

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes
```

### What's Cached

- ✅ Individual statement by ID (key: `statements::STM-XXXXXXXXXXXX`)
- ✅ Account statement list (key: `statements::TR330006100519786457841326`)
- ❌ PDF file data (too large, served from file system)

### Cache Invalidation

- Statement generated → evict account's cache
- Statement deleted → evict individual cache
- Statement updated → evict individual cache

---

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock
    private StatementRepository statementRepository;

    @Mock
    private TransactionServiceClient transactionServiceClient;

    @Mock
    private PdfGenerationService pdfGenerationService;

    @InjectMocks
    private StatementService statementService;

    @Test
    void generateMonthlyStatement_Success() {
        // Given
        StatementGenerationRequest request = new StatementGenerationRequest();
        request.setAccountNumber("TR330006100519786457841326");
        request.setStatementType(StatementType.MONTHLY);
        request.setMonth(12);
        request.setYear(2025);

        List<TransactionDTO> transactions = createSampleTransactions();
        when(transactionServiceClient.getTransactionsByDateRange(any(), any(), any()))
            .thenReturn(transactions);

        when(pdfGenerationService.generateStatementPdf(any(), any()))
            .thenReturn("/statements/STM-TEST123.pdf");

        Statement savedStatement = Statement.builder()
            .statementId("STM-TEST123")
            .status(StatementStatus.GENERATED)
            .build();
        when(statementRepository.save(any())).thenReturn(savedStatement);

        // When
        StatementResponse response = statementService.generateStatement(request, "user123");

        // Then
        assertNotNull(response);
        assertEquals("STM-TEST123", response.getStatementId());
        assertEquals(StatementStatus.GENERATED, response.getStatus());
        verify(transactionServiceClient).getTransactionsByDateRange(any(), any(), any());
        verify(pdfGenerationService).generateStatementPdf(any(), any());
    }

    @Test
    void generateStatement_PdfGenerationFails_StatusFailed() {
        // Given
        StatementGenerationRequest request = new StatementGenerationRequest();
        request.setAccountNumber("TR330006100519786457841326");
        request.setStatementType(StatementType.MONTHLY);

        when(transactionServiceClient.getTransactionsByDateRange(any(), any(), any()))
            .thenReturn(Collections.emptyList());

        when(pdfGenerationService.generateStatementPdf(any(), any()))
            .thenThrow(new RuntimeException("PDF generation error"));

        Statement savedStatement = Statement.builder()
            .statementId("STM-TEST123")
            .build();
        when(statementRepository.save(any())).thenReturn(savedStatement);

        // When & Then
        assertThrows(RuntimeException.class, () ->
            statementService.generateStatement(request, "user123"));

        verify(statementRepository, times(2)).save(argThat(stmt ->
            stmt.getStatus() == StatementStatus.FAILED));
    }

    @Test
    void calculatePeriod_Quarterly_Q4() {
        // Test period calculation for Q4 2025
        // Expected: 2025-10-01 to 2025-12-31
    }

    @Test
    void calculateTotalCredits_MultipleTransactions() {
        // Test credit summation
    }

    @Test
    void downloadStatement_MarksAsDownloaded() {
        // Test download tracking
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class StatementIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("statement_db_test");

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private StatementService statementService;

    @Test
    void fullStatementGenerationFlow() {
        // 1. Generate statement
        // 2. Verify database record
        // 3. Download PDF
        // 4. Verify status changes
        // 5. Verify file exists
    }

    @Test
    void getAccountStatements_OrderedByDateDesc() {
        // 1. Create multiple statements
        // 2. Fetch by account number
        // 3. Verify ordering
    }
}
```

### API Tests (PowerShell)

```powershell
# Test: Generate Monthly Statement
$statementRequest = @{
    accountNumber = "TR330006100519786457841326"
    statementType = "MONTHLY"
    month = 12
    year = 2025
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Uri "http://localhost:8091/statements/generate" `
    -Method POST `
    -Headers @{ "Authorization" = "Bearer $token" } `
    -Body $statementRequest `
    -ContentType "application/json"

Write-Host "Statement generated: $($response.statementId)"

# Test: Download PDF
$pdfBytes = Invoke-RestMethod `
    -Uri "http://localhost:8091/statements/download/$($response.statementId)" `
    -Method GET `
    -Headers @{ "Authorization" = "Bearer $token" }

[System.IO.File]::WriteAllBytes("statement.pdf", $pdfBytes)
Write-Host "PDF downloaded successfully"
```

---

## Integration with Other Services

### Transaction Service (Feign Client)

```java
@FeignClient(name = "transaction-service", url = "${services.transaction-service.url}")
public interface TransactionServiceClient {

    @GetMapping("/transactions/account/{accountNumber}/date-range")
    List<TransactionDTO> getTransactionsByDateRange(
        @PathVariable("accountNumber") String accountNumber,
        @RequestParam("startDate") LocalDateTime startDate,
        @RequestParam("endDate") LocalDateTime endDate
    );
}
```

**Purpose:** Fetch transaction history for specified period

---

## Future Enhancements

1. **Email Delivery**
   - Integrate with Notification Service
   - Send PDF via email automatically
   - Event: `statement.generated` → Notification Service

2. **Statement Scheduling**
   - Automatic monthly statement generation
   - Scheduled job (1st of each month)
   - Batch processing for all active accounts

3. **Statement Templates**
   - Multiple PDF templates (classic, modern, minimalist)
   - Customizable branding per customer
   - Template selection in request

4. **Cloud Storage**
   - Store PDFs in S3/Azure Blob Storage
   - Signed URLs for secure downloads
   - Automatic cleanup (delete after 90 days)

5. **Statement Analytics**
   - Spending analysis
   - Category breakdown
   - Month-over-month comparison charts

6. **Multi-Currency Support**
   - Convert all transactions to single currency
   - Display exchange rates used
   - Currency preference per customer

---

**Last Updated:** 01 January 2026
**API Version:** 1.0
**Service Status:** ✅ Production Ready
