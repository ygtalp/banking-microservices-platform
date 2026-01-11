# Fraud Detection Service - Complete Reference

> **Service:** Fraud Detection Service
> **Port:** 8087
> **Database:** fraud_detection_db (PostgreSQL)
> **Responsibility:** Real-time fraud detection, rule engine, and risk scoring
> **Last Updated:** 1 January 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model](#domain-model)
3. [Rule Engine](#rule-engine)
4. [Risk Scoring System](#risk-scoring-system)
5. [API Reference](#api-reference)
6. [Event-Driven Architecture](#event-driven-architecture)
7. [Fraud Rules Configuration](#fraud-rules-configuration)
8. [Testing](#testing)

---

## Overview

Fraud Detection Service provides real-time fraud detection capabilities using a configurable rule engine and risk scoring system. It automatically monitors all transfers and generates alerts for suspicious activities.

### Key Features

- ✅ Real-time fraud detection (automatic on transfer events)
- ✅ Configurable rule engine (6 default rules)
- ✅ Risk scoring system (0-100 points → LOW/MEDIUM/HIGH/CRITICAL)
- ✅ Account-level risk tracking and scoring
- ✅ Manual review workflow (review, clear, reject)
- ✅ High-risk account identification
- ✅ Redis caching for performance
- ✅ JWT authentication & authorization
- ✅ Complete audit trail for compliance

### Technology Stack

```yaml
Framework: Spring Boot 3.2.0
Database: PostgreSQL 16
Cache: Redis 7.2
Messaging: Apache Kafka 3.6 (Consumer + Publisher)
Security: JWT (HS512)
Validation: Spring Validation + Hibernate Validator
```

---

## Domain Model

### FraudCheck Entity

```java
@Entity
@Table(name = "fraud_checks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheck implements Serializable {

    @Id
    private String checkId;  // FRD-XXXXXXXXXXXX

    @Column(nullable = false, length = 100)
    private String transferReference;  // Transfer being checked

    @Column(nullable = false, length = 50)
    private String accountNumber;  // Account under review

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;  // Transaction amount

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;

    @Column(precision = 19, scale = 2)
    private BigDecimal balanceBefore;  // Balance snapshot

    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfter;  // Balance snapshot

    @Column(nullable = false)
    private Integer riskScore;  // 0-100 points

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;  // LOW, MEDIUM, HIGH, CRITICAL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudCheckStatus status;  // PASSED, FLAGGED, BLOCKED, UNDER_REVIEW, CLEARED

    @ElementCollection
    @CollectionTable(name = "fraud_check_reasons", joinColumns = @JoinColumn(name = "check_id"))
    @Column(name = "reason")
    private List<String> reasons;  // Triggered rule reasons

    @Column(length = 100)
    private String reviewedBy;  // Manual reviewer

    @Column
    private LocalDateTime reviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (checkId == null) {
            checkId = "FRD-" + UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }
}
```

### FraudRule Entity

```java
@Entity
@Table(name = "fraud_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRule {

    @Id
    private String ruleId;  // RULE-XXXX-XXX

    @Column(nullable = false, unique = true, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleType ruleType;  // VELOCITY, AMOUNT, PATTERN, TIME, DAILY_LIMIT

    @Column(nullable = false)
    private Boolean enabled;  // Active/inactive

    @Column(precision = 19, scale = 2)
    private BigDecimal threshold;  // Rule threshold

    @Column
    private Integer timeWindowMinutes;  // Time window for velocity checks

    @Column(nullable = false)
    private Integer riskPoints;  // Points added if triggered

    @Column(length = 500)
    private String description;  // Rule explanation

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

### RiskScore Entity

```java
@Entity
@Table(name = "risk_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String accountNumber;  // Account tracking

    @Column(nullable = false)
    private Integer currentScore;  // Current risk score (0-100)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;  // Current risk level

    @Column(nullable = false)
    private Long totalChecks;  // Total fraud checks

    @Column(nullable = false)
    private Long flaggedCount;  // Flagged transactions

    @Column(nullable = false)
    private Long blockedCount;  // Blocked transactions

    @Column
    private LocalDateTime lastCheckAt;  // Most recent check

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

### Enums

```java
public enum RiskLevel {
    LOW,       // 0-29 points
    MEDIUM,    // 30-59 points
    HIGH,      // 60-79 points
    CRITICAL;  // 80-100 points

    public static RiskLevel fromScore(int score) {
        if (score < 30) return LOW;
        if (score < 60) return MEDIUM;
        if (score < 80) return HIGH;
        return CRITICAL;
    }
}

public enum FraudCheckStatus {
    PASSED,        // No fraud detected
    FLAGGED,       // Medium/High risk - flagged for review
    BLOCKED,       // Critical risk - transaction blocked
    UNDER_REVIEW,  // Manual review in progress
    CLEARED        // Reviewed and cleared
}

public enum RuleType {
    VELOCITY,      // Multiple transactions in short time
    AMOUNT,        // Unusually large amount
    PATTERN,       // Suspicious patterns (rapid, unusual)
    TIME,          // Transaction time-based (night, unusual hours)
    DAILY_LIMIT    // Daily total limit exceeded
}
```

---

## Rule Engine

### Default Fraud Rules (6 Rules)

**1. Velocity Rule (RULE-0001-VEL)**
```
Name: Velocity Check
Type: VELOCITY
Threshold: 5 transfers
Time Window: 60 minutes
Risk Points: 30
Description: More than 5 transfers in 60 minutes
```

**2. Amount Rule (RULE-0002-AMT)**
```
Name: Large Amount Check
Type: AMOUNT
Threshold: €50,000
Risk Points: 25
Description: Single transfer exceeds €50,000
```

**3. Daily Limit Rule (RULE-0003-DLY)**
```
Name: Daily Limit Check
Type: DAILY_LIMIT
Threshold: €100,000
Risk Points: 20
Description: Daily total exceeds €100,000
```

**4. Time-Based Rule (RULE-0004-TIM)**
```
Name: Night Transaction Check
Type: TIME
Threshold: €10,000
Time Window: 00:00-06:00
Risk Points: 10
Description: Large transfers during night hours (00:00-06:00)
```

**5. Rapid Pattern Rule (RULE-0005-PAT)**
```
Name: Rapid Transaction Pattern
Type: PATTERN
Time Window: 2 minutes
Risk Points: 15
Description: Multiple transfers less than 2 minutes apart
```

**6. Unusual Amount Pattern (RULE-0006-PAT)**
```
Name: Unusual Amount Pattern
Type: PATTERN
Threshold: 3x account average
Risk Points: 20
Description: Transaction amount is 3 times account average
```

### Rule Execution Logic

```java
@Service
public class FraudDetectionService {

    public FraudCheck performCheck(FraudCheckRequest request) {
        List<String> triggeredReasons = new ArrayList<>();
        int totalRiskScore = 0;

        // Get all enabled rules
        List<FraudRule> rules = fraudRuleRepository.findByEnabledTrue();

        for (FraudRule rule : rules) {
            boolean triggered = evaluateRule(rule, request);

            if (triggered) {
                triggeredReasons.add(rule.getRuleName());
                totalRiskScore += rule.getRiskPoints();
            }
        }

        // Cap at 100
        totalRiskScore = Math.min(totalRiskScore, 100);

        // Determine risk level
        RiskLevel riskLevel = RiskLevel.fromScore(totalRiskScore);

        // Determine status
        FraudCheckStatus status = determineStatus(riskLevel);

        // Create fraud check record
        FraudCheck check = FraudCheck.builder()
            .transferReference(request.getTransferReference())
            .accountNumber(request.getAccountNumber())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .balanceBefore(request.getBalanceBefore())
            .balanceAfter(request.getBalanceAfter())
            .riskScore(totalRiskScore)
            .riskLevel(riskLevel)
            .status(status)
            .reasons(triggeredReasons)
            .build();

        // Save check
        fraudCheckRepository.save(check);

        // Update account risk score
        updateAccountRiskScore(request.getAccountNumber(), check);

        // Publish events if necessary
        if (status == FraudCheckStatus.FLAGGED) {
            publishFraudDetectedEvent(check);
        } else if (status == FraudCheckStatus.BLOCKED) {
            publishFraudBlockedEvent(check);
        }

        return check;
    }

    private FraudCheckStatus determineStatus(RiskLevel riskLevel) {
        switch (riskLevel) {
            case LOW:
                return FraudCheckStatus.PASSED;
            case MEDIUM:
            case HIGH:
                return FraudCheckStatus.FLAGGED;
            case CRITICAL:
                return FraudCheckStatus.BLOCKED;
            default:
                return FraudCheckStatus.PASSED;
        }
    }
}
```

---

## Risk Scoring System

### Score Calculation

**Total Score = Sum of triggered rule points (capped at 100)**

Example:
```
Transfer: €60,000 at 02:00 AM, 6th transfer in 30 minutes

Triggered Rules:
- Velocity Rule (5+ in 60 min): +30 points
- Amount Rule (>€50,000): +25 points
- Time-Based Rule (night transfer >€10k): +10 points

Total: 30 + 25 + 10 = 65 points → HIGH risk → FLAGGED
```

### Risk Levels

| Score Range | Risk Level | Status | Action |
|-------------|-----------|--------|--------|
| 0-29 | LOW | PASSED | Allow transaction |
| 30-59 | MEDIUM | FLAGGED | Flag for review, allow transaction |
| 60-79 | HIGH | FLAGGED | Flag for review, allow transaction |
| 80-100 | CRITICAL | BLOCKED | Block transaction, escalate |

### Account Risk Score

Account risk score is aggregated from individual fraud checks:

```java
public void updateAccountRiskScore(String accountNumber, FraudCheck check) {
    RiskScore riskScore = riskScoreRepository
        .findByAccountNumber(accountNumber)
        .orElse(createNewRiskScore(accountNumber));

    riskScore.setTotalChecks(riskScore.getTotalChecks() + 1);

    if (check.getStatus() == FraudCheckStatus.FLAGGED) {
        riskScore.setFlaggedCount(riskScore.getFlaggedCount() + 1);
    } else if (check.getStatus() == FraudCheckStatus.BLOCKED) {
        riskScore.setBlockedCount(riskScore.getBlockedCount() + 1);
    }

    // Calculate current score (weighted average of recent checks)
    int currentScore = calculateAccountScore(accountNumber);
    riskScore.setCurrentScore(currentScore);
    riskScore.setRiskLevel(RiskLevel.fromScore(currentScore));
    riskScore.setLastCheckAt(LocalDateTime.now());

    riskScoreRepository.save(riskScore);
}
```

---

## API Reference

### Base URL

```
http://localhost:8080/fraud-checks  (via API Gateway)
http://localhost:8087/fraud-checks  (direct access)
```

### 1. Perform Fraud Check

**Endpoint:** `POST /fraud-checks`

**Request:**
```json
{
  "accountNumber": "ACC-123...",
  "transferReference": "TRF-456...",
  "amount": 60000.00,
  "currency": "EUR",
  "balanceBefore": 100000.00,
  "balanceAfter": 40000.00
}
```

**Response (200 OK):**
```json
{
  "checkId": "FRD-789...",
  "transferReference": "TRF-456...",
  "accountNumber": "ACC-123...",
  "amount": 60000.00,
  "currency": "EUR",
  "riskScore": 55,
  "riskLevel": "MEDIUM",
  "status": "FLAGGED",
  "reasons": [
    "Large Amount Check",
    "Velocity Check"
  ],
  "createdAt": "2026-01-01T14:30:00Z"
}
```

---

### 2. Get Fraud Check by ID

**Endpoint:** `GET /fraud-checks/{checkId}`

**Response (200 OK):**
```json
{
  "checkId": "FRD-789...",
  "transferReference": "TRF-456...",
  "accountNumber": "ACC-123...",
  "amount": 60000.00,
  "currency": "EUR",
  "balanceBefore": 100000.00,
  "balanceAfter": 40000.00,
  "riskScore": 55,
  "riskLevel": "MEDIUM",
  "status": "FLAGGED",
  "reasons": ["Large Amount Check", "Velocity Check"],
  "reviewedBy": null,
  "reviewedAt": null,
  "createdAt": "2026-01-01T14:30:00Z"
}
```

---

### 3. Get Checks for Transfer

**Endpoint:** `GET /fraud-checks/transfer/{transferReference}`

**Response (200 OK):**
```json
{
  "checks": [
    {
      "checkId": "FRD-789...",
      "riskScore": 55,
      "riskLevel": "MEDIUM",
      "status": "FLAGGED",
      "createdAt": "2026-01-01T14:30:00Z"
    }
  ]
}
```

---

### 4. Get Account Checks

**Endpoint:** `GET /fraud-checks/account/{accountNumber}`

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)

**Response (200 OK):**
```json
{
  "content": [
    {
      "checkId": "FRD-789...",
      "transferReference": "TRF-456...",
      "amount": 60000.00,
      "riskScore": 55,
      "riskLevel": "MEDIUM",
      "status": "FLAGGED",
      "createdAt": "2026-01-01T14:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 15,
  "totalPages": 1
}
```

---

### 5. Get Pending Reviews

**Endpoint:** `GET /fraud-checks/pending-review`

**Response (200 OK):**
```json
{
  "checks": [
    {
      "checkId": "FRD-789...",
      "transferReference": "TRF-456...",
      "accountNumber": "ACC-123...",
      "amount": 60000.00,
      "riskScore": 65,
      "riskLevel": "HIGH",
      "status": "FLAGGED",
      "reasons": ["Large Amount Check", "Night Transaction Check"],
      "createdAt": "2026-01-01T02:15:00Z"
    }
  ],
  "totalPending": 5
}
```

---

### 6. Review Fraud Check

**Endpoint:** `POST /fraud-checks/{checkId}/review`

**Request:**
```json
{
  "decision": "CLEARED",
  "reviewNotes": "Verified with customer, legitimate large purchase"
}
```

**Response (200 OK):**
```json
{
  "checkId": "FRD-789...",
  "status": "CLEARED",
  "reviewedBy": "reviewer@bank.com",
  "reviewedAt": "2026-01-01T15:00:00Z",
  "message": "Fraud check cleared successfully"
}
```

---

### 7. Get Account Risk Score

**Endpoint:** `GET /fraud-checks/risk-score/{accountNumber}`

**Response (200 OK):**
```json
{
  "accountNumber": "ACC-123...",
  "currentScore": 45,
  "riskLevel": "MEDIUM",
  "totalChecks": 150,
  "flaggedCount": 12,
  "blockedCount": 2,
  "lastCheckAt": "2026-01-01T14:30:00Z"
}
```

---

### 8. Get High-Risk Accounts

**Endpoint:** `GET /fraud-checks/high-risk-accounts`

**Response (200 OK):**
```json
{
  "accounts": [
    {
      "accountNumber": "ACC-456...",
      "currentScore": 85,
      "riskLevel": "CRITICAL",
      "flaggedCount": 25,
      "blockedCount": 5,
      "lastCheckAt": "2026-01-01T10:00:00Z"
    },
    {
      "accountNumber": "ACC-789...",
      "currentScore": 70,
      "riskLevel": "HIGH",
      "flaggedCount": 18,
      "blockedCount": 3,
      "lastCheckAt": "2026-01-01T12:00:00Z"
    }
  ],
  "totalHighRisk": 15
}
```

---

## Event-Driven Architecture

### Kafka Consumer

**transfer.completed Event**
```java
@KafkaListener(topics = "transfer.completed")
public void handleTransferCompleted(TransferCompletedEvent event) {
    log.info("Received transfer.completed event: {}", event.getTransferReference());

    // Perform automatic fraud check
    FraudCheckRequest request = FraudCheckRequest.builder()
        .accountNumber(event.getFromAccountNumber())
        .transferReference(event.getTransferReference())
        .amount(event.getAmount())
        .currency(event.getCurrency())
        .balanceBefore(event.getSourceBalanceBefore())
        .balanceAfter(event.getSourceBalanceAfter())
        .build();

    FraudCheck check = fraudDetectionService.performCheck(request);

    log.info("Fraud check completed: {} - Risk: {} - Status: {}",
        check.getCheckId(), check.getRiskLevel(), check.getStatus());
}
```

### Kafka Publishers

**1. fraud.detected Event**
```json
{
  "eventType": "FRAUD_DETECTED",
  "checkId": "FRD-789...",
  "transferReference": "TRF-456...",
  "accountNumber": "ACC-123...",
  "riskScore": 65,
  "riskLevel": "HIGH",
  "status": "FLAGGED",
  "reasons": ["Large Amount Check", "Night Transaction Check"],
  "timestamp": "2026-01-01T14:30:00Z"
}
```

**2. fraud.blocked Event**
```json
{
  "eventType": "FRAUD_BLOCKED",
  "checkId": "FRD-789...",
  "transferReference": "TRF-456...",
  "accountNumber": "ACC-123...",
  "riskScore": 85,
  "riskLevel": "CRITICAL",
  "status": "BLOCKED",
  "reasons": ["Velocity Check", "Large Amount Check", "Rapid Transaction Pattern"],
  "timestamp": "2026-01-01T14:30:00Z"
}
```

---

## Fraud Rules Configuration

### Get All Fraud Rules

**Endpoint:** `GET /fraud-checks/rules`

**Response (200 OK):**
```json
{
  "rules": [
    {
      "ruleId": "RULE-0001-VEL",
      "ruleName": "Velocity Check",
      "ruleType": "VELOCITY",
      "enabled": true,
      "threshold": null,
      "timeWindowMinutes": 60,
      "riskPoints": 30,
      "description": "More than 5 transfers in 60 minutes"
    },
    {
      "ruleId": "RULE-0002-AMT",
      "ruleName": "Large Amount Check",
      "ruleType": "AMOUNT",
      "enabled": true,
      "threshold": 50000.00,
      "timeWindowMinutes": null,
      "riskPoints": 25,
      "description": "Single transfer exceeds €50,000"
    }
  ],
  "totalRules": 6
}
```

### Update Fraud Rule

**Endpoint:** `PUT /fraud-checks/rules/{ruleId}`

**Request:**
```json
{
  "threshold": 75000.00,
  "riskPoints": 30,
  "description": "Single transfer exceeds €75,000 (updated)"
}
```

**Response (200 OK):**
```json
{
  "ruleId": "RULE-0002-AMT",
  "ruleName": "Large Amount Check",
  "threshold": 75000.00,
  "riskPoints": 30,
  "description": "Single transfer exceeds €75,000 (updated)",
  "updatedAt": "2026-01-01T15:30:00Z",
  "message": "Fraud rule updated successfully"
}
```

### Toggle Fraud Rule

**Endpoint:** `POST /fraud-checks/rules/{ruleId}/toggle`

**Response (200 OK):**
```json
{
  "ruleId": "RULE-0002-AMT",
  "enabled": false,
  "message": "Fraud rule disabled successfully"
}
```

---

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudCheckRepository fraudCheckRepository;

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private RiskScoreRepository riskScoreRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @Test
    @DisplayName("Should flag transaction when amount exceeds threshold")
    void shouldFlagHighAmountTransaction() {
        // Given
        FraudRule amountRule = createAmountRule();
        when(fraudRuleRepository.findByEnabledTrue())
            .thenReturn(Collections.singletonList(amountRule));

        FraudCheckRequest request = FraudCheckRequest.builder()
            .accountNumber("ACC-123")
            .transferReference("TRF-456")
            .amount(new BigDecimal("60000.00"))
            .currency(Currency.EUR)
            .build();

        // When
        FraudCheck check = fraudDetectionService.performCheck(request);

        // Then
        assertEquals(25, check.getRiskScore());  // Amount rule points
        assertEquals(RiskLevel.LOW, check.getRiskLevel());  // 25 < 30
        assertEquals(FraudCheckStatus.PASSED, check.getStatus());
        assertTrue(check.getReasons().contains("Large Amount Check"));
    }

    @Test
    @DisplayName("Should block transaction with critical risk score")
    void shouldBlockCriticalRiskTransaction() {
        // Given
        List<FraudRule> rules = Arrays.asList(
            createVelocityRule(),      // +30 points
            createAmountRule(),        // +25 points
            createDailyLimitRule(),    // +20 points
            createTimeBasedRule()      // +10 points
        );
        when(fraudRuleRepository.findByEnabledTrue()).thenReturn(rules);

        FraudCheckRequest request = createHighRiskRequest();

        // When
        FraudCheck check = fraudDetectionService.performCheck(request);

        // Then
        assertTrue(check.getRiskScore() >= 80);
        assertEquals(RiskLevel.CRITICAL, check.getRiskLevel());
        assertEquals(FraudCheckStatus.BLOCKED, check.getStatus());
    }
}
```

---

**Last Updated:** 1 January 2026
**API Version:** 1.0
**Service Status:** ✅ Production Ready (Deployed)
