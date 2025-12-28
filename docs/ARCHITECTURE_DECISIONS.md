# Architectural Decisions

> **Purpose:** Document all critical architectural decisions made during platform development  
> **Audience:** Developers, architects, stakeholders  
> **Last Updated:** 23 December 2025

---

## Overview

This document explains the 15 critical architectural decisions made during the development of the Banking Microservices Platform. Each decision includes context, alternatives considered, rationale, and consequences.

---

## Decision 1: Java 17 LTS

**Date:** December 2024  
**Status:** ✅ Accepted  
**Decision Maker:** Tech Lead

### Context

Need to select a Java version that:
- Provides long-term support
- Offers modern language features
- Meets banking sector requirements
- Ensures ecosystem compatibility

### Decision

**Use Java 17 LTS**

### Alternatives Considered

1. **Java 11 LTS**
   - ✅ Stable, well-tested
   - ✅ Wide ecosystem support
   - ❌ Missing modern features (records, sealed classes)
   - ❌ Virtual threads not available

2. **Java 21 LTS**
   - ✅ Newest features
   - ✅ Virtual threads production-ready
   - ❌ Too new, limited adoption
   - ❌ Ecosystem catching up

3. **Java 8 (Legacy)**
   - ✅ Maximum compatibility
   - ❌ End of life approaching
   - ❌ Missing critical features
   - ❌ Not future-proof

### Rationale

Java 17 chosen because:
1. **LTS Support:** Supported until September 2029
2. **Modern Features:**
   - Records (immutable data carriers)
   - Sealed classes (controlled inheritance)
   - Pattern matching improvements
   - Text blocks
3. **Virtual Threads Ready:** Preparation for Project Loom
4. **Industry Standard:** Becoming standard in banking sector
5. **Spring Boot 3.x:** Requires Java 17 minimum
6. **Ecosystem Maturity:** Good library support, stable

### Consequences

**Positive:**
- ✅ Access to modern language features
- ✅ Long-term support guaranteed
- ✅ Better performance (GC improvements)
- ✅ Future-proof for virtual threads

**Negative:**
- ❌ Requires JDK 17 in all environments
- ❌ Some legacy libraries may not support
- ❌ Team needs training on new features

**Mitigation:**
- Document Java 17 setup requirements
- Provide team training materials
- Gradual adoption of new features

---

## Decision 2: SAGA Pattern - Orchestration vs Choreography

**Date:** December 2024  
**Status:** ✅ Accepted  
**Decision Maker:** Tech Lead

### Context

Need to handle distributed transactions across microservices (Account Service and Transfer Service). Traditional 2PC (two-phase commit) not suitable for microservices due to:
- Long-held locks
- Tight coupling
- Reduced availability
- Poor scalability

### Decision

**Use Orchestration-based SAGA Pattern**

### Alternatives Considered

1. **Two-Phase Commit (2PC)**
   - ✅ ACID guarantees
   - ❌ Long-held locks (poor performance)
   - ❌ Synchronous coordination (low availability)
   - ❌ Not suitable for microservices

2. **Choreography-based SAGA**
   - ✅ Highly decoupled services
   - ✅ No central orchestrator
   - ❌ Complex debugging (no central view)
   - ❌ Difficult to understand flow
   - ❌ Harder to test
   - ❌ Risk of cyclic dependencies

3. **Event Sourcing + CQRS**
   - ✅ Complete audit trail
   - ✅ Replay capability
   - ❌ High complexity
   - ❌ Steep learning curve
   - ❌ Overkill for current scale

### Rationale

Orchestration-based SAGA chosen because:
1. **Centralized Control:** Single orchestrator in Transfer Service
2. **Clear Transaction Flow:** Easy to understand and visualize
3. **Easier Debugging:** All logic in one place
4. **Simpler Testing:** Can mock orchestrator
5. **Better Error Handling:** Centralized compensation logic
6. **Scale Appropriate:** Good for 2-10 services

### Implementation Details

```java
TransferSagaOrchestrator {
  Steps:
    1. ValidationStep
    2. DebitStep
    3. CreditStep
    
  Compensation:
    - Reverse order execution
    - Automatic rollback on failure
}
```

### Consequences

**Positive:**
- ✅ Clear transaction visibility
- ✅ Easier to maintain and debug
- ✅ Predictable behavior
- ✅ Testable compensation paths

**Negative:**
- ❌ Single point of orchestration
- ❌ Orchestrator can become complex
- ❌ Less loosely coupled than choreography

**Mitigation:**
- Keep orchestrator focused (only transaction flow)
- Extract step logic to separate classes
- Comprehensive testing of compensation paths

**Future Consideration:**
- If system scales to 20+ services, reconsider choreography
- Current orchestration suitable for 2-10 services

---

## Decision 3: Database per Service

**Date:** December 2024  
**Status:** ✅ Accepted  
**Decision Maker:** Tech Lead

### Context

Need to decide database strategy for microservices. Options range from shared database to complete isolation.

### Decision

**Each service has its own PostgreSQL database**

### Database Allocation

```
banking_accounts  → Account Service
banking_transfers → Transfer Service
banking_customers → Customer Service (future)
```

### Alternatives Considered

1. **Shared Database**
   - ✅ Simple to implement
   - ✅ Easy to query across services
   - ❌ Tight coupling
   - ❌ Schema changes affect all services
   - ❌ No independent scaling
   - ❌ Violates microservices principles

2. **Database per Service (Chosen)**
   - ✅ Data isolation
   - ✅ Independent scaling
   - ✅ Technology flexibility
   - ❌ More complex queries across services
   - ❌ Data consistency challenges

3. **Single Database, Schema per Service**
   - ✅ Some isolation
   - ❌ Still coupled at infrastructure level
   - ❌ Scaling limitations

### Rationale

Database per service chosen because:
1. **Bounded Context:** Each service owns its data
2. **Independent Scaling:** Scale databases independently
3. **Technology Flexibility:** Can use different DB types if needed
4. **Failure Isolation:** Database failure doesn't affect all services
5. **Microservices Best Practice:** Industry standard pattern

### Data Consistency Strategy

**For Distributed Transactions:**
- Use SAGA pattern (orchestration)
- Eventual consistency acceptable

**For Queries Across Services:**
- Use API calls (Feign client)
- Consider read models (future CQRS)

### Consequences

**Positive:**
- ✅ True service independence
- ✅ Can optimize each database separately
- ✅ Clear ownership boundaries
- ✅ Technology flexibility

**Negative:**
- ❌ Cannot use database joins across services
- ❌ Eventual consistency (not immediate)
- ❌ More complex to query across services

**Mitigation:**
- SAGA pattern for transactions
- Feign client for cross-service queries
- Event-driven sync for read models (future)

---

## Decision 4: BigDecimal for Money

**Date:** December 2024  
**Status:** ✅ Accepted - CRITICAL  
**Decision Maker:** Tech Lead

### Context

Need to represent monetary values accurately. This is CRITICAL in banking - errors in money calculations can have legal and financial consequences.

### Decision

**ALWAYS use `BigDecimal` for monetary amounts**

### Alternatives Considered

1. **float/double**
   - ❌ NEVER USE FOR MONEY
   - ❌ Binary floating point = rounding errors
   - ❌ Example: 0.1 + 0.2 = 0.30000000000000004

2. **int/long (cents)**
   - ✅ No rounding errors
   - ❌ Fixed precision (only cents)
   - ❌ Division problems
   - ❌ Currency conversion issues

3. **BigDecimal (Chosen)**
   - ✅ Arbitrary precision
   - ✅ Exact decimal representation
   - ✅ Banking standard
   - ✅ Regulatory compliance

### Rationale

BigDecimal chosen because:
1. **Accuracy:** No rounding errors
2. **Precision:** Arbitrary decimal places
3. **Standard:** Financial industry standard
4. **Compliance:** Meets regulatory requirements
5. **Safety:** Explicit rounding control

### Implementation Rules

```java
// ✅ CORRECT
BigDecimal amount = new BigDecimal("100.00");
BigDecimal price = BigDecimal.valueOf(99.95);

// ❌ WRONG - Don't use double constructor
BigDecimal wrong = new BigDecimal(0.1);  // Rounding error!

// ✅ Arithmetic operations
BigDecimal sum = amount.add(price);
BigDecimal difference = amount.subtract(price);
BigDecimal product = amount.multiply(BigDecimal.valueOf(0.1));
BigDecimal quotient = amount.divide(price, RoundingMode.HALF_UP);

// ✅ Comparison
if (amount.compareTo(BigDecimal.ZERO) > 0) {
    // amount is positive
}

// ❌ NEVER use equals() for comparison
if (amount.equals(BigDecimal.TEN)) {  // WRONG!
    // Scale difference causes false negatives
}
```

### Consequences

**Positive:**
- ✅ Accurate financial calculations
- ✅ Regulatory compliance
- ✅ No rounding errors
- ✅ Audit trail accuracy

**Negative:**
- ❌ Slightly slower than primitives
- ❌ More verbose code
- ❌ Requires explicit rounding mode

**Mitigation:**
- Performance impact negligible in I/O-bound systems
- Use helper methods for common operations
- Document rounding rules clearly

**CRITICAL RULE:**
```java
// NEVER, EVER use float or double for money
// ❌ float balance = 100.00f;   // ILLEGAL!
// ❌ double amount = 50.50;     // ILLEGAL!

// ✅ BigDecimal balance = new BigDecimal("100.00");
// ✅ BigDecimal amount = new BigDecimal("50.50");
```

---

## Decision 5: Redis for Caching and Idempotency

**Date:** December 2024  
**Status:** ✅ Accepted  
**Decision Maker:** Tech Lead

### Context

Need a caching solution and idempotency key storage for:
- Account data caching (reduce database load)
- Idempotency keys (prevent duplicate transfers)
- Session storage (future authentication)

### Decision

**Use Redis for caching and idempotency**

### Alternatives Considered

1. **In-Memory (Caffeine/Guava)**
   - ✅ Fast, simple
   - ❌ Not distributed (lost on restart)
   - ❌ Each service has separate cache
   - ❌ No shared state

2. **Memcached**
   - ✅ Fast, simple
   - ✅ Distributed
   - ❌ Limited data structures
   - ❌ No persistence
   - ❌ Less feature-rich than Redis

3. **Redis (Chosen)**
   - ✅ Fast in-memory storage
   - ✅ Rich data structures
   - ✅ TTL support
   - ✅ Atomic operations
   - ✅ Optional persistence
   - ✅ Pub/Sub support

4. **Hazelcast**
   - ✅ Feature-rich
   - ❌ Heavier than Redis
   - ❌ More complex setup
   - ❌ Overkill for current needs

### Rationale

Redis chosen because:
1. **Performance:** Extremely fast (sub-millisecond)
2. **TTL Support:** Automatic expiration for cache and keys
3. **Atomic Operations:** Safe for idempotency checks
4. **Data Structures:** Strings, hashes, sets, sorted sets
5. **Distributed:** Shared across service instances
6. **Battle-Tested:** Industry standard
7. **Spring Integration:** Excellent Spring Data Redis support

### Use Cases

**1. Account Caching**
```yaml
Key Pattern: account:{accountNumber}
TTL: 5 minutes (300 seconds)
Operations:
  - GET: Check cache first
  - SET: Cache DB results
  - DEL: Invalidate on updates
```

**2. Idempotency Keys**
```yaml
Key Pattern: idempotency:{key}
TTL: 24 hours
Operations:
  - SETNX: Set if not exists
  - GET: Check for duplicates
Value: Transfer reference
```

**3. IBAN Lookup Cache**
```yaml
Key Pattern: account:iban:{iban}
TTL: 5 minutes
Value: Account number
```

### Consequences

**Positive:**
- ✅ Significantly reduced database load
- ✅ Faster response times (cache hits)
- ✅ Reliable idempotency
- ✅ Scalable across instances

**Negative:**
- ❌ Additional infrastructure component
- ❌ Cache invalidation complexity
- ❌ Potential cache inconsistency

**Mitigation:**
- Short TTL (5 minutes) for data freshness
- Invalidate cache on all updates
- Use idempotency keys for critical operations
- Monitor cache hit rates

---

## Decision 6: Apache Kafka for Event Streaming

**Date:** December 2024  
**Status:** ✅ Accepted  
**Decision Maker:** Tech Lead

### Context

Need event streaming solution for:
- Asynchronous communication between services
- Event-driven architecture
- Audit logging
- Future event sourcing

### Decision

**Use Apache Kafka for event streaming**

### Alternatives Considered

1. **RabbitMQ**
   - ✅ Mature, reliable
   - ✅ Multiple messaging patterns
   - ✅ Easier to setup
   - ❌ Lower throughput than Kafka
   - ❌ No native replay capability
   - ❌ Less suitable for event streaming

2. **Apache Kafka (Chosen)**
   - ✅ High throughput (millions/sec)
   - ✅ Durable storage
   - ✅ Replay capability
   - ✅ Multiple consumers per topic
   - ✅ Industry standard for events
   - ❌ More complex than RabbitMQ

3. **AWS SNS/SQS**
   - ✅ Managed service
   - ❌ Cloud-specific (vendor lock-in)
   - ❌ Not suitable for on-premise

4. **Redis Streams**
   - ✅ Simple
   - ❌ Less mature than Kafka
   - ❌ Limited ecosystem

### Rationale

Kafka chosen because:
1. **Event Streaming Focus:** Built specifically for event logs
2. **High Throughput:** Handles millions of events per second
3. **Durability:** Persistent storage, configurable retention
4. **Replay:** Can replay events from any point
5. **Scalability:** Horizontal scaling with partitions
6. **Ecosystem:** Rich tooling (Kafka UI, connectors)
7. **Banking Standard:** Used by most banks

### Topics Structure

```yaml
account.events:
  - AccountCreatedEvent
  - AccountStatusChangedEvent
  - BalanceChangedEvent
  
transfer.events:
  - TransferInitiatedEvent
  - TransferCompletedEvent
  - TransferFailedEvent
  - TransferCompensatedEvent
```

### Consequences

**Positive:**
- ✅ Decoupled services
- ✅ Scalable event processing
- ✅ Complete audit trail
- ✅ Replay capability for debugging

**Negative:**
- ❌ More complex than simple messaging
- ❌ Requires Zookeeper (additional component)
- ❌ Learning curve for team

**Mitigation:**
- Use Kafka UI for monitoring
- Document topic naming conventions
- Provide Kafka training materials
- Start with simple pub/sub patterns

---

## Decision 7: Circuit Breaker with Resilience4j

**Date:** December 2024  
**Status:** ✅ Accepted  
**Decision Maker:** Tech Lead

### Context

Inter-service communication (Transfer Service → Account Service) can fail due to:
- Network issues
- Service downtime
- High latency
- Resource exhaustion

Need to prevent cascading failures.

### Decision

**Use Resilience4j Circuit Breaker on Feign clients**

### Pattern Explanation

```
Circuit Breaker States:

CLOSED → Normal operation
  ↓ (failure threshold exceeded)
OPEN → Reject requests immediately
  ↓ (wait duration elapsed)
HALF_OPEN → Allow limited requests
  ↓ (success) or (failure)
CLOSED or OPEN
```

### Configuration

```yaml
resilience4j.circuitbreaker:
  configs:
    default:
      slidingWindowSize: 10
      failureRateThreshold: 50        # 50% failures
      waitDurationInOpenState: 60s    # Wait before half-open
      permittedNumberOfCallsInHalfOpenState: 3
      automaticTransitionFromOpenToHalfOpenEnabled: true
```

### Alternatives Considered

1. **No Circuit Breaker**
   - ❌ Cascading failures
   - ❌ Resource exhaustion
   - ❌ Poor user experience

2. **Hystrix (Netflix)**
   - ✅ Mature
   - ❌ Maintenance mode (deprecated)
   - ❌ Heavy dependency

3. **Resilience4j (Chosen)**
   - ✅ Lightweight
   - ✅ Active development
   - ✅ Java 17 compatible
   - ✅ Functional programming style
   - ✅ Spring Boot integration

### Rationale

Resilience4j chosen because:
1. **Modern:** Built for Java 8+ functional style
2. **Lightweight:** No external dependencies
3. **Modular:** Pick only needed modules
4. **Active:** Still actively developed
5. **Spring Integration:** Excellent Spring Boot support

### Fallback Strategy

```java
@FeignClient(
    name = "account-service",
    fallbackFactory = AccountServiceClientFallbackFactory.class
)
public interface AccountServiceClient {
    // ...
}

// Fallback returns meaningful error
public class AccountServiceClientFallbackFactory {
    public AccountServiceClient create(Throwable cause) {
        return new AccountServiceClient() {
            public AccountBalanceResponse getAccount(String accountNumber) {
                throw new ServiceUnavailableException(
                    "Account Service temporarily unavailable"
                );
            }
        };
    }
}
```

### Consequences

**Positive:**
- ✅ Prevents cascading failures
- ✅ Fast failure (no waiting)
- ✅ Automatic recovery
- ✅ System stability

**Negative:**
- ❌ May reject valid requests when open
- ❌ Additional complexity
- ❌ Requires monitoring and tuning

**Mitigation:**
- Monitor circuit breaker states
- Alert on circuit opens
- Tune thresholds based on metrics
- Clear error messages to users

---

## Decision 8: Spring Cloud Eureka for Service Discovery

**Date:** December 2024  
**Status:** ✅ Accepted  
**Decision Maker:** Tech Lead

### Context

In microservices, services need to discover each other dynamically. Hard-coding service URLs is brittle and doesn't scale.

### Decision

**Use Netflix Eureka Server for service discovery**

### Alternatives Considered

1. **Consul (HashiCorp)**
   - ✅ Feature-rich (KV store, health checks)
   - ✅ Multi-datacenter support
   - ❌ More complex than needed
   - ❌ Additional operational overhead

2. **Zookeeper**
   - ✅ Mature, reliable
   - ❌ Not designed for service discovery
   - ❌ Complex configuration
   - ❌ Already using for Kafka

3. **Netflix Eureka (Chosen)**
   - ✅ Simple to setup
   - ✅ Spring Cloud integration
   - ✅ Self-healing
   - ✅ No external dependencies
   - ❌ Not as feature-rich as Consul

4. **Kubernetes DNS**
   - ✅ Native to K8s
   - ❌ Cloud-specific
   - ❌ Not suitable for Docker Compose

### Rationale

Eureka chosen because:
1. **Spring Integration:** First-class Spring Cloud support
2. **Simple:** Easy setup, minimal config
3. **Self-Healing:** Automatic peer replication
4. **No Dependencies:** Standalone Java application
5. **Battle-Tested:** Used by Netflix at scale

### Architecture

```
Eureka Server (8761)
    ↓ register
Account Service (8081)
Transfer Service (8082)
API Gateway (8080)
    ↓ discover
Client requests via service name
```

### Consequences

**Positive:**
- ✅ Dynamic service discovery
- ✅ Load balancing
- ✅ Health monitoring
- ✅ Failover support

**Negative:**
- ❌ 30-60s initial registration delay
- ❌ Additional component to manage
- ❌ Single point of failure (mitigated by clustering)

**Mitigation:**
- Fast registration config (10s intervals)
- Health check endpoints
- Document registration process
- Plan for Eureka clustering (production)

---

## Decisions 9-15: Summary

### Decision 9: Spring Cloud Gateway
**API Gateway pattern for routing, load balancing, and future auth integration**

### Decision 10: OpenFeign for Inter-Service Communication
**Declarative REST client with circuit breaker support**

### Decision 11: Docker Compose for Local Development
**Containerized deployment for reproducibility and easy setup**

### Decision 12: PowerShell for Automation Scripts
**Cross-platform scripting for build/deploy/test automation**

### Decision 13: Liquibase for Database Migrations
**Version-controlled schema changes and rollback capability**

### Decision 14: Idempotency Keys Pattern
**Client-provided keys for duplicate prevention in critical operations**

### Decision 15: Eventual Consistency Model
**Accept eventual consistency in favor of availability and partition tolerance (CAP theorem)**

---

## Decision Review Process

### When to Review

Decisions should be reviewed when:
1. System scale changes significantly (10x)
2. New requirements emerge
3. Technology landscape shifts
4. Performance issues arise
5. Annually (scheduled review)

### Review Checklist

```
□ Is the decision still valid?
□ Have alternatives improved?
□ Are consequences as expected?
□ Any new information?
□ Should we change course?
```

---

## References

- [Microservices Patterns](https://microservices.io/patterns/index.html)
- [SAGA Pattern](https://microservices.io/patterns/data/saga.html)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Martin Fowler's Blog](https://martinfowler.com/)

---

**Last Updated:** 23 December 2025  
**Next Review:** June 2026
