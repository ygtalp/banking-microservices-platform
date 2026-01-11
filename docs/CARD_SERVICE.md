# Card Service - Complete Reference

> **Service:** Card Service
> **Port:** 8089
> **Database:** card_db (PostgreSQL)
> **Responsibility:** Card lifecycle management, activation, blocking, and PIN management
> **Last Updated:** 1 January 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model](#domain-model)
3. [API Reference](#api-reference)
4. [Card Types](#card-types)
5. [Card Lifecycle](#card-lifecycle)
6. [Security Features](#security-features)
7. [Business Rules](#business-rules)
8. [Testing](#testing)

---

## Overview

Card Service manages the complete lifecycle of bank cards including creation, activation, blocking, PIN management, and transaction authorization.

### Key Features

- ✅ Multiple card types (Debit, Credit, Prepaid)
- ✅ Card lifecycle management (creation → activation → blocking → closure)
- ✅ PIN management (set, change, reset)
- ✅ Card limits (daily, monthly)
- ✅ Card blocking/unblocking
- ✅ Transaction authorization (future)
- ✅ Expiry date management
- ✅ Swagger/OpenAPI documentation

### Technology Stack

```yaml
Framework: Spring Boot 3.2.0
Database: PostgreSQL 16
Documentation: Swagger/OpenAPI 3.0
Security: BCrypt (for PIN hashing)
Validation: Spring Validation + Hibernate Validator
```

---

## Domain Model

### Card Entity

```java
@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    private String cardId;  // CARD-XXXXXXXXXXXX

    @Column(nullable = false, unique = true, length = 16)
    private String cardNumber;  // 16-digit card number

    @Column(nullable = false, length = 50)
    private String accountNumber;  // Linked account

    @Column(nullable = false, length = 100)
    private String cardholderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardType cardType;  // DEBIT, CREDIT, PREPAID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;  // PENDING, ACTIVE, BLOCKED, EXPIRED, CLOSED

    @Column(nullable = false, length = 7)
    private String expiryDate;  // MM/YYYY format

    @Column(nullable = false, length = 3)
    private String cvv;  // CVV code (encrypted/hashed)

    @Column(length = 255)
    private String pinHash;  // BCrypt hashed PIN

    @Column
    private Boolean pinSet;  // PIN has been set

    @Column(precision = 19, scale = 2)
    private BigDecimal dailyLimit;  // Daily transaction limit

    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyLimit;  // Monthly transaction limit

    @Column(precision = 19, scale = 2)
    private BigDecimal dailySpent;  // Amount spent today

    @Column(precision = 19, scale = 2)
    private BigDecimal monthlySpent;  // Amount spent this month

    @Column
    private LocalDate lastResetDate;  // Last limit reset date

    @Column(length = 100)
    private String blockedReason;  // Reason for blocking

    @Column
    private LocalDateTime blockedAt;

    @Column
    private LocalDateTime activatedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (cardId == null) {
            cardId = "CARD-" + UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = CardStatus.PENDING;
        pinSet = false;
        dailySpent = BigDecimal.ZERO;
        monthlySpent = BigDecimal.ZERO;
        lastResetDate = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### CardTransaction Entity (Future)

```java
@Entity
@Table(name = "card_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTransaction {

    @Id
    private String transactionId;  // CTXN-XXXXXXXXXXXX

    @Column(nullable = false, length = 50)
    private String cardId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;

    @Column(length = 100)
    private String merchantName;

    @Column(length = 50)
    private String merchantCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus transactionStatus;  // AUTHORIZED, DECLINED, SETTLED

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### Enums

```java
public enum CardType {
    DEBIT,      // Debit card (linked to account balance)
    CREDIT,     // Credit card (credit line)
    PREPAID     // Prepaid card (preloaded balance)
}

public enum CardStatus {
    PENDING,    // Card created, not yet activated
    ACTIVE,     // Card active and usable
    BLOCKED,    // Card blocked (temporarily)
    EXPIRED,    // Card expired
    CLOSED      // Card permanently closed
}

public enum TransactionStatus {
    AUTHORIZED,  // Transaction authorized
    DECLINED,    // Transaction declined
    SETTLED      // Transaction settled
}
```

---

## API Reference

### Base URL

```
http://localhost:8080/cards  (via API Gateway)
http://localhost:8089/cards  (direct access)
```

### 1. Create Card

**Endpoint:** `POST /cards`

**Request:**
```json
{
  "accountNumber": "ACC-123...",
  "cardholderName": "JOHN DOE",
  "cardType": "DEBIT",
  "dailyLimit": 5000.00,
  "monthlyLimit": 50000.00
}
```

**Response (201 Created):**
```json
{
  "cardId": "CARD-789...",
  "cardNumber": "4532015112830366",
  "accountNumber": "ACC-123...",
  "cardholderName": "JOHN DOE",
  "cardType": "DEBIT",
  "status": "PENDING",
  "expiryDate": "12/2029",
  "dailyLimit": 5000.00,
  "monthlyLimit": 50000.00,
  "pinSet": false,
  "createdAt": "2026-01-01T10:00:00Z"
}
```

**Note:** CVV is not returned in response for security reasons.

---

### 2. Get Card by ID

**Endpoint:** `GET /cards/{cardId}`

**Response (200 OK):**
```json
{
  "cardId": "CARD-789...",
  "cardNumber": "4532 **** **** 0366",
  "accountNumber": "ACC-123...",
  "cardholderName": "JOHN DOE",
  "cardType": "DEBIT",
  "status": "ACTIVE",
  "expiryDate": "12/2029",
  "dailyLimit": 5000.00,
  "monthlyLimit": 50000.00,
  "dailySpent": 150.00,
  "monthlySpent": 3250.00,
  "pinSet": true,
  "activatedAt": "2026-01-01T14:00:00Z"
}
```

**Note:** Card number is masked (only first 4 and last 4 digits shown).

---

### 3. Activate Card

**Endpoint:** `POST /cards/{cardId}/activate`

**Request:**
```json
{
  "cardNumber": "4532015112830366",
  "cvv": "123"
}
```

**Response (200 OK):**
```json
{
  "cardId": "CARD-789...",
  "status": "ACTIVE",
  "activatedAt": "2026-01-01T14:00:00Z",
  "message": "Card activated successfully"
}
```

**Validation:**
- Card number must match
- CVV must match
- Card status must be PENDING

---

### 4. Set PIN

**Endpoint:** `POST /cards/{cardId}/set-pin`

**Request:**
```json
{
  "pin": "1234",
  "confirmPin": "1234"
}
```

**Response (200 OK):**
```json
{
  "cardId": "CARD-789...",
  "pinSet": true,
  "message": "PIN set successfully"
}
```

**Validation:**
- PIN must be 4 or 6 digits
- PIN and confirmPin must match
- Card must be ACTIVE

**Security:**
- PIN is hashed using BCrypt (strength 12)
- Plain PIN is NEVER stored
- PIN is NEVER returned in API responses

---

### 5. Change PIN

**Endpoint:** `POST /cards/{cardId}/change-pin`

**Request:**
```json
{
  "currentPin": "1234",
  "newPin": "5678",
  "confirmNewPin": "5678"
}
```

**Response (200 OK):**
```json
{
  "cardId": "CARD-789...",
  "message": "PIN changed successfully"
}
```

**Validation:**
- Current PIN must be correct
- New PIN must be different from current
- New PIN and confirmNewPin must match

---

### 6. Block Card

**Endpoint:** `POST /cards/{cardId}/block`

**Request:**
```json
{
  "reason": "Card lost/stolen"
}
```

**Response (200 OK):**
```json
{
  "cardId": "CARD-789...",
  "status": "BLOCKED",
  "blockedReason": "Card lost/stolen",
  "blockedAt": "2026-01-01T15:00:00Z",
  "message": "Card blocked successfully"
}
```

**Effect:**
- Card status changed to BLOCKED
- All transactions will be declined
- Card can be unblocked later

---

### 7. Unblock Card

**Endpoint:** `POST /cards/{cardId}/unblock`

**Response (200 OK):**
```json
{
  "cardId": "CARD-789...",
  "status": "ACTIVE",
  "message": "Card unblocked successfully"
}
```

**Validation:**
- Card status must be BLOCKED
- Card must not be EXPIRED or CLOSED

---

### 8. Close Card

**Endpoint:** `DELETE /cards/{cardId}`

**Response (200 OK):**
```json
{
  "cardId": "CARD-789...",
  "status": "CLOSED",
  "message": "Card closed successfully"
}
```

**Effect:**
- Card status changed to CLOSED
- Card cannot be reactivated
- All future transactions will be declined

---

### 9. Get Account Cards

**Endpoint:** `GET /cards/account/{accountNumber}`

**Response (200 OK):**
```json
{
  "cards": [
    {
      "cardId": "CARD-789...",
      "cardNumber": "4532 **** **** 0366",
      "cardType": "DEBIT",
      "status": "ACTIVE",
      "expiryDate": "12/2029"
    },
    {
      "cardId": "CARD-456...",
      "cardNumber": "5425 **** **** 1234",
      "cardType": "CREDIT",
      "status": "BLOCKED",
      "expiryDate": "06/2028"
    }
  ],
  "totalCards": 2,
  "activeCards": 1
}
```

---

### 10. Update Card Limits

**Endpoint:** `PUT /cards/{cardId}/limits`

**Request:**
```json
{
  "dailyLimit": 10000.00,
  "monthlyLimit": 100000.00
}
```

**Response (200 OK):**
```json
{
  "cardId": "CARD-789...",
  "dailyLimit": 10000.00,
  "monthlyLimit": 100000.00,
  "message": "Card limits updated successfully"
}
```

---

## Card Types

### 1. Debit Card
- **Purpose:** Linked to customer's bank account
- **Balance:** Uses account balance
- **Limit:** Daily and monthly limits
- **Overdraft:** Not allowed (unless account has overdraft facility)
- **Typical Daily Limit:** €1,000 - €10,000
- **Typical Monthly Limit:** €10,000 - €100,000

### 2. Credit Card
- **Purpose:** Credit line provided by bank
- **Balance:** Separate credit balance
- **Limit:** Credit limit (e.g., €5,000)
- **Repayment:** Monthly billing cycle
- **Interest:** Charged on unpaid balance
- **Typical Credit Limit:** €1,000 - €50,000

### 3. Prepaid Card
- **Purpose:** Preloaded with funds
- **Balance:** Fixed amount loaded by customer
- **Limit:** Loaded balance
- **Reload:** Can be reloaded
- **Use Case:** Travel, budgeting, gift cards
- **Typical Load Amount:** €100 - €5,000

---

## Card Lifecycle

### Creation → Activation → Usage → Expiry/Closure

**1. Creation (PENDING)**
- Card issued to customer
- Card number, expiry date, CVV generated
- Default limits set
- Status: PENDING

**2. Activation (ACTIVE)**
- Customer activates card (verify card number + CVV)
- Status changes to ACTIVE
- Card ready for use
- PIN must be set before first transaction

**3. Usage (ACTIVE)**
- Customer uses card for transactions
- Daily/monthly limits enforced
- Transaction authorization checks:
  - Card status is ACTIVE
  - PIN is correct (for PIN transactions)
  - Sufficient balance/credit
  - Within daily/monthly limits
  - Card not expired

**4. Blocking (BLOCKED)**
- Customer or bank blocks card (lost, stolen, suspicious activity)
- Status changes to BLOCKED
- All transactions declined
- Can be unblocked

**5. Expiry (EXPIRED)**
- Card reaches expiry date
- Status automatically changes to EXPIRED
- Replacement card can be issued

**6. Closure (CLOSED)**
- Customer or bank closes card permanently
- Status changes to CLOSED
- Cannot be reactivated

---

## Security Features

### 1. PIN Protection

**BCrypt Hashing:**
```java
// Set PIN
public void setPin(String cardId, String pin) {
    // Validate PIN format (4 or 6 digits)
    if (!pin.matches("\\d{4}|\\d{6}")) {
        throw new InvalidPinException("PIN must be 4 or 6 digits");
    }

    // Hash PIN using BCrypt
    String pinHash = BCrypt.hashpw(pin, BCrypt.gensalt(12));

    // Store hash (NEVER store plain PIN)
    card.setPinHash(pinHash);
    card.setPinSet(true);
}

// Verify PIN
public boolean verifyPin(String cardId, String pin) {
    Card card = getCard(cardId);

    if (!card.getPinSet()) {
        throw new PinNotSetException("PIN has not been set for this card");
    }

    // Verify using BCrypt
    return BCrypt.checkpw(pin, card.getPinHash());
}
```

### 2. Card Number Masking

**API Response:**
```java
public String getMaskedCardNumber(String cardNumber) {
    // Format: 4532 **** **** 0366
    return cardNumber.substring(0, 4) +
           " **** **** " +
           cardNumber.substring(12, 16);
}
```

### 3. CVV Protection

- CVV is NEVER returned in API responses
- CVV is stored encrypted or hashed
- CVV is only used for card activation

### 4. Transaction Limits

**Daily Limit Reset:**
```java
@Scheduled(cron = "0 0 0 * * *")  // Daily at midnight
public void resetDailyLimits() {
    List<Card> cards = cardRepository.findByStatusIn(
        Arrays.asList(CardStatus.ACTIVE, CardStatus.BLOCKED)
    );

    for (Card card : cards) {
        card.setDailySpent(BigDecimal.ZERO);
        card.setLastResetDate(LocalDate.now());
    }

    cardRepository.saveAll(cards);
}
```

**Monthly Limit Reset:**
```java
@Scheduled(cron = "0 0 0 1 * *")  // First day of month at midnight
public void resetMonthlyLimits() {
    List<Card> cards = cardRepository.findByStatusIn(
        Arrays.asList(CardStatus.ACTIVE, CardStatus.BLOCKED)
    );

    for (Card card : cards) {
        card.setMonthlySpent(BigDecimal.ZERO);
    }

    cardRepository.saveAll(cards);
}
```

---

## Business Rules

### Card Issuance

1. **One Account, Multiple Cards**
   - Customer can have multiple cards per account
   - Each card type limited (e.g., max 2 debit cards)

2. **Card Number Generation**
   - 16-digit number
   - Follows Luhn algorithm for validation
   - Unique across system

3. **Expiry Date**
   - Default: 5 years from issue date
   - Format: MM/YYYY
   - Auto-expire at end of expiry month

4. **CVV Generation**
   - 3-digit random number
   - Encrypted/hashed for storage

### Transaction Authorization

1. **Status Check**
   - Card status must be ACTIVE
   - If BLOCKED, EXPIRED, or CLOSED → decline

2. **PIN Verification**
   - PIN must match stored hash
   - 3 failed attempts → block card

3. **Balance/Credit Check**
   - Debit: Account balance >= transaction amount
   - Credit: Available credit >= transaction amount
   - Prepaid: Loaded balance >= transaction amount

4. **Limit Check**
   - Daily spent + transaction amount <= daily limit
   - Monthly spent + transaction amount <= monthly limit

5. **Expiry Check**
   - Current date <= expiry date
   - If expired → decline

---

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private CardService cardService;

    @Test
    @DisplayName("Should create card successfully")
    void shouldCreateCardSuccessfully() {
        // Given
        CreateCardRequest request = CreateCardRequest.builder()
            .accountNumber("ACC-123")
            .cardholderName("JOHN DOE")
            .cardType(CardType.DEBIT)
            .dailyLimit(new BigDecimal("5000.00"))
            .monthlyLimit(new BigDecimal("50000.00"))
            .build();

        // When
        Card card = cardService.createCard(request);

        // Then
        assertNotNull(card.getCardId());
        assertNotNull(card.getCardNumber());
        assertEquals(16, card.getCardNumber().length());
        assertEquals(CardStatus.PENDING, card.getStatus());
        assertFalse(card.getPinSet());
    }

    @Test
    @DisplayName("Should activate card with correct details")
    void shouldActivateCardWithCorrectDetails() {
        // Given
        Card card = createTestCard();
        card.setStatus(CardStatus.PENDING);

        when(cardRepository.findById(card.getCardId())).thenReturn(Optional.of(card));

        ActivateCardRequest request = new ActivateCardRequest(
            card.getCardNumber(),
            card.getCvv()
        );

        // When
        Card activatedCard = cardService.activateCard(card.getCardId(), request);

        // Then
        assertEquals(CardStatus.ACTIVE, activatedCard.getStatus());
        assertNotNull(activatedCard.getActivatedAt());
    }

    @Test
    @DisplayName("Should set PIN successfully")
    void shouldSetPinSuccessfully() {
        // Given
        Card card = createTestCard();
        card.setStatus(CardStatus.ACTIVE);
        card.setPinSet(false);

        when(cardRepository.findById(card.getCardId())).thenReturn(Optional.of(card));
        when(passwordEncoder.encode("1234")).thenReturn("$2a$12$hashedPin...");

        SetPinRequest request = new SetPinRequest("1234", "1234");

        // When
        cardService.setPin(card.getCardId(), request);

        // Then
        assertTrue(card.getPinSet());
        assertNotNull(card.getPinHash());
    }

    @Test
    @DisplayName("Should block card successfully")
    void shouldBlockCardSuccessfully() {
        // Given
        Card card = createTestCard();
        card.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(card.getCardId())).thenReturn(Optional.of(card));

        // When
        Card blockedCard = cardService.blockCard(card.getCardId(), "Lost card");

        // Then
        assertEquals(CardStatus.BLOCKED, blockedCard.getStatus());
        assertEquals("Lost card", blockedCard.getBlockedReason());
        assertNotNull(blockedCard.getBlockedAt());
    }
}
```

---

**Last Updated:** 1 January 2026
**API Version:** 1.0
**Service Status:** ✅ Deployed (Needs Unit Tests)
**Swagger UI:** http://localhost:8089/swagger-ui/index.html
