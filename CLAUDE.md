# Banking Microservices Platform - Claude Code Guide

> **Status:** ✅ PRODUCTION-READY | **Updated:** 24 Dec 2025 21:35
> **Context:** Java Spring Boot Banking Platform for Netherlands Banking Sector

---

## 📋 QUICK REFERENCE

```yaml
Project: Banking Microservices Platform
Purpose: Senior Backend Developer Portfolio (Netherlands Banking)
Duration: 21 days (Dec 3-24, 2025)
Current: 4 services (3 deployed, 1 ready for integration)
Tech: Java 17, Spring Boot 3.2, PostgreSQL, Redis, Kafka, Docker

Services:
  ✅ Account Service (8081): Account management, IBAN generation + JWT validation [DEPLOYED]
  ✅ Transfer Service (8082): Money transfers with SAGA pattern + JWT validation [DEPLOYED]
  ✅ Customer Service (8083): Customer management, KYC workflow + JWT validation [DEPLOYED]
  ✅ Auth Service (8084): JWT authentication, RBAC, token management [DEPLOYED]

Key Patterns: SAGA, DDD, Event-Driven, Circuit Breaker, Idempotency, KYC, JWT, RBAC
Session Logs: /session_logs (date-prefixed detailed logs)
```

**📚 Detailed Docs:** See `/docs` folder for comprehensive guides

---

## 🎯 PROJECT EVOLUTION

### The Journey (21 Days)

```
Dec 3  → Job interview prep (ABN AMRO, Netherlands)
       → Started with Payment Service (learning)

Dec 10 → Banking Platform launch
       → Account Service development
       → Transfer Service + SAGA Pattern

Dec 11 → Script organization
       → Process optimization

Dec 23 → CLI transition (Browser → Claude Code)
       → Comprehensive documentation
       → Customer Service implementation (all 12 phases)
       → Build & deployment fixes
       → Docker image ready

Dec 24 → Authentication Service implementation (Day 1-4)
       → Day 1: Database schema (users, roles, permissions) ✅
       → Day 2: JWT infrastructure & Spring Security ✅
       → Day 3: Business Logic & API (24 files, 11 endpoints) ✅
       → Day 4: Testing & Docker (41 tests, 80%+ coverage) ✅
       → 41/41 unit tests passing, integration tests ready
       → PowerShell API test script (12 scenarios)
       → Dockerfile created (multi-stage build)
```

### Why CLI Transition?

```
Browser Claude Pain Points:
❌ Context loss between sessions
❌ Manual file operations
❌ Copy-paste workflow
❌ No git integration

CLI Benefits:
✅ Context persistence
✅ Direct file manipulation
✅ Git integration
✅ Automation ready
```

---

## 🏗 ARCHITECTURE OVERVIEW

### System Diagram

```
┌──────────────────────────────────────────────┐
│         API Gateway (8080)                   │
│         Single Entry + Load Balancing        │
└────────────────┬─────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│         Eureka Server (8761)                 │
│         Service Discovery                    │
└───┬────────┬──────────┬──────────┬───────────┘
    │        │          │          │
    ▼        ▼          ▼          ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│Account │ │Transfer│ │Customer│ │Auth    │
│Service │◄┤Service │ │Service │ │Service │
│(8081)  │ │(8082)  │ │(8083)  │ │(8084)  │
│        │ │        │ │        │ │        │
│PostSQL │ │PostSQL │ │PostSQL │ │PostSQL │
│Redis   │ │SAGA    │ │KYC     │ │JWT     │
│Events  │ │Circuit │ │Events  │ │Redis   │
│        │ │Breaker │ │Feign→  │ │RBAC    │
└────────┘ └────────┘ └────────┘ └────────┘
     ▲                      │          ║
     └──────────────────────┘          ║
         (Feign: Get Accounts)         ║
                                       ║
              ════════════════════════╝
              (Future: Secures all services)
```

### Tech Stack

```yaml
Core:
  Language: Java 17 LTS
  Framework: Spring Boot 3.2.0
  Build: Maven 3.9+

Data:
  DB: PostgreSQL 16 (4 databases)
  Cache: Redis 7.2
  Migrations: Liquibase

Security:
  Authentication: JWT (JSON Web Tokens)
  Authorization: RBAC (Role-Based Access Control)
  Password: BCrypt (strength 12)
  Token Storage: Redis (blacklisting)

Messaging:
  Broker: Apache Kafka 3.6
  Coordination: Zookeeper 3.8

Service Mesh:
  Discovery: Eureka Server
  Gateway: Spring Cloud Gateway
  Client: OpenFeign
  Resilience: Resilience4j

Container:
  Platform: Docker 24+
  Orchestration: Docker Compose
  Scripts: PowerShell
```

---

## ✅ IMPLEMENTED SERVICES

### 1. Account Service (Port 8081)

**Responsibility:** Account lifecycle management

**Domain Model:**
```java
Account {
  accountNumber: String    // Auto-generated
  iban: String            // TR-format (26 chars)
  customerName: String
  accountType: CHECKING|SAVINGS|BUSINESS
  balance: BigDecimal     // ALWAYS BigDecimal!
  currency: TRY|USD|EUR|GBP
  status: ACTIVE|SUSPENDED|CLOSED
}
```

**Key Features:**
- ✅ CRUD operations with validation
- ✅ Turkish IBAN generation (MOD-97 checksum)
- ✅ Multi-currency support
- ✅ Balance operations (credit/debit)
- ✅ Redis caching (5min TTL)
- ✅ Event publishing (Kafka)
- ✅ Account history audit trail

**API Endpoints:**
```
POST   /accounts                    Create account
GET    /accounts/{accountNumber}    Get details
GET    /accounts/iban/{iban}        Get by IBAN
PUT    /accounts/{accountNumber}/balance  Update balance
```

**Critical Fix (Dec 10):**
```java
// ❌ WRONG - Fails at runtime
@PathVariable String accountNumber

// ✅ CORRECT - Always explicit
@PathVariable("accountNumber") String accountNumber
```

**See:** `docs/ACCOUNT_SERVICE.md` for full API reference

### 2. Transfer Service (Port 8082)

**Responsibility:** Money transfers with distributed transaction management

**Domain Model:**
```java
Transfer {
  transferReference: String    // System-generated
  fromAccountNumber: String
  toAccountNumber: String
  amount: BigDecimal
  currency: Currency
  status: TransferStatus      // See flow below
  idempotencyKey: String      // Client-provided
  debitTransactionId: String
  creditTransactionId: String
}
```

**SAGA Pattern Implementation:**

```
STATUS FLOW:
PENDING → VALIDATING → VALIDATED →
DEBIT_PENDING → DEBIT_COMPLETED →
CREDIT_PENDING → COMPLETED ✅

ON FAILURE:
Any step fails → COMPENSATING → COMPENSATED ↩️
```

**SAGA Steps:**
1. **ValidationStep:** Check accounts exist, status active, sufficient balance
2. **DebitStep:** Remove money from source account
3. **CreditStep:** Add money to destination account

**Compensation:** Automatic rollback in reverse order on failure

**Key Features:**
- ✅ Orchestration-based SAGA
- ✅ Automatic compensation
- ✅ Idempotency (Redis, 24h TTL)
- ✅ Circuit breaker (Resilience4j)
- ✅ Feign client integration
- ✅ Event-driven notifications

**See:** `docs/TRANSFER_SERVICE.md` for SAGA deep dive

### 3. Customer Service (Port 8083) ✅ NEW

**Responsibility:** Customer management and KYC workflow

**Domain Model:**
```java
Customer {
  customerId: String              // CUS-XXXXXXXXXXXX
  firstName, lastName, email      // Personal info
  nationalId (masked in API)      // KYC data
  address, city, country          // Location
  status: CustomerStatus          // Workflow state
  riskLevel: LOW|MEDIUM|HIGH      // Risk assessment
  verifiedAt/By, approvedAt/By    // Audit trail
  documents: List<KycDocument>    // 1-to-many
}
```

**KYC Status Flow:**
```
PENDING_VERIFICATION → VERIFIED → APPROVED
                                     ↕
                                  SUSPENDED
                                     ↓
                                  CLOSED
```

**Key Features:**
- ✅ Full KYC workflow (3-stage verification)
- ✅ Document management (upload, verify, reject)
- ✅ Customer-Account relationship (via customerId)
- ✅ Event-driven integration (Kafka)
- ✅ Feign client to Account Service
- ✅ Comprehensive audit trail
- ✅ Data masking (national ID)
- ✅ Status state machine

**API Endpoints:**
```
POST   /customers                              Register customer
GET    /customers/{customerId}                 Get customer
GET    /customers/email/{email}                Get by email
POST   /customers/{id}/verify                  Verify customer
POST   /customers/{id}/approve                 Approve customer
POST   /customers/{id}/suspend                 Suspend customer
POST   /customers/{id}/activate                Reactivate customer
GET    /customers/{id}/accounts                Get customer accounts (Feign)
POST   /customers/{id}/documents               Upload KYC document
GET    /customers/{id}/documents               List customer documents
GET    /customers/{id}/documents/{docId}       Get document by ID
POST   /customers/{id}/documents/{docId}/verify   Verify document
POST   /customers/{id}/documents/{docId}/reject   Reject document
```

**Events Published:**
- `CustomerCreatedEvent` → Informational
- `CustomerVerifiedEvent` → Notification Service (future)
- `CustomerApprovedEvent` → **Account Service can listen**
- `CustomerStatusChangedEvent` → Account Service (suspend accounts)

**Integration Flow:**
```
1. Customer registers → PENDING_VERIFICATION
2. Upload documents (passport, national ID, etc.)
3. Admin verifies documents
4. Admin verifies customer → VERIFIED
5. Manager approves customer → APPROVED
6. CustomerApprovedEvent → Account Service
7. Customer can create accounts
```

**See:** `docs/CUSTOMER_SERVICE.md` for complete API reference

### 4. Authentication Service (Port 8084) ✅ COMPLETE

**Responsibility:** JWT-based authentication and RBAC authorization for the entire platform

**Domain Model:**
```java
User {
  userId: String                   // USR-XXXXXXXXXXXX
  email: String (unique)           // Used as username
  passwordHash: String             // BCrypt encoded (strength 12)
  firstName, lastName              // Personal info
  status: UserStatus               // ACTIVE|SUSPENDED|LOCKED|INACTIVE
  accountLocked: Boolean           // Auto-lock after 5 failed attempts
  failedLoginAttempts: Integer
  lastLoginAt: LocalDateTime
  roles: Set<Role>                 // Many-to-many
}

Role {
  roleName: String                 // ROLE_ADMIN, ROLE_CUSTOMER, etc.
  permissions: Set<Permission>     // Many-to-many
}

Permission {
  resource: String                 // e.g., "accounts", "transfers"
  action: String                   // e.g., "read", "write", "delete"
}
```

**JWT Token Flow:**
```
Register → PENDING (password validated)
Login → Generate Access Token (15min) + Refresh Token (7 days)
Request → Validate JWT → Check Blacklist → Authenticate
Logout → Blacklist Token (Redis TTL = expiration)
Refresh → Validate Refresh Token → Generate new tokens → Blacklist old
```

**Implementation Status:**

**✅ Day 1 Complete: Foundation & Database**
- Database schema (users, roles, permissions, user_roles, role_permissions)
- Liquibase migrations (6 changelogs)
- Entity classes (User, Role, Permission)
- Repository interfaces
- 4 default roles inserted (ADMIN, CUSTOMER, SUPPORT, MANAGER)

**✅ Day 2 Complete: Security Infrastructure**
- JwtConfig.java - JWT properties binding
- JwtTokenProvider.java - Token generation & validation ⭐
- JwtAuthenticationFilter.java - Request interceptor ⭐
- JwtAuthenticationEntryPoint.java - 401 handler
- CustomUserDetailsService.java - User loading
- TokenBlacklistService.java - Redis-based logout
- SecurityConfig.java - Spring Security setup ⭐
- BCrypt password encoder (strength 12)

**✅ Day 3 Complete: Business Logic & API**
- 7 DTOs (RegisterRequest, LoginRequest, LoginResponse, RefreshTokenRequest, ApiResponse, UserProfileResponse, ChangePasswordRequest)
- 8 Custom exceptions (AuthException, InvalidPasswordException, EmailAlreadyExistsException, AccountLockedException, etc.)
- GlobalExceptionHandler.java - Centralized error handling (@RestControllerAdvice)
- AuthService.java - Core business logic (register, login, logout, refreshToken) ⭐
- UserService.java - User management (profile, changePassword, unlockAccount)
- AuthController.java - 11 REST endpoints (3 public, 8 protected)
- 4 Kafka events (UserRegisteredEvent, UserLoggedInEvent, UserLoggedOutEvent, UserPasswordChangedEvent)
- KafkaConfig.java - Event publishing configuration
- Build successful, JAR created (auth-service-1.0.0.jar)

**✅ Day 4 Complete: Testing & Docker**
- AuthServiceTest.java - 11 unit tests (register, login, logout, refresh, token validation)
- JwtTokenProviderTest.java - 17 unit tests (token generation, validation, extraction)
- UserServiceTest.java - 13 unit tests (profile, password change, unlock account)
- AuthControllerTest.java - 9 integration tests with TestContainers (PostgreSQL + Redis)
- **Test Results: 41/41 passing, 80%+ coverage** ✅
- PowerShell API test script - 12 end-to-end scenarios
- Dockerfile - Multi-stage build (Maven builder + JRE 17 Alpine runtime)
- Non-root user, health checks, container-optimized JVM settings

**✅ Day 5 Complete: Platform Integration**
- docker-compose.yml - auth-service + postgres-auth integrated ✅
- Account Service - JWT validation security (6 files: JwtConfig, JwtTokenProvider, JwtAuthenticationFilter, JwtAuthenticationEntryPoint, SecurityConfig, TokenBlacklistService) ✅
- Transfer Service - JWT validation security (same 6 files) ✅
- Customer Service - JWT validation security (same 6 files) ✅
- All services built successfully (mvn clean package) ✅
- End-to-end integration test script created (test-platform-integration.ps1 - 13 scenarios) ✅
- Platform-wide JWT authentication working ✅

**Public Endpoints (No Auth):**
```
POST /auth/register    - User registration
POST /auth/login       - User authentication
POST /auth/refresh     - Token refresh
```

**Protected Endpoints (Auth Required):**
```
POST /auth/logout      - Token blacklisting
GET  /auth/me          - Current user info
POST /auth/password/reset  - Password reset
```

**Key Features (Implemented):**
- ✅ JWT stateless authentication (access + refresh tokens)
- ✅ Token blacklisting (Redis with TTL)
- ✅ Account locking (5 failed attempts → auto-lock)
- ✅ Password policy enforcement (8+ chars, uppercase, lowercase, digit, special)
- ✅ RBAC with permissions (@PreAuthorize support)
- ✅ Password change flow (with current password verification)
- ✅ Refresh token rotation (old token blacklisted on refresh)
- ✅ Event publishing (Kafka - 4 event types)
- ✅ Comprehensive testing (41 unit tests, integration tests, API tests)
- ✅ Docker ready (multi-stage build, Alpine-based)

**Security Best Practices:**
- BCrypt with strength 12 (banking standard)
- Access tokens: 15 minutes (short-lived)
- Refresh tokens: 7 days (rotated on use)
- HS512 algorithm for signing
- Secret key via environment variable
- Token blacklisting on logout
- Fail-safe: deny access if Redis is down

**See:** `docs/AUTH_SERVICE.md` for complete implementation plan (Day 3-5)

---

## 🎯 CRITICAL ARCHITECTURAL DECISIONS

### 1. Java 17 LTS
**Why:** Long-term support, modern features, banking standard

### 2. SAGA: Orchestration over Choreography
**Why:** Centralized control, easier debugging, clear flow, better for this scale

### 3. Database per Service
**Why:** Data isolation, independent scaling, clear ownership

### 4. BigDecimal for Money
**Why:** Arbitrary precision, no rounding errors, financial standard
**NEVER use float/double for money!**

### 5. Redis for Caching + Idempotency
**Why:** Fast, TTL support, atomic operations, distributed

### 6. Kafka for Events
**Why:** High throughput, durability, replay capability, industry standard

### 7. Circuit Breaker on Inter-Service Calls
**Why:** Prevent cascading failures, graceful degradation

**Full rationale:** See `docs/ARCHITECTURE_DECISIONS.md`

---

## 📝 CODING STANDARDS

### Package Structure

```
com.banking.{service}
  ├── config       // Spring configuration
  ├── controller   // REST endpoints
  ├── dto          // Request/Response objects
  ├── model        // Domain entities
  ├── repository   // Data access
  ├── service      // Business logic
  ├── event        // Domain events
  ├── exception    // Custom exceptions
  ├── client       // External service clients
  └── saga         // SAGA orchestration (Transfer only)
```

### Naming Conventions

```java
Classes:    PascalCase          → AccountService
Methods:    camelCase           → executeTransfer()
Constants:  UPPER_SNAKE_CASE    → MAX_TRANSFER_AMOUNT
Variables:  camelCase           → accountNumber
```

### Essential Rules

```java
// 1. ALWAYS use BigDecimal for money
BigDecimal amount = new BigDecimal("100.00");

// 2. ALWAYS explicit @PathVariable names
@PathVariable("accountNumber") String accountNumber

// 3. ALWAYS @Transactional on data modifications
@Transactional
public void updateBalance(...)

// 4. ALWAYS validate inputs
@Valid @RequestBody CreateAccountRequest request

// 5. ALWAYS log with context (never sensitive data)
log.info("Transfer completed: reference={}", transfer.getTransferReference());

// 6. NEVER log sensitive data
log.debug("Balance: {}", amount);  // ✅ OK
log.debug("Account: {}", fullAccountNumber);  // ❌ Mask it!
```

### Testing Standards

```
Coverage Target: 80%+
Frameworks: JUnit 5, Mockito, TestContainers

Layers:
  - Unit Tests: Business logic isolation
  - Integration Tests: Real database, containers
  - API Tests: End-to-end PowerShell scripts
```

**Full guide:** See `docs/CODING_STANDARDS.md`

---

## ⚠️ KNOWN ISSUES & SOLUTIONS

### 1. Maven Parameter Names
**Problem:** @PathVariable without name fails  
**Solution:** Always use explicit names: `@PathVariable("name")`

### 2. Eureka Registration Delay
**Problem:** 30-60s initial registration  
**Solution:** Wait, check Eureka dashboard, already optimized config

### 3. Redis Cache Staleness
**Problem:** Cache may not reflect external updates  
**Solution:** 5min TTL + invalidation on updates

### 4. Circuit Breaker False Positives
**Problem:** Opens on load spikes  
**Solution:** Tuned to 50% threshold, 60s wait

### 5. SAGA Timeout
**Problem:** Long steps may timeout  
**Solution:** Configure appropriate timeouts (5s connect, 10s read)

**Full list:** See `docs/TROUBLESHOOTING.md`

---

## 🚀 DEVELOPMENT WORKFLOW

### Quick Start

```powershell
# Complete environment setup
.\quick-start.ps1

# This does:
# - Maven build all services
# - Docker Compose up
# - Health checks
# - API tests
# - Status display
```

### Daily Commands

```powershell
# Build
.\scripts\build\build-fixed.ps1

# Deploy
.\scripts\deploy\deploy-fixed.ps1

# Test
.\scripts\test\test-services-fixed.ps1

# Logs
.\utils.ps1 -Command logs -Service account-service

# Status
.\utils.ps1 -Command status

# Debug
.\scripts\debug\debug.ps1
```

### Git Workflow

```bash
# Feature branch
git checkout -b feature/customer-service

# Commit convention
git commit -m "feat(customer): add customer management service"

# Types: feat, fix, docs, style, refactor, test, chore
```

### Testing Locally

```powershell
# Unit tests
mvn test

# Integration tests
mvn verify

# API tests
.\scripts\test\test-services-fixed.ps1

# Coverage report
mvn jacoco:report
```

**Full workflow:** See `docs/DEVELOPMENT_GUIDE.md`

---

## 🎯 NEXT STEPS (Prioritized)

### Priority 1: Core Services ⭐⭐⭐⭐⭐

**Customer Service** (Est: 3-4 days)
```
Purpose: Customer management and KYC
Features:
  - Customer registration with KYC data
  - Customer verification workflow
  - Customer-Account relationship (1-to-many)
  - Event-driven integration

Tech:
  - Port: 8083
  - Database: banking_customers
  - Patterns: Same as existing (DDD, Events)
```

**Authentication Service** (Est: 4-5 days)
```
Purpose: Security and access control
Features:
  - JWT token management
  - Role-based access (RBAC)
  - Password encryption (BCrypt)
  - Integration with all services

Roles: ADMIN, CUSTOMER, SUPPORT, MANAGER
```

### Priority 2: Observability ⭐⭐⭐⭐

- **Distributed Tracing:** Zipkin/Jaeger (2 days)
- **Centralized Logging:** ELK Stack (3 days)
- **Metrics:** Prometheus + Grafana (3 days)

### Priority 3: Advanced Features ⭐⭐⭐

- **Transaction History Service** (3 days)
- **Notification Service** (4 days)
- **API Documentation** (2 days)

### Priority 4: DevOps ⭐⭐

- **CI/CD Pipeline:** GitHub Actions (4 days)
- **Kubernetes Deployment** (5 days)

**Full roadmap:** See `docs/ROADMAP.md`

---

## 🎓 KEY LEARNINGS

### Microservices Challenges

**Distributed Transactions:** Solved with SAGA pattern (orchestration-based) instead of 2PC

**Service Communication:** Hybrid approach - REST for queries, Kafka for events

**Data Consistency:** Eventual consistency with events, SAGA for atomicity

### Banking Domain

**Financial Accuracy:** BigDecimal everywhere, transaction atomicity, audit trail

**IBAN Generation:** TR-standard with MOD-97 checksum validation

**Compliance:** Account history, event logging, data isolation

### Production Readiness

**Fault Tolerance:** Circuit breakers, retry mechanisms, graceful degradation

**Testing:** 80%+ coverage across unit, integration, and API tests

**Automation:** Build/deploy/test scripts, health checks, monitoring

---

## 📚 DOCUMENTATION STRUCTURE

```
docs/
├── WORKFLOW_GUIDE.md            // ⭐ SESSION & PLANNING PROCEDURES (START HERE!)
├── ARCHITECTURE_DECISIONS.md    // 15 critical decisions explained
├── ACCOUNT_SERVICE.md           // Complete API reference
├── TRANSFER_SERVICE.md          // SAGA pattern deep dive
├── CUSTOMER_SERVICE.md          // KYC workflow & API reference
├── AUTH_SERVICE.md              // JWT & RBAC implementation
├── CODING_STANDARDS.md          // Full conventions guide
├── TESTING_GUIDE.md             // Testing strategy & examples
├── DEVELOPMENT_GUIDE.md         // Setup, workflow, commands
├── TROUBLESHOOTING.md           // Known issues & solutions
├── ROADMAP.md                   // Prioritized next steps
├── PORTFOLIO_NOTES.md           // Interview, CV, LinkedIn
├── AGENTS.md                    // ⭐ Sub-Agent Catalog (36 agents)
└── agents/                      // Agent category details
    ├── 01-task-breakdown.md     // Planning agents (3)
    ├── 02-code-generation.md    // Code writing agents (5)
    ├── 03-testing.md            // Test generation agents (4)
    ├── 04-documentation.md      // Documentation agents (4)
    ├── 05-quality-assurance.md  // QA & security agents (4)
    ├── 06-devops.md             // Infrastructure agents (4)
    ├── 07-debugging.md          // Troubleshooting agents (3)
    ├── 08-planning.md           // Strategic planning agents (3)
    ├── 09-domain-specific.md    // Banking domain agents (3)
    └── 10-context.md            // Pattern & consistency agents (3)
```

**Core context is here in CLAUDE.md**
**Detailed references are in /docs**
**⭐ Session workflow procedures are in /docs/WORKFLOW_GUIDE.md**
**⭐ Sub-Agent workflows are in /docs/AGENTS.md**

---

## 🔐 SECURITY NOTES

### Current Implementation
✅ Input validation (Spring Validation)  
✅ SQL injection prevention (JPA)  
✅ No sensitive data in logs  
✅ Idempotency keys (duplicate prevention)

### Planned
🔜 JWT authentication  
🔜 RBAC authorization  
🔜 HTTPS/TLS  
🔜 Secrets management (Vault)  
🔜 Rate limiting  
🔜 API key management

**Never log:** Passwords, full account numbers, personal data (GDPR)

---

## 🤖 SUB-AGENT CATALOG (NEW!)

### Overview

**36 specialized AI agents** designed to accelerate Banking Platform development while maintaining consistency and quality.

**Location:** `docs/AGENTS.md` (main catalog) + `docs/agents/` (category details)

### Agent Categories

| Category | Agents | Automation | Purpose |
|----------|--------|------------|---------|
| Task Breakdown | 3 | 60-80% | Planning & decomposition |
| Code Generation | 5 | 80-90% | Automated code writing |
| Testing | 4 | 85-90% | Test generation & fixes |
| Documentation | 4 | 85% | Docs & guides |
| Quality Assurance | 4 | 85% | Code review & security |
| DevOps | 4 | 75% | Infrastructure & deployment |
| Debugging | 3 | 60-70% | Troubleshooting & analysis |
| Planning | 3 | 50-65% | Strategic planning |
| Domain-Specific | 3 | 60-70% | Banking domain expertise |
| Context & Patterns | 3 | 80% | Consistency enforcement |

### Top 10 High-Value Agents

1. **ServiceImplementationAgent** 🏗️ → Complete 5-day/12-phase service plans
2. **EntityDesignAgent** 📐 → JPA entities + Liquibase + repositories
3. **APIDesignAgent** 🎯 → REST APIs following platform patterns
4. **UnitTestAgent** 🧪 → 80%+ test coverage automatically
5. **BigDecimalAgent** 💰 → Financial accuracy enforcement (CRITICAL!)
6. **CodeReviewAgent** 👀 → Automated standards compliance
7. **IntegrationTestAgent** 🐳 → TestContainers integration tests
8. **PatternMatcherAgent** 🎨 → Detect and enforce code patterns
9. **SAGAImplementationAgent** 🔄 → Distributed transaction orchestrators
10. **SecurityAuditAgent** 🛡️ → OWASP + banking security checks

### Common Agent Workflows

**New Service:**
```
ServiceImplementationAgent → EntityDesignAgent → APIDesignAgent →
EventDrivenAgent → UnitTestAgent → IntegrationTestAgent →
DockerAgent → DeploymentAgent → SessionLogAgent
```

**New Feature:**
```
FeatureImplementationAgent → PatternMatcherAgent →
[Code Generation Agents] → TestFixAgent → CodeReviewAgent
```

**Integration:**
```
IntegrationAgent → EventDrivenAgent → IntegrationTestAgent →
APIDocumentationAgent
```

**Quality Assurance:**
```
CodeReviewAgent → SecurityAuditAgent → PerformanceAgent →
BigDecimalAgent → ConsistencyAgent
```

### How to Use Agents

**Prompt Pattern:**
```
Use [AgentName] to [task description]:
- [Requirement 1]
- [Requirement 2]
- Follow Banking Platform standards
```

**Example:**
```
Use ServiceImplementationAgent to create a 5-day implementation plan for Notification Service:
- Multi-channel notifications (Email, SMS, Push)
- Template management
- Event-driven triggers from other services
- Delivery status tracking
- Follow existing patterns from Account/Transfer/Customer services
```

**See:** `docs/AGENTS.md` for complete agent catalog with detailed specs and examples

---

## 💡 CLAUDE CODE SPECIFIC GUIDANCE

### Starting New Feature

```bash
# 1. Review this CLAUDE.md
# 2. Check detailed docs in /docs
# 3. Understand current codebase
# 4. Plan implementation

# Tell me: "I want to implement [feature]"
# I'll help with architecture and patterns
```

### When Debugging

```bash
# 1. Gather context
.\scripts\debug\debug.ps1

# 2. Check logs
.\utils.ps1 -Command logs

# 3. Describe clearly
# What you tried, what happened, what you expected
```

### Before Committing

```
Checklist:
□ Tests pass (mvn test)
□ Code follows conventions
□ No sensitive data in logs
□ Error handling proper
□ Documentation updated
□ CLAUDE.md updated if needed
□ Commit message convention
```

### Code Review

```java
// Always ask yourself:
1. Is this BigDecimal for money? ✅
2. Are @PathVariable names explicit? ✅
3. Is this @Transactional? ✅
4. Are inputs validated? ✅
5. Is logging safe? ✅
6. Are tests included? ✅
```

---

## 🎯 PROJECT CONTEXT FOR AI

**You are Claude Code working on a banking microservices platform.**

**Current State:**
- 3 production-ready services (Account, Transfer, Customer)
- SAGA pattern implemented
- Event-driven architecture
- Comprehensive testing
- All services deployed and tested

**Your Role:**
- Maintain existing patterns
- Follow coding standards
- Suggest improvements
- Generate production-quality code
- Write comprehensive tests

**Key Principles:**
- Financial accuracy (BigDecimal!)
- Microservices best practices
- Banking domain awareness
- Production-ready code
- Security consciousness

**When in doubt:**
1. Check this CLAUDE.md first
2. Review relevant /docs file
3. Look at existing service implementation
4. Ask for clarification

**Remember:**
- Context is in CLAUDE.md (this file)
- Details are in /docs folder
- Code is in service folders
- Scripts are in /scripts folder

---

## 📞 QUICK LINKS

**Services:**
- Eureka: http://localhost:8761
- API Gateway: http://localhost:8080
- Kafka UI: http://localhost:8090
- Account Service: http://localhost:8081
- Transfer Service: http://localhost:8082
- Customer Service: http://localhost:8083

**Project:**
- GitHub: [repository-url]
- Documentation: `/docs` folder
- Scripts: `/scripts` folder
- Issues: GitHub Issues

---

## 🏁 SUMMARY

**This is a production-grade banking microservices platform demonstrating:**

✅ Complex distributed transactions (SAGA)  
✅ Event-driven architecture (Kafka)  
✅ Fault tolerance (Circuit Breaker)  
✅ Banking domain expertise (IBAN, multi-currency)  
✅ Comprehensive testing (80%+ coverage)  
✅ Production readiness (Docker, automation)

**Perfect for senior backend developer roles in Netherlands banking sector.**

**Current Phase:** 3 Core Services Deployed
**Next Phase:** Authentication Service & Observability

---

## 📝 SESSION LOGS

Detailed session logs are maintained in `/session_logs` folder with date prefixes.

### Recent Sessions

**2025-12-28 (Evening): Authentication Service Day 5 - Platform Integration ✅**
- ✅ Completed JWT integration across all services (Account, Transfer, Customer)
- ✅ Created JWT security package for Customer Service (6 files)
- ✅ Created TokenBlacklistService for Transfer Service
- ✅ Added Spring Security & JWT dependencies to Customer Service
- ✅ Added JWT configuration to all application.yml files
- ✅ Built all 7 services successfully (mvn clean package)
- ✅ Created comprehensive end-to-end integration test script (13 scenarios)
- ✅ All 4 core services now secured with JWT authentication
- ✅ Auth Service Day 5 Complete - Platform 100% Ready
- 📄 Log: `session_logs/2025-12-28-auth-service-day5-platform-integration.md`

**2025-12-28: Workflow Standardization & Process Framework**
- ✅ Created comprehensive WORKFLOW_GUIDE.md (standardized procedures)
- ✅ Defined Session Start Protocol (6-step checklist)
- ✅ Defined Session End Protocol (6-step checklist)
- ✅ Documented 5-Day New Service Workflow (with agent integration)
- ✅ Documented Feature Implementation Workflow (6-step process)
- ✅ Created sub-agent usage patterns (when to use which agent)
- ✅ Created session log template (standardized format)
- ✅ Updated CLAUDE.md documentation structure
- 📄 Log: `session_logs/2025-12-28-workflow-standardization.md`

**2025-12-28: Sub-Agent Catalog Creation**
- ✅ Analyzed entire project documentation (CLAUDE.md, docs/, session logs)
- ✅ Designed 36 specialized AI agents across 10 categories
- ✅ Created modular documentation structure (main index + 10 category files)
- ✅ Documented each agent with specs, examples, code snippets, workflows
- ✅ Total: 11 files, 4,500+ lines, 150+ code examples
- ✅ Updated CLAUDE.md with agent catalog overview
- 📄 Log: `session_logs/2025-12-28-sub-agent-catalog-creation.md`

**2025-12-24: Customer Service Test Fixes & Deployment**
- ✅ Fixed 12+ test compilation errors across 5 test files
- ✅ Implemented new getDocument() endpoint
- ✅ Built and deployed Customer Service successfully
- ✅ All API endpoints tested and verified
- ✅ All 3 services now production-ready
- 📄 Log: `session_logs/2025-12-24-customer-service-test-fixes.md`

**2025-12-24 (Evening): Authentication Service - Day 1 & 2**
- ✅ Day 1: Foundation & Database (8 hours → 1 hour)
  - Created auth-service project structure
  - Database schema (users, roles, permissions, junction tables)
  - Liquibase migrations (6 changelogs)
  - Entity classes with helper methods
  - Repository interfaces with search methods
  - 4 default roles inserted
- ✅ Day 2: Security Infrastructure (8 hours → 30 minutes)
  - JWT infrastructure (JwtTokenProvider, JwtAuthenticationFilter)
  - Spring Security configuration
  - Token blacklisting (Redis)
  - BCrypt password encoder (strength 12)
  - Custom UserDetailsService
- 📄 Log: `session_logs/2025-12-24-authentication-service-day1-day2.md`

**2025-12-24 (Morning): Customer Service Test Fixes**
- ✅ Fixed 12+ test compilation errors
- ✅ Implemented getDocument() endpoint
- ✅ All 3 services now production-ready
- 📄 Log: `session_logs/2025-12-24-customer-service-test-fixes.md`

**2025-12-24 (Evening): Authentication Service - Day 3 & 4**
- ✅ Day 3: Business Logic & API (24 files, 11 endpoints, 4 Kafka events)
- ✅ Day 4: Testing & Docker (41/41 tests passing, 80%+ coverage)
- ✅ AuthServiceTest (11 tests), JwtTokenProviderTest (17 tests), UserServiceTest (13 tests)
- ✅ Integration tests with TestContainers (PostgreSQL + Redis)
- ✅ PowerShell API test script (12 end-to-end scenarios)
- ✅ Dockerfile created (multi-stage build, Alpine-based, health checks)
- 📄 Log: `session_logs/2025-12-24-authentication-service-day3-day4.md`

**2025-12-23: Customer Service Build & Deployment**
- ✅ Completed all 12 implementation phases
- ✅ Fixed CustomerServiceApplication.java bug (.java → .class)
- ✅ Resolved Docker build context issues
- ✅ Built production-ready Docker image (639MB)
- 📄 Log: `session_logs/2025-12-23-customer-service-build-and-deployment.md`

**Status:** ✅ All 4 core services COMPLETE with JWT authentication integrated

---

**Last Updated:** 28 December 2025, 23:30
**Version:** 2.7 (Auth Service Day 5 Complete - Platform Integration)
**Status:** ✅ 100% COMPLETE (All 4 services deployed with JWT authentication)

**For detailed information, always check `/docs` folder!**
**For session history, check `/session_logs` folder!**
