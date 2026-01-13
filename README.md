# Banking Microservices Platform

> **Production-ready banking platform with 12 microservices, complete observability, and AML/SEPA compliance**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Passing-success.svg)](https://github.com/ygtalp/banking-microservices-platform)
[![Coverage](https://img.shields.io/badge/Coverage-80%25-brightgreen.svg)](https://github.com/ygtalp/banking-microservices-platform)

---

## 📖 Overview

A **comprehensive banking microservices platform** demonstrating enterprise-grade architecture patterns including SAGA orchestration, event-driven design, multi-currency support, real-time fraud detection, and full regulatory compliance (AML/KYC/SEPA/PSD2).

Built as a **senior backend developer portfolio** for the Dutch/European banking sector, this platform showcases production-ready code with 80%+ test coverage, complete observability, and modern DevOps practices.

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                       API Gateway (8080)                                  │
│                   Single Entry + Load Balancing                           │
└────────────────────────────┬─────────────────────────────────────────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                       Eureka Server (8761)                                  │
│                       Service Discovery                                     │
└─┬─────┬─────┬─────┬─────┬─────┬─────┬──────┬──────┬──────┬──────┬────────┘
  │     │     │     │     │     │     │      │      │      │      │
  ▼     ▼     ▼     ▼     ▼     ▼     ▼      ▼      ▼      ▼      ▼
┌──────────────────────────────────────────────────────────────────────────┐
│ CORE BANKING SERVICES                                                     │
├──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬────────┤
│Account   │Transfer  │Customer  │Auth      │Notify    │Transaction│        │
│(8081)    │(8082)    │(8083)    │(8084)    │(8085)    │(8086)     │        │
│          │          │          │          │          │           │        │
│PostgreSQL│PostgreSQL│PostgreSQL│PostgreSQL│PostgreSQL│PostgreSQL │        │
│Redis     │SAGA      │KYC       │JWT+MFA   │Kafka     │Events     │        │
│Events    │Circuit   │Events    │RBAC      │Redis     │Redis      │        │
│          │Breaker   │Feign     │Events    │Templates │Audit      │        │
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│ COMPLIANCE & RISK SERVICES                                                │
├──────────┬──────────┬──────────┬──────────┐                               │
│Fraud     │AML       │SEPA      │          │                               │
│(8087)    │(8093)    │(8092)    │          │                               │
│          │          │          │          │                               │
│Rules     │Sanctions │ISO 20022 │          │                               │
│Risk Score│Screening │IBAN      │          │                               │
│Events    │Cases     │BIC       │          │                               │
│Redis     │Reporting │Batches   │          │                               │
└──────────┴──────────┴──────────┴──────────┘                               │

┌──────────────────────────────────────────────────────────────────────────┐
│ PRODUCT SERVICES                                                          │
├──────────┬──────────┬──────────┐                                          │
│Loan      │Card      │Statement │                                          │
│(8088)    │(8089)    │(8091)    │                                          │
│          │          │          │                                          │
│PostgreSQL│PostgreSQL│PostgreSQL│                                          │
│Decisions │Lifecycle │PDF Gen   │                                          │
│Events    │Security  │Reports   │                                          │
└──────────┴──────────┴──────────┘                                          │

┌──────────────────────────────────────────────────────────────────────────┐
│ OBSERVABILITY STACK                                                       │
├─────────────────┬─────────────────┬─────────────────┬───────────────────┤
│ Zipkin (9411)   │ Prometheus (9090)│ Grafana (3000)  │ ELK Stack         │
│ Distributed     │ Metrics          │ Dashboards      │ Centralized Logs  │
│ Tracing         │ Collection       │ Visualization   │ Search & Analytics│
└─────────────────┴─────────────────┴─────────────────┴───────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│ DATA & MESSAGING LAYER                                                    │
├─────────────────┬─────────────────┬─────────────────┐                    │
│ PostgreSQL (5432)│ Redis (6379)    │ Kafka (9092)    │                    │
│ 12 Databases     │ Cache + Sessions│ Event Streaming │                    │
│ Liquibase        │ Idempotency     │ Zookeeper (2181)│                    │
└─────────────────┴─────────────────┴─────────────────┘                    │
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 17** (LTS)
- **Maven 3.9+**
- **Docker Desktop** (for Docker Compose)
- **Git**

### 1. Clone Repository

```bash
git clone https://github.com/ygtalp/banking-microservices-platform.git
cd banking-microservices-platform
```

### 2. Build All Services

```bash
mvn clean package -DskipTests
```

### 3. Start Infrastructure & Services

```bash
docker-compose up -d
```

This will start:
- **12 Microservices** (Account, Transfer, Customer, Auth, Notification, Transaction, Fraud Detection, Loan, Card, Statement, AML, SEPA)
- **Infrastructure:** PostgreSQL, Redis, Kafka, Zookeeper, Kafka UI
- **Observability:** Zipkin, Prometheus, Grafana, Elasticsearch, Logstash, Kibana
- **Service Discovery:** Eureka Server
- **API Gateway:** Spring Cloud Gateway

### 4. Verify Services

```bash
# Check all services are UP
curl http://localhost:8761

# Check API Gateway
curl http://localhost:8080/actuator/health

# View Swagger UI (example - Account Service)
open http://localhost:8081/swagger-ui.html
```

### 5. Access Dashboards

- **Eureka Dashboard:** http://localhost:8761
- **API Gateway:** http://localhost:8080
- **Kafka UI:** http://localhost:8090
- **Zipkin (Tracing):** http://localhost:9411
- **Prometheus (Metrics):** http://localhost:9090
- **Grafana (Dashboards):** http://localhost:3000 (admin/admin)
- **Kibana (Logs):** http://localhost:5601

---

## 📦 Services Overview

| Service | Port | Purpose | Key Features | Documentation |
|---------|------|---------|--------------|---------------|
| **Account Service** | 8081 | Account lifecycle management | IBAN generation, multi-currency, Redis cache, events | [Docs](docs/ACCOUNT_SERVICE.md) |
| **Transfer Service** | 8082 | Money transfers | SAGA orchestration, idempotency, circuit breaker | [Docs](docs/TRANSFER_SERVICE.md) |
| **Customer Service** | 8083 | Customer management | KYC workflow, document verification, events | [Docs](docs/CUSTOMER_SERVICE.md) |
| **Auth Service** | 8084 | Authentication & Authorization | JWT, MFA (OTP/TOTP), RBAC, token blacklisting | [Docs](docs/AUTH_SERVICE.md) |
| **Notification Service** | 8085 | Multi-channel notifications | Email, SMS, Push, In-App, templates, preferences | [Docs](docs/NOTIFICATION_SERVICE.md) |
| **Transaction Service** | 8086 | Transaction history | Event-driven recording, audit trail, analytics | [Docs](docs/TRANSACTION_SERVICE.md) |
| **Fraud Detection** | 8087 | Real-time fraud detection | 6-rule engine, risk scoring, manual review | [Docs](docs/FRAUD_DETECTION_SERVICE.md) |
| **Loan Service** | 8088 | Loan management | Loan lifecycle, applications, approvals | [Docs](docs/LOAN_SERVICE.md) |
| **Card Service** | 8089 | Card management | Card issuance, activation, blocking, security | [Docs](docs/CARD_SERVICE.md) |
| **Statement Service** | 8091 | Statement generation | PDF generation, transaction reports, caching | [Docs](docs/STATEMENT_SERVICE.md) |
| **SEPA Service** | 8092 | SEPA payments | ISO 20022, IBAN/BIC validation, batches, R-transactions | [Docs](docs/SEPA_SERVICE.md) |
| **AML Service** | 8093 | Anti-Money Laundering | Sanctions screening, risk scoring, regulatory reporting | [Docs](docs/AML_SERVICE.md) |

---

## 🛠️ Technology Stack

### Core Framework
- **Language:** Java 17 LTS
- **Framework:** Spring Boot 3.2.0
- **Build Tool:** Maven 3.9+

### Data Layer
- **Database:** PostgreSQL 15 (12 databases)
- **ORM:** Jakarta Persistence (JPA) with Hibernate
- **Migrations:** Liquibase (50+ migrations)
- **Cache:** Redis 7.2

### Messaging & Events
- **Message Broker:** Apache Kafka 3.6
- **Coordination:** Apache Zookeeper 3.8

### Microservices Patterns
- **Service Discovery:** Spring Cloud Eureka
- **API Gateway:** Spring Cloud Gateway
- **Client Communication:** OpenFeign
- **Resilience:** Resilience4j (Circuit Breaker, Retry, Rate Limiter)

### Security
- **Authentication:** JWT (JSON Web Tokens)
- **Authorization:** RBAC (Role-Based Access Control)
- **Password Hashing:** BCrypt (strength 12)
- **MFA:** TOTP (Time-based OTP), Email/SMS OTP
- **Token Storage:** Redis (blacklisting)

### Observability
- **Distributed Tracing:** Zipkin (Brave)
- **Metrics:** Micrometer + Prometheus
- **Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)
- **Dashboards:** Grafana (3 pre-built dashboards)

### API Documentation
- **Standard:** OpenAPI 3.0
- **UI:** Swagger UI (springdoc-openapi 2.2.0)
- **Security:** Bearer JWT authentication documented

### DevOps & Deployment
- **Containerization:** Docker
- **Orchestration:** Docker Compose
- **Health Checks:** Spring Boot Actuator

---

## ✨ Key Features

### Enterprise Architecture Patterns

✅ **SAGA Pattern** → Distributed transaction management with automatic compensation
✅ **Event-Driven Architecture** → Kafka-based asynchronous communication
✅ **Circuit Breaker** → Resilience4j for fault tolerance
✅ **Idempotency** → Redis-based duplicate prevention (24h TTL)
✅ **Database per Service** → Data isolation and independent scaling
✅ **API Gateway Pattern** → Single entry point with routing

### Banking Domain Expertise

✅ **Turkish IBAN Generation** → MOD-97 checksum validation
✅ **Multi-Currency Support** → TRY, USD, EUR, GBP
✅ **SEPA Compliance** → ISO 20022 XML, IBAN/BIC validation, 36 countries
✅ **AML/CFT Compliance** → Sanctions screening (OFAC, EU, UN), risk scoring
✅ **KYC Workflows** → 3-stage customer verification
✅ **Fraud Detection** → Real-time risk scoring with 6 detection rules

### Security & Compliance

✅ **JWT Authentication** → HS512 with 15min access + 7-day refresh tokens
✅ **Multi-Factor Authentication** → TOTP + Email/SMS OTP
✅ **Role-Based Access Control** → 4 default roles (ADMIN, CUSTOMER, SUPPORT, MANAGER)
✅ **Account Locking** → Auto-lock after 5 failed attempts
✅ **Token Blacklisting** → Redis-based logout with TTL
✅ **Input Validation** → Jakarta Validation annotations
✅ **Password Policy** → 8+ chars, uppercase, lowercase, digit, special

### Financial Accuracy

✅ **BigDecimal Everywhere** → No rounding errors (precision 19, scale 2)
✅ **Pessimistic Locking** → Concurrent transaction safety
✅ **Transaction Audit Trail** → Immutable event sourcing
✅ **Balance Snapshots** → Before/after tracking for compliance

### Observability & Monitoring

✅ **Distributed Tracing** → Zipkin with trace ID propagation
✅ **Metrics Collection** → Prometheus scraping all services (15s interval)
✅ **Centralized Logging** → ELK Stack with JSON format
✅ **Pre-built Dashboards** → Grafana (System Overview, JVM, Business metrics)
✅ **Health Checks** → Actuator endpoints with liveness/readiness probes

---

## 📊 Project Statistics

- **Services:** 12 microservices + 2 infrastructure (Eureka, API Gateway)
- **Lines of Code:** ~50,000 (production Java)
- **Entities:** 30+ JPA entities
- **Repositories:** 30+ Spring Data repositories
- **REST Endpoints:** 150+ (documented with OpenAPI)
- **Database Migrations:** 50+ Liquibase changelogs
- **Kafka Events:** 20+ event types
- **Test Coverage:** 80%+ (unit + integration tests)
- **Documentation:** 22 comprehensive docs (18,000+ lines)

---

## 📁 Project Structure

```
banking-microservices-platform/
├── account-service/           # Account lifecycle management
├── transfer-service/          # Money transfers with SAGA
├── customer-service/          # Customer + KYC management
├── auth-service/              # JWT authentication + RBAC
├── notification-service/      # Multi-channel notifications
├── transaction-service/       # Transaction history + audit
├── fraud-detection-service/   # Real-time fraud detection
├── loan-service/              # Loan management
├── card-service/              # Card issuance + management
├── statement-service/         # Statement generation
├── aml-service/               # AML compliance + sanctions
├── sepa-service/              # SEPA payment processing
├── eureka-server/             # Service discovery
├── api-gateway/               # API gateway
├── observability/             # Monitoring configs
│   ├── prometheus/            # Prometheus config
│   ├── grafana/               # Grafana dashboards
│   └── logstash/              # Logstash pipeline
├── postgres-init/             # Database initialization scripts
├── docs/                      # Comprehensive documentation
├── scripts/                   # Build/deploy/test scripts
├── session_logs/              # Implementation session logs
├── docker-compose.yml         # Complete stack orchestration
├── pom.xml                    # Maven parent POM
├── CLAUDE.md                  # Project context (master doc)
└── README.md                  # This file
```

---

## 🧪 Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Run All Tests with Coverage

```bash
mvn clean verify jacoco:report
```

Coverage reports will be generated at:
- `{service}/target/site/jacoco/index.html`

### API Testing

PowerShell scripts provided for end-to-end testing:

```powershell
# Test all services
.\scripts\test\test-services-fixed.ps1

# Test specific service
.\scripts\test\test-account-service.ps1
```

---

## 📚 Documentation

### Core Documentation
- **[Project Context (CLAUDE.md)](CLAUDE.md)** → Complete project overview, architecture decisions, 30-day development journey
- **[Roadmap](docs/ROADMAP.md)** → Implementation phases, current status, future plans
- **[Architecture Decisions](docs/ARCHITECTURE_DECISIONS.md)** → 15+ ADRs explaining critical decisions
- **[Development Guide](docs/DEVELOPMENT_GUIDE.md)** → Setup, workflow, commands
- **[Coding Standards](docs/CODING_STANDARDS.md)** → Code conventions, patterns
- **[Testing Guide](docs/TESTING_GUIDE.md)** → Test strategy, examples
- **[Observability](docs/OBSERVABILITY.md)** → Monitoring stack setup and usage
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** → Known issues and solutions

### Service Documentation
Each service has comprehensive documentation including:
- Domain model and business logic
- API reference (all endpoints)
- Configuration options
- Event schemas
- Testing strategies
- Deployment instructions

See [docs/](docs/) folder for complete service documentation.

---

## 🎯 Use Cases

### For Portfolio
- **Demonstrate senior-level architecture skills** (SAGA, event-driven, microservices)
- **Show banking domain expertise** (IBAN, SEPA, AML, fraud detection)
- **Prove production-ready code** (80%+ test coverage, observability, security)
- **Highlight modern tech stack** (Spring Boot 3.2, Kafka, Docker, Kubernetes-ready)

### For Learning
- **Study microservices patterns** (well-documented, clear code structure)
- **Understand banking systems** (realistic domain models, compliance)
- **Practice DevOps** (Docker Compose, observability stack)
- **Explore event-driven design** (Kafka integration examples)

### For Interviews
- **Technical Talking Points:** SAGA orchestration, circuit breakers, JWT security, fraud detection algorithms, SEPA ISO 20022
- **Business Value:** Compliance-ready (AML/KYC/SEPA), production-grade observability, comprehensive audit trails
- **Scale & Performance:** Designed for horizontal scaling, caching strategy, async event processing

---

## 🏆 Achievements

- ✅ **200% Feature Delivery** → 12 services vs 4-5 originally planned
- ✅ **80% Faster Timeline** → 30 days vs 16+ weeks estimated
- ✅ **220% Documentation** → 22 comprehensive docs exceeding plan
- ✅ **80%+ Test Coverage** → Unit + integration tests across all services
- ✅ **Production-Ready** → Complete observability, Docker deployment, health checks
- ✅ **Compliance-Ready** → AML, SEPA, PSD2 foundations in place

---

## 🛣️ Roadmap

### Completed Phases (Dec 2025 - Jan 2026)

- ✅ **Phase 0:** Core Banking Services (Account, Transfer, Customer)
- ✅ **Phase 1:** Security & Auth (JWT, MFA, RBAC)
- ✅ **Phase 2:** Observability Stack (Zipkin, ELK, Prometheus, Grafana)
- ✅ **Phase 3:** Advanced Services (Transaction, Notification, Fraud Detection)
- ✅ **Phase 4:** Lending & Cards (Loan, Card, Statement)
- ✅ **Phase 5:** Compliance (AML, SEPA)

### Future Enhancements

- 🔜 **Phase 6:** Multi-Tenancy (Schema-per-tenant isolation for BaaS)
- 🔜 **Phase 7:** Global Expansion (SWIFT, FX, real-time payment rails)
- 🔜 **Phase 8:** Advanced Products (Credit decisioning, insurance, wealth management)
- 🔜 **Phase 9:** Developer Platform (API marketplace, SDKs, webhooks)
- 🔜 **Phase 10:** Kubernetes Deployment (Helm charts, CI/CD, auto-scaling)

See [docs/ROADMAP.md](docs/ROADMAP.md) for detailed roadmap.

---

## 🤝 Contributing

This is a portfolio project, but contributions are welcome for:
- Bug fixes
- Documentation improvements
- Test coverage enhancements
- Performance optimizations

Please open an issue first to discuss proposed changes.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Your Name**
- Portfolio: [your-portfolio-site.com](https://your-portfolio-site.com)
- LinkedIn: [linkedin.com/in/yourprofile](https://linkedin.com/in/yourprofile)
- GitHub: [@yourusername](https://github.com/yourusername)
- Email: your.email@example.com

---

## Acknowledgments

Educational project built for learning and portfolio purposes.

---

## 🔗 Quick Links

### Service Endpoints (Local Development)

- **Account Service:** http://localhost:8081/swagger-ui.html
- **Transfer Service:** http://localhost:8082/swagger-ui.html
- **Customer Service:** http://localhost:8083/swagger-ui.html
- **Auth Service:** http://localhost:8084/swagger-ui.html
- **Notification Service:** http://localhost:8085/swagger-ui.html
- **Transaction Service:** http://localhost:8086/swagger-ui.html
- **Fraud Detection Service:** http://localhost:8087/swagger-ui.html
- **Loan Service:** http://localhost:8088/swagger-ui/index.html
- **Card Service:** http://localhost:8089/swagger-ui/index.html
- **Statement Service:** http://localhost:8091/swagger-ui/index.html

### Observability Dashboards

- **Eureka:** http://localhost:8761
- **Zipkin:** http://localhost:9411
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (admin/admin)
- **Kibana:** http://localhost:5601
- **Kafka UI:** http://localhost:8090

---

---

## 🔗 Repository

**GitHub:** https://github.com/ygtalp/banking-microservices-platform

**⭐ Star this repository if you found it helpful for learning microservices architecture!**
