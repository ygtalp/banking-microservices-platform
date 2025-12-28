# Banking Platform Development Workflow

> **Purpose:** Standardized procedures for session management, planning, and feature development
> **Created:** 28 December 2025
> **Status:** PRODUCTION STANDARD

---

## 📋 SESSION START PROTOCOL

**Checklist: Every New Session**

```markdown
□ 1. Read Project Context
   - Open CLAUDE.md (current project state)
   - Check Recent Sessions section
   - Note version number

□ 2. Review Recent Activity
   - Read latest session log (session_logs/)
   - Identify what was completed
   - Note any pending items or blockers

□ 3. Check Active Plans
   - Open .claude/plans/ directory
   - Identify active vs completed plans
   - Determine continuation point

□ 4. Determine Task Priority
   - Check ROADMAP.md for prioritized features
   - Review NEXT_SESSION_PROMPT.md for suggested next steps
   - Decide: Continue existing work OR Start new feature

□ 5. Select Sub-Agents
   - Open docs/AGENTS.md
   - Identify agents needed for this task
   - Review usage patterns in relevant category files

□ 6. Prepare Context
   - Read relevant service docs (docs/*_SERVICE.md)
   - Review coding standards (docs/CODING_STANDARDS.md)
   - Check existing code patterns in similar services
```

---

## 📋 SESSION END PROTOCOL

**Checklist: Before Ending Session**

```markdown
□ 1. Create Session Log
   - File: session_logs/YYYY-MM-DD-topic-description.md
   - Include: Objectives, Achievements, Files Modified, Next Steps
   - Template: See "Session Log Template" below

□ 2. Update CLAUDE.md
   - Add session to Recent Sessions section
   - Update version number if significant changes
   - Update service status if applicable
   - Update completion percentages

□ 3. Update Service Documentation
   - If new service: Create docs/{SERVICE}_SERVICE.md
   - If modified: Update existing service docs
   - Document new endpoints, features, or patterns

□ 4. Enter Plan Mode (if needed)
   - Use EnterPlanMode for next session planning
   - Create detailed implementation plan
   - Save in .claude/plans/
   - Reference plan in session log

□ 5. Commit Code Changes
   - Follow Git Workflow (see below)
   - Use conventional commit messages
   - Create PR if feature complete

□ 6. Document Blockers
   - In session log: List unresolved issues
   - In NEXT_SESSION_PROMPT.md: Add context if needed
   - In service docs: Note any TODOs
```

---

## 🎯 FEATURE DEVELOPMENT STANDARD PROCESS

### Decision Tree: Which Approach?

```
Is this a NEW SERVICE?
├─ YES → Use 5-Day or 12-Phase Plan
│         └─ ServiceImplementationAgent
│
└─ NO → Is this a MAJOR FEATURE (3+ files)?
   ├─ YES → Use Feature Implementation Workflow
   │         └─ FeatureImplementationAgent
   │
   └─ NO → Is this a BUG FIX or MINOR CHANGE?
            └─ Direct Implementation
                  └─ CodeReviewAgent (after)
```

---

## 🏗 NEW SERVICE WORKFLOW (5-Day Plan)

**Pattern:** Account Service, Transfer Service, Customer Service, Auth Service

**Day 1: Foundation & Database (25%)**
```markdown
Agent: EntityDesignAgent
Tasks:
  1. Create service project structure
  2. Design database schema
  3. Create entity classes
  4. Create repository interfaces
  5. Write Liquibase migrations
  6. Create configuration files

Success Criteria:
  - Maven build succeeds
  - Database schema created
  - Basic entity tests pass

Files Created: ~15 files
Time Estimate: 6-8 hours (or 30 minutes with agent)
```

**Day 2: Business Logic (25%)**
```markdown
Agent: APIDesignAgent + PatternMatcherAgent
Tasks:
  1. Create service interfaces
  2. Implement service layer
  3. Add business validations
  4. Implement helper methods
  5. Add logging

Success Criteria:
  - Service layer compiles
  - Business logic complete
  - Helper methods tested

Files Created: ~10 files
Time Estimate: 6-8 hours (or 45 minutes with agent)
```

**Day 3: API & Integration (30%)**
```markdown
Agents: APIDesignAgent + EventDrivenAgent + IntegrationAgent
Tasks:
  1. Create DTOs (Request/Response)
  2. Create REST controllers
  3. Add validation annotations
  4. Configure Kafka events
  5. Create Feign clients (if needed)
  6. Add exception handling

Success Criteria:
  - All endpoints working
  - Events published correctly
  - Feign clients tested

Files Created: ~20 files
Time Estimate: 8 hours (or 1 hour with agents)
```

**Day 4: Testing & Quality (15%)**
```markdown
Agents: UnitTestAgent + IntegrationTestAgent + CodeReviewAgent
Tasks:
  1. Write unit tests (80%+ coverage)
  2. Write integration tests (TestContainers)
  3. Create API test scripts (PowerShell)
  4. Run code review
  5. Fix violations

Success Criteria:
  - All tests passing
  - 80%+ coverage achieved
  - Code review clean

Files Created: ~15 test files
Time Estimate: 6 hours (or 1 hour with agents)
```

**Day 5: Docker & Deployment (5%)**
```markdown
Agents: DockerAgent + DeploymentAgent
Tasks:
  1. Create Dockerfile (multi-stage)
  2. Update docker-compose.yml
  3. Create deployment scripts
  4. Deploy and test
  5. Create service documentation
  6. Update CLAUDE.md

Success Criteria:
  - Docker image builds
  - Service deploys successfully
  - Health checks pass
  - Documentation complete

Files Created: ~5 files
Time Estimate: 4 hours (or 30 minutes with agents)
```

**Total: 5 days (30-34 hours) → 3-4 hours with sub-agents**

---

## 🔧 FEATURE IMPLEMENTATION WORKFLOW

**Pattern:** Multi-currency support, Enhanced KYC, Advanced reporting

**Step 1: Impact Analysis (15 minutes)**
```markdown
Agent: FeatureImplementationAgent
Tasks:
  1. Analyze existing codebase
  2. Identify affected files
  3. Assess backward compatibility
  4. Create migration strategy
  5. Estimate effort

Output: Impact analysis report
```

**Step 2: Pattern Matching (10 minutes)**
```markdown
Agent: PatternMatcherAgent
Tasks:
  1. Find similar existing patterns
  2. Extract reusable code
  3. Identify naming conventions
  4. Document consistency requirements

Output: Pattern reference guide
```

**Step 3: Implementation (varies)**
```markdown
Agents: EntityDesignAgent + APIDesignAgent (parallel)
Tasks:
  1. Create/modify entities
  2. Update database schema (Liquibase)
  3. Create/modify repositories
  4. Create/modify DTOs
  5. Create/modify endpoints
  6. Update service layer

Output: Feature code complete
```

**Step 4: Testing (30 minutes)**
```markdown
Agents: UnitTestAgent + TestFixAgent
Tasks:
  1. Generate unit tests
  2. Update integration tests
  3. Fix failing tests
  4. Verify coverage (80%+)

Output: All tests passing
```

**Step 5: Quality Assurance (20 minutes)**
```markdown
Agents: CodeReviewAgent + SecurityAuditAgent
Tasks:
  1. Review code standards
  2. Check security vulnerabilities
  3. Verify BigDecimal usage
  4. Check consistency
  5. Fix violations

Output: Code review report + fixes
```

**Step 6: Deployment (15 minutes)**
```markdown
Agent: DeploymentAgent
Tasks:
  1. Update docker-compose if needed
  2. Deploy with feature flag (if applicable)
  3. Test end-to-end
  4. Update documentation

Output: Feature deployed
```

**Total: 2-4 hours depending on complexity**

---

## 🐛 BUG FIX WORKFLOW

**For minor fixes and small changes**

```markdown
1. Identify Issue
   - Read logs (LogAnalyzerAgent)
   - Locate error source

2. Fix Code
   - Make minimal changes
   - Follow existing patterns
   - Add test case

3. Verify Fix
   - Run affected tests
   - Run full test suite
   - Test manually

4. Code Review
   - Use CodeReviewAgent
   - Verify standards compliance

5. Deploy
   - Commit with "fix:" prefix
   - Deploy to dev
   - Verify in production

Time: 30 minutes - 2 hours
```

---

## 🤖 SUB-AGENT USAGE PATTERNS

### When to Use Which Agent?

**Planning & Decomposition:**
```
New Service → ServiceImplementationAgent (5-day plan)
Major Feature → FeatureImplementationAgent (impact analysis)
Platform Integration → IntegrationAgent (cross-service plan)
```

**Code Generation:**
```
Database → EntityDesignAgent (entities + migrations)
REST API → APIDesignAgent (controllers + DTOs)
SAGA → SAGAImplementationAgent (orchestration)
Events → EventDrivenAgent (Kafka producers/consumers)
Security → SecurityImplementationAgent (JWT + RBAC)
```

**Testing:**
```
Unit Tests → UnitTestAgent (80%+ coverage)
Integration → IntegrationTestAgent (TestContainers)
API Tests → APITestAgent (PowerShell scripts)
Test Failures → TestFixAgent (diagnosis + auto-fix)
```

**Quality Assurance:**
```
Code Review → CodeReviewAgent (standards check)
Security → SecurityAuditAgent (OWASP scan)
Performance → PerformanceAgent (N+1 queries, indexes)
Money Operations → BigDecimalAgent (CRITICAL!)
Consistency → ConsistencyAgent (naming, formatting)
```

**DevOps:**
```
Containerization → DockerAgent (Dockerfile + compose)
Database Migration → DatabaseAgent (Liquibase)
Deployment → DeploymentAgent (scripts + health checks)
Monitoring → MonitoringAgent (Prometheus + Grafana)
```

**Debugging:**
```
Log Analysis → LogAnalyzerAgent (error patterns)
Known Issues → ErrorResolutionAgent (solution lookup)
Dependencies → DependencyAgent (Maven conflicts)
```

**Domain-Specific:**
```
Banking Rules → BankingDomainAgent (limits, PSD2)
KYC Workflow → KYCAgent (verification logic)
Payments → PaymentAgent (fraud detection, fees)
```

**Context & Patterns:**
```
Project Context → ProjectContextAgent (standards extraction)
Pattern Detection → PatternMatcherAgent (code patterns)
Consistency Check → ConsistencyAgent (98%+ score)
```

### Agent Collaboration Examples

**Example 1: Create New Service**
```
ServiceImplementationAgent (plan)
  ↓
ProjectContextAgent (load standards)
  ↓
EntityDesignAgent (database)
  ↓
APIDesignAgent (REST API)
  ↓
EventDrivenAgent (Kafka)
  ↓
UnitTestAgent (tests)
  ↓
IntegrationTestAgent (end-to-end)
  ↓
DockerAgent (containerize)
  ↓
DeploymentAgent (deploy)
  ↓
SessionLogAgent (document)
```

**Example 2: Add Feature to Existing Service**
```
FeatureImplementationAgent (impact analysis)
  ↓
PatternMatcherAgent (find patterns)
  ↓
[EntityDesignAgent + APIDesignAgent] (parallel)
  ↓
UnitTestAgent (tests)
  ↓
TestFixAgent (fix failures)
  ↓
CodeReviewAgent (standards)
  ↓
DeploymentAgent (deploy)
```

**Example 3: Quality Audit**
```
CodeReviewAgent (conventions)
  ↓
SecurityAuditAgent (vulnerabilities)
  ↓
PerformanceAgent (bottlenecks)
  ↓
BigDecimalAgent (financial accuracy)
  ↓
ConsistencyAgent (formatting)
  ↓
[Generate Report + Auto-Fix Suggestions]
```

---

## 📊 PLANNING INTEGRATION

### When to Use Plan Mode?

**Always Use Plan Mode For:**
- ✅ New microservice implementation
- ✅ Major feature (3+ files, complex logic)
- ✅ Cross-service integration
- ✅ Refactoring projects
- ✅ Technology migrations

**Don't Use Plan Mode For:**
- ❌ Bug fixes
- ❌ Minor tweaks (1-2 line changes)
- ❌ Documentation updates
- ❌ Simple configuration changes

### Plan Mode Workflow

```markdown
1. Enter Plan Mode
   - Tool: EnterPlanMode
   - Describe the feature/service
   - AI creates detailed implementation plan

2. Review Plan
   - Plan saved to .claude/plans/
   - Review all phases/days
   - Ask questions if unclear
   - Request modifications if needed

3. Exit Plan Mode
   - Tool: ExitPlanMode
   - AI creates session log with plan reference
   - Plan ready for execution

4. Execute Plan
   - Follow day-by-day or phase-by-phase
   - Use sub-agents for each step
   - Update progress in session logs

5. Complete Plan
   - Mark plan as complete in session log
   - Update CLAUDE.md
   - Archive plan or keep for reference
```

### Plan Naming Convention

Plans are auto-generated with random names (e.g., `glistening-imagining-salamander.md`). Track them in session logs:

```markdown
Session Log Example:
- Plan Created: sleepy-questing-sprout.md (Auth Service Day 5)
- Plan Status: 80% complete (Day 1-4 done)
- Next Session: Continue Day 5
```

---

## 📝 SESSION LOG TEMPLATE

**File:** `session_logs/YYYY-MM-DD-topic-description.md`

```markdown
# Session Log: [Topic Description]

**Date:** DD Month YYYY
**Duration:** ~X hours
**Objective:** [Primary goal of this session]

---

## 📋 Session Overview

[Brief summary of what this session accomplished]

---

## 🎯 Achievements

### 1. [Major Achievement 1] ✅
- [Detail 1]
- [Detail 2]
- [Detail 3]

### 2. [Major Achievement 2] ✅
- [Detail 1]
- [Detail 2]

---

## 📊 Statistics

```yaml
Files Created: X files
Files Modified: Y files
Lines Added: ~Z lines
Tests Added: N tests (all passing)
Test Coverage: XX%
```

---

## 📁 Files Modified/Created

### Modified
- `CLAUDE.md`
  - [What changed]
  - [Why changed]

- `path/to/file.java`
  - [What changed]

### Created
1. `path/to/new/file.java` - [Purpose]
2. `path/to/another/file.java` - [Purpose]

---

## 🔄 Agent Usage

**Agents Used:**
- ServiceImplementationAgent → [What it did]
- EntityDesignAgent → [What it did]
- UnitTestAgent → [What it did]

**Agent Performance:**
- Time Saved: X hours (Y hours manually → Z hours with agents)
- Quality Score: XX%
- Files Generated: N files

---

## 🚀 Next Steps

### Immediate (Next Session)
1. [Priority 1 task]
2. [Priority 2 task]

### Short-term (Next 1-2 Weeks)
1. [Feature/service to implement]
2. [Integration to complete]

### Long-term (Next Month)
1. [Major milestone]
2. [Platform enhancement]

---

## 💡 Lessons Learned

### What Went Well
- [Success 1]
- [Success 2]

### Challenges
- [Challenge 1] → [Solution]
- [Challenge 2] → [Solution]

### Improvements for Next Time
- [Improvement 1]
- [Improvement 2]

---

## ✅ Success Metrics

```yaml
Completeness: [XX%]
  - [Criterion 1]: ✅
  - [Criterion 2]: ✅

Quality: [Rating]
  - Code Standards: ✅
  - Test Coverage: XX%
  - Documentation: ✅

Integration: [Status]
  - Build: ✅
  - Deploy: ✅
  - Tests: ✅
```

---

**End of Session**

**Next Session Priority:** [What to focus on next]
**Plan Reference:** [Plan file if applicable]
**Documentation Status:** [What docs were updated]
```

---

## 🔄 GIT WORKFLOW

### Commit Message Convention

```bash
<type>(<scope>): <subject>

Types:
  feat     - New feature
  fix      - Bug fix
  docs     - Documentation
  style    - Formatting
  refactor - Code restructuring
  test     - Tests
  chore    - Maintenance

Examples:
  feat(customer): add KYC verification workflow
  fix(transfer): correct SAGA compensation logic
  docs(auth): add JWT configuration guide
  test(account): increase coverage to 85%
```

### Branch Strategy

```bash
# Feature branch
git checkout -b feature/notification-service

# Bug fix branch
git checkout -b fix/transfer-saga-timeout

# Documentation branch
git checkout -b docs/api-documentation
```

### Commit Checklist

```markdown
Before Committing:
□ Tests pass (mvn test)
□ Code follows CODING_STANDARDS.md
□ No sensitive data in code/logs
□ BigDecimal used for money operations
□ @PathVariable names explicit
□ @Transactional on data modifications
□ Documentation updated
□ Session log created
```

---

## 🎯 QUICK REFERENCE CARDS

### Card 1: Session Start (2 minutes)

```
1. Read CLAUDE.md Recent Sessions
2. Read latest session log
3. Check .claude/plans for active plans
4. Decide: Continue OR New task
5. Open docs/AGENTS.md for agent selection
```

### Card 2: Session End (5 minutes)

```
1. Create session log (use template)
2. Update CLAUDE.md Recent Sessions
3. Enter Plan Mode if needed
4. Commit code with proper message
5. Document blockers/pending items
```

### Card 3: New Service (3-4 hours with agents)

```
1. ServiceImplementationAgent → Plan
2. EntityDesignAgent → Database
3. APIDesignAgent → REST API
4. EventDrivenAgent → Kafka
5. UnitTestAgent → Tests
6. DockerAgent → Container
7. DeploymentAgent → Deploy
8. SessionLogAgent → Document
```

### Card 4: Add Feature (2-3 hours with agents)

```
1. FeatureImplementationAgent → Impact
2. PatternMatcherAgent → Patterns
3. [EntityDesignAgent + APIDesignAgent] → Code
4. UnitTestAgent → Tests
5. CodeReviewAgent → Review
6. DeploymentAgent → Deploy
```

### Card 5: Quality Check (1 hour with agents)

```
1. CodeReviewAgent → Standards
2. SecurityAuditAgent → OWASP
3. PerformanceAgent → Bottlenecks
4. BigDecimalAgent → Money operations
5. ConsistencyAgent → Formatting
```

---

## 📚 Documentation Cross-Reference

```
WORKFLOW_GUIDE.md (this file) → Session procedures, workflows
    ↓
CLAUDE.md → Current project state, quick reference
    ↓
AGENTS.md → Sub-agent catalog (36 agents)
    ↓
agents/01-10.md → Detailed agent specifications
    ↓
ROADMAP.md → Prioritized features
    ↓
{SERVICE}_SERVICE.md → Service-specific documentation
    ↓
ARCHITECTURE_DECISIONS.md → Why we chose this approach
    ↓
CODING_STANDARDS.md → How to write code
    ↓
TESTING_GUIDE.md → How to test
    ↓
DEVELOPMENT_GUIDE.md → Setup and commands
```

---

## ⚠️ CRITICAL REMINDERS

### Never Forget

```java
// ❌ NEVER
float money = 100.50f;                    // Rounding errors!
@PathVariable String id;                  // Runtime failure!
public void updateData() { }              // No transaction!

// ✅ ALWAYS
BigDecimal money = new BigDecimal("100.50");
@PathVariable("id") String id;
@Transactional
public void updateData() { }
```

### Banking Platform Non-Negotiables

1. **BigDecimal for money** → Financial accuracy
2. **Explicit @PathVariable** → Runtime safety
3. **@Transactional** → Data integrity
4. **80%+ test coverage** → Quality assurance
5. **Session logs** → Context preservation
6. **Sub-agent usage** → Productivity & consistency

---

## 🎯 SUCCESS CRITERIA

### Every Session Should Result In:

```markdown
✅ Clear objective achieved
✅ Session log created
✅ CLAUDE.md updated (if significant)
✅ Code committed with proper message
✅ Tests passing (if code changed)
✅ Documentation updated (if needed)
✅ Next session planned (if needed)
✅ No blockers undocumented
```

### Every Feature Should Have:

```markdown
✅ Implementation plan (5-day or 12-phase)
✅ 80%+ test coverage
✅ All tests passing
✅ Code review complete
✅ Security audit passed
✅ BigDecimal verification done
✅ Docker deployment successful
✅ API documentation updated
✅ Session log documenting the work
```

---

## 📞 WHEN TO USE WHAT

```
Question: "Should I use Plan Mode?"
Answer:
  - New service? → YES
  - Major feature (3+ files)? → YES
  - Bug fix? → NO
  - Minor change? → NO

Question: "Which agent should I use?"
Answer: See "Sub-Agent Usage Patterns" section above

Question: "How do I start a new session?"
Answer: Follow "Session Start Protocol" checklist

Question: "How do I end a session?"
Answer: Follow "Session End Protocol" checklist

Question: "What's the standard for new services?"
Answer: Follow "New Service Workflow (5-Day Plan)"

Question: "How do I add a feature to existing service?"
Answer: Follow "Feature Implementation Workflow"
```

---

**Last Updated:** 28 December 2025
**Version:** 1.0
**Status:** PRODUCTION STANDARD

**This workflow guide is the foundation for consistent, high-quality development on the Banking Platform.**
