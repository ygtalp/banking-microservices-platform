# Customer Service - Complete Reference

## Overview

The Customer Service is a core microservice in the Banking Platform responsible for customer lifecycle management, KYC (Know Your Customer) workflow, and document verification. It implements a comprehensive state machine for customer onboarding and compliance.

**Port:** 8083
**Database:** banking_customers (PostgreSQL)
**Pattern:** Domain-Driven Design, Event-Driven Architecture
**Dependencies:** Account Service (Feign), Kafka (Events), Redis (Cache), Eureka (Discovery)

---

## Architecture

### Domain Model

```
Customer
├── customerId (CUS-XXXXXXXXXXXX)
├── Personal Information
│   ├── firstName, lastName
│   ├── email (unique)
│   ├── phoneNumber
│   └── dateOfBirth
├── KYC Information
│   ├── nationalId (unique, masked in responses)
│   ├── address, city, country, postalCode
│   └── documents (1-to-many)
├── Status Management
│   ├── status (PENDING_VERIFICATION → VERIFIED → APPROVED → SUSPENDED/CLOSED)
│   ├── statusReason
│   ├── verifiedAt, verifiedBy
│   └── approvedAt, approvedBy
├── Risk Assessment
│   └── riskLevel (LOW, MEDIUM, HIGH)
└── Audit
    ├── createdAt, updatedAt
    ├── version (optimistic locking)
    └── history (audit trail)
```

### Status Flow

```
PENDING_VERIFICATION
        ↓ (verify)
    VERIFIED
        ↓ (approve)
    APPROVED ←→ SUSPENDED (suspend/reactivate)
        ↓ (close)
    CLOSED
```

**Business Rules:**
- Customer must be VERIFIED before APPROVED
- Only APPROVED customers can create accounts
- SUSPENDED customers can be reactivated
- CLOSED status is final

---

## API Reference

Base URL: `http://localhost:8080/api/v1/customers`

### 1. Register Customer

Create a new customer record.

**Endpoint:** `POST /api/v1/customers`

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+31612345678",
  "dateOfBirth": "1990-01-15",
  "nationalId": "12345678901",
  "address": "Test Street 123",
  "city": "Amsterdam",
  "country": "Netherlands",
  "postalCode": "1015 CJ"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Customer registered successfully",
  "data": {
    "id": 1,
    "customerId": "CUS-A1B2C3D4E5F6",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+31612345678",
    "dateOfBirth": "1990-01-15",
    "nationalId": "123****8901",
    "address": "Test Street 123",
    "city": "Amsterdam",
    "country": "Netherlands",
    "postalCode": "1015 CJ",
    "status": "PENDING_VERIFICATION",
    "createdAt": "2025-12-23T10:00:00",
    "updatedAt": "2025-12-23T10:00:00"
  }
}
```

**Events Published:**
- `CustomerCreatedEvent` → Kafka topic: `customer.events`

**Validation:**
- firstName, lastName: 2-100 characters
- email: valid email format, unique
- phoneNumber: valid international format
- dateOfBirth: must be in the past
- nationalId: 11-20 characters, unique

### 2. Get Customer

Retrieve customer by ID.

**Endpoint:** `GET /api/v1/customers/{customerId}`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Customer retrieved successfully",
  "data": {
    "customerId": "CUS-A1B2C3D4E5F6",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "nationalId": "123****8901",
    "status": "PENDING_VERIFICATION",
    ...
  }
}
```

**Errors:**
- `404 Not Found` - Customer not found

### 3. Get Customer by Email

**Endpoint:** `GET /api/v1/customers/email/{email}`

**Response:** Same as Get Customer

### 4. Get Customer by National ID

**Endpoint:** `GET /api/v1/customers/national-id/{nationalId}`

**Response:** Same as Get Customer

### 5. Update Customer

Update customer information (phone, address, postal code).

**Endpoint:** `PUT /api/v1/customers/{customerId}`

**Request Body:**
```json
{
  "phoneNumber": "+31687654321",
  "address": "New Avenue 456",
  "city": "Rotterdam",
  "postalCode": "3011 AD"
}
```

**Response:** `200 OK`

**Notes:**
- Cannot change: firstName, lastName, email, nationalId, dateOfBirth
- Updates recorded in customer history

### 6. Verify Customer

Mark customer as verified (after KYC document verification).

**Endpoint:** `POST /api/v1/customers/{customerId}/verify`

**Request Body:**
```json
{
  "verifiedBy": "admin@bank.com"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Customer verified successfully",
  "data": {
    "customerId": "CUS-A1B2C3D4E5F6",
    "status": "VERIFIED",
    "verifiedBy": "admin@bank.com",
    "verifiedAt": "2025-12-23T11:00:00",
    ...
  }
}
```

**Events Published:**
- `CustomerVerifiedEvent` → Kafka

**Errors:**
- `400 Bad Request` - Customer must be in PENDING_VERIFICATION status

### 7. Approve Customer

Approve customer (final KYC approval).

**Endpoint:** `POST /api/v1/customers/{customerId}/approve`

**Request Body:**
```json
{
  "approvedBy": "manager@bank.com",
  "riskLevel": "LOW"
}
```

**Risk Levels:** `LOW`, `MEDIUM`, `HIGH`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Customer approved successfully",
  "data": {
    "customerId": "CUS-A1B2C3D4E5F6",
    "status": "APPROVED",
    "approvedBy": "manager@bank.com",
    "approvedAt": "2025-12-23T12:00:00",
    "riskLevel": "LOW",
    ...
  }
}
```

**Events Published:**
- `CustomerApprovedEvent` → Kafka → **Account Service** can listen

**Errors:**
- `400 Bad Request` - Customer must be VERIFIED before approval

**Note:** Only APPROVED customers can create bank accounts.

### 8. Suspend Customer

Suspend customer account (compliance/fraud).

**Endpoint:** `POST /api/v1/customers/{customerId}/suspend`

**Request Body:**
```json
{
  "reason": "Suspicious activity detected",
  "suspendedBy": "compliance@bank.com"
}
```

**Response:** `200 OK`

**Events Published:**
- `CustomerStatusChangedEvent` → Kafka

### 9. Reactivate Customer

Reactivate suspended customer.

**Endpoint:** `POST /api/v1/customers/{customerId}/activate`

**Response:** `200 OK`

**Errors:**
- `400 Bad Request` - Customer must be SUSPENDED

### 10. Close Customer

Permanently close customer account.

**Endpoint:** `POST /api/v1/customers/{customerId}/close`

**Response:** `200 OK`

**Note:** CLOSED status is final, cannot be reversed.

### 11. Get Customer History

Retrieve audit trail for customer.

**Endpoint:** `GET /api/v1/customers/{customerId}/history`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Customer history retrieved successfully",
  "data": [
    {
      "action": "APPROVE",
      "previousStatus": "VERIFIED",
      "newStatus": "APPROVED",
      "description": "Customer approved",
      "performedBy": "manager@bank.com",
      "timestamp": "2025-12-23T12:00:00"
    },
    {
      "action": "VERIFY",
      "previousStatus": "PENDING_VERIFICATION",
      "newStatus": "VERIFIED",
      "description": "Customer verified",
      "performedBy": "admin@bank.com",
      "timestamp": "2025-12-23T11:00:00"
    },
    {
      "action": "REGISTER",
      "previousStatus": null,
      "newStatus": "PENDING_VERIFICATION",
      "description": "Customer registered",
      "performedBy": "system",
      "timestamp": "2025-12-23T10:00:00"
    }
  ]
}
```

### 12. Get Customer Accounts

Get all accounts for a customer (via Account Service).

**Endpoint:** `GET /api/v1/customers/{customerId}/accounts`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Customer accounts retrieved successfully",
  "data": [
    {
      "accountNumber": "1234567890",
      "customerId": "CUS-A1B2C3D4E5F6",
      "customerName": "John Doe",
      "balance": "1000.00",
      "currency": "EUR",
      "status": "ACTIVE",
      "accountType": "CHECKING"
    }
  ]
}
```

**Integration:** Uses Feign client to call Account Service

---

## KYC Document Management

Base URL: `http://localhost:8080/api/v1/documents`

### 1. Upload Document

**Endpoint:** `POST /api/v1/documents`

**Request Body:**
```json
{
  "customerId": "CUS-A1B2C3D4E5F6",
  "documentType": "PASSPORT",
  "documentNumber": "P12345678",
  "issuingAuthority": "Netherlands Government",
  "issueDate": "2020-01-01",
  "expiryDate": "2030-01-01",
  "documentUrl": "/documents/passport.pdf"
}
```

**Document Types:**
- `PASSPORT`
- `NATIONAL_ID`
- `DRIVERS_LICENSE`
- `PROOF_OF_ADDRESS`
- `BANK_STATEMENT`
- `TAX_DOCUMENT`

**Response:** `201 Created`

**Events Published:**
- `KycDocumentUploadedEvent` → Kafka

**Validation:**
- Document must not be expired
- Document number must be unique for customer

### 2. List Documents

**Endpoint:** `GET /api/v1/documents/customer/{customerId}`

**Response:** `200 OK` - Array of documents

### 3. Get Document

**Endpoint:** `GET /api/v1/documents/{documentId}`

**Response:** `200 OK`

### 4. Verify Document

**Endpoint:** `POST /api/v1/documents/{documentId}/verify`

**Request Body:**
```json
{
  "verifiedBy": "admin@bank.com"
}
```

**Response:** `200 OK`

**Events Published:**
- `KycDocumentVerifiedEvent` → Kafka

**Errors:**
- `400 Bad Request` - Document must be in PENDING status

### 5. Reject Document

**Endpoint:** `POST /api/v1/documents/{documentId}/reject`

**Request Body:**
```json
{
  "rejectedBy": "admin@bank.com",
  "rejectionReason": "Document quality is too low"
}
```

**Response:** `200 OK`

---

## Event Schema

### CustomerCreatedEvent

**Topic:** `customer.events`
**Key:** `customerId`

```json
{
  "eventType": "CUSTOMER_CREATED",
  "customerId": "CUS-A1B2C3D4E5F6",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "nationalId": "12345678901",
  "status": "PENDING_VERIFICATION",
  "timestamp": "2025-12-23T10:00:00"
}
```

**Consumers:** None (informational)

### CustomerVerifiedEvent

```json
{
  "eventType": "CUSTOMER_VERIFIED",
  "customerId": "CUS-A1B2C3D4E5F6",
  "email": "john.doe@example.com",
  "verifiedBy": "admin@bank.com",
  "verifiedAt": "2025-12-23T11:00:00",
  "timestamp": "2025-12-23T11:00:00"
}
```

**Consumers:** Notification Service (future)

### CustomerApprovedEvent

```json
{
  "eventType": "CUSTOMER_APPROVED",
  "customerId": "CUS-A1B2C3D4E5F6",
  "email": "john.doe@example.com",
  "approvedBy": "manager@bank.com",
  "riskLevel": "LOW",
  "approvedAt": "2025-12-23T12:00:00",
  "timestamp": "2025-12-23T12:00:00"
}
```

**Consumers:** Account Service (can listen to create accounts automatically)

### CustomerStatusChangedEvent

```json
{
  "eventType": "CUSTOMER_STATUS_CHANGED",
  "customerId": "CUS-A1B2C3D4E5F6",
  "previousStatus": "APPROVED",
  "newStatus": "SUSPENDED",
  "reason": "Suspicious activity",
  "timestamp": "2025-12-23T13:00:00"
}
```

**Consumers:** Account Service (to suspend related accounts)

---

## Integration with Account Service

### Flow

1. Customer registers → `PENDING_VERIFICATION`
2. Admin uploads and verifies KYC documents
3. Admin verifies customer → `VERIFIED`
4. Manager approves customer → `APPROVED`
5. **CustomerApprovedEvent** published to Kafka
6. **Account Service** can listen and allow account creation
7. Customer creates account using `customerId`

### Feign Client Usage

Customer Service → Account Service:

```java
@FeignClient(name = "account-service")
public interface AccountServiceClient {
    @GetMapping("/api/v1/accounts/customer/{customerId}")
    ApiResponse<List<AccountResponse>> getAccountsByCustomerId(
        @PathVariable("customerId") String customerId
    );
}
```

**Endpoint:** `GET /api/v1/customers/{customerId}/accounts`
Uses Feign client to fetch accounts from Account Service.

---

## Testing

### Unit Tests

```bash
mvn test -f customer-service/pom.xml
```

**Coverage:** 80%+ target

**Test Files:**
- `CustomerServiceImplTest.java` - Business logic tests
- `KycDocumentServiceTest.java` - Document workflow tests
- `CustomerControllerTest.java` - API endpoint tests
- `KycDocumentControllerTest.java` - Document API tests

### Integration Tests

```bash
mvn verify -f customer-service/pom.xml
```

**Uses:** TestContainers (PostgreSQL)

**Test File:** `CustomerServiceIntegrationTest.java`

**Tests:**
- Full KYC workflow (register → verify → approve)
- Database persistence
- State transitions
- Constraint validation

### API Tests

```bash
.\scripts\test\test-customer-service.ps1
```

**Tests:** 20 end-to-end scenarios including:
- Customer registration
- Document upload and verification
- Customer verification and approval
- Status transitions (suspend/reactivate)
- Error handling
- Integration with Account Service

---

## Deployment

### Build

```bash
.\scripts\build\build-customer-service.ps1
```

Builds JAR and Docker image.

### Deploy

```bash
.\scripts\deploy\deploy-customer-service.ps1
```

Starts container with health checks.

### Docker Compose

```bash
docker-compose up -d customer-service
```

**Dependencies:**
- postgres (banking_customers database)
- redis
- kafka
- eureka-server
- account-service

---

## Configuration

### application.yml

```yaml
server:
  port: 8083

spring:
  application:
    name: customer-service
  datasource:
    url: jdbc:postgresql://localhost:5432/banking_customers
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

### Environment Variables (Docker)

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/banking_customers
SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
SPRING_DATA_REDIS_HOST: redis
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
```

---

## Database Schema

### customers table

```sql
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    date_of_birth DATE NOT NULL,
    national_id VARCHAR(50) UNIQUE NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    status VARCHAR(50) NOT NULL,
    status_reason TEXT,
    risk_level VARCHAR(20),
    verified_at TIMESTAMP,
    verified_by VARCHAR(255),
    approved_at TIMESTAMP,
    approved_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_customer_id ON customers(customer_id);
CREATE INDEX idx_email ON customers(email);
CREATE INDEX idx_national_id ON customers(national_id);
CREATE INDEX idx_status ON customers(status);
CREATE INDEX idx_created_at ON customers(created_at);
```

### kyc_documents table

```sql
CREATE TABLE kyc_documents (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    issuing_authority VARCHAR(255),
    issue_date DATE,
    expiry_date DATE,
    document_url TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    verified_at TIMESTAMP,
    verified_by VARCHAR(255),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_id ON kyc_documents(customer_id);
CREATE INDEX idx_document_type ON kyc_documents(document_type);
CREATE INDEX idx_status ON kyc_documents(status);
```

### customer_history table

```sql
CREATE TABLE customer_history (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50),
    description TEXT,
    performed_by VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_id ON customer_history(customer_id);
CREATE INDEX idx_timestamp ON customer_history(timestamp DESC);
```

---

## Common Use Cases

### 1. New Customer Onboarding

```bash
# 1. Register customer
POST /api/v1/customers
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  ...
}

# 2. Upload passport
POST /api/v1/documents
{
  "customerId": "CUS-XXX",
  "documentType": "PASSPORT",
  ...
}

# 3. Upload proof of address
POST /api/v1/documents
{
  "customerId": "CUS-XXX",
  "documentType": "PROOF_OF_ADDRESS",
  ...
}

# 4. Verify documents
POST /api/v1/documents/{docId}/verify

# 5. Verify customer
POST /api/v1/customers/{customerId}/verify

# 6. Approve customer
POST /api/v1/customers/{customerId}/approve
{
  "approvedBy": "manager@bank.com",
  "riskLevel": "LOW"
}

# 7. Customer can now create accounts!
```

### 2. Compliance Suspension

```bash
# Suspend customer
POST /api/v1/customers/{customerId}/suspend
{
  "reason": "Suspicious activity - transaction pattern analysis",
  "suspendedBy": "compliance@bank.com"
}

# Later: Reactivate
POST /api/v1/customers/{customerId}/activate
```

### 3. Customer Profile Update

```bash
# Update contact information
PUT /api/v1/customers/{customerId}
{
  "phoneNumber": "+31687654321",
  "address": "New Street 100",
  "city": "Rotterdam"
}

# View history
GET /api/v1/customers/{customerId}/history
```

---

## Security Considerations

### Data Masking

- **National ID** is masked in all API responses: `123****8901`
- Full national ID only visible in database
- Implemented in `Customer.getMaskedNationalId()`

### Sensitive Data

**Never log:**
- Full national ID numbers
- Document URLs (may contain sensitive paths)
- Personal addresses

**Always log:**
- Customer ID (CUS-XXX)
- Actions performed
- Status transitions

### GDPR Compliance

- Customer data can be deleted (implement `DELETE /customers/{id}`)
- Audit trail maintained for compliance
- History records all data changes

---

## Performance

### Caching

- Redis cache for frequently accessed customers
- TTL: 5 minutes
- Cache invalidation on updates

### Database Indexes

- `customer_id`, `email`, `national_id` for fast lookups
- `status` for filtering
- `created_at` for time-based queries

### Optimistic Locking

- `@Version` field prevents concurrent update conflicts
- Uses `findByCustomerIdForUpdate()` for transactional updates

---

## Troubleshooting

### Customer Not Found (404)

**Cause:** Invalid customerId
**Solution:** Verify customerId format (CUS-XXXXXXXXXXXX)

### Duplicate Email (409)

**Cause:** Email already registered
**Solution:** Use different email or retrieve existing customer

### Invalid State Transition (400)

**Cause:** Attempting invalid status change
**Solution:** Follow status flow: PENDING → VERIFIED → APPROVED

### Document Expired (400)

**Cause:** Expiry date in the past
**Solution:** Upload valid, non-expired document

### Feign Client Timeout

**Cause:** Account Service unavailable
**Solution:** Check Eureka registration, verify Account Service health

---

## Future Enhancements

1. **Email Verification:** Send OTP during registration
2. **Two-Factor Authentication:** SMS/Email OTP for sensitive operations
3. **Document OCR:** Automatic data extraction from documents
4. **Third-Party KYC:** Integration with external KYC providers
5. **Risk Scoring:** Automated risk assessment
6. **Customer Portal:** Self-service document upload
7. **Notification Service Integration:** Real-time customer notifications
8. **Account Auto-Creation:** Automatic account creation on approval

---

## Monitoring

### Health Check

```bash
GET http://localhost:8083/actuator/health
```

### Metrics

```bash
GET http://localhost:8083/actuator/metrics
```

### Eureka Dashboard

```
http://localhost:8761
```

Check `CUSTOMER-SERVICE` registration.

---

## Summary

The Customer Service provides:

✅ Complete KYC workflow (3-stage verification)
✅ Document management (upload, verify, reject)
✅ Customer lifecycle (register → verify → approve → suspend/close)
✅ Event-driven integration (Kafka)
✅ Feign client integration with Account Service
✅ Comprehensive audit trail
✅ Data security (masking, validation)
✅ Production-ready (Docker, health checks, tests)

**Total Implementation:** 11 phases completed in sequential order following established patterns from Account and Transfer services.

---

**Last Updated:** 23 December 2025
**Version:** 1.0
**Status:** ✅ PRODUCTION-READY
