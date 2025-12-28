# Quality Assurance Agents

> **Category:** Code Quality & Security
> **Agent Count:** 4
> **Automation Level:** High (85%)
> **Last Updated:** 28 December 2025

---

## 1. CodeReviewAgent 👀

**Objective:** Automate code review against CLAUDE.md coding standards.

**Checks:**
- ✅ Naming conventions (PascalCase, camelCase, UPPER_SNAKE_CASE)
- ✅ @PathVariable explicit names
- ✅ @Transactional on data modifications
- ✅ BigDecimal for money (CRITICAL!)
- ✅ No sensitive data in logs
- ✅ Proper validation (@Valid, @NotNull)
- ✅ Error handling (@RestControllerAdvice)
- ✅ Test coverage (80%+)

**Output:**
```markdown
# Code Review Report: Account Service

## Critical Issues ❌ (Must Fix)
1. **float used for balance (AccountServiceImpl.java:45)**
   - Severity: CRITICAL
   - Issue: `float balance = 100.50f`
   - Fix: Use `BigDecimal balance = new BigDecimal("100.50")`
   - Reason: Financial accuracy requirement

2. **Missing @PathVariable name (AccountController.java:67)**
   - Severity: HIGH
   - Issue: `@PathVariable String accountNumber`
   - Fix: `@PathVariable("accountNumber") String accountNumber`

## Major Issues ⚠️ (Should Fix)
1. **Sensitive data logged (TransferService.java:123)**
   - Issue: `log.info("Transfer: {}", fullTransferObject)`
   - Fix: `log.info("Transfer: ref={}", transfer.getReference())`

## Minor Issues 💡 (Nice to Have)
1. **Magic number (AccountValidator.java:34)**
   - Issue: `if (balance > 100000)`
   - Fix: `public static final BigDecimal MAX_BALANCE = new BigDecimal("100000")`

## Summary
- Critical: 2 (MUST FIX BEFORE MERGE)
- Major: 1
- Minor: 1
- Compliant: 95%
```

---

## 2. SecurityAuditAgent 🛡️

**Objective:** Detect security vulnerabilities (OWASP Top 10, banking-specific).

**Checks:**
- SQL injection (never concatenate SQL)
- XSS vulnerabilities
- Sensitive data exposure
- Password handling (BCrypt, never plaintext)
- JWT security (signature, expiration)
- Authentication bypasses
- Authorization checks (@PreAuthorize)

**Output:**
```markdown
# Security Audit Report

## Critical Vulnerabilities 🚨
1. **SQL Injection Risk (CustomerRepository.java:45)**
   ```java
   // ❌ VULNERABLE
   @Query("SELECT c FROM Customer WHERE name = '" + name + "'")

   // ✅ SAFE
   @Query("SELECT c FROM Customer WHERE name = :name")
   ```

2. **Password Logged (AuthService.java:89)**
   ```java
   // ❌ DANGEROUS
   log.debug("Login attempt: {}, {}", email, password)

   // ✅ SAFE
   log.debug("Login attempt: {}", email)
   ```

## Recommendations
- ✅ Use @Query with parameters
- ✅ Never log passwords or tokens
- ✅ Validate all user inputs
- ✅ Use HTTPS in production
- ✅ Implement rate limiting

## Compliance
- OWASP Top 10: ✅ Compliant
- PCI DSS: ⚠️ Needs HTTPS
- GDPR: ✅ Compliant (data masking)
```

---

## 3. PerformanceAgent ⚡

**Objective:** Identify performance bottlenecks and optimization opportunities.

**Analysis:**
- N+1 query detection
- Missing database indexes
- Inefficient caching
- Large result sets
- Synchronous blocking calls
- Connection pool tuning

**Output:**
```markdown
# Performance Analysis: Customer Service

## Critical Issues 🐌
1. **N+1 Query Problem (CustomerService.java:67)**
   ```java
   // ❌ SLOW (1 + N queries)
   customers.forEach(c -> c.getDocuments().size())

   // ✅ FAST (1 query with JOIN FETCH)
   @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.documents")
   ```

2. **Missing Index (customers.email)**
   - Query: `SELECT * FROM customers WHERE email = ?`
   - Execution time: 250ms (full table scan)
   - Recommendation: `CREATE INDEX idx_email ON customers(email)`
   - Expected improvement: 250ms → 5ms

## Recommendations
- Add database indexes on frequently queried columns
- Enable Redis caching for customer lookup
- Use @Async for notification sending
- Implement pagination (default limit: 20)

## Benchmarks
| Operation | Before | After | Improvement |
|-----------|--------|-------|-------------|
| Get customer by email | 250ms | 5ms | 98% |
| List customers (1000) | 2.5s | 0.1s | 96% |
| Full KYC workflow | 3.2s | 1.8s | 44% |
```

---

## 4. BigDecimalAgent 💰

**Objective:** Ensure financial accuracy (CRITICAL for banking!).

**Checks:**
- ❌ float/double for money (ILLEGAL!)
- ✅ BigDecimal usage
- ✅ Correct rounding modes
- ✅ Precision/scale (19,2 for money)
- ✅ compareTo() vs equals()
- ✅ String constructor (not double)

**Output:**
```markdown
# Financial Accuracy Audit

## CRITICAL VIOLATIONS ❌
1. **float used for amount (TransferService.java:123)**
   ```java
   // ❌ ILLEGAL - Rounding errors!
   float amount = 100.50f;

   // ✅ CORRECT
   BigDecimal amount = new BigDecimal("100.50");
   ```
   **Impact:** Potential money loss due to rounding errors
   **Example:** 0.1 + 0.2 = 0.30000000000000004 (with double)

2. **Using equals() instead of compareTo() (AccountService.java:89)**
   ```java
   // ❌ WRONG - Scale difference causes false negative
   if (balance.equals(BigDecimal.TEN)) { }

   // ✅ CORRECT
   if (balance.compareTo(BigDecimal.TEN) == 0) { }
   ```

## Warnings ⚠️
1. **No explicit rounding mode (FeeCalculator.java:45)**
   ```java
   // ❌ May throw ArithmeticException
   BigDecimal fee = amount.divide(BigDecimal.valueOf(100));

   // ✅ SAFE
   BigDecimal fee = amount.divide(
       BigDecimal.valueOf(100),
       2,
       RoundingMode.HALF_UP
   );
   ```

## Summary
- float/double violations: 1 (MUST FIX!)
- compareTo violations: 1
- Rounding mode warnings: 1
- **Compliance:** 97% (FIX CRITICAL ISSUE!)

## Test Cases Generated
```java
@Test
void shouldPreventRoundingErrorsWithBigDecimal() {
    // This would fail with double
    BigDecimal result = new BigDecimal("0.1")
        .add(new BigDecimal("0.2"));
    assertEquals(new BigDecimal("0.3"), result);
}
```
```

---

**Next:** [DevOps Agents →](./06-devops.md)
