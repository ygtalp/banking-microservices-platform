# 🎯 Next Session - Perfect Startup Prompt

> **Bu dosyayı bir sonraki Claude Code session'ında kullanın!**

---

## 📋 Temel Prompt (Herhangi Bir Görev İçin)

```
I want to continue developing the Banking Microservices Platform from where we left off.

CURRENT PROJECT STATUS:
- 4 microservices: Account (8081), Transfer (8082), Customer (8083), Auth (8084)
- 3 services fully deployed and tested ✅
- Auth Service 80% complete (Day 4/5 done, platform integration pending)
- 36 specialized AI sub-agents documented and ready to use
- Tech Stack: Java 17, Spring Boot 3.2, PostgreSQL, Redis, Kafka, Docker

CRITICAL STANDARDS (NEVER VIOLATE):
- ALWAYS use BigDecimal for money (NEVER float/double)
- ALWAYS explicit @PathVariable names: @PathVariable("name") String name
- ALWAYS @Transactional on data modifications
- Follow SAGA pattern for distributed transactions
- 80%+ test coverage required

NEXT TASK:
[Burada ne yapmak istediğinizi belirtin]

Please:
1. Read docs/WORKFLOW_GUIDE.md for standardized session procedures ⭐ START HERE!
2. Follow Session Start Protocol (6-step checklist, 2 minutes)
3. Read CLAUDE.md for current project state
4. Check docs/AGENTS.md for available sub-agents
5. Use appropriate workflow (New Service, Feature, Bug Fix)
6. Follow Session End Protocol when done (create session log)

Key Documentation:
- docs/WORKFLOW_GUIDE.md → ⭐ Session procedures, workflows, agent patterns
- CLAUDE.md → Main project guide (current state, standards, patterns)
- docs/AGENTS.md → 36 AI agents (workflows, examples, usage)
- docs/AUTH_SERVICE.md → JWT & RBAC implementation details
- docs/CUSTOMER_SERVICE.md → KYC workflow reference
- session_logs/2025-12-28-workflow-standardization.md → Latest session
```

---

## 🔥 Öncelikli Görevler için Özel Promptlar

### 1️⃣ Auth Service Day 5 Tamamlama (EN ÖNCELİKLİ)

```
Continue Banking Platform development - Auth Service Day 5 (Platform Integration)

COMPLETED SO FAR:
- Day 1-4: Auth Service core functionality (41/41 tests passing)
- JWT generation, token blacklisting, RBAC all working
- Dockerfile ready, service tested independently

DAY 5 REMAINING TASKS:
1. Add auth-service to docker-compose.yml
2. Create postgres-auth database initialization
3. Deploy auth-service container
4. Secure Account/Transfer/Customer services with JWT validation
5. End-to-end integration tests
6. API Gateway JWT validation
7. Documentation updates

USE THESE AGENTS:
- IntegrationAgent → Plan platform-wide JWT integration
- SecurityImplementationAgent → Add JWT validation to services
- DockerAgent → Deploy auth-service
- IntegrationTestAgent → End-to-end auth flow tests
- DeploymentAgent → Deploy and verify

FOLLOW:
- docs/AUTH_SERVICE.md (Day 5 checklist)
- Integration pattern from docs/agents/01-task-breakdown.md (IntegrationAgent section)

SUCCESS CRITERIA:
- All 4 services running with JWT authentication
- User can register → login → access protected endpoints
- Token blacklisting works across services
- All endpoints secured (except /actuator/health)
- End-to-end test scenarios passing

Please start with IntegrationAgent to create the integration plan.
```

---

### 2️⃣ Notification Service Oluşturma (YENİ SERVİS)

```
Create Notification Service using the Banking Platform sub-agent workflow.

SERVICE REQUIREMENTS:
- Port: 8085
- Multi-channel notifications: Email, SMS, Push, In-App
- Template management with variable substitution (Thymeleaf)
- Event-driven triggers from existing services
- Delivery status tracking and retry mechanism
- User preferences management
- Channels: SendGrid (Email), Twilio (SMS), Firebase (Push)

USE THIS WORKFLOW:
1. ServiceImplementationAgent → Create 5-day implementation plan
2. EntityDesignAgent → Design Notification, NotificationTemplate, UserPreference entities
3. APIDesignAgent → REST API endpoints
4. EventDrivenAgent → Kafka event consumers (account.events, transfer.events, customer.events)
5. UnitTestAgent → 80%+ test coverage
6. IntegrationTestAgent → Full notification flow with TestContainers
7. DockerAgent → Multi-stage Dockerfile
8. DeploymentAgent → docker-compose integration & scripts
9. SessionLogAgent → Document the process

FOLLOW PATTERNS FROM:
- Account Service (entity patterns, Redis caching)
- Customer Service (event-driven integration)
- Transfer Service (circuit breaker for external APIs)

START WITH:
Use ServiceImplementationAgent to create a detailed 5-day plan following the Notification Service example in docs/agents/01-task-breakdown.md

SUCCESS CRITERIA:
- 50+ files created
- 30+ tests (all passing)
- Docker image built
- Service deployed and registered with Eureka
- Events consumed from other services
- Notifications sent successfully via all channels
```

---

### 3️⃣ Multi-Currency Feature (MEVCUT SERVİSE EKLEME)

```
Add multi-currency support to Account Service using FeatureImplementationAgent workflow.

FEATURE REQUIREMENTS:
- Account can hold balances in multiple currencies (TRY, USD, EUR, GBP)
- Real-time currency conversion with live exchange rates
- Multi-currency transfer support
- Currency conversion fee calculation
- Transaction history per currency

USE THIS WORKFLOW:
1. FeatureImplementationAgent → Impact analysis & implementation plan
2. PatternMatcherAgent → Extract existing balance handling patterns
3. EntityDesignAgent → AccountBalance entity + Liquibase migration
4. APIDesignAgent → New endpoints (GET /accounts/{id}/balances, POST /accounts/{id}/convert)
5. UnitTestAgent → Currency conversion tests
6. IntegrationTestAgent → Multi-currency transfer flow
7. CodeReviewAgent → Verify BigDecimal usage (CRITICAL!)
8. DeploymentAgent → Deploy with feature flag

FOLLOW THE EXAMPLE:
See docs/agents/01-task-breakdown.md → FeatureImplementationAgent → Example 1

BACKWARD COMPATIBILITY:
- Keep existing single balance API
- Default to TRY if currency not specified
- No breaking changes

START WITH:
Use FeatureImplementationAgent to analyze impact and create detailed implementation plan with migration strategy.
```

---

### 4️⃣ Code Review & Quality Audit (MEVCUT KODA)

```
Perform comprehensive quality audit on Account Service using QA agent workflow.

AUDIT SCOPE:
- Account Service codebase (all files)
- Check compliance with CLAUDE.md standards
- Identify security vulnerabilities
- Detect performance bottlenecks
- Verify financial accuracy (BigDecimal usage)

USE THIS WORKFLOW:
1. CodeReviewAgent → Check all coding standards
2. SecurityAuditAgent → OWASP Top 10 + banking-specific
3. PerformanceAgent → N+1 queries, missing indexes
4. BigDecimalAgent → CRITICAL: Verify NO float/double for money
5. ConsistencyAgent → Naming, formatting, package structure

GENERATE REPORTS FOR:
- Critical violations (must fix immediately)
- Major issues (should fix soon)
- Minor issues (nice to have)
- Recommendations for improvement

EXPECTED OUTPUT:
- Detailed audit report with file:line references
- Code fix suggestions
- Security risk assessment
- Performance improvement recommendations
- Consistency score (target: 98%+)

START WITH:
Use CodeReviewAgent to scan Account Service against CLAUDE.md standards, then chain other QA agents.
```

---

## 🎨 Sub-Agent Kullanım Örnekleri

### Tek Bir Agent Kullanma
```
Use [AgentName] to [task]:
- [Requirement 1]
- [Requirement 2]

Example:
Use BigDecimalAgent to audit the entire codebase for float/double usage in money operations:
- Scan all service folders
- Report every violation with file:line
- Suggest BigDecimal replacements
- Estimate financial risk of each violation
```

### Agent Zinciri Kullanma
```
Create a complete feature using this agent chain:

1. FeatureImplementationAgent → Plan the feature
2. EntityDesignAgent → Create database schema
3. APIDesignAgent → Create REST endpoints
4. UnitTestAgent → Generate tests
5. CodeReviewAgent → Review generated code

Feature: [Your feature description]
```

### Paralel Agent Kullanımı
```
Run these agents in parallel to accelerate development:

Parallel Group 1 (Code Generation):
- EntityDesignAgent → Database layer
- APIDesignAgent → API layer
- EventDrivenAgent → Event layer

Then Sequential:
- IntegrationTestAgent → Test all together
- DockerAgent → Containerize
```

---

## 📚 Önemli Referanslar

### Proje Durumu
- **CLAUDE.md** → En güncel proje durumu, standartlar, patternler
- **session_logs/2025-12-28-...md** → Son session'da neler yapıldı

### Agent Kataloğu
- **docs/AGENTS.md** → 36 agent özeti, workflow'lar, öncelikler
- **docs/agents/01-task-breakdown.md** → Planlama agentleri (5-day/12-phase plans)
- **docs/agents/02-code-generation.md** → Kod yazma agentleri (Entity, API, SAGA)
- **docs/agents/03-testing.md** → Test agentleri (Unit, Integration, API, Fix)
- **docs/agents/05-quality-assurance.md** → QA agentleri (CodeReview, Security, BigDecimal)

### Servis Referansları
- **docs/AUTH_SERVICE.md** → JWT implementation, Day 5 checklist
- **docs/CUSTOMER_SERVICE.md** → KYC workflow, event integration
- **docs/TRANSFER_SERVICE.md** → SAGA pattern, compensation logic
- **docs/ACCOUNT_SERVICE.md** → IBAN generation, Redis caching

---

## ⚠️ KRİTİK HATIRLATMALAR

### ASLA YAPMAYACAKLAR
```java
// ❌ ASLA float/double para için
float balance = 100.50f;  // HATA! Rounding errors

// ❌ ASLA @PathVariable isim belirtmeden
@PathVariable String id;  // HATA! Runtime'da fail olur

// ❌ ASLA data modifikasyonunda @Transactional olmadan
public void updateBalance() { }  // HATA! Atomicity yok
```

### HER ZAMAN YAPILACAKLAR
```java
// ✅ HER ZAMAN BigDecimal para için
BigDecimal balance = new BigDecimal("100.50");

// ✅ HER ZAMAN explicit @PathVariable
@PathVariable("accountNumber") String accountNumber

// ✅ HER ZAMAN @Transactional
@Transactional
public void updateBalance() { }

// ✅ HER ZAMAN validation
@Valid @RequestBody CreateAccountRequest request

// ✅ HER ZAMAN test coverage 80%+
// Unit tests + Integration tests + API tests
```

---

## 🎯 Başarı Kriterleri

Her session sonunda kontrol et:

- [ ] Kod CLAUDE.md standartlarına uygun
- [ ] BigDecimal kullanılmış (para işlemlerinde)
- [ ] @PathVariable isimleri explicit
- [ ] @Transactional kullanılmış (data modifications)
- [ ] Test coverage 80%+
- [ ] Testler passing (mvn test)
- [ ] Docker image build oluyor
- [ ] Servis deploy oluyor ve health check passing
- [ ] Session log oluşturuldu
- [ ] CLAUDE.md güncellendi (gerekirse)

---

## 💡 Pro Tips

1. **Her zaman CLAUDE.md'yi oku** - En güncel project state burada
2. **Agent kataloğunu kullan** - Manuel yazmak yerine agent kullan
3. **Mevcut servislere bak** - Pattern'lar Account/Transfer/Customer'da
4. **Session log tut** - Gelecek sessiona context sağlar
5. **Test coverage takip et** - 80% altına düşme
6. **BigDecimal kontrolü yap** - Banking için KRITIK!

---

**Son Güncelleme:** 28 Aralık 2025
**Proje Versiyonu:** 2.5 (Sub-Agent Catalog Added)
**Hazırlayan:** Claude Code (Session: 2025-12-28)
