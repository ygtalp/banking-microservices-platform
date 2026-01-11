# Loan Service - Complete Reference

> **Service:** Loan Service
> **Port:** 8088
> **Database:** loan_db (PostgreSQL)
> **Responsibility:** Loan application processing, amortization schedules, and loan lifecycle management
> **Last Updated:** 1 January 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model](#domain-model)
3. [API Reference](#api-reference)
4. [Loan Types](#loan-types)
5. [Amortization Calculation](#amortization-calculation)
6. [Loan Lifecycle](#loan-lifecycle)
7. [Business Rules](#business-rules)
8. [Testing](#testing)

---

## Overview

Loan Service manages the complete lifecycle of bank loans including application processing, approval workflow, disbursement, amortization schedule generation, and repayment tracking.

### Key Features

- ✅ Multiple loan types (Personal, Home, Business, Auto)
- ✅ Automated application processing
- ✅ Approval workflow (pending → approved → disbursed)
- ✅ Amortization schedule generation (monthly installments)
- ✅ Repayment tracking and early payment support
- ✅ Interest calculation (simple and compound)
- ✅ Swagger/OpenAPI documentation
- ✅ Complete audit trail

### Technology Stack

```yaml
Framework: Spring Boot 3.2.0
Database: PostgreSQL 16
Documentation: Swagger/OpenAPI 3.0
Validation: Spring Validation + Hibernate Validator
```

---

## Domain Model

### Loan Entity

```java
@Entity
@Table(name = "loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    private String loanId;  // LOAN-XXXXXXXXXXXX

    @Column(nullable = false, length = 50)
    private String accountNumber;  // Borrower's account

    @Column(nullable = false, length = 100)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanType loanType;  // PERSONAL, HOME, BUSINESS, AUTO

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal loanAmount;  // Principal amount

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;  // Annual interest rate (e.g., 5.25%)

    @Column(nullable = false)
    private Integer termMonths;  // Loan term in months

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPayment;  // Calculated monthly installment

    @Column(precision = 19, scale = 2)
    private BigDecimal totalInterest;  // Total interest to be paid

    @Column(precision = 19, scale = 2)
    private BigDecimal totalAmount;  // Total amount (principal + interest)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance;  // Remaining balance

    @Column(nullable = false)
    private Integer paidInstallments;  // Number of installments paid

    @Column(nullable = false)
    private Integer remainingInstallments;  // Remaining installments

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;  // PENDING, APPROVED, DISBURSED, ACTIVE, PAID_OFF, DEFAULTED

    @Column(length = 100)
    private String approvedBy;  // Loan officer

    @Column
    private LocalDateTime approvedAt;

    @Column
    private LocalDateTime disbursedAt;

    @Column
    private LocalDate nextPaymentDue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (loanId == null) {
            loanId = "LOAN-" + UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        paidInstallments = 0;
        remainingInstallments = termMonths;
        outstandingBalance = loanAmount;
        status = LoanStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### LoanPayment Entity

```java
@Entity
@Table(name = "loan_payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String loanId;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;  // Principal portion

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;  // Interest portion

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;  // Balance after payment

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;  // PENDING, PAID, OVERDUE

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### Enums

```java
public enum LoanType {
    PERSONAL,   // Personal loan
    HOME,       // Home/Mortgage loan
    BUSINESS,   // Business loan
    AUTO        // Auto/Vehicle loan
}

public enum LoanStatus {
    PENDING,      // Application pending approval
    APPROVED,     // Approved but not disbursed
    DISBURSED,    // Funds disbursed, loan active
    ACTIVE,       // Repayment in progress
    PAID_OFF,     // Fully paid
    DEFAULTED     // Payment default
}

public enum PaymentStatus {
    PENDING,   // Payment due
    PAID,      // Payment received
    OVERDUE    // Payment overdue
}
```

---

## API Reference

### Base URL

```
http://localhost:8080/loans  (via API Gateway)
http://localhost:8088/loans  (direct access)
```

### 1. Apply for Loan

**Endpoint:** `POST /loans`

**Request:**
```json
{
  "accountNumber": "ACC-123...",
  "customerName": "John Doe",
  "loanType": "PERSONAL",
  "loanAmount": 50000.00,
  "interestRate": 5.25,
  "termMonths": 36
}
```

**Response (201 Created):**
```json
{
  "loanId": "LOAN-789...",
  "accountNumber": "ACC-123...",
  "customerName": "John Doe",
  "loanType": "PERSONAL",
  "loanAmount": 50000.00,
  "interestRate": 5.25,
  "termMonths": 36,
  "monthlyPayment": 1501.48,
  "totalInterest": 4053.28,
  "totalAmount": 54053.28,
  "status": "PENDING",
  "createdAt": "2026-01-01T10:00:00Z"
}
```

---

### 2. Get Loan by ID

**Endpoint:** `GET /loans/{loanId}`

**Response (200 OK):**
```json
{
  "loanId": "LOAN-789...",
  "accountNumber": "ACC-123...",
  "customerName": "John Doe",
  "loanType": "PERSONAL",
  "loanAmount": 50000.00,
  "interestRate": 5.25,
  "termMonths": 36,
  "monthlyPayment": 1501.48,
  "totalInterest": 4053.28,
  "totalAmount": 54053.28,
  "outstandingBalance": 50000.00,
  "paidInstallments": 0,
  "remainingInstallments": 36,
  "status": "PENDING",
  "nextPaymentDue": null,
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

---

### 3. Approve Loan

**Endpoint:** `POST /loans/{loanId}/approve`

**Request:**
```json
{
  "approvedBy": "loan.officer@bank.com"
}
```

**Response (200 OK):**
```json
{
  "loanId": "LOAN-789...",
  "status": "APPROVED",
  "approvedBy": "loan.officer@bank.com",
  "approvedAt": "2026-01-01T11:00:00Z",
  "message": "Loan approved successfully"
}
```

---

### 4. Disburse Loan

**Endpoint:** `POST /loans/{loanId}/disburse`

**Response (200 OK):**
```json
{
  "loanId": "LOAN-789...",
  "status": "DISBURSED",
  "disbursedAt": "2026-01-01T12:00:00Z",
  "nextPaymentDue": "2026-02-01",
  "message": "Loan disbursed successfully. Funds credited to account ACC-123..."
}
```

---

### 5. Get Amortization Schedule

**Endpoint:** `GET /loans/{loanId}/amortization-schedule`

**Response (200 OK):**
```json
{
  "loanId": "LOAN-789...",
  "loanAmount": 50000.00,
  "interestRate": 5.25,
  "termMonths": 36,
  "monthlyPayment": 1501.48,
  "schedule": [
    {
      "installmentNumber": 1,
      "dueDate": "2026-02-01",
      "paymentAmount": 1501.48,
      "principalAmount": 1282.81,
      "interestAmount": 218.67,
      "remainingBalance": 48717.19,
      "paymentStatus": "PENDING"
    },
    {
      "installmentNumber": 2,
      "dueDate": "2026-03-01",
      "paymentAmount": 1501.48,
      "principalAmount": 1288.42,
      "interestAmount": 213.06,
      "remainingBalance": 47428.77,
      "paymentStatus": "PENDING"
    }
    // ... 34 more installments
  ],
  "totalPayments": 54053.28,
  "totalInterest": 4053.28
}
```

---

### 6. Make Payment

**Endpoint:** `POST /loans/{loanId}/payments`

**Request:**
```json
{
  "paymentAmount": 1501.48,
  "paymentDate": "2026-02-01"
}
```

**Response (200 OK):**
```json
{
  "paymentId": 1,
  "loanId": "LOAN-789...",
  "installmentNumber": 1,
  "paymentAmount": 1501.48,
  "principalAmount": 1282.81,
  "interestAmount": 218.67,
  "remainingBalance": 48717.19,
  "paidDate": "2026-02-01",
  "paymentStatus": "PAID",
  "message": "Payment processed successfully"
}
```

---

### 7. Get Loan Payments

**Endpoint:** `GET /loans/{loanId}/payments`

**Response (200 OK):**
```json
{
  "payments": [
    {
      "installmentNumber": 1,
      "paymentAmount": 1501.48,
      "principalAmount": 1282.81,
      "interestAmount": 218.67,
      "paidDate": "2026-02-01",
      "paymentStatus": "PAID"
    }
  ],
  "totalPaid": 1501.48,
  "totalPrincipalPaid": 1282.81,
  "totalInterestPaid": 218.67,
  "remainingBalance": 48717.19
}
```

---

### 8. Get Account Loans

**Endpoint:** `GET /loans/account/{accountNumber}`

**Response (200 OK):**
```json
{
  "loans": [
    {
      "loanId": "LOAN-789...",
      "loanType": "PERSONAL",
      "loanAmount": 50000.00,
      "outstandingBalance": 48717.19,
      "status": "ACTIVE",
      "nextPaymentDue": "2026-03-01"
    }
  ],
  "totalActiveLoans": 1,
  "totalOutstandingBalance": 48717.19
}
```

---

## Loan Types

### 1. Personal Loan
- **Purpose:** General personal expenses
- **Typical Amount:** €5,000 - €50,000
- **Typical Term:** 12-60 months
- **Interest Rate:** 5% - 15%
- **Collateral:** Unsecured

### 2. Home Loan (Mortgage)
- **Purpose:** Home purchase or construction
- **Typical Amount:** €50,000 - €500,000
- **Typical Term:** 120-360 months (10-30 years)
- **Interest Rate:** 3% - 6%
- **Collateral:** Property

### 3. Business Loan
- **Purpose:** Business expenses, equipment, expansion
- **Typical Amount:** €10,000 - €250,000
- **Typical Term:** 12-84 months
- **Interest Rate:** 6% - 12%
- **Collateral:** Business assets

### 4. Auto Loan
- **Purpose:** Vehicle purchase
- **Typical Amount:** €5,000 - €75,000
- **Typical Term:** 24-72 months
- **Interest Rate:** 4% - 10%
- **Collateral:** Vehicle

---

## Amortization Calculation

### Formula

**Monthly Payment (PMT):**
```
PMT = P × [r(1+r)^n] / [(1+r)^n - 1]

Where:
P = Principal (loan amount)
r = Monthly interest rate (annual rate / 12)
n = Number of payments (term in months)
```

### Java Implementation

```java
public BigDecimal calculateMonthlyPayment(
    BigDecimal principal,
    BigDecimal annualInterestRate,
    Integer termMonths
) {
    // Convert annual rate to monthly rate (decimal)
    BigDecimal monthlyRate = annualInterestRate
        .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
        .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);

    // Calculate (1 + r)^n
    BigDecimal onePlusRate = monthlyRate.add(BigDecimal.ONE);
    BigDecimal power = onePlusRate.pow(termMonths);

    // Calculate numerator: r * (1+r)^n
    BigDecimal numerator = monthlyRate.multiply(power);

    // Calculate denominator: (1+r)^n - 1
    BigDecimal denominator = power.subtract(BigDecimal.ONE);

    // Calculate monthly payment: P * [numerator / denominator]
    return principal
        .multiply(numerator.divide(denominator, 10, RoundingMode.HALF_UP))
        .setScale(2, RoundingMode.HALF_UP);
}
```

### Example Calculation

```
Loan Amount: €50,000
Annual Interest Rate: 5.25%
Term: 36 months

Monthly Rate: 5.25% / 12 = 0.4375% = 0.004375
Monthly Payment: €1,501.48

Total Amount: €1,501.48 × 36 = €54,053.28
Total Interest: €54,053.28 - €50,000 = €4,053.28
```

---

## Loan Lifecycle

### Application → Approval → Disbursement → Repayment → Completion

**1. Application (PENDING)**
- Customer submits loan application
- System calculates monthly payment and amortization schedule
- Loan saved with status PENDING

**2. Approval (APPROVED)**
- Loan officer reviews application
- Decision: Approve or Reject
- If approved, status changes to APPROVED

**3. Disbursement (DISBURSED)**
- Funds transferred to customer account
- Status changes to DISBURSED
- Next payment due date set (typically 1 month from disbursement)
- Amortization schedule activated

**4. Repayment (ACTIVE)**
- Customer makes monthly payments
- Each payment updates:
  - Outstanding balance (decreases)
  - Paid installments (increases)
  - Remaining installments (decreases)
  - Next payment due date (advances by 1 month)

**5. Completion (PAID_OFF)**
- All installments paid
- Outstanding balance = 0
- Status changes to PAID_OFF

**6. Default (DEFAULTED)**
- Payment overdue by 90+ days
- Status changes to DEFAULTED
- Collections process triggered

---

## Business Rules

### Loan Approval Criteria

1. **Credit Score** (future implementation)
   - Minimum score required varies by loan type
   - Personal: 650+, Home: 700+, Business: 680+, Auto: 640+

2. **Debt-to-Income Ratio**
   - Maximum 40% of monthly income
   - Calculated from existing loans + new loan

3. **Minimum Account Balance**
   - Account must have sufficient balance for first payment
   - Prevents immediate default

4. **Maximum Loan Amount**
   - Personal: €50,000
   - Home: €500,000
   - Business: €250,000
   - Auto: €75,000

### Payment Rules

1. **On-Time Payment**
   - Payment made on or before due date
   - Status: PAID

2. **Early Payment**
   - Payment made before due date
   - Allowed without penalty
   - Reduces outstanding balance

3. **Late Payment**
   - Payment made after due date
   - Status: OVERDUE
   - Late fee may apply (future)

4. **Partial Payment**
   - Payment less than monthly installment
   - Not currently supported
   - Future enhancement

### Prepayment

Customer can pay off loan early:
- Calculate remaining balance
- No prepayment penalty
- Update status to PAID_OFF
- Close amortization schedule

---

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanPaymentRepository loanPaymentRepository;

    @InjectMocks
    private LoanService loanService;

    @Test
    @DisplayName("Should calculate monthly payment correctly")
    void shouldCalculateMonthlyPaymentCorrectly() {
        // Given
        BigDecimal principal = new BigDecimal("50000.00");
        BigDecimal annualRate = new BigDecimal("5.25");
        Integer termMonths = 36;

        // When
        BigDecimal monthlyPayment = loanService.calculateMonthlyPayment(
            principal, annualRate, termMonths
        );

        // Then
        assertEquals(new BigDecimal("1501.48"), monthlyPayment);
    }

    @Test
    @DisplayName("Should generate correct amortization schedule")
    void shouldGenerateCorrectAmortizationSchedule() {
        // Given
        Loan loan = createTestLoan();

        // When
        List<LoanPayment> schedule = loanService.generateAmortizationSchedule(loan);

        // Then
        assertEquals(36, schedule.size());

        // First payment
        LoanPayment firstPayment = schedule.get(0);
        assertEquals(1, firstPayment.getInstallmentNumber());
        assertEquals(new BigDecimal("1501.48"), firstPayment.getPaymentAmount());
        assertTrue(firstPayment.getPrincipalAmount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(firstPayment.getInterestAmount().compareTo(BigDecimal.ZERO) > 0);

        // Last payment
        LoanPayment lastPayment = schedule.get(35);
        assertEquals(36, lastPayment.getInstallmentNumber());
        assertEquals(BigDecimal.ZERO, lastPayment.getRemainingBalance());
    }

    @Test
    @DisplayName("Should approve loan successfully")
    void shouldApproveLoanSuccessfully() {
        // Given
        Loan loan = createTestLoan();
        loan.setStatus(LoanStatus.PENDING);
        when(loanRepository.findById(loan.getLoanId())).thenReturn(Optional.of(loan));

        // When
        Loan approvedLoan = loanService.approveLoan(
            loan.getLoanId(),
            "loan.officer@bank.com"
        );

        // Then
        assertEquals(LoanStatus.APPROVED, approvedLoan.getStatus());
        assertEquals("loan.officer@bank.com", approvedLoan.getApprovedBy());
        assertNotNull(approvedLoan.getApprovedAt());
    }
}
```

---

**Last Updated:** 1 January 2026
**API Version:** 1.0
**Service Status:** ✅ Deployed (Needs Unit Tests)
**Swagger UI:** http://localhost:8088/swagger-ui/index.html
