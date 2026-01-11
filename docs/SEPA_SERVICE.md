# SEPA Service - Complete Reference

> **Service:** SEPA (Single Euro Payments Area) Service
> **Port:** 8092
> **Database:** sepa_db (PostgreSQL)
> **Responsibility:** SEPA Credit Transfers (SCT/SCT Inst), SEPA Direct Debits (SDD Core/B2B), ISO 20022 XML generation, BIC/IBAN validation, batch processing, and R-transactions
> **Last Updated:** 01 January 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Domain Model](#domain-model)
3. [API Reference](#api-reference)
4. [SEPA Credit Transfers (SCT)](#sepa-credit-transfers-sct)
5. [SEPA Instant Transfers (SCT Inst)](#sepa-instant-transfers-sct-inst)
6. [SEPA Direct Debits (SDD)](#sepa-direct-debits-sdd)
7. [Batch Processing](#batch-processing)
8. [R-Transactions (Returns)](#r-transactions-returns)
9. [ISO 20022 XML Generation](#iso-20022-xml-generation)
10. [IBAN & BIC Validation](#iban--bic-validation)
11. [SAGA Orchestration](#saga-orchestration)
12. [Testing](#testing)

---

## Overview

The SEPA Service enables the Banking Microservices Platform to process international Euro payments within the Single Euro Payments Area (34 countries). It supports all SEPA payment schemes: Credit Transfers, Instant Credit Transfers, Direct Debits (Core and B2B), with full ISO 20022 XML message generation and EPC compliance.

### Key Features

- ✅ **SEPA Credit Transfer (SCT)** - Standard Euro transfers (D+1 settlement)
- ✅ **SEPA Instant Credit Transfer (SCT Inst)** - Real-time Euro transfers (<10 seconds)
- ✅ **SEPA Direct Debit Core (SDD Core)** - Consumer direct debits with refund rights
- ✅ **SEPA Direct Debit B2B (SDD B2B)** - Business-to-business direct debits
- ✅ **Batch Processing** - Bulk payment processing (pain.001/pacs.008 XML)
- ✅ **R-Transactions** - Returns, rejections, refunds, recalls, reversals
- ✅ **Mandate Management** - SDD mandate lifecycle (signature, activation, cancellation)
- ✅ **ISO 20022 XML** - pain.001.001.03, pacs.008.001.02 generation
- ✅ **IBAN Validation** - MOD-97 checksum validation for 34 SEPA countries
- ✅ **BIC Validation** - SWIFT BIC code validation
- ✅ **EPC Compliance** - Amount limits, character sets, time windows
- ✅ **SAGA Pattern** - 4-step orchestration with compensation
- ✅ **Event-Driven** - Kafka integration (events published for completed transfers)
- ✅ **JWT Security** - Complete authentication and authorization

### Technology Stack

```yaml
Framework: Spring Boot 3.2.0
Database: PostgreSQL 16 (sepa_db)
Cache: Redis 7.2
Messaging: Apache Kafka (event publishing)
XML: JAXB 2.3 (ISO 20022 generation)
Validation: Custom IBAN/BIC validators
Integration: OpenFeign (Account Service, Fraud Detection)
Authentication: JWT (HS512)
API Documentation: Swagger/OpenAPI 3.0
Testing: JUnit 5, Mockito, TestContainers
```

### SEPA Countries (34)

```
Austria, Belgium, Bulgaria, Croatia, Cyprus, Czech Republic, Denmark, Estonia,
Finland, France, Germany, Greece, Hungary, Iceland, Ireland, Italy, Latvia,
Liechtenstein, Lithuania, Luxembourg, Malta, Monaco, Netherlands, Norway, Poland,
Portugal, Romania, San Marino, Slovakia, Slovenia, Spain, Sweden, Switzerland,
United Kingdom
```

---

## Domain Model

### 1. SepaTransfer Entity (Original)

```java
@Entity
@Table(name = "sepa_transfers")
public class SepaTransfer implements Serializable {

    @Id
    @Column(name = "sepa_reference", length = 50)
    private String sepaReference;  // SEPA-XXXXXXXXXXXX

    @Column(name = "transfer_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SepaTransferType transferType;  // SCT, SCT_INST, SDD_CORE, SDD_B2B

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SepaTransferStatus status;  // PENDING, VALIDATED, SUBMITTED, COMPLETED, FAILED

    // Debtor (Payer)
    @Column(name = "debtor_name", nullable = false, length = 140)
    private String debtorName;

    @Column(name = "debtor_iban", nullable = false, length = 34)
    private String debtorIban;

    @Column(name = "debtor_bic", length = 11)
    private String debtorBic;

    @Column(name = "debtor_account_number", length = 50)
    private String debtorAccountNumber;  // Internal account reference

    // Creditor (Payee)
    @Column(name = "creditor_name", nullable = false, length = 140)
    private String creditorName;

    @Column(name = "creditor_iban", nullable = false, length = 34)
    private String creditorIban;

    @Column(name = "creditor_bic", length = 11)
    private String creditorBic;

    @Column(name = "creditor_account_number", length = 50)
    private String creditorAccountNumber;

    // Payment Details
    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;  // ALWAYS BigDecimal!

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "EUR";  // SEPA is Euro-only

    @Column(name = "end_to_end_id", length = 35)
    private String endToEndId;  // Customer-provided reference

    @Column(name = "remittance_information", length = 140)
    private String remittanceInformation;  // Payment description

    @Column(name = "purpose_code", length = 4)
    private String purposeCode;  // SEPA purpose codes (e.g., SALA, PENS)

    // Execution
    @Column(name = "requested_execution_date")
    private LocalDate requestedExecutionDate;

    @Column(name = "execution_timestamp")
    private LocalDateTime executionTimestamp;

    // SAGA Tracking
    @Column(name = "saga_step", length = 30)
    private String sagaStep;

    @Column(name = "debit_transaction_id", length = 50)
    private String debitTransactionId;

    @Column(name = "credit_transaction_id", length = 50)
    private String creditTransactionId;

    // Timestamps
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### 2. SepaMandate Entity (Enhanced - 39 Columns)

```java
@Entity
@Table(name = "sepa_mandates")
public class SepaMandate implements Serializable {

    @Id
    @Column(name = "mandate_id", length = 50)
    private String mandateId;  // Unique Mandate Reference (UMR)

    @Column(name = "mandate_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MandateType mandateType;  // SDD_CORE, SDD_B2B

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MandateStatus status = MandateStatus.PENDING;

    // Debtor Information (Customer who authorizes debit)
    @Column(name = "debtor_name", nullable = false, length = 140)
    private String debtorName;

    @Column(name = "debtor_iban", nullable = false, length = 34)
    private String debtorIban;

    @Column(name = "debtor_bic", length = 11)
    private String debtorBic;

    @Column(name = "debtor_account_number", length = 50)
    private String debtorAccountNumber;  // Internal reference

    @Column(name = "debtor_address", length = 200)
    private String debtorAddress;

    @Column(name = "debtor_country", length = 2)
    private String debtorCountry;  // ISO country code

    // Creditor Information (Merchant/Company collecting payment)
    @Column(name = "creditor_name", nullable = false, length = 140)
    private String creditorName;

    @Column(name = "creditor_id", length = 35)
    private String creditorId;  // Creditor Identifier (mandatory for SDD)

    @Column(name = "creditor_iban", nullable = false, length = 34)
    private String creditorIban;

    @Column(name = "creditor_bic", length = 11)
    private String creditorBic;

    // Mandate Details
    @Column(name = "sequence_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SequenceType sequenceType = SequenceType.OOFF;  // FRST, RCUR, FNAL, OOFF

    @Column(name = "amendment_indicator", nullable = false)
    private Boolean amendmentIndicator = false;

    @Column(name = "original_mandate_id", length = 50)
    private String originalMandateId;  // For amendments

    @Column(name = "original_creditor_id", length = 35)
    private String originalCreditorId;  // For amendments

    // Authorization
    @Column(name = "signature_date", nullable = false)
    private LocalDate signatureDate;

    @Column(name = "signed_by", length = 100)
    private String signedBy;

    @Column(name = "signature_method", length = 50)
    private String signatureMethod;  // PAPER, ELECTRONIC, VERBAL

    @Column(name = "signature_location", length = 100)
    private String signatureLocation;

    // Activation
    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "first_collection_date")
    private LocalDate firstCollectionDate;

    @Column(name = "last_collection_date")
    private LocalDate lastCollectionDate;

    // Limits and Frequency
    @Column(name = "max_amount", precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "frequency", length = 20)
    private String frequency;  // MONTHLY, WEEKLY, ANNUAL, etc.

    // Cancellation
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // Statistics
    @Column(name = "total_collections", nullable = false)
    private Long totalCollections = 0L;

    @Column(name = "successful_collections", nullable = false)
    private Long successfulCollections = 0L;

    @Column(name = "failed_collections", nullable = false)
    private Long failedCollections = 0L;

    @Column(name = "total_amount_collected", precision = 19, scale = 2)
    private BigDecimal totalAmountCollected = BigDecimal.ZERO;

    // Audit
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

**Mandate Types:**
```java
public enum MandateType {
    SDD_CORE,    // SEPA Direct Debit Core Scheme (B2C)
    SDD_B2B      // SEPA Direct Debit Business-to-Business
}
```

**Sequence Types:**
```java
public enum SequenceType {
    FRST,  // First collection (requires pre-notification)
    RCUR,  // Recurring collection
    FNAL,  // Final collection
    OOFF   // One-off collection
}
```

---

### 3. SepaBatch Entity (Enhanced - 35 Columns)

```java
@Entity
@Table(name = "sepa_batches")
public class SepaBatch implements Serializable {

    @Id
    @Column(name = "batch_id", length = 50)
    private String batchId;  // BATCH-{timestamp}-{random}

    @Column(name = "batch_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BatchType batchType;  // SCT, SCT_INST, SDD_CORE, SDD_B2B

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BatchStatus status = BatchStatus.PENDING;

    // Batch Details
    @Column(name = "message_id", unique = true, length = 35)
    private String messageId;  // ISO 20022 Message ID

    @Column(name = "creation_date_time", nullable = false)
    private LocalDateTime creationDateTime;

    @Column(name = "requested_execution_date")
    private LocalDate requestedExecutionDate;

    // Initiating Party (who creates the batch)
    @Column(name = "initiating_party_name", length = 140)
    private String initiatingPartyName;

    @Column(name = "initiating_party_id", length = 35)
    private String initiatingPartyId;

    // Batch Statistics
    @Column(name = "number_of_transactions", nullable = false)
    private Integer numberOfTransactions = 0;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "EUR";

    // Processing Statistics
    @Column(name = "successful_transactions")
    private Integer successfulTransactions = 0;

    @Column(name = "failed_transactions")
    private Integer failedTransactions = 0;

    @Column(name = "pending_transactions")
    private Integer pendingTransactions = 0;

    @Column(name = "successful_amount", precision = 19, scale = 2)
    private BigDecimal successfulAmount = BigDecimal.ZERO;

    // Transfer References
    @ElementCollection
    @CollectionTable(name = "sepa_batch_transfers", joinColumns = @JoinColumn(name = "batch_id"))
    @Column(name = "transfer_reference")
    private List<String> transferReferences = new ArrayList<>();

    // ISO 20022 XML
    @Column(name = "pain_xml", columnDefinition = "TEXT")
    private String painXml;  // pain.001 XML for credit transfers

    @Column(name = "pacs_xml", columnDefinition = "TEXT")
    private String pacsXml;  // pacs.008 XML for interbank transfers

    // Submission
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "submission_reference", length = 50)
    private String submissionReference;  // External reference from SEPA network

    // Processing
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    // Errors
    @Column(name = "has_errors", nullable = false)
    private Boolean hasErrors = false;

    @Column(name = "error_count")
    private Integer errorCount = 0;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    // File Information
    @Column(name = "source_file_name", length = 200)
    private String sourceFileName;

    @Column(name = "source_file_hash", length = 64)
    private String sourceFileHash;

    // Audit
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

---

### 4. SepaReturn Entity (Enhanced - 24 Columns)

```java
@Entity
@Table(name = "sepa_returns")
public class SepaReturn implements Serializable {

    @Id
    @Column(name = "return_id", length = 50)
    private String returnId;  // RET-{timestamp}-{random}

    @Column(name = "return_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReturnType returnType;  // RETURN, REJECTION, REFUND, RECALL, REVERSAL

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReturnStatus status = ReturnStatus.INITIATED;

    // Original Transaction Reference
    @Column(name = "original_sepa_reference", nullable = false, length = 50)
    private String originalSepaReference;

    @Column(name = "original_end_to_end_id", length = 35)
    private String originalEndToEndId;

    @Column(name = "original_transaction_id", length = 35)
    private String originalTransactionId;

    // Return Details
    @Column(name = "return_reason_code", nullable = false, length = 10)
    private String returnReasonCode;  // SEPA reason codes (e.g., AC01, AC04, MD07)

    @Column(name = "return_reason_description", columnDefinition = "TEXT")
    private String returnReasonDescription;

    @Column(name = "return_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal returnAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "EUR";

    // Parties Involved
    @Column(name = "debtor_name", length = 140)
    private String debtorName;

    @Column(name = "debtor_iban", length = 34)
    private String debtorIban;

    @Column(name = "creditor_name", length = 140)
    private String creditorName;

    @Column(name = "creditor_iban", length = 34)
    private String creditorIban;

    // Return Initiator
    @Column(name = "initiated_by", nullable = false, length = 100)
    private String initiatedBy;  // DEBTOR_BANK, CREDITOR_BANK, DEBTOR, CREDITOR

    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    // Processing
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Refund Details
    @Column(name = "refund_sepa_reference", length = 50)
    private String refundSepaReference;

    @Column(name = "refund_transaction_id", length = 35)
    private String refundTransactionId;

    @Column(name = "refund_completed_at")
    private LocalDateTime refundCompletedAt;

    // Additional Information
    @Column(name = "additional_information", columnDefinition = "TEXT")
    private String additionalInformation;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    // Audit
    @CreatedDate
    private LocalDateTime createdAt;
}
```

**SEPA Return Reason Codes:**
```java
// Account Related
AC01 - IncorrectAccountNumber
AC04 - ClosedAccountNumber
AC06 - BlockedAccount

// Amount Related
AM04 - InsufficientFunds
AM05 - Duplication

// Mandate Related
MD01 - No Mandate
MD02 - MissingMandatoryInformation
MD06 - RefusedByCustomer
MD07 - EndCustomerDeceased

// Technical
MS02 - NotSpecifiedReasonCustomerGenerated
MS03 - NotSpecifiedReasonAgentGenerated

// Regulatory
RR01 - MissingDebtorAccountOrIdentification
RR04 - RegulatoryReason
```

---

## API Reference

### Base URL
```
http://localhost:8092
```

### SEPA Transfer Endpoints (9 endpoints)

#### 1. Initiate SEPA Transfer
```http
POST /sepa/transfers
Authorization: Bearer <JWT_TOKEN>

{
  "transferType": "SCT",
  "debtorName": "John Doe",
  "debtorIban": "NL91ABNA0417164300",
  "debtorAccountNumber": "ACC-123",
  "creditorName": "Jane Smith",
  "creditorIban": "DE89370400440532013000",
  "creditorBic": "COBADEFFXXX",
  "amount": 500.00,
  "remittanceInformation": "Invoice payment INV-2025-001",
  "requestedExecutionDate": "2026-01-05"
}
```

**Response (201 Created):**
```json
{
  "sepaReference": "SEPA-A1B2C3D4E5F6",
  "transferType": "SCT",
  "status": "PENDING",
  "amount": 500.00,
  "currency": "EUR",
  "debtorIban": "NL91ABNA0417164300",
  "creditorIban": "DE89370400440532013000",
  "requestedExecutionDate": "2026-01-05",
  "endToEndId": "E2E-12345",
  "createdAt": "2026-01-01T10:30:00"
}
```

#### 2. Initiate Instant Transfer
```http
POST /sepa/transfers/instant
Authorization: Bearer <JWT_TOKEN>

{
  "debtorIban": "NL91ABNA0417164300",
  "creditorIban": "BE68539007547034",
  "amount": 1000.00,
  "remittanceInformation": "Urgent payment"
}
```

**SCT Inst Requirements:**
- Amount: €0.01 - €100,000
- Execution: <10 seconds
- Availability: 24/7/365
- Irrevocable: Cannot be cancelled once submitted

#### 3. Get Transfer by Reference
```http
GET /sepa/transfers/{sepaReference}
Authorization: Bearer <JWT_TOKEN>
```

#### 4. Get Transfers by Account
```http
GET /sepa/transfers/account/{accountNumber}
Authorization: Bearer <JWT_TOKEN>
```

#### 5. Get Transfer Status
```http
GET /sepa/transfers/{sepaReference}/status
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "sepaReference": "SEPA-A1B2C3D4E5F6",
  "status": "COMPLETED",
  "sagaStep": "CONFIRM_TRANSFER",
  "executionTimestamp": "2026-01-05T09:15:30",
  "painXml": "<?xml version=\"1.0\"?>..."
}
```

#### 6. Cancel Transfer
```http
POST /sepa/transfers/{sepaReference}/cancel
Authorization: Bearer <JWT_TOKEN>
```

**Note:** Only PENDING/VALIDATED transfers can be cancelled

#### 7. Get Pending Transfers
```http
GET /sepa/transfers/pending
Authorization: Bearer <JWT_TOKEN>
```

#### 8. Get Failed Transfers
```http
GET /sepa/transfers/failed
Authorization: Bearer <JWT_TOKEN>
```

#### 9. Get Transfer Statistics
```http
GET /sepa/transfers/statistics
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "totalTransfers": 1250,
  "sctCount": 980,
  "sctInstCount": 270,
  "sddCount": 0,
  "totalVolume": 125000.00,
  "successRate": 98.5
}
```

---

### Mandate Endpoints (10 endpoints)

#### 1. Create Mandate
```http
POST /sepa/mandates
Authorization: Bearer <JWT_TOKEN>

{
  "mandateType": "SDD_CORE",
  "debtorName": "John Doe",
  "debtorIban": "NL91ABNA0417164300",
  "creditorName": "Utility Company Inc",
  "creditorId": "NL98ZZZ999999990000",
  "creditorIban": "NL12BANK0123456789",
  "sequenceType": "RCUR",
  "signatureDate": "2026-01-01",
  "signatureMethod": "ELECTRONIC",
  "maxAmount": 200.00,
  "frequency": "MONTHLY"
}
```

**Response:**
```json
{
  "mandateId": "UMR-1234567890",
  "mandateType": "SDD_CORE",
  "status": "PENDING",
  "debtorIban": "NL91ABNA0417164300",
  "creditorId": "NL98ZZZ999999990000",
  "signatureDate": "2026-01-01",
  "createdAt": "2026-01-01T10:00:00"
}
```

#### 2. Activate Mandate
```http
POST /sepa/mandates/{mandateId}/activate
Authorization: Bearer <JWT_TOKEN>
```

#### 3. Get Mandate
```http
GET /sepa/mandates/{mandateId}
Authorization: Bearer <JWT_TOKEN>
```

#### 4. Get Mandates by Debtor
```http
GET /sepa/mandates/debtor/{debtorIban}
Authorization: Bearer <JWT_TOKEN>
```

#### 5. Get Active Mandates
```http
GET /sepa/mandates/active
Authorization: Bearer <JWT_TOKEN>
```

#### 6. Suspend Mandate
```http
POST /sepa/mandates/{mandateId}/suspend
Authorization: Bearer <JWT_TOKEN>
```

#### 7. Resume Mandate
```http
POST /sepa/mandates/{mandateId}/resume
Authorization: Bearer <JWT_TOKEN>
```

#### 8. Cancel Mandate
```http
POST /sepa/mandates/{mandateId}/cancel
Authorization: Bearer <JWT_TOKEN>

{
  "cancellationReason": "Customer request",
  "cancelledBy": "customer"
}
```

#### 9. Update Mandate (Amendment)
```http
PUT /sepa/mandates/{mandateId}
Authorization: Bearer <JWT_TOKEN>

{
  "amendmentIndicator": true,
  "maxAmount": 300.00,
  "creditorIban": "NL99NEWB0123456789"
}
```

#### 10. Get Mandate Statistics
```http
GET /sepa/mandates/{mandateId}/statistics
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "mandateId": "UMR-1234567890",
  "totalCollections": 24,
  "successfulCollections": 22,
  "failedCollections": 2,
  "totalAmountCollected": 4400.00,
  "lastCollectionDate": "2026-01-01"
}
```

---

### Batch Processing Endpoints (10 endpoints)

#### 1. Create Batch
```http
POST /sepa/batches
Authorization: Bearer <JWT_TOKEN>

{
  "batchType": "SCT",
  "requestedExecutionDate": "2026-01-05",
  "initiatingPartyName": "Corporate Treasury"
}
```

#### 2. Add Transfer to Batch
```http
POST /sepa/batches/{batchId}/transfers
Authorization: Bearer <JWT_TOKEN>

{
  "debtorIban": "NL91ABNA0417164300",
  "creditorIban": "DE89370400440532013000",
  "amount": 150.00,
  "remittanceInformation": "Salary payment"
}
```

#### 3. Submit Batch
```http
POST /sepa/batches/{batchId}/submit
Authorization: Bearer <JWT_TOKEN>
```

**Batch Validation:**
- All IBANs valid
- All amounts > €0.01
- Total amount <= batch limit
- Correct currency (EUR only)
- No duplicate end-to-end IDs

#### 4. Get Batch
```http
GET /sepa/batches/{batchId}
Authorization: Bearer <JWT_TOKEN>
```

#### 5. Get Batch Transfers
```http
GET /sepa/batches/{batchId}/transfers
Authorization: Bearer <JWT_TOKEN>
```

#### 6. Get Batch Status
```http
GET /sepa/batches/{batchId}/status
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "batchId": "BATCH-1735689600000-123",
  "status": "PROCESSING",
  "numberOfTransactions": 100,
  "successfulTransactions": 87,
  "failedTransactions": 3,
  "pendingTransactions": 10,
  "totalAmount": 15000.00,
  "successfulAmount": 13050.00
}
```

#### 7. Download Batch XML
```http
GET /sepa/batches/{batchId}/download/pain
Authorization: Bearer <JWT_TOKEN>
```

**Response:** pain.001.001.03 XML file

#### 8. Cancel Batch
```http
POST /sepa/batches/{batchId}/cancel
Authorization: Bearer <JWT_TOKEN>
```

#### 9. Get Pending Batches
```http
GET /sepa/batches/pending
Authorization: Bearer <JWT_TOKEN>
```

#### 10. Get Batch Statistics
```http
GET /sepa/batches/statistics
Authorization: Bearer <JWT_TOKEN>
```

---

### Return/R-Transaction Endpoints (8 endpoints)

#### 1. Initiate Return
```http
POST /sepa/returns
Authorization: Bearer <JWT_TOKEN>

{
  "returnType": "RETURN",
  "originalSepaReference": "SEPA-A1B2C3D4E5F6",
  "returnReasonCode": "AM04",
  "returnAmount": 500.00,
  "initiatedBy": "DEBTOR_BANK"
}
```

**Response:**
```json
{
  "returnId": "RET-1735689600000-456",
  "returnType": "RETURN",
  "status": "INITIATED",
  "returnReasonCode": "AM04",
  "returnReasonDescription": "InsufficientFunds",
  "returnAmount": 500.00,
  "initiatedAt": "2026-01-01T11:00:00"
}
```

#### 2. Get Return
```http
GET /sepa/returns/{returnId}
Authorization: Bearer <JWT_TOKEN>
```

#### 3. Get Returns by Original Transfer
```http
GET /sepa/returns/transfer/{sepaReference}
Authorization: Bearer <JWT_TOKEN>
```

#### 4. Get Returns by Reason Code
```http
GET /sepa/returns/reason/{reasonCode}
Authorization: Bearer <JWT_TOKEN>
```

#### 5. Process Return
```http
POST /sepa/returns/{returnId}/process
Authorization: Bearer <JWT_TOKEN>
```

#### 6. Complete Refund
```http
POST /sepa/returns/{returnId}/refund
Authorization: Bearer <JWT_TOKEN>
```

#### 7. Get Pending Returns
```http
GET /sepa/returns/pending
Authorization: Bearer <JWT_TOKEN>
```

#### 8. Get Return Statistics
```http
GET /sepa/returns/statistics
Authorization: Bearer <JWT_TOKEN>
```

---

## SEPA Credit Transfers (SCT)

### SCT Characteristics

```yaml
Settlement: D+1 (next business day)
Amount Limit: No limit (but practical limit ~€999,999,999.99)
Execution Window: Business days only (Mon-Fri, bank hours)
Character Set: SEPA character set (a-z, A-Z, 0-9, and limited special chars)
Revocation: Possible until cut-off time (usually 17:00 CET)
```

### SCT Business Flow

```
1. Customer initiates transfer via API
2. SEPA Service validates IBAN/BIC
3. SEPA Service generates pain.001 XML
4. SAGA Step 1: Validate transfer (EPC compliance)
5. SAGA Step 2: Debit sender account (Account Service via Feign)
6. SAGA Step 3: Submit to SEPA network (generate pacs.008 XML)
7. SAGA Step 4: Confirm transfer (publish event)
8. Interbank settlement (D+1)
9. Credit receiver account (via their bank)
```

### EPC Compliance Rules

```java
public class EpcComplianceValidator {

    public void validateSct(SepaTransfer transfer) {
        // 1. Currency must be EUR
        if (!"EUR".equals(transfer.getCurrency())) {
            throw new SepaValidationException("SCT requires EUR currency");
        }

        // 2. IBAN must be from SEPA country
        if (!isSepaIban(transfer.getCreditorIban())) {
            throw new SepaValidationException("Creditor IBAN not from SEPA country");
        }

        // 3. Character set validation
        if (!hasValidCharacters(transfer.getRemittanceInformation())) {
            throw new SepaValidationException("Invalid characters in remittance info");
        }

        // 4. Field length validation
        if (transfer.getDebtorName().length() > 140) {
            throw new SepaValidationException("Debtor name exceeds 140 characters");
        }

        // 5. Amount precision (2 decimal places)
        if (transfer.getAmount().scale() > 2) {
            throw new SepaValidationException("Amount cannot have more than 2 decimal places");
        }
    }
}
```

---

## SEPA Instant Transfers (SCT Inst)

### SCT Inst Characteristics

```yaml
Settlement: <10 seconds
Amount Limit: €0.01 - €100,000
Availability: 24/7/365
Irrevocability: Cannot be cancelled once submitted
Reachability: Requires both banks to support SCT Inst
```

### SCT Inst Implementation

```java
@Service
public class InstantTransferService {

    public SepaTransferResponse executeInstantTransfer(InstantTransferRequest request) {
        // 1. Validate amount limit
        if (request.getAmount().compareTo(new BigDecimal("100000")) > 0) {
            throw new InstantTransferException("Amount exceeds €100,000 limit");
        }

        // 2. Check creditor bank supports SCT Inst
        if (!isInstantReachable(request.getCreditorBic())) {
            throw new InstantTransferException("Creditor bank does not support SCT Inst");
        }

        // 3. Validate available balance (must be immediate)
        BigDecimal balance = accountService.getAvailableBalance(request.getDebtorAccountNumber());
        if (balance.compareTo(request.getAmount()) < 0) {
            throw new InstantTransferException("Insufficient funds");
        }

        // 4. Execute with timeout (10 seconds max)
        SepaTransfer transfer = executeWithTimeout(request, Duration.ofSeconds(10));

        return mapToResponse(transfer);
    }

    private boolean isInstantReachable(String creditorBic) {
        // Check against SCT Inst participant directory
        return sctInstDirectory.isParticipant(creditorBic);
    }
}
```

---

## SEPA Direct Debits (SDD)

### SDD Core Scheme

**Characteristics:**
```yaml
Target: Business-to-Consumer (B2C)
Refund Rights: 8 weeks unconditional, 13 months for unauthorized
Pre-Notification: Minimum 14 calendar days
Mandate Required: Yes (UMR)
First Collection: FRST sequence type
Recurring: RCUR sequence type
```

**SDD Core Flow:**
```
1. Debtor signs mandate (UMR created)
2. Creditor sends pre-notification (14 days before)
3. Creditor initiates collection (pain.008 XML)
4. Debtor's bank validates mandate
5. Debtor's bank debits account (D-1)
6. Settlement (D)
7. Creditor receives funds (D+1)
8. Debtor can request refund (up to 8 weeks)
```

### SDD B2B Scheme

**Characteristics:**
```yaml
Target: Business-to-Business (B2B)
Refund Rights: No refund for authorized debits
Pre-Notification: Agreed between parties
Mandate Required: Yes (must be lodged with debtor bank)
Validation: Debtor bank must validate mandate before each collection
```

**SDD B2B Advantages:**
- No refund risk (authorized debits are final)
- Faster settlement
- Lower dispute rates

---

## Batch Processing

### Batch Creation Flow

```java
@Service
public class BatchProcessingService {

    public SepaBatch createAndSubmitBatch(List<TransferRequest> transfers) {
        // 1. Create batch
        SepaBatch batch = SepaBatch.builder()
            .batchType(BatchType.SCT)
            .requestedExecutionDate(LocalDate.now().plusDays(1))
            .build();
        batchRepository.save(batch);

        // 2. Add transfers to batch
        for (TransferRequest request : transfers) {
            SepaTransfer transfer = createTransfer(request);
            transfer.setBatchId(batch.getBatchId());
            sepaTransferRepository.save(transfer);
            batch.getTransferReferences().add(transfer.getSepaReference());
        }

        // 3. Update batch statistics
        batch.setNumberOfTransactions(transfers.size());
        batch.setTotalAmount(calculateTotalAmount(transfers));

        // 4. Generate pain.001 XML
        String painXml = isoXmlGenerator.generatePain001(batch);
        batch.setPainXml(painXml);

        // 5. Validate batch
        validateBatch(batch);
        batch.setStatus(BatchStatus.VALIDATED);

        // 6. Submit batch
        submitToSepaNetwork(batch);
        batch.setStatus(BatchStatus.SUBMITTED);
        batch.setSubmittedAt(LocalDateTime.now());

        return batchRepository.save(batch);
    }

    private void validateBatch(SepaBatch batch) {
        // Validate all IBANs
        // Validate all amounts
        // Check for duplicates
        // Verify total amount
        // Validate XML against XSD schema
    }
}
```

### Batch XML Structure (pain.001.001.03)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03">
  <CstmrCdtTrfInitn>
    <GrpHdr>
      <MsgId>MSG-1735689600000</MsgId>
      <CreDtTm>2026-01-01T10:00:00</CreDtTm>
      <NbOfTxs>100</NbOfTxs>
      <CtrlSum>15000.00</CtrlSum>
      <InitgPty>
        <Nm>Corporate Treasury</Nm>
      </InitgPty>
    </GrpHdr>
    <PmtInf>
      <PmtInfId>BATCH-1735689600000-123</PmtInfId>
      <PmtMtd>TRF</PmtMtd>
      <NbOfTxs>100</NbOfTxs>
      <CtrlSum>15000.00</CtrlSum>
      <ReqdExctnDt>2026-01-05</ReqdExctnDt>
      <Dbtr>
        <Nm>Company Inc</Nm>
      </Dbtr>
      <DbtrAcct>
        <Id>
          <IBAN>NL91ABNA0417164300</IBAN>
        </Id>
      </DbtrAcct>
      <CdtTrfTxInf>
        <!-- Transfer 1 -->
        <Amt>
          <InstdAmt Ccy="EUR">150.00</InstdAmt>
        </Amt>
        <Cdtr>
          <Nm>Employee 1</Nm>
        </Cdtr>
        <CdtrAcct>
          <Id>
            <IBAN>DE89370400440532013000</IBAN>
          </Id>
        </CdtrAcct>
        <RmtInf>
          <Ustrd>Salary payment</Ustrd>
        </RmtInf>
      </CdtTrfTxInf>
      <!-- Transfer 2... -->
      <!-- Transfer 100 -->
    </PmtInf>
  </CstmrCdtTrfInitn>
</Document>
```

---

## R-Transactions (Returns)

### Return Types

**RETURN:**
- Payment returned after settlement
- Reason: AC01 (incorrect account), AM04 (insufficient funds)

**REJECTION:**
- Payment rejected before settlement
- Reason: Invalid IBAN, blocked account

**REFUND:**
- Customer-requested refund (SDD only)
- 8 weeks for SDD Core, no refund for SDD B2B

**RECALL:**
- Originator requests payment back
- Requires beneficiary approval

**REVERSAL:**
- System-generated reversal
- Technical errors

### Return Processing

```java
@Service
public class ReturnProcessingService {

    public SepaReturn processReturn(ReturnRequest request) {
        // 1. Validate original transfer exists
        SepaTransfer original = sepaTransferRepository
            .findBySepaReference(request.getOriginalSepaReference())
            .orElseThrow(() -> new NotFoundException("Original transfer not found"));

        // 2. Create return entity
        SepaReturn sepaReturn = SepaReturn.builder()
            .returnType(request.getReturnType())
            .originalSepaReference(original.getSepaReference())
            .returnReasonCode(request.getReturnReasonCode())
            .returnAmount(request.getReturnAmount())
            .initiatedBy(request.getInitiatedBy())
            .build();
        sepaReturnRepository.save(sepaReturn);

        // 3. Reverse original transfer
        if (request.getReturnType() == ReturnType.RETURN ||
            request.getReturnType() == ReturnType.REFUND) {

            // Credit original debtor
            accountService.credit(
                original.getDebtorAccountNumber(),
                sepaReturn.getReturnAmount(),
                "SEPA Return: " + sepaReturn.getReturnId()
            );

            // Debit original creditor
            accountService.debit(
                original.getCreditorAccountNumber(),
                sepaReturn.getReturnAmount(),
                "SEPA Return: " + sepaReturn.getReturnId()
            );

            sepaReturn.setStatus(ReturnStatus.COMPLETED);
            sepaReturn.setCompletedAt(LocalDateTime.now());
        }

        // 4. Publish return event
        kafkaTemplate.send("sepa.return.completed", new SepaReturnEvent(sepaReturn));

        return sepaReturnRepository.save(sepaReturn);
    }
}
```

---

## ISO 20022 XML Generation

### pain.001.001.03 (Customer Credit Transfer)

```java
@Service
public class IsoXmlGeneratorService {

    public String generatePain001(SepaBatch batch) {
        Document document = new Document();
        CstmrCdtTrfInitn initiation = new CstmrCdtTrfInitn();

        // Group Header
        GroupHeader grpHdr = new GroupHeader();
        grpHdr.setMsgId(batch.getMessageId());
        grpHdr.setCreDtTm(batch.getCreationDateTime());
        grpHdr.setNbOfTxs(String.valueOf(batch.getNumberOfTransactions()));
        grpHdr.setCtrlSum(batch.getTotalAmount());

        PartyIdentification initiatingParty = new PartyIdentification();
        initiatingParty.setNm(batch.getInitiatingPartyName());
        grpHdr.setInitgPty(initiatingParty);

        initiation.setGrpHdr(grpHdr);

        // Payment Information
        PaymentInstructionInformation pmtInf = new PaymentInstructionInformation();
        pmtInf.setPmtInfId(batch.getBatchId());
        pmtInf.setPmtMtd(PaymentMethod.TRF);  // Transfer
        pmtInf.setReqdExctnDt(batch.getRequestedExecutionDate());

        // Add each transfer as CdtTrfTxInf
        List<SepaTransfer> transfers = getTransfersForBatch(batch.getBatchId());
        for (SepaTransfer transfer : transfers) {
            CreditTransferTransactionInformation txInfo = createTxInfo(transfer);
            pmtInf.getCdtTrfTxInf().add(txInfo);
        }

        initiation.getPmtInf().add(pmtInf);
        document.setCstmrCdtTrfInitn(initiation);

        // Marshal to XML
        return marshalToXml(document);
    }

    private CreditTransferTransactionInformation createTxInfo(SepaTransfer transfer) {
        CreditTransferTransactionInformation txInfo = new CreditTransferTransactionInformation();

        // Amount
        AmountType amt = new AmountType();
        amt.setInstdAmt(new ActiveOrHistoricCurrencyAndAmount());
        amt.getInstdAmt().setCcy(transfer.getCurrency());
        amt.getInstdAmt().setValue(transfer.getAmount());
        txInfo.setAmt(amt);

        // Creditor
        PartyIdentification creditor = new PartyIdentification();
        creditor.setNm(transfer.getCreditorName());
        txInfo.setCdtr(creditor);

        // Creditor Account
        CashAccount cdtrAcct = new CashAccount();
        AccountIdentification cdtrAcctId = new AccountIdentification();
        cdtrAcctId.setIBAN(transfer.getCreditorIban());
        cdtrAcct.setId(cdtrAcctId);
        txInfo.setCdtrAcct(cdtrAcct);

        // Remittance Information
        RemittanceInformation rmtInf = new RemittanceInformation();
        rmtInf.setUstrd(transfer.getRemittanceInformation());
        txInfo.setRmtInf(rmtInf);

        return txInfo;
    }
}
```

---

## IBAN & BIC Validation

### IBAN Validation (MOD-97)

```java
@Service
public class IbanValidationService {

    public boolean validateIban(String iban) {
        // 1. Remove spaces and convert to uppercase
        iban = iban.replaceAll("\\s", "").toUpperCase();

        // 2. Check length (15-34 characters)
        if (iban.length() < 15 || iban.length() > 34) {
            return false;
        }

        // 3. Check country code
        String countryCode = iban.substring(0, 2);
        if (!isSepaCountry(countryCode)) {
            return false;
        }

        // 4. Validate country-specific length
        Integer expectedLength = IBAN_LENGTHS.get(countryCode);
        if (expectedLength != null && iban.length() != expectedLength) {
            return false;
        }

        // 5. MOD-97 checksum validation
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        String numericIban = convertToNumeric(rearranged);
        BigInteger ibanNumber = new BigInteger(numericIban);
        BigInteger remainder = ibanNumber.mod(BigInteger.valueOf(97));

        return remainder.intValue() == 1;
    }

    private String convertToNumeric(String iban) {
        StringBuilder numeric = new StringBuilder();
        for (char c : iban.toCharArray()) {
            if (Character.isDigit(c)) {
                numeric.append(c);
            } else if (Character.isLetter(c)) {
                // A=10, B=11, ..., Z=35
                numeric.append(c - 'A' + 10);
            }
        }
        return numeric.toString();
    }

    private static final Map<String, Integer> IBAN_LENGTHS = Map.ofEntries(
        entry("NL", 18),  // Netherlands
        entry("DE", 22),  // Germany
        entry("FR", 27),  // France
        entry("ES", 24),  // Spain
        entry("IT", 27),  // Italy
        entry("BE", 16),  // Belgium
        entry("GB", 22)   // United Kingdom
        // ... other SEPA countries
    );
}
```

### BIC Validation

```java
@Service
public class BicValidationService {

    public boolean validateBic(String bic) {
        // BIC format: 4-char bank code + 2-char country code + 2-char location + (optional 3-char branch)
        // Example: ABNANL2A or ABNANL2AXXX

        if (bic == null || bic.isEmpty()) {
            return true;  // BIC is optional for SEPA countries
        }

        bic = bic.toUpperCase();

        // Check length (8 or 11)
        if (bic.length() != 8 && bic.length() != 11) {
            return false;
        }

        // Validate format: [A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?
        if (!bic.matches("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$")) {
            return false;
        }

        // Validate country code
        String countryCode = bic.substring(4, 6);
        return isSepaCountry(countryCode);
    }
}
```

---

## SAGA Orchestration

### 4-Step SAGA Flow

```java
@Service
public class SepaTransferOrchestrator {

    public SepaTransfer executeTransfer(SepaTransferRequest request) {
        SepaTransfer transfer = createTransferEntity(request);

        try {
            // Step 1: Validate SEPA Transfer
            validateSepaTransferStep.execute(transfer);
            transfer.setSagaStep("VALIDATE_SEPA_TRANSFER");
            transfer.setStatus(SepaTransferStatus.VALIDATED);
            sepaTransferRepository.save(transfer);

            // Step 2: Debit Account
            debitAccountStep.execute(transfer);
            transfer.setSagaStep("DEBIT_ACCOUNT");
            sepaTransferRepository.save(transfer);

            // Step 3: Submit to SEPA Network
            submitToSepaNetworkStep.execute(transfer);
            transfer.setSagaStep("SUBMIT_TO_SEPA_NETWORK");
            transfer.setStatus(SepaTransferStatus.SUBMITTED);
            sepaTransferRepository.save(transfer);

            // Step 4: Confirm Transfer
            confirmTransferStep.execute(transfer);
            transfer.setSagaStep("CONFIRM_TRANSFER");
            transfer.setStatus(SepaTransferStatus.COMPLETED);
            transfer.setExecutionTimestamp(LocalDateTime.now());
            sepaTransferRepository.save(transfer);

            // Publish event
            publishSepaTransferCompletedEvent(transfer);

            return transfer;

        } catch (Exception e) {
            // Compensation: Reverse completed steps
            compensate(transfer);
            transfer.setStatus(SepaTransferStatus.FAILED);
            sepaTransferRepository.save(transfer);
            throw e;
        }
    }

    private void compensate(SepaTransfer transfer) {
        String currentStep = transfer.getSagaStep();

        if ("CONFIRM_TRANSFER".equals(currentStep) ||
            "SUBMIT_TO_SEPA_NETWORK".equals(currentStep) ||
            "DEBIT_ACCOUNT".equals(currentStep)) {
            // Reverse debit
            debitAccountStep.compensate(transfer);
        }
    }
}
```

---

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class SepaTransferServiceTest {

    @Mock
    private IbanValidationService ibanValidationService;

    @Mock
    private SepaTransferRepository sepaTransferRepository;

    @InjectMocks
    private SepaTransferService sepaTransferService;

    @Test
    void initiateTransfer_ValidIbans_Success() {
        // Given
        SepaTransferRequest request = createValidRequest();
        when(ibanValidationService.validateIban(any())).thenReturn(true);

        // When
        SepaTransfer result = sepaTransferService.initiateTransfer(request);

        // Then
        assertNotNull(result.getSepaReference());
        assertEquals("EUR", result.getCurrency());
        assertEquals(SepaTransferStatus.PENDING, result.getStatus());
    }

    @Test
    void validateIban_ValidDutchIban_ReturnsTrue() {
        // Given
        String iban = "NL91ABNA0417164300";

        // When
        boolean result = ibanValidationService.validateIban(iban);

        // Then
        assertTrue(result);
    }

    @Test
    void validateIban_InvalidChecksum_ReturnsFalse() {
        // Given
        String iban = "NL91ABNA0417164399";  // Invalid checksum

        // When
        boolean result = ibanValidationService.validateIban(iban);

        // Then
        assertFalse(result);
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class SepaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("sepa_db_test");

    @Autowired
    private SepaTransferOrchestrator orchestrator;

    @Test
    void fullSepaTransferFlow_Success() {
        // 1. Create transfer request
        SepaTransferRequest request = createSctRequest();

        // 2. Execute SAGA
        SepaTransfer result = orchestrator.executeTransfer(request);

        // 3. Verify status
        assertEquals(SepaTransferStatus.COMPLETED, result.getStatus());
        assertEquals("CONFIRM_TRANSFER", result.getSagaStep());

        // 4. Verify XML generated
        assertNotNull(result.getPainXml());
        assertTrue(result.getPainXml().contains("pain.001.001.03"));

        // 5. Verify event published
        // Check Kafka topic
    }
}
```

---

**Last Updated:** 01 January 2026
**API Version:** 1.0
**Service Status:** ✅ Production Ready (12-step enhancement complete)
**Total Endpoints:** 37 REST endpoints across 4 controllers
**Supported SEPA Schemes:** SCT, SCT Inst, SDD Core, SDD B2B
**ISO 20022 Messages:** pain.001.001.03, pacs.008.001.02
**Database Entities:** 4 (SepaTransfer, SepaMandate, SepaBatch, SepaReturn)
