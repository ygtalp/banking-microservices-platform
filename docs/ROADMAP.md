# Project Roadmap

> **Purpose:** Prioritized feature development plan  
> **Timeline:** Q1 2025 - Q2 2025  
> **Last Updated:** 23 December 2025

---

## Current Status ✅

```
✅ Account Service (Production-ready)
✅ Transfer Service with SAGA (Production-ready)
✅ API Gateway
✅ Service Discovery (Eureka)
✅ Redis Caching
✅ Kafka Events
✅ Docker Deployment
✅ Comprehensive Testing (80%+)
```

---

## Priority 1: Core Services ⭐⭐⭐⭐⭐

### 1.1 Customer Service
**Timeline:** 3-4 days  
**Status:** 🎯 Next

**Features:**
- Customer registration with KYC data
- Customer verification workflow (PENDING → VERIFIED → APPROVED)
- Customer-Account relationship (1-to-many)
- Event-driven integration with Account Service
- Search and filter customers

**Technical Details:**
```yaml
Service: Customer Service
Port: 8083
Database: banking_customers
Entities:
  - Customer (id, name, email, phone, nationalId, status)
  - KYCDocument (type, number, expiryDate, verified)
  - CustomerAccount (linking table)
Events:
  - CustomerCreatedEvent
  - CustomerVerifiedEvent
  - CustomerAccountLinkedEvent
```

**API Endpoints:**
```
POST   /customers              Create customer
GET    /customers/{id}         Get customer details
PUT    /customers/{id}/verify  Verify KYC
POST   /customers/{id}/accounts Link account
GET    /customers/{id}/accounts List customer accounts
```

**Patterns:**
- Domain-Driven Design
- Event-driven integration
- Status-based workflow
- One-to-many relationship with accounts

---

### 1.2 Authentication & Authorization Service
**Timeline:** 4-5 days  
**Status:** 🔜 High Priority

**Features:**
- JWT token generation and validation
- Role-Based Access Control (RBAC)
- Password encryption (BCrypt)
- Token refresh mechanism
- API key management
- Integration with all services

**Technical Details:**
```yaml
Service: Auth Service
Port: 8084
Database: banking_auth
Entities:
  - User (username, passwordHash, roles, enabled)
  - Role (name, permissions)
  - RefreshToken (token, expiryDate)
Security:
  - Spring Security
  - JWT (JSON Web Tokens)
  - BCrypt password hashing
```

**Roles:**
```
ADMIN       → Full system access
CUSTOMER    → Own account access
SUPPORT     → Read-only customer data
MANAGER     → Approve operations
```

**API Endpoints:**
```
POST   /auth/login            Login and get JWT
POST   /auth/refresh          Refresh access token
POST   /auth/logout           Invalidate token
POST   /auth/register         Register new user
GET    /auth/validate         Validate token
```

**Integration:**
- API Gateway: JWT validation filter
- All Services: @PreAuthorize annotations
- Customer Service: User creation on customer registration

---

## Priority 2: Observability ⭐⭐⭐⭐

### 2.1 Distributed Tracing (Zipkin/Jaeger)
**Timeline:** 2 days  
**Status:** 🔜 Observability Critical

**Implementation:**
```yaml
Tracing:
  - Spring Cloud Sleuth
  - Zipkin Server (port 9411)
  - Trace ID propagation across services
  - Span creation for SAGA steps
```

**Benefits:**
- Track request flow across services
- Identify performance bottlenecks
- Debug distributed transactions
- Visualize SAGA execution

---

### 2.2 Centralized Logging (ELK Stack)
**Timeline:** 3 days  
**Status:** 🔜 Production Requirement

**Components:**
```yaml
ELK Stack:
  - Elasticsearch: Log storage
  - Logstash: Log aggregation
  - Kibana: Visualization (port 5601)
  - Filebeat: Log shipping
```

**Log Structure:**
```json
{
  "timestamp": "2025-12-23T10:30:00Z",
  "service": "account-service",
  "traceId": "abc123",
  "level": "INFO",
  "message": "Account created",
  "accountNumber": "ACC-xxx",
  "userId": "user123"
}
```

---

### 2.3 Metrics & Monitoring (Prometheus + Grafana)
**Timeline:** 3 days  
**Status:** 🔜 Production Requirement

**Metrics:**
```yaml
Business Metrics:
  - Accounts created per hour
  - Transfers completed/failed
  - Average transfer amount
  - SAGA compensation rate

Technical Metrics:
  - Request latency (p50, p95, p99)
  - Error rate
  - Circuit breaker state
  - Database connection pool
  - Cache hit rate
```

**Dashboards:**
- Service Health Overview
- SAGA Transaction Monitoring
- Business Metrics Dashboard
- Infrastructure Monitoring

---

## Priority 3: Advanced Features ⭐⭐⭐

### 3.1 Transaction History Service
**Timeline:** 3 days  
**Status:** 📋 Planned

**Features:**
- Complete transaction audit trail
- Event sourcing pattern
- Advanced filtering and search
- Export capabilities (CSV, PDF)
- Real-time balance calculation

**Technical Details:**
```yaml
Service: Transaction History Service
Port: 8085
Database: banking_transactions
Pattern: Event Sourcing
Storage: Time-series optimized
```

---

### 3.2 Notification Service
**Timeline:** 4 days  
**Status:** 📋 Planned

**Features:**
- Multi-channel notifications (Email, SMS, Push)
- Template management
- Event-driven triggers
- Delivery status tracking
- User preferences

**Channels:**
```yaml
Channels:
  - Email (SendGrid/AWS SES)
  - SMS (Twilio)
  - Push Notifications (Firebase)
  - In-app notifications
```

**Triggers:**
```
- Account created → Welcome email
- Transfer completed → SMS notification
- Large transfer → Push notification + Email
- Account suspended → Email alert
```

---

### 3.3 API Documentation (OpenAPI 3.0)
**Timeline:** 2 days  
**Status:** 📋 Planned

**Implementation:**
```yaml
Tools:
  - SpringDoc OpenAPI
  - Swagger UI (interactive docs)
  - ReDoc (static docs)
  - Postman collection generation
```

**Features:**
- Interactive API testing
- Request/response examples
- Authentication flows
- Error response documentation

---

## Priority 4: DevOps & Production ⭐⭐

### 4.1 CI/CD Pipeline (GitHub Actions)
**Timeline:** 4 days  
**Status:** 📋 Planned

**Pipeline Stages:**
```yaml
Stages:
  1. Build (Maven compile)
  2. Test (Unit + Integration)
  3. Code Quality (SonarQube)
  4. Security Scan (OWASP Dependency Check)
  5. Docker Build & Push
  6. Deploy to Staging
  7. E2E Tests
  8. Deploy to Production (manual approval)
```

**Tools:**
- GitHub Actions
- SonarQube
- Docker Hub
- Snyk (security)

---

### 4.2 Kubernetes Deployment
**Timeline:** 5 days  
**Status:** 📋 Planned

**Resources:**
```yaml
K8s Resources:
  - Deployments (each service)
  - Services (ClusterIP, LoadBalancer)
  - ConfigMaps (configuration)
  - Secrets (credentials)
  - Ingress (API Gateway)
  - HPA (Horizontal Pod Autoscaler)
  - PVC (Persistent Volume Claims)
```

**Helm Charts:**
- banking-platform-chart
- Sub-charts for each service
- Environment-specific values

---

## Priority 5: Testing & Quality ⭐⭐⭐

### 5.1 Comprehensive Test Coverage
**Timeline:** 3 days  
**Status:** 📋 Planned

**Target:** 85%+ coverage

**Focus Areas:**
- Unit tests for new services
- Integration tests with TestContainers
- Contract tests (Spring Cloud Contract)
- SAGA compensation scenarios
- Performance tests (JMeter)

---

### 5.2 Performance Testing
**Timeline:** 3 days  
**Status:** 📋 Planned

**Tests:**
```yaml
Load Tests:
  - 100 req/s for 10 minutes
  - Account creation throughput
  - Transfer processing latency

Stress Tests:
  - Gradually increase to 500 req/s
  - Find breaking point
  - Test circuit breaker behavior

Endurance Tests:
  - 50 req/s for 2 hours
  - Check for memory leaks
  - Database connection stability
```

**Tools:**
- JMeter
- Gatling
- K6

---

## Priority 6: Business Features ⭐⭐

### 6.1 Transfer Limits & Rules Engine
**Timeline:** 2 days  
**Status:** 📋 Future

**Features:**
- Daily transfer limits per account
- Velocity checks (3 transfers/hour max)
- Amount-based rules
- Time-based restrictions
- Configurable rule engine

---

### 6.2 Fees & Commissions
**Timeline:** 3 days  
**Status:** 📋 Future

**Features:**
- Fee calculation engine
- Transaction type-based fees
- Percentage and fixed fees
- Fee history and reporting
- Configurable fee rules

---

## Milestones

### Milestone 1: Core Platform (Current)
**Date:** December 2025  
**Status:** ✅ COMPLETED
- Account Service
- Transfer Service with SAGA
- Infrastructure (Docker, Kafka, Redis)

### Milestone 2: Security & Observability
**Date:** January 2026  
**Status:** 🎯 IN PROGRESS
- Customer Service
- Authentication Service
- Distributed Tracing
- Centralized Logging

### Milestone 3: Production Readiness
**Date:** February 2026  
**Status:** 📋 PLANNED
- CI/CD Pipeline
- Kubernetes Deployment
- Performance Testing
- Security Hardening

### Milestone 4: Advanced Features
**Date:** March 2026  
**Status:** 📋 PLANNED
- Transaction History
- Notification Service
- API Documentation
- Business Rules Engine

---

## Resource Allocation

```
Week 1-2:   Customer Service + Auth Service
Week 3:     Distributed Tracing + Logging
Week 4:     Metrics & Monitoring
Week 5-6:   CI/CD + K8s Deployment
Week 7:     Performance Testing
Week 8:     Transaction History + Notifications
```

---

## Success Metrics

```yaml
Technical:
  - 99.9% uptime
  - < 200ms p95 latency
  - 85%+ test coverage
  - 0 critical security vulnerabilities

Business:
  - 100 accounts/second creation rate
  - < 3s transfer completion time
  - 95%+ successful SAGA completion
  - < 1% compensation rate
```

---

**Last Updated:** 23 December 2025  
**Next Review:** January 15, 2026  
**Status:** 🎯 2/14 Features Complete
