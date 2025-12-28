# Domain-Specific Agents

> **Category:** Banking Domain Expertise
> **Agent Count:** 3
> **Automation Level:** Medium (60-70%)
> **Last Updated:** 28 December 2025

---

## 1. BankingDomainAgent 🏦

**Objective:** Implement banking business rules, regulations, and compliance requirements.

**Domain Knowledge:**
- Banking regulations (PSD2, GDPR, PCI DSS)
- Transaction processing rules
- Compliance requirements
- Audit trail standards
- Risk management

**Example: Transaction Limits Implementation**

```java
/**
 * Banking business rules for transfer limits
 */
@Component
public class TransferLimitValidator {

    private static final BigDecimal DAILY_LIMIT_INDIVIDUAL = new BigDecimal("50000.00");
    private static final BigDecimal DAILY_LIMIT_BUSINESS = new BigDecimal("500000.00");
    private static final BigDecimal SINGLE_TRANSFER_LIMIT = new BigDecimal("10000.00");
    private static final int MAX_TRANSFERS_PER_HOUR = 10;

    private final TransferRepository transferRepository;

    public void validateTransferLimits(Transfer transfer, Account sourceAccount) {
        // Rule 1: Single transfer limit
        if (transfer.getAmount().compareTo(SINGLE_TRANSFER_LIMIT) > 0) {
            throw new TransferLimitExceededException(
                "Single transfer limit exceeded: " +
                SINGLE_TRANSFER_LIMIT + " " + transfer.getCurrency()
            );
        }

        // Rule 2: Daily limit (by account type)
        BigDecimal dailyLimit = sourceAccount.getAccountType() == AccountType.BUSINESS
            ? DAILY_LIMIT_BUSINESS
            : DAILY_LIMIT_INDIVIDUAL;

        BigDecimal dailyTotal = calculateDailyTotal(sourceAccount.getAccountNumber());

        if (dailyTotal.add(transfer.getAmount()).compareTo(dailyLimit) > 0) {
            throw new DailyLimitExceededException(
                String.format("Daily limit exceeded. Limit: %s, Used: %s, Attempted: %s",
                    dailyLimit, dailyTotal, transfer.getAmount())
            );
        }

        // Rule 3: Velocity check (anti-fraud)
        int transfersLastHour = countTransfersLastHour(sourceAccount.getAccountNumber());
        if (transfersLastHour >= MAX_TRANSFERS_PER_HOUR) {
            throw new VelocityCheckFailedException(
                "Too many transfers in the last hour. Please try again later."
            );
        }

        // Rule 4: Weekend/holiday restrictions (if configured)
        if (isWeekend() && transfer.getAmount().compareTo(new BigDecimal("5000")) > 0) {
            throw new BusinessRuleViolationException(
                "High-value transfers (>5000) not allowed on weekends"
            );
        }
    }

    private BigDecimal calculateDailyTotal(String accountNumber) {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return transferRepository
            .findByFromAccountNumberAndCreatedAtAfter(accountNumber, startOfDay)
            .stream()
            .map(Transfer::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

**PSD2 Compliance (Strong Customer Authentication)**

```java
/**
 * PSD2 Strong Customer Authentication (SCA)
 */
@Service
public class StrongCustomerAuthenticationService {

    // Transactions over €30 require SCA
    private static final BigDecimal SCA_THRESHOLD = new BigDecimal("30.00");

    public boolean requiresSCA(Transfer transfer) {
        // Rule 1: Amount threshold
        if (transfer.getAmount().compareTo(SCA_THRESHOLD) > 0) {
            return true;
        }

        // Rule 2: Beneficiary not whitelisted
        if (!isWhitelistedBeneficiary(transfer.getToAccountNumber())) {
            return true;
        }

        // Rule 3: Suspicious activity detected
        if (isSuspiciousTransaction(transfer)) {
            return true;
        }

        return false;
    }

    public void performSCA(String userId, String transferId, String otpCode) {
        // Verify OTP
        if (!verifyOTP(userId, otpCode)) {
            throw new SCAFailedException("Invalid OTP code");
        }

        // Mark transfer as authenticated
        Transfer transfer = transferRepository.findByTransferReference(transferId)
            .orElseThrow();
        transfer.setScaCompleted(true);
        transfer.setScaCompletedAt(LocalDateTime.now());
        transferRepository.save(transfer);
    }
}
```

---

## 2. KYCAgent 📄

**Objective:** Implement Know Your Customer (KYC) workflows and document verification logic.

**KYC Implementation:**

```java
/**
 * KYC Verification Workflow (3-tier)
 */
@Service
public class KYCVerificationService {

    // Tier 1: Basic verification (automated)
    public KYCResult verifyTier1(Customer customer) {
        List<String> issues = new ArrayList<>();

        // Check 1: Email verification
        if (!customer.isEmailVerified()) {
            issues.add("Email not verified");
        }

        // Check 2: Phone verification
        if (!customer.isPhoneVerified()) {
            issues.add("Phone not verified");
        }

        // Check 3: Basic information completeness
        if (isMissingBasicInfo(customer)) {
            issues.add("Incomplete personal information");
        }

        if (issues.isEmpty()) {
            customer.setKycTier(KYCTier.TIER_1);
            customer.setTier1VerifiedAt(LocalDateTime.now());
            return KYCResult.passed("Tier 1 verification passed");
        }

        return KYCResult.failed(issues);
    }

    // Tier 2: Document verification (manual)
    public KYCResult verifyTier2(Customer customer, List<KycDocument> documents) {
        List<String> issues = new ArrayList<>();

        // Requirement 1: Government-issued ID
        boolean hasValidID = documents.stream()
            .anyMatch(doc ->
                (doc.getDocumentType() == DocumentType.PASSPORT ||
                 doc.getDocumentType() == DocumentType.NATIONAL_ID) &&
                doc.getStatus() == DocumentStatus.VERIFIED &&
                !isExpired(doc)
            );

        if (!hasValidID) {
            issues.add("Valid government-issued ID required");
        }

        // Requirement 2: Proof of address (< 3 months old)
        boolean hasProofOfAddress = documents.stream()
            .anyMatch(doc ->
                doc.getDocumentType() == DocumentType.PROOF_OF_ADDRESS &&
                doc.getStatus() == DocumentStatus.VERIFIED &&
                isRecent(doc, 90) // 90 days
            );

        if (!hasProofOfAddress) {
            issues.add("Recent proof of address required (< 3 months)");
        }

        if (issues.isEmpty()) {
            customer.setKycTier(KYCTier.TIER_2);
            customer.setTier2VerifiedAt(LocalDateTime.now());
            return KYCResult.passed("Tier 2 verification passed");
        }

        return KYCResult.failed(issues);
    }

    // Tier 3: Enhanced due diligence (for high-risk customers)
    public KYCResult verifyTier3(Customer customer, List<KycDocument> documents) {
        List<String> issues = new ArrayList<>();

        // Additional requirement 1: Source of funds
        boolean hasSourceOfFunds = documents.stream()
            .anyMatch(doc ->
                doc.getDocumentType() == DocumentType.BANK_STATEMENT &&
                doc.getStatus() == DocumentStatus.VERIFIED
            );

        if (!hasSourceOfFunds) {
            issues.add("Source of funds documentation required");
        }

        // Additional requirement 2: Tax documents
        boolean hasTaxDoc = documents.stream()
            .anyMatch(doc ->
                doc.getDocumentType() == DocumentType.TAX_DOCUMENT &&
                doc.getStatus() == DocumentStatus.VERIFIED
            );

        if (!hasTaxDoc) {
            issues.add("Tax documentation required");
        }

        // Check 3: PEP (Politically Exposed Person) screening
        if (isPoliticallyExposed(customer)) {
            // Require additional approval
            issues.add("PEP screening requires compliance approval");
        }

        if (issues.isEmpty()) {
            customer.setKycTier(KYCTier.TIER_3);
            customer.setTier3VerifiedAt(LocalDateTime.now());
            customer.setRiskLevel(RiskLevel.LOW);
            return KYCResult.passed("Tier 3 verification passed");
        }

        return KYCResult.failed(issues);
    }

    // Risk scoring algorithm
    public RiskLevel calculateRiskLevel(Customer customer) {
        int riskScore = 0;

        // Factor 1: Country risk
        if (isHighRiskCountry(customer.getCountry())) {
            riskScore += 30;
        }

        // Factor 2: Transaction volume
        BigDecimal monthlyVolume = getMonthlyTransactionVolume(customer);
        if (monthlyVolume.compareTo(new BigDecimal("100000")) > 0) {
            riskScore += 20;
        }

        // Factor 3: Business type
        if (customer.getAccountType() == AccountType.BUSINESS) {
            String industry = customer.getIndustry();
            if (isHighRiskIndustry(industry)) {
                riskScore += 25;
            }
        }

        // Factor 4: Document verification failures
        if (customer.getDocumentRejectionCount() > 2) {
            riskScore += 15;
        }

        // Calculate final risk level
        if (riskScore >= 50) return RiskLevel.HIGH;
        if (riskScore >= 25) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }
}
```

---

## 3. PaymentAgent 💳

**Objective:** Implement payment processing logic, fee calculation, and fraud detection.

**Payment Processing:**

```java
/**
 * Payment Processing with Fraud Detection
 */
@Service
public class PaymentProcessingService {

    // Fee calculation based on amount and type
    public BigDecimal calculateFee(Transfer transfer) {
        BigDecimal amount = transfer.getAmount();
        TransferType type = transfer.getType();

        // Internal transfer (same bank): Free
        if (type == TransferType.INTERNAL) {
            return BigDecimal.ZERO;
        }

        // SEPA transfer: Flat fee €1
        if (type == TransferType.SEPA) {
            return new BigDecimal("1.00");
        }

        // International transfer: Percentage-based + flat fee
        if (type == TransferType.INTERNATIONAL) {
            BigDecimal percentageFee = amount
                .multiply(new BigDecimal("0.02"))  // 2%
                .setScale(2, RoundingMode.HALF_UP);

            BigDecimal flatFee = new BigDecimal("5.00");
            BigDecimal totalFee = percentageFee.add(flatFee);

            // Cap at €50
            BigDecimal maxFee = new BigDecimal("50.00");
            return totalFee.min(maxFee);
        }

        return BigDecimal.ZERO;
    }

    // Fraud detection rules
    public FraudCheckResult checkForFraud(Transfer transfer, Account sourceAccount) {
        List<String> alerts = new ArrayList<>();
        int riskScore = 0;

        // Rule 1: First transfer to new beneficiary with high amount
        if (isNewBeneficiary(transfer) &&
            transfer.getAmount().compareTo(new BigDecimal("5000")) > 0) {
            alerts.add("First transfer to new beneficiary with high amount");
            riskScore += 30;
        }

        // Rule 2: Rapid succession of transfers
        int transfersLast10Minutes = countTransfersLast10Minutes(sourceAccount);
        if (transfersLast10Minutes >= 5) {
            alerts.add("Multiple transfers in short time span");
            riskScore += 40;
        }

        // Rule 3: Round amount (possible money laundering)
        if (isRoundAmount(transfer.getAmount())) {
            alerts.add("Round amount (possible structuring)");
            riskScore += 10;
        }

        // Rule 4: Geographic anomaly
        if (isGeographicAnomaly(transfer)) {
            alerts.add("Geographic anomaly detected");
            riskScore += 25;
        }

        // Rule 5: Amount just below reporting threshold
        BigDecimal reportingThreshold = new BigDecimal("10000");
        if (transfer.getAmount().compareTo(reportingThreshold.multiply(new BigDecimal("0.95"))) > 0 &&
            transfer.getAmount().compareTo(reportingThreshold) < 0) {
            alerts.add("Amount just below reporting threshold (possible structuring)");
            riskScore += 35;
        }

        // Determine action
        if (riskScore >= 70) {
            return FraudCheckResult.block(alerts, "High fraud risk - transaction blocked");
        } else if (riskScore >= 40) {
            return FraudCheckResult.review(alerts, "Medium fraud risk - requires manual review");
        } else {
            return FraudCheckResult.allow(alerts);
        }
    }

    private boolean isRoundAmount(BigDecimal amount) {
        // Check if amount is a round number (1000, 5000, 10000, etc.)
        return amount.remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) == 0;
    }

    // Currency conversion with exchange rates
    public BigDecimal convertCurrency(
        BigDecimal amount,
        Currency fromCurrency,
        Currency toCurrency
    ) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        // Fetch live exchange rate (cached)
        BigDecimal exchangeRate = getExchangeRate(fromCurrency, toCurrency);

        // Apply conversion
        BigDecimal converted = amount
            .multiply(exchangeRate)
            .setScale(2, RoundingMode.HALF_UP);

        // Add markup (0.5% for currency conversion)
        BigDecimal markup = converted
            .multiply(new BigDecimal("0.005"))
            .setScale(2, RoundingMode.HALF_UP);

        return converted.add(markup);
    }
}
```

---

**Next:** [Context & Pattern Agents →](./10-context.md)
