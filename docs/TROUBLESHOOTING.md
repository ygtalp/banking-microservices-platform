# Troubleshooting Guide

> **Purpose:** Common issues and solutions  
> **Audience:** Developers, DevOps  
> **Last Updated:** 23 December 2025

---

## Table of Contents

1. [Maven Build Issues](#maven-build-issues)
2. [Service Discovery Issues](#service-discovery-issues)
3. [Docker Issues](#docker-issues)
4. [Database Issues](#database-issues)
5. [Redis Issues](#redis-issues)
6. [Kafka Issues](#kafka-issues)
7. [Circuit Breaker Issues](#circuit-breaker-issues)
8. [SAGA Issues](#saga-issues)

---

## Maven Build Issues

### Issue 1: @PathVariable Parameter Name Mismatch

**Symptom:**
```
HTTP 500 - Internal Server Error
Failed to convert value of type 'java.lang.String' to required type
```

**Root Cause:**
Maven compiler not preserving parameter names in bytecode.

**Solution:**
```java
// ❌ WRONG - Fails at runtime
@GetMapping("/accounts/{accountNumber}")
public ResponseEntity<AccountResponse> getAccount(
    @PathVariable String accountNumber  // No explicit name!
) { }

// ✅ CORRECT - Works reliably
@GetMapping("/accounts/{accountNumber}")
public ResponseEntity<AccountResponse> getAccount(
    @PathVariable("accountNumber") String accountNumber  // Explicit name
) { }
```

**Prevention:**
Always use explicit parameter names in @PathVariable annotations.

**Fix Applied:** December 10, 2025 (AccountController.java backup created)

---

## Service Discovery Issues

### Issue 2: Eureka Registration Delay

**Symptom:**
```
Transfer Service cannot find Account Service
FeignException: Service not available
```

**Root Cause:**
Initial registration takes 30-60 seconds due to heartbeat intervals.

**Solution:**
```yaml
# eureka-server/src/main/resources/application.yml
eureka:
  client:
    registerWithEureka: false
    fetchRegistry: false
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 10000  # 10 seconds
```

```yaml
# Service application.yml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 10    # Heartbeat every 10s
    lease-expiration-duration-in-seconds: 30 # Expire after 30s
  client:
    registry-fetch-interval-seconds: 10      # Fetch registry every 10s
```

**Verification:**
```bash
# Check Eureka dashboard
http://localhost:8761

# Look for service status: UP
```

**Workaround:**
Wait 30-60 seconds after starting services before making requests.

---

## Docker Issues

### Issue 3: Volume Permissions (Windows)

**Symptom:**
```
Error: Permission denied when mounting volumes
PostgreSQL container fails to start
```

**Root Cause:**
Windows file permissions incompatible with Linux containers.

**Solution:**
```powershell
# 1. Use WSL2 backend in Docker Desktop
Docker Desktop → Settings → General → Use WSL 2 based engine

# 2. Store project in WSL2 filesystem
wsl
cd ~
git clone https://github.com/{user}/banking-microservices-platform.git

# 3. Run from WSL2
cd banking-microservices-platform
./quick-start.ps1
```

**Alternative:**
Use Docker volumes instead of bind mounts:
```yaml
# docker-compose.yml
volumes:
  - postgres-data:/var/lib/postgresql/data  # Named volume

volumes:
  postgres-data:  # Define volume
```

---

### Issue 4: Port Already in Use

**Symptom:**
```
Error: Bind for 0.0.0.0:8081 failed: port is already allocated
```

**Solution:**
```powershell
# Windows - Find and kill process
netstat -ano | findstr :8081
taskkill /PID <PID> /F

# Or stop all Docker containers
docker-compose down

# Verify ports are free
netstat -ano | findstr "8080 8081 8082"
```

---

## Database Issues

### Issue 5: Connection Pool Exhaustion

**Symptom:**
```
HikariPool-1 - Connection is not available, request timed out after 30000ms
```

**Root Cause:**
Too many concurrent connections, pool size too small.

**Solution:**
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # Increase from default 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**Verification:**
```sql
-- Check active connections
SELECT count(*) FROM pg_stat_activity 
WHERE datname = 'banking_accounts';

-- Should be less than maximum-pool-size
```

---

### Issue 6: Database Schema Not Created

**Symptom:**
```
org.postgresql.util.PSQLException: ERROR: relation "accounts" does not exist
```

**Root Cause:**
JPA/Liquibase not running schema creation.

**Solution:**
```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # or 'create' for fresh start
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

**Manual Fix:**
```bash
# Connect to database
.\utils.ps1 -Command db

# Create tables manually
\i sql/schema.sql
```

---

## Redis Issues

### Issue 7: Cache Inconsistency

**Symptom:**
```
Account balance shows 1000.00 in cache
Database shows 1500.00 (after external update)
```

**Root Cause:**
Cache not invalidated when Account Service updated externally.

**Solution:**
```java
// Short TTL for data freshness
@Cacheable(value = "accounts", key = "#accountNumber", 
           cacheManager = "redisCacheManager")
public Account getAccount(String accountNumber) {
    // TTL: 5 minutes (configured in CacheConfig)
}

// Explicit invalidation on updates
@CacheEvict(value = "accounts", key = "#accountNumber")
public void updateAccount(String accountNumber, UpdateRequest request) {
    // ...
}
```

**Configuration:**
```yaml
spring:
  redis:
    ttl: 300  # 5 minutes in seconds
```

**Verification:**
```bash
# Connect to Redis
docker exec -it redis redis-cli

# Check keys
KEYS account:*

# Check TTL
TTL account:ACC-123

# Manual invalidation
DEL account:ACC-123
```

---

## Kafka Issues

### Issue 8: Topic Auto-Creation

**Symptom:**
```
Producer cannot send to topic 'account.events'
KafkaException: Topic does not exist
```

**Root Cause:**
Kafka auto-create disabled or service starts before Kafka ready.

**Solution:**
```yaml
# kafka in docker-compose.yml
KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
KAFKA_CREATE_TOPICS: "account.events:1:1,transfer.events:1:1"
```

**Manual Creation:**
```bash
# Enter Kafka container
docker exec -it kafka /bin/bash

# Create topic
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic account.events \
  --partitions 1 \
  --replication-factor 1

# Verify
kafka-topics.sh --list --bootstrap-server localhost:9092
```

**Restart Services:**
```powershell
docker-compose restart account-service
docker-compose restart transfer-service
```

---

## Circuit Breaker Issues

### Issue 9: False Positives (Circuit Opens Unnecessarily)

**Symptom:**
```
Circuit breaker OPEN for account-service
Transfer requests failing even though Account Service is healthy
```

**Root Cause:**
Threshold too sensitive (e.g., 30% failure rate).

**Solution:**
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        failureRateThreshold: 50        # 50% (was 30%)
        waitDurationInOpenState: 60s
        permittedNumberOfCallsInHalfOpenState: 3
```

**Monitoring:**
```bash
# Check circuit breaker metrics
curl http://localhost:8082/actuator/circuitbreakers
curl http://localhost:8082/actuator/circuitbreakerevents
```

**Manual Reset:**
```bash
# Restart Transfer Service
docker-compose restart transfer-service
```

---

## SAGA Issues

### Issue 10: Timeout in Long-Running Steps

**Symptom:**
```
Transfer status stuck in DEBIT_PENDING
FeignException: Read timed out
```

**Root Cause:**
Default timeout (5s) too short for Account Service operations.

**Solution:**
```yaml
# application.yml
feign:
  client:
    config:
      account-service:
        connectTimeout: 5000    # 5 seconds
        readTimeout: 10000      # 10 seconds (increased)
```

```java
// Individual request timeout
@FeignClient(
    name = "account-service",
    configuration = FeignConfig.class
)
public interface AccountServiceClient {
    
    @GetMapping("/accounts/{accountNumber}")
    @Headers("Request-Timeout: 10000")  // 10s timeout
    AccountBalanceResponse getAccount(@PathVariable String accountNumber);
}
```

**Verification:**
```bash
# Check transfer status
curl http://localhost:8082/transfers/TRF-xxx

# Should see status: COMPLETED (not stuck)
```

---

### Issue 11: SAGA Compensation Failed

**Symptom:**
```
Transfer status: FAILED
Log: "Compensation partially failed - Manual intervention required"
```

**Root Cause:**
- Debit succeeded
- Credit failed
- Compensation (reverse debit) also failed

**Solution:**
```bash
# 1. Check logs
.\utils.ps1 -Command logs -Service transfer-service

# 2. Check database state
.\utils.ps1 -Command db
SELECT * FROM transfers WHERE status = 'FAILED';
SELECT * FROM accounts WHERE account_number IN ('ACC-xxx', 'ACC-yyy');

# 3. Manual correction
-- If debit succeeded but credit failed:
UPDATE accounts SET balance = balance + 100.00 
WHERE account_number = 'ACC-source';

-- Mark transfer as compensated
UPDATE transfers SET status = 'COMPENSATED' 
WHERE transfer_reference = 'TRF-xxx';
```

**Prevention:**
```yaml
# Increase retry attempts
resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 1s
```

---

## Idempotency Issues

### Issue 12: Idempotency Key Expiration

**Symptom:**
```
Duplicate transfer created after 24 hours
Idempotency key expired
```

**Root Cause:**
Redis TTL (24h) expired, allowing duplicate requests.

**Solution:**
```yaml
# Increase TTL if needed
spring:
  redis:
    idempotency-ttl: 86400  # 24 hours (adjust if needed)
```

**Check Idempotency:**
```bash
# Redis CLI
docker exec -it redis redis-cli

# Check key
GET idempotency:UNIQUE-KEY-123

# Check TTL
TTL idempotency:UNIQUE-KEY-123
```

**Manual Fix:**
```bash
# If duplicate created, mark as duplicate in database
UPDATE transfers SET status = 'DUPLICATE' 
WHERE transfer_reference = 'TRF-duplicate';
```

---

## Event Ordering Issues

### Issue 13: Events Out of Order

**Symptom:**
```
BalanceChangedEvent received before AccountCreatedEvent
Consumer processing in wrong order
```

**Root Cause:**
Multiple partitions, no ordering guarantee.

**Solution:**
```java
// Use partition key for ordering
@Component
public class EventPublisher {
    
    public void publishAccountCreated(Account account) {
        kafkaTemplate.send(
            "account.events",
            account.getAccountNumber(),  // Partition key
            event
        );
    }
}
```

```yaml
# Configure single partition for strict ordering
spring:
  kafka:
    producer:
      properties:
        partitioner.class: org.apache.kafka.clients.producer.internals.DefaultPartitioner
```

**Verification:**
```bash
# Check partition assignment
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group account-service-group
```

---

## Quick Diagnostic Commands

```powershell
# Full diagnostic
.\scripts\debug\debug.ps1

# Service logs
.\utils.ps1 -Command logs -Service account-service

# Service status
.\utils.ps1 -Command status

# Database inspection
.\utils.ps1 -Command db

# Redis inspection
docker exec -it redis redis-cli

# Kafka topics
docker exec -it kafka kafka-topics.sh --list \
  --bootstrap-server localhost:9092

# Container health
docker-compose ps
```

---

## Emergency Recovery

### Complete System Reset

```powershell
# 1. Stop everything
docker-compose down -v

# 2. Clean build
mvn clean

# 3. Remove old images
docker system prune -a

# 4. Fresh start
.\quick-start.ps1
```

---

**Last Updated:** 23 December 2025  
**Status:** ✅ 10 Known Issues Documented
