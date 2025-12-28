# Banking Platform - AI Sub-Agent Catalog

> **Purpose:** AI Sub-Agent specifications for Banking Microservices Platform
> **Total Agents:** 36 specialized agents across 10 categories
> **Last Updated:** 28 December 2025

---

## 📋 Overview

This document catalogs all specialized AI sub-agents designed for the Banking Microservices Platform. Each agent is optimized for specific tasks following the project's established patterns, coding standards, and architectural decisions.

### Project Context

- **Platform:** Java Spring Boot Banking Microservices
- **Services:** Account, Transfer, Customer, Auth (4 core services)
- **Patterns:** SAGA, DDD, Event-Driven, Circuit Breaker, KYC, JWT, RBAC
- **Tech Stack:** Java 17, Spring Boot 3.2, PostgreSQL, Redis, Kafka, Docker
- **Standards:** See [CLAUDE.md](../CLAUDE.md) and [CODING_STANDARDS.md](./CODING_STANDARDS.md)

---

## 🗂️ Agent Categories

| Category | Agent Count | Purpose | Documentation |
|----------|-------------|---------|---------------|
| [Task Breakdown](#task-breakdown) | 3 | Planning, decomposition | [📄 Details](./agents/01-task-breakdown.md) |
| [Code Generation](#code-generation) | 5 | Automated code writing | [📄 Details](./agents/02-code-generation.md) |
| [Testing](#testing) | 4 | Test generation & fixes | [📄 Details](./agents/03-testing.md) |
| [Documentation](#documentation) | 4 | Docs generation | [📄 Details](./agents/04-documentation.md) |
| [Quality Assurance](#quality-assurance) | 4 | Code review, security | [📄 Details](./agents/05-quality-assurance.md) |
| [DevOps](#devops) | 4 | Infrastructure, deployment | [📄 Details](./agents/06-devops.md) |
| [Debugging](#debugging) | 3 | Troubleshooting | [📄 Details](./agents/07-debugging.md) |
| [Planning & Strategy](#planning-strategy) | 3 | Strategic planning | [📄 Details](./agents/08-planning.md) |
| [Domain-Specific](#domain-specific) | 3 | Banking domain logic | [📄 Details](./agents/09-domain-specific.md) |
| [Context & Patterns](#context-patterns) | 3 | Consistency, patterns | [📄 Details](./agents/10-context.md) |

**Total:** 36 specialized agents

---

## 📊 Quick Reference Matrix

### By Automation Level

| Automation Level | Agents | Use Case |
|------------------|--------|----------|
| **High** (90%+ automated) | CodeGeneration, Testing, Documentation, Context | Repetitive, pattern-based tasks |
| **Medium** (60-90%) | TaskBreakdown, QualityAssurance, DevOps, DomainSpecific | Requires some human judgment |
| **Low** (30-60%) | Planning, Debugging | High human oversight needed |

### By Development Phase

| Phase | Primary Agents | Secondary Agents |
|-------|----------------|------------------|
| **Planning** | ServicePlanner, FeaturePlanner | ProjectContext, PatternMatcher |
| **Implementation** | EntityDesign, APIDesign, EventDriven | SAGA, Security |
| **Testing** | UnitTest, IntegrationTest, APITest | TestFix |
| **Review** | CodeReview, SecurityAudit, Performance | BigDecimal, Consistency |
| **Deployment** | Docker, Database, Deployment | Monitoring |
| **Maintenance** | LogAnalyzer, ErrorResolution | Dependency |

---

## 🎯 Top 10 High-Value Agents

Based on project needs and impact:

1. **ServicePlannerAgent** - Automates new service planning (5-day breakdown)
2. **UnitTestAgent** - Critical for 80%+ test coverage
3. **CodeReviewAgent** - Maintains code quality standards
4. **SecurityAuditAgent** - Essential for banking security
5. **BigDecimalAgent** - Ensures financial accuracy (critical!)
6. **APIDocumentationAgent** - Improves developer experience
7. **IntegrationAgent** - Simplifies service-to-service integration
8. **TestFixAgent** - Accelerates test maintenance
9. **SAGAImplementationAgent** - Handles complex distributed transactions
10. **SessionLogAgent** - Preserves institutional knowledge

---

## 🚀 Quick Start Examples

### Example 1: Implement New Service

```
Step 1: ServicePlannerAgent
Prompt: "Create 5-day implementation plan for Notification Service"
Output: Detailed plan with phases, files, timeline

Step 2: EntityDesignAgent
Prompt: "Design entities for Notification Service"
Output: Entity classes, Liquibase migrations

Step 3: APIDesignAgent
Prompt: "Design REST API for Notification Service"
Output: Controllers, DTOs, endpoints

Step 4: UnitTestAgent
Prompt: "Generate unit tests for NotificationService"
Output: Test classes with 80%+ coverage

Step 5: DockerAgent
Prompt: "Create Dockerfile for Notification Service"
Output: Multi-stage Dockerfile, docker-compose config
```

### Example 2: Add Feature to Existing Service

```
FeatureImplementationAgent
Prompt: "Add scheduled transfers to Transfer Service"
Output:
  - Feature breakdown
  - Affected files list
  - Database migrations
  - Event schemas
  - Test scenarios
```

### Example 3: Fix Test Failures

```
TestFixAgent
Prompt: "Fix CustomerServiceTest compilation errors"
Output:
  - Error analysis
  - Fixed test files
  - Mock configuration corrections
  - Best practices reminder
```

---

## 📚 Agent Documentation Structure

Each agent category has its own detailed documentation file:

### Documentation Template

Each agent doc includes:

1. **Agent Specification**
   - Name, ID, Category
   - Scope and Objectives
   - Primary Responsibilities

2. **Capabilities**
   - Input Parameters
   - Processing Logic
   - Output Artifacts

3. **Usage Examples**
   - Common Use Cases
   - Sample Prompts
   - Expected Outputs

4. **Integration Points**
   - Related Agents
   - Workflow Integration
   - Dependencies

5. **Best Practices**
   - When to Use
   - When NOT to Use
   - Performance Tips

---

## 🔗 Agent Workflows

### Workflow 1: New Service Implementation

```
ServicePlannerAgent (Planning)
    ↓
EntityDesignAgent (Domain Model)
    ↓
APIDesignAgent (REST Layer)
    ↓
EventDrivenAgent (Kafka Integration)
    ↓
UnitTestAgent (Testing)
    ↓
IntegrationTestAgent (E2E Testing)
    ↓
DockerAgent (Containerization)
    ↓
APIDocumentationAgent (Documentation)
    ↓
SessionLogAgent (Knowledge Capture)
```

### Workflow 2: Feature Development

```
FeatureImplementationAgent (Planning)
    ↓
PatternMatcherAgent (Extract Patterns)
    ↓
[Code Generation Agents] (Parallel)
    ↓
TestFixAgent (Update Tests)
    ↓
CodeReviewAgent (Quality Check)
    ↓
DeploymentAgent (Deploy)
```

### Workflow 3: Bug Resolution

```
LogAnalyzerAgent (Log Analysis)
    ↓
ErrorResolutionAgent (Solution Lookup)
    ↓
[Fix Implementation]
    ↓
TestFixAgent (Verify Tests)
    ↓
CodeReviewAgent (Review)
    ↓
SessionLogAgent (Document Fix)
```

---

## 🛠️ Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
**Priority:** Critical for daily development

- [ ] ServicePlannerAgent
- [ ] CodeReviewAgent
- [ ] TestFixAgent
- [ ] BigDecimalAgent

**Impact:** Immediate productivity gains

### Phase 2: Quality (Week 3-4)
**Priority:** Improve code quality

- [ ] UnitTestAgent
- [ ] SecurityAuditAgent
- [ ] APIDocumentationAgent
- [ ] ConsistencyAgent

**Impact:** Better code quality, fewer bugs

### Phase 3: Advanced (Week 5-6)
**Priority:** Handle complex scenarios

- [ ] SAGAImplementationAgent
- [ ] IntegrationAgent
- [ ] PerformanceAgent
- [ ] EventDrivenAgent

**Impact:** Accelerate complex implementations

### Phase 4: Automation (Week 7-8)
**Priority:** Full automation

- [ ] All remaining agents
- [ ] Agent orchestration
- [ ] Workflow automation
- [ ] Monitoring & metrics

**Impact:** Maximum automation level

---

## 📖 Detailed Documentation

Each category has comprehensive documentation:

1. **[Task Breakdown Agents](./agents/01-task-breakdown.md)**
   - ServiceImplementationAgent
   - FeatureImplementationAgent
   - IntegrationAgent

2. **[Code Generation Agents](./agents/02-code-generation.md)**
   - EntityDesignAgent
   - APIDesignAgent
   - SAGAImplementationAgent
   - EventDrivenAgent
   - SecurityImplementationAgent

3. **[Testing Agents](./agents/03-testing.md)**
   - UnitTestAgent
   - IntegrationTestAgent
   - APITestAgent
   - TestFixAgent

4. **[Documentation Agents](./agents/04-documentation.md)**
   - APIDocumentationAgent
   - ArchitectureDocAgent
   - SessionLogAgent
   - OnboardingDocAgent

5. **[Quality Assurance Agents](./agents/05-quality-assurance.md)**
   - CodeReviewAgent
   - SecurityAuditAgent
   - PerformanceAgent
   - BigDecimalAgent

6. **[DevOps Agents](./agents/06-devops.md)**
   - DockerAgent
   - DatabaseAgent
   - MonitoringAgent
   - DeploymentAgent

7. **[Debugging Agents](./agents/07-debugging.md)**
   - LogAnalyzerAgent
   - ErrorResolutionAgent
   - DependencyAgent

8. **[Planning & Strategy Agents](./agents/08-planning.md)**
   - ServicePlannerAgent
   - RefactoringAgent
   - MigrationAgent

9. **[Domain-Specific Agents](./agents/09-domain-specific.md)**
   - BankingDomainAgent
   - KYCAgent
   - PaymentAgent

10. **[Context & Pattern Agents](./agents/10-context.md)**
    - ProjectContextAgent
    - PatternMatcherAgent
    - ConsistencyAgent

---

## 🎓 Agent Design Principles

All agents follow these principles:

### 1. Context-Aware
- Read project documentation (CLAUDE.md, CODING_STANDARDS.md)
- Understand existing patterns
- Maintain consistency

### 2. Pattern-Based
- Follow established patterns (Account, Transfer, Customer services)
- Reuse proven solutions
- Document new patterns

### 3. Quality-Focused
- 80%+ test coverage
- Banking-grade security
- Financial accuracy (BigDecimal)

### 4. Automation-Friendly
- Clear input/output contracts
- Deterministic behavior
- Error handling

### 5. Documentation-First
- Generate comprehensive docs
- Maintain session logs
- Knowledge preservation

---

## 💡 Best Practices

### When to Use Agents

✅ **DO Use Agents For:**
- Repetitive tasks (test generation)
- Pattern-based code (CRUD operations)
- Documentation generation
- Code review automation
- Consistency checks

❌ **DON'T Use Agents For:**
- Novel architectural decisions
- Business requirement gathering
- High-level strategy (require human judgment)
- Creative problem-solving
- Stakeholder communication

### Agent Collaboration

Agents work best when:
1. **Chained in workflows** (output of one → input of next)
2. **Run in parallel** when independent
3. **Supervised by human** for critical decisions
4. **Iterative refinement** allowed

---

## 📊 Metrics & Monitoring

Track agent effectiveness:

### Success Metrics
- **Code Generated:** Lines of code per agent
- **Test Coverage:** Improvement percentage
- **Bug Detection:** Issues found per review
- **Time Saved:** Hours per task
- **Consistency Score:** Adherence to standards

### Quality Metrics
- **Compilation Success Rate:** % of generated code that compiles
- **Test Pass Rate:** % of generated tests that pass
- **Review Approval Rate:** % of code that passes review
- **Documentation Completeness:** % coverage

---

## 🔄 Continuous Improvement

### Feedback Loop
1. **Capture Results:** Session logs document agent usage
2. **Analyze Patterns:** Identify common issues
3. **Refine Agents:** Update prompts, logic
4. **Share Learnings:** Update documentation

### Agent Evolution
- **Version Control:** Track agent specifications
- **A/B Testing:** Compare agent variations
- **Performance Tuning:** Optimize for speed/quality
- **Capability Expansion:** Add new features

---

## 📞 Quick Links

- **Project Documentation:** [CLAUDE.md](../CLAUDE.md)
- **Coding Standards:** [CODING_STANDARDS.md](./CODING_STANDARDS.md)
- **Architecture Decisions:** [ARCHITECTURE_DECISIONS.md](./ARCHITECTURE_DECISIONS.md)
- **Testing Guide:** [TESTING_GUIDE.md](./TESTING_GUIDE.md)
- **Development Guide:** [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md)

---

## 📝 Maintenance

**Owners:** Development Team
**Review Frequency:** Monthly
**Last Review:** 28 December 2025
**Next Review:** 28 January 2026

**Change Log:**
- 2025-12-28: Initial catalog creation (36 agents, 10 categories)

---

**Version:** 1.0
**Status:** 📋 ACTIVE
**Last Updated:** 28 December 2025
