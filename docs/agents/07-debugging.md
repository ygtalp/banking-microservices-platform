# Debugging & Troubleshooting Agents

> **Category:** Error Resolution & Analysis
> **Agent Count:** 3
> **Automation Level:** Medium (60-70%)
> **Last Updated:** 28 December 2025

---

## 1. LogAnalyzerAgent 🔍

**Objective:** Parse logs, detect error patterns, and identify root causes.

**Capabilities:**
- Error pattern detection
- Stack trace analysis
- Frequency analysis
- Anomaly detection
- Root cause hypotheses

**Example Analysis:**
```markdown
# Log Analysis Report

## Time Period
2025-12-28 10:00:00 - 11:00:00 (1 hour)

## Error Summary
| Error Type | Count | Severity | First Seen | Last Seen |
|------------|-------|----------|------------|-----------|
| NullPointerException | 47 | HIGH | 10:15:23 | 10:58:42 |
| SocketTimeoutException | 12 | MEDIUM | 10:22:11 | 10:55:33 |
| DataIntegrityViolation | 3 | HIGH | 10:31:05 | 10:45:22 |

## Pattern Analysis

### Pattern 1: NullPointerException in TransferService
**Frequency:** 47 occurrences (every 1.3 minutes)
**Stack Trace:**
```
java.lang.NullPointerException
    at com.banking.transfer.service.TransferServiceImpl.executeTransfer(TransferServiceImpl.java:123)
    at com.banking.transfer.controller.TransferController.createTransfer(TransferController.java:67)
```

**Root Cause Hypothesis:**
Line 123: `transfer.getFeeAmount().add(amount)` - feeAmount is null when not provided in request.

**Recommended Fix:**
```java
// ❌ CURRENT (NPE when feeAmount is null)
BigDecimal total = transfer.getFeeAmount().add(amount);

// ✅ FIX
BigDecimal feeAmount = transfer.getFeeAmount() != null
    ? transfer.getFeeAmount()
    : BigDecimal.ZERO;
BigDecimal total = feeAmount.add(amount);
```

### Pattern 2: SocketTimeoutException to Account Service
**Frequency:** 12 occurrences
**Context:** All during 10:22-10:56 (34 minutes)

**Logs:**
```
10:22:11 ERROR [transfer-service] Timeout calling account-service
10:25:33 ERROR [transfer-service] Timeout calling account-service
...
```

**Root Cause Hypothesis:**
- Account Service degraded performance during this period
- Possible database connection pool exhaustion
- Circuit breaker opened at 10:30 (50% failure threshold)

**Recommendations:**
1. Check Account Service logs for same period
2. Review database connection pool metrics
3. Consider increasing timeout (currently 5s)
4. Check network latency

## Anomalies Detected
- ⚠️ Spike in transfer requests at 10:20 (300% above normal)
- ⚠️ Database query time increased 5x (50ms → 250ms)
- ⚠️ Redis cache miss rate: 80% (normally 20%)

## Action Items
- [ ] Fix NullPointerException (HIGH PRIORITY)
- [ ] Investigate Account Service performance
- [ ] Review database indexes
- [ ] Check Redis configuration
```

---

## 2. ErrorResolutionAgent 🩺

**Objective:** Match errors to known issues and provide solutions.

**Knowledge Base:**
- Common Spring Boot errors
- Banking platform-specific issues
- Database errors
- Network errors
- Configuration errors

**Example Resolution:**
```markdown
# Error Resolution Guide

## Error: "Database 'banking_customers' does not exist"

### Symptoms
```
org.postgresql.util.PSQLException: FATAL: database "banking_customers" does not exist
    at org.postgresql.core.v3.ConnectionFactoryImpl.doAuthentication
```

### Root Cause
Database not created during initial setup.

### Solution
```bash
# Option 1: Create database manually
docker exec banking-postgres psql -U postgres -c "CREATE DATABASE banking_customers;"

# Option 2: Run initialization script
docker exec banking-postgres psql -U postgres -f /docker-entrypoint-initdb.d/03-create-customer-db.sql

# Option 3: Recreate postgres container (will lose data!)
docker-compose down -v postgres
docker-compose up -d postgres
```

### Prevention
Add to docker-compose postgres service:
```yaml
postgres:
  volumes:
    - ./postgres-init:/docker-entrypoint-initdb.d
```

### Related Issues
- #12: Database initialization fails silently
- #34: Missing database creation script

---

## Error: "Eureka registration timeout"

### Symptoms
Service starts but doesn't appear in Eureka dashboard after 60 seconds.

### Root Cause
- Eureka server not ready
- Network connectivity issue
- Wrong Eureka URL

### Solution
```yaml
# Check Eureka is running
docker ps | grep eureka

# Check logs
docker logs eureka-server

# Verify configuration
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/  # ✅ Correct (Docker network)
      # NOT: http://localhost:8761/eureka/  # ❌ Wrong in Docker

# Wait longer (initial registration takes 30-60s)
```

### Prevention
- Health check Eureka before starting services
- Use Docker service names (not localhost)
- Configure faster registration intervals (dev only)
```

---

## 3. DependencyAgent 📦

**Objective:** Resolve Maven dependency conflicts and version mismatches.

**Capabilities:**
- Dependency tree analysis
- Conflict detection
- Version alignment
- Security vulnerability scanning
- Update suggestions

**Example Analysis:**
```markdown
# Dependency Analysis: Auth Service

## Conflicts Detected

### 1. jackson-databind version conflict
**Severity:** HIGH

**Conflict:**
```
[INFO] +- org.springframework.boot:spring-boot-starter-web:3.2.0
[INFO] |  +- com.fasterxml.jackson.core:jackson-databind:2.15.3
[INFO] +- com.fasterxml.jackson.core:jackson-databind:2.14.0 (conflict)
```

**Resolution:**
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.3</version> <!-- Use Spring Boot's version -->
</dependency>
```

Or better, let Spring Boot manage it:
```xml
<!-- Remove explicit jackson-databind dependency -->
<!-- Spring Boot starter already includes it -->
```

### 2. Multiple SLF4J bindings
**Severity:** MEDIUM

**Issue:**
```
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [logback-classic-1.4.11.jar]
SLF4J: Found binding in [slf4j-simple-1.7.36.jar]
```

**Resolution:**
```xml
<dependency>
    <groupId>some-dependency</groupId>
    <artifactId>artifact</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

## Security Vulnerabilities

### CVE-2023-xxxxx: jackson-databind < 2.15.3
**Severity:** CRITICAL
**Current Version:** 2.14.0
**Fixed In:** 2.15.3

**Action:** Upgrade immediately
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.3</version>
</dependency>
```

## Recommendations

### Update Spring Boot Parent
**Current:** 3.2.0
**Latest:** 3.2.1
**Benefits:** Security patches, bug fixes

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.1</version>
</parent>
```

### Remove Unused Dependencies
```xml
<!-- Not used anywhere -->
<dependency>
    <groupId>commons-lang</groupId>
    <artifactId>commons-lang</artifactId>
</dependency>
```

## Dependency Tree
```
mvn dependency:tree > dependency-tree.txt
```

## Commands
```bash
# Check for updates
mvn versions:display-dependency-updates

# Check for security vulnerabilities
mvn org.owasp:dependency-check-maven:check

# Resolve conflicts
mvn dependency:tree -Dverbose
```
```

---

**Next:** [Planning & Strategy Agents →](./08-planning.md)
