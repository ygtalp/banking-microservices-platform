# Planning & Strategy Agents

> **Category:** Strategic Planning & Migration
> **Agent Count:** 3
> **Automation Level:** Medium (50-65%)
> **Last Updated:** 28 December 2025

---

## 1. ServicePlannerAgent 📋

**Objective:** Create comprehensive 5-day or 12-phase implementation plans for new microservices.

**Plans Generated:**
- Day-by-day breakdown (Auth Service pattern)
- 12-phase implementation (Customer Service pattern)
- File structure planning
- Timeline estimation
- Risk assessment

**Example (5-Day Plan):**
```markdown
# Transaction History Service - 5-Day Implementation Plan

## Day 1: Foundation & Database (8 hours)
- Database schema (transactions table, indexes)
- Entity classes (Transaction, TransactionType enum)
- Repository interfaces
- Liquibase migrations

**Deliverables:** 12 files
**Success Criteria:** Database schema created, entities tested

## Day 2: Core Business Logic (8 hours)
- TransactionService implementation
- Event sourcing pattern
- Balance calculation logic
- Transaction filtering (date, type, amount)

**Deliverables:** 8 files
**Success Criteria:** Service layer 80%+ test coverage

## Day 3: API & Event Integration (8 hours)
- REST API endpoints (list, search, export)
- Kafka event consumers (account.events, transfer.events)
- DTOs and validation
- Export functionality (CSV, PDF)

**Deliverables:** 15 files
**Success Criteria:** All endpoints working, events consumed

## Day 4: Testing & Quality (6 hours)
- Unit tests (service layer)
- Integration tests (TestContainers)
- API tests (PowerShell)
- Performance testing (1M records)

**Deliverables:** 10 test files
**Success Criteria:** 85%+ coverage, all tests passing

## Day 5: Docker & Deployment (4 hours)
- Dockerfile (multi-stage build)
- docker-compose configuration
- Deployment scripts
- Documentation

**Deliverables:** 5 files
**Success Criteria:** Service deployed, health checks passing

## Total Estimation
- Time: 34 hours (~5 days)
- Files: 50+ files
- Tests: 40+ tests
- Endpoints: 8 API endpoints
```

---

## 2. RefactoringAgent 🔨

**Objective:** Plan and execute code refactoring to reduce technical debt.

**Refactoring Patterns:**
- Extract Method
- Extract Class
- Rename
- Move Method
- Introduce Parameter Object
- Replace Conditional with Polymorphism

**Example:**
```markdown
# Refactoring Plan: AccountService

## Code Smells Detected

### 1. Long Method (AccountServiceImpl.createAccount)
**Current:** 87 lines
**Target:** < 30 lines per method

**Refactoring:**
```java
// ❌ BEFORE (87 lines)
public AccountResponse createAccount(CreateAccountRequest request) {
    // Validate request (15 lines)
    // Generate account number (10 lines)
    // Generate IBAN (25 lines)
    // Create entity (10 lines)
    // Save to database (5 lines)
    // Publish event (10 lines)
    // Map to response (12 lines)
}

// ✅ AFTER (Extracted methods)
public AccountResponse createAccount(CreateAccountRequest request) {
    validateRequest(request);
    Account account = buildAccount(request);
    account = saveAccount(account);
    publishAccountCreated(account);
    return mapToResponse(account);
}

private void validateRequest(CreateAccountRequest request) { }
private Account buildAccount(CreateAccountRequest request) { }
private String generateAccountNumber() { }
private String generateIban(String accountNumber) { }
private Account saveAccount(Account account) { }
private void publishAccountCreated(Account account) { }
```

### 2. Duplicate Code (IBAN Generation)
**Found in:** AccountService, IbanValidator
**Recommendation:** Extract to IbanGenerator utility class

```java
// ✅ NEW UTILITY CLASS
public class IbanGenerator {
    public static String generate(String countryCode, String accountNumber) {
        String bban = "00" + accountNumber;
        String checkDigits = calculateCheckDigits(countryCode + bban);
        return countryCode + checkDigits + bban;
    }

    private static String calculateCheckDigits(String iban) {
        // MOD-97 calculation
    }
}
```

### 3. Feature Envy (TransferService accessing Account fields)
**Issue:** TransferService knows too much about Account internals

**Refactoring:** Introduce methods in Account class
```java
// ❌ BEFORE (Feature Envy)
if (account.getStatus() == AccountStatus.ACTIVE &&
    account.getBalance().compareTo(amount) >= 0 &&
    !account.isFrozen()) {
    // Transfer logic
}

// ✅ AFTER (Tell, Don't Ask)
if (account.canDebit(amount)) {
    // Transfer logic
}

// In Account class
public boolean canDebit(BigDecimal amount) {
    return this.status == AccountStatus.ACTIVE &&
           this.balance.compareTo(amount) >= 0 &&
           !this.frozen;
}
```

## Refactoring Steps
1. ✅ Write tests for current behavior
2. ⏳ Extract methods (in progress)
3. ⏳ Extract classes
4. ⏳ Run tests (verify no breakage)
5. ⏳ Code review
6. ⏳ Deploy

## Risk Assessment
- **Low Risk:** Extract method (safe with tests)
- **Medium Risk:** Extract class (affects imports)
- **High Risk:** Change interfaces (breaking change)

## Estimated Time: 12 hours
```

---

## 3. MigrationAgent 🔄

**Objective:** Plan technology migrations with minimal downtime.

**Migration Types:**
- Database version upgrade
- Service discovery (Eureka → Consul)
- Java version upgrade
- Spring Boot upgrade
- Message broker (Kafka → RabbitMQ)

**Example (PostgreSQL 16 → 17):**
```markdown
# Migration Plan: PostgreSQL 16 → 17

## Motivation
- Performance improvements (20% faster queries)
- JSON improvements
- Security patches

## Compatibility Analysis

### Breaking Changes
✅ None affecting our usage
✅ All queries compatible
✅ JDBC driver compatible

### New Features We Can Use
- Incremental sorting (faster ORDER BY)
- Better vacuum performance
- Improved JSONB indexing

## Migration Strategy: Blue-Green Deployment

### Phase 1: Preparation (1 day)
- [ ] Backup production database
- [ ] Test migration on staging
- [ ] Verify application compatibility
- [ ] Prepare rollback plan

### Phase 2: Setup PostgreSQL 17 (2 hours)
```yaml
# docker-compose.yml
postgres-17:
  image: postgres:17-alpine
  environment:
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: postgres
  volumes:
    - postgres-17-data:/var/lib/postgresql/data
  ports:
    - "5433:5432"  # Different port
```

### Phase 3: Data Migration (30 minutes)
```bash
# Dump from PostgreSQL 16
docker exec banking-postgres pg_dump -U postgres banking_accounts > accounts.sql

# Restore to PostgreSQL 17
docker exec postgres-17 psql -U postgres -c "CREATE DATABASE banking_accounts"
docker exec -i postgres-17 psql -U postgres banking_accounts < accounts.sql

# Verify data
docker exec postgres-17 psql -U postgres banking_accounts -c "SELECT COUNT(*) FROM accounts"
```

### Phase 4: Switch Services (Rolling Update)
```bash
# Update account-service to use PostgreSQL 17
# Update environment variable
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-17:5432/banking_accounts

# Restart service
docker-compose restart account-service

# Verify health
curl http://localhost:8081/actuator/health

# If OK, proceed with other services
# If issues, rollback immediately
```

### Phase 5: Monitoring (24 hours)
- Monitor query performance
- Check error rates
- Compare metrics with PostgreSQL 16

### Phase 6: Decommission PostgreSQL 16 (1 week later)
```bash
# If no issues after 1 week
docker-compose stop postgres-16
docker volume rm postgres-16-data
```

## Rollback Plan
```bash
# If issues detected
docker-compose stop postgres-17

# Revert services to PostgreSQL 16
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/banking_accounts
docker-compose restart account-service

# Time to rollback: < 5 minutes
# Data loss: None (16 still has all data)
```

## Testing Checklist
- [ ] All existing tests pass
- [ ] Performance tests (same or better)
- [ ] Load test (100 req/s for 10 min)
- [ ] Backup/restore procedure
- [ ] Rollback procedure

## Timeline
- Preparation: 1 day
- Migration (off-peak): 2 hours
- Monitoring: 24 hours
- Sign-off: 1 week
- **Total:** 10 days (conservative)

## Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| Query performance regression | HIGH | Load testing before cutover |
| Incompatibility | HIGH | Staging environment testing |
| Data loss during migration | CRITICAL | Multiple backups, verification |
| Downtime | MEDIUM | Blue-green deployment (zero downtime) |
```

---

**Next:** [Domain-Specific Agents →](./09-domain-specific.md)
