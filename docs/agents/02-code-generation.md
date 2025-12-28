# Code Generation Agents

> **Category:** Automated Code Writing
> **Agent Count:** 5
> **Automation Level:** High (80-90%)
> **Last Updated:** 28 December 2025

---

## Overview

Code Generation agents create production-ready code following Banking Platform patterns, coding standards, and best practices. They generate entities, APIs, SAGA orchestrators, events, and security components with comprehensive validation and error handling.

---

## 1. EntityDesignAgent 📐

### Specification

**ID:** `AGENT-CG-001`
**Name:** EntityDesignAgent
**Category:** Code Generation
**Scope:** JPA entities, database schema, repositories
**Automation Level:** High (85%)

### Objective

Generate domain entities with proper JPA annotations, relationships, validation, and corresponding Liquibase migrations following DDD principles and Banking Platform standards.

### Capabilities

#### Input Parameters
- Entity name and description
- Fields with types and constraints
- Relationships (one-to-many, many-to-many)
- Business rules
- Indexing requirements

#### Processing Logic

1. **Domain Model Analysis**
   - Parse entity requirements
   - Identify relationships
   - Determine cardinality
   - Map to database schema

2. **Entity Class Generation**
   - JPA annotations (@Entity, @Table)
   - Lombok annotations (@Data, @Builder)
   - Field validation (@NotNull, @Size)
   - Relationships (@OneToMany, @ManyToOne)
   - Helper methods (@PrePersist, getMasked*())
   - Audit fields (createdAt, updatedAt)

3. **Liquibase Migration Generation**
   - CREATE TABLE statements
   - Column definitions with constraints
   - Indexes (primary, unique, foreign keys)
   - Sequences for auto-increment
   - Rollback scripts

4. **Repository Interface Generation**
   - JpaRepository extension
   - Custom query methods
   - @Query annotations for complex queries
   - Naming conventions

#### Output Artifacts

1. **Entity Class**
   ```java
   package com.banking.notification.model;

   import jakarta.persistence.*;
   import lombok.*;
   import java.math.BigDecimal;
   import java.time.LocalDateTime;

   @Entity
   @Table(
       name = "notifications",
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_created_at", columnList = "created_at")
       }
   )
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   @ToString(exclude = {"template"})
   @EqualsAndHashCode(exclude = {"id"})
   public class Notification {

       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       @Column(unique = true, nullable = false, length = 50)
       private String notificationId;  // NOT-XXXXXXXXXXXX

       @Column(nullable = false, length = 50)
       private String userId;

       @Enumerated(EnumType.STRING)
       @Column(nullable = false, length = 20)
       private NotificationChannel channel;  // EMAIL, SMS, PUSH

       @Column(nullable = false, length = 100)
       private String subject;

       @Column(nullable = false, columnDefinition = "TEXT")
       private String content;

       @Enumerated(EnumType.STRING)
       @Column(nullable = false, length = 20)
       private NotificationStatus status;  // PENDING, SENT, FAILED

       @ManyToOne(fetch = FetchType.LAZY)
       @JoinColumn(name = "template_id")
       private NotificationTemplate template;

       @Column(name = "sent_at")
       private LocalDateTime sentAt;

       @Column(name = "failed_reason")
       private String failedReason;

       @Column(name = "retry_count", nullable = false)
       private Integer retryCount = 0;

       @Column(nullable = false, updatable = false)
       private LocalDateTime createdAt;

       @Column(nullable = false)
       private LocalDateTime updatedAt;

       @Version
       private Long version;  // Optimistic locking

       @PrePersist
       protected void onCreate() {
           this.createdAt = LocalDateTime.now();
           this.updatedAt = LocalDateTime.now();
           if (this.notificationId == null) {
               this.notificationId = generateNotificationId();
           }
       }

       @PreUpdate
       protected void onUpdate() {
           this.updatedAt = LocalDateTime.now();
       }

       private String generateNotificationId() {
           return "NOT-" + UUID.randomUUID().toString()
               .replace("-", "").substring(0, 12).toUpperCase();
       }

       // Helper method
       public void markAsSent() {
           this.status = NotificationStatus.SENT;
           this.sentAt = LocalDateTime.now();
       }

       public void markAsFailed(String reason) {
           this.status = NotificationStatus.FAILED;
           this.failedReason = reason;
           this.retryCount++;
       }
   }
   ```

2. **Enum Classes**
   ```java
   public enum NotificationChannel {
       EMAIL,
       SMS,
       PUSH,
       IN_APP
   }

   public enum NotificationStatus {
       PENDING,
       SENT,
       FAILED,
       CANCELLED
   }
   ```

3. **Liquibase Migration**
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <databaseChangeLog
       xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
       http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

       <changeSet id="001-create-notifications-table" author="system">
           <createTable tableName="notifications">
               <column name="id" type="BIGSERIAL">
                   <constraints primaryKey="true" nullable="false"/>
               </column>
               <column name="notification_id" type="VARCHAR(50)">
                   <constraints unique="true" nullable="false"/>
               </column>
               <column name="user_id" type="VARCHAR(50)">
                   <constraints nullable="false"/>
               </column>
               <column name="channel" type="VARCHAR(20)">
                   <constraints nullable="false"/>
               </column>
               <column name="subject" type="VARCHAR(100)">
                   <constraints nullable="false"/>
               </column>
               <column name="content" type="TEXT">
                   <constraints nullable="false"/>
               </column>
               <column name="status" type="VARCHAR(20)">
                   <constraints nullable="false"/>
               </column>
               <column name="template_id" type="BIGINT"/>
               <column name="sent_at" type="TIMESTAMP"/>
               <column name="failed_reason" type="VARCHAR(500)"/>
               <column name="retry_count" type="INT" defaultValue="0">
                   <constraints nullable="false"/>
               </column>
               <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
                   <constraints nullable="false"/>
               </column>
               <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
                   <constraints nullable="false"/>
               </column>
               <column name="version" type="BIGINT" defaultValue="0">
                   <constraints nullable="false"/>
               </column>
           </createTable>

           <createIndex tableName="notifications" indexName="idx_notification_id">
               <column name="notification_id"/>
           </createIndex>

           <createIndex tableName="notifications" indexName="idx_user_id">
               <column name="user_id"/>
           </createIndex>

           <createIndex tableName="notifications" indexName="idx_status">
               <column name="status"/>
           </createIndex>

           <createIndex tableName="notifications" indexName="idx_created_at">
               <column name="created_at"/>
           </createIndex>

           <addForeignKeyConstraint
               baseTableName="notifications"
               baseColumnNames="template_id"
               constraintName="fk_notifications_template"
               referencedTableName="notification_templates"
               referencedColumnNames="id"
               onDelete="SET NULL"/>
       </changeSet>

       <changeSet id="002-create-notification-templates-table" author="system">
           <createTable tableName="notification_templates">
               <column name="id" type="BIGSERIAL">
                   <constraints primaryKey="true" nullable="false"/>
               </column>
               <column name="name" type="VARCHAR(100)">
                   <constraints unique="true" nullable="false"/>
               </column>
               <column name="channel" type="VARCHAR(20)">
                   <constraints nullable="false"/>
               </column>
               <column name="subject_template" type="VARCHAR(200)"/>
               <column name="body_template" type="TEXT">
                   <constraints nullable="false"/>
               </column>
               <column name="variables" type="JSONB"/>
               <column name="active" type="BOOLEAN" defaultValue="true">
                   <constraints nullable="false"/>
               </column>
               <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
                   <constraints nullable="false"/>
               </column>
           </createTable>
       </changeSet>

       <rollback>
           <dropTable tableName="notifications"/>
           <dropTable tableName="notification_templates"/>
       </rollback>
   </databaseChangeLog>
   ```

4. **Repository Interface**
   ```java
   package com.banking.notification.repository;

   import com.banking.notification.model.Notification;
   import com.banking.notification.model.NotificationStatus;
   import org.springframework.data.jpa.repository.JpaRepository;
   import org.springframework.data.jpa.repository.Query;
   import org.springframework.data.repository.query.Param;
   import org.springframework.stereotype.Repository;

   import java.time.LocalDateTime;
   import java.util.List;
   import java.util.Optional;

   @Repository
   public interface NotificationRepository extends JpaRepository<Notification, Long> {

       Optional<Notification> findByNotificationId(String notificationId);

       List<Notification> findByUserId(String userId);

       List<Notification> findByUserIdAndStatus(String userId, NotificationStatus status);

       List<Notification> findByStatus(NotificationStatus status);

       @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.createdAt < :before")
       List<Notification> findStaleNotifications(
           @Param("status") NotificationStatus status,
           @Param("before") LocalDateTime before
       );

       @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
              "ORDER BY n.createdAt DESC LIMIT :limit")
       List<Notification> findRecentByUserId(
           @Param("userId") String userId,
           @Param("limit") int limit
       );

       Long countByUserIdAndStatus(String userId, NotificationStatus status);

       boolean existsByNotificationId(String notificationId);
   }
   ```

### Usage Examples

#### Example 1: Transfer Entity with Relationships

**Prompt:**
```
Generate Transfer entity with the following:
- transferReference (unique, system-generated)
- fromAccount, toAccount (relationships to Account entity)
- amount (BigDecimal, 19,2 precision)
- currency (enum: TRY, USD, EUR, GBP)
- status (enum: PENDING, VALIDATED, COMPLETED, FAILED, COMPENSATED)
- idempotencyKey (unique, client-provided)
- fee (BigDecimal)
- description (optional)
- Timestamps and audit fields
- Optimistic locking

Include Liquibase migration and repository.
```

**Expected Output:** (Full entity + migration + repository similar to above)

#### Example 2: Customer Entity with Masked Fields

**Prompt:**
```
Generate Customer entity with:
- customerId (CUS-XXXXXXXXXXXX)
- Personal info (firstName, lastName, email, phone)
- National ID (should be masked in responses)
- Address fields
- KYC status (enum)
- Risk level (enum)
- Verification/approval tracking
- One-to-many relationship with KycDocument
- Audit trail

Include helper method getMaskedNationalId().
```

**Expected Output:** (Entity with masking logic + migration)

### Best Practices

#### When to Use EntityDesignAgent

✅ **DO Use When:**
- Creating new entities
- Complex relationships
- Need Liquibase migrations
- Want consistent entity patterns

❌ **DON'T Use When:**
- Modifying existing entity (minor change)
- DTO classes (not JPA entities)
- Simple POJO

#### Generated Code Quality Checks

**The agent ensures:**
- ✅ BigDecimal for money fields (precision 19, scale 2)
- ✅ Enums use @Enumerated(EnumType.STRING)
- ✅ Indexes on frequently queried columns
- ✅ Optimistic locking (@Version) for critical entities
- ✅ Audit fields (createdAt, updatedAt, @PrePersist, @PreUpdate)
- ✅ Helper methods for domain logic
- ✅ ToString/Equals exclude heavy relations
- ✅ Liquibase rollback scripts

---

## 2. APIDesignAgent 🎯

### Specification

**ID:** `AGENT-CG-002`
**Name:** APIDesignAgent
**Category:** Code Generation
**Scope:** REST controllers, DTOs, validation
**Automation Level:** High (85%)

### Objective

Generate RESTful API endpoints with proper HTTP methods, status codes, DTOs, validation, and consistent ApiResponse wrapper following Banking Platform API standards.

### Capabilities

#### Input Parameters
- Service name
- Resource name (e.g., "Account", "Transfer")
- Operations (CRUD, custom)
- Validation rules
- Authorization requirements

#### Processing Logic

1. **API Design**
   - Map operations to HTTP methods
   - Design URL structure
   - Define status codes
   - Plan error responses

2. **Controller Generation**
   - @RestController with @RequestMapping
   - Method annotations (@GetMapping, @PostMapping, etc.)
   - @PathVariable with explicit names
   - @Valid for request validation
   - Proper HTTP status codes
   - ApiResponse wrapper
   - Logging

3. **DTO Generation**
   - Request DTOs (validation annotations)
   - Response DTOs (no sensitive data)
   - Mappers (entity ↔ DTO)
   - ApiResponse wrapper class

4. **Validation Rules**
   - @NotNull, @NotBlank
   - @Size, @Min, @Max
   - @Email, @Pattern
   - Custom validators

#### Output Artifacts

1. **Controller Class**
   ```java
   package com.banking.notification.controller;

   import com.banking.notification.dto.*;
   import com.banking.notification.service.NotificationService;
   import jakarta.validation.Valid;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.http.HttpStatus;
   import org.springframework.http.ResponseEntity;
   import org.springframework.security.access.prepost.PreAuthorize;
   import org.springframework.web.bind.annotation.*;

   import java.util.List;

   @RestController
   @RequestMapping("/api/v1/notifications")
   @Slf4j
   @Validated
   public class NotificationController {

       private final NotificationService notificationService;

       public NotificationController(NotificationService notificationService) {
           this.notificationService = notificationService;
       }

       @PostMapping("/send")
       @ResponseStatus(HttpStatus.CREATED)
       @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
       public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
           @Valid @RequestBody SendNotificationRequest request
       ) {
           log.info("Sending notification: channel={}, userId={}",
               request.getChannel(), request.getUserId());

           NotificationResponse response = notificationService.sendNotification(request);

           return ResponseEntity
               .status(HttpStatus.CREATED)
               .body(ApiResponse.success(response, "Notification sent successfully"));
       }

       @GetMapping("/{notificationId}")
       @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
       public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
           @PathVariable("notificationId") String notificationId
       ) {
           log.debug("Fetching notification: {}", notificationId);

           NotificationResponse response = notificationService.getNotification(notificationId);

           return ResponseEntity.ok(
               ApiResponse.success(response, "Notification retrieved successfully")
           );
       }

       @GetMapping("/user/{userId}")
       @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
       public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
           @PathVariable("userId") String userId,
           @RequestParam(value = "status", required = false) NotificationStatus status,
           @RequestParam(value = "limit", defaultValue = "20") int limit
       ) {
           log.debug("Fetching notifications for user: {}, status: {}, limit: {}",
               userId, status, limit);

           List<NotificationResponse> notifications =
               notificationService.getUserNotifications(userId, status, limit);

           return ResponseEntity.ok(
               ApiResponse.success(notifications, "Notifications retrieved successfully")
           );
       }

       @PostMapping("/{notificationId}/retry")
       @PreAuthorize("hasRole('ADMIN')")
       public ResponseEntity<ApiResponse<NotificationResponse>> retryNotification(
           @PathVariable("notificationId") String notificationId
       ) {
           log.info("Retrying notification: {}", notificationId);

           NotificationResponse response = notificationService.retryNotification(notificationId);

           return ResponseEntity.ok(
               ApiResponse.success(response, "Notification retry initiated")
           );
       }

       @DeleteMapping("/{notificationId}")
       @PreAuthorize("hasRole('ADMIN')")
       public ResponseEntity<ApiResponse<Void>> cancelNotification(
           @PathVariable("notificationId") String notificationId
       ) {
           log.info("Cancelling notification: {}", notificationId);

           notificationService.cancelNotification(notificationId);

           return ResponseEntity.ok(
               ApiResponse.success(null, "Notification cancelled successfully")
           );
       }

       @GetMapping("/stats")
       @PreAuthorize("hasRole('ADMIN')")
       public ResponseEntity<ApiResponse<NotificationStatsResponse>> getStats() {
           log.debug("Fetching notification statistics");

           NotificationStatsResponse stats = notificationService.getStats();

           return ResponseEntity.ok(
               ApiResponse.success(stats, "Statistics retrieved successfully")
           );
       }
   }
   ```

2. **Request DTOs**
   ```java
   package com.banking.notification.dto;

   import com.banking.notification.model.NotificationChannel;
   import jakarta.validation.constraints.*;
   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;

   import java.util.Map;

   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class SendNotificationRequest {

       @NotBlank(message = "User ID is required")
       @Size(max = 50, message = "User ID must not exceed 50 characters")
       private String userId;

       @NotNull(message = "Channel is required")
       private NotificationChannel channel;

       @NotBlank(message = "Subject is required")
       @Size(max = 100, message = "Subject must not exceed 100 characters")
       private String subject;

       @NotBlank(message = "Content is required")
       private String content;

       @Size(max = 100, message = "Template name must not exceed 100 characters")
       private String templateName;

       private Map<String, String> templateVariables;

       @Min(value = 0, message = "Priority must be non-negative")
       @Max(value = 10, message = "Priority must not exceed 10")
       private Integer priority = 5;

       @Email(message = "Invalid email format")
       private String recipientEmail;  // For EMAIL channel

       @Pattern(
           regexp = "^\\+[1-9]\\d{1,14}$",
           message = "Phone number must be in E.164 format (e.g., +905551234567)"
       )
       private String recipientPhone;  // For SMS channel

       private String deviceToken;  // For PUSH channel
   }
   ```

3. **Response DTOs**
   ```java
   package com.banking.notification.dto;

   import com.banking.notification.model.NotificationChannel;
   import com.banking.notification.model.NotificationStatus;
   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;

   import java.time.LocalDateTime;

   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class NotificationResponse {

       private Long id;
       private String notificationId;
       private String userId;
       private NotificationChannel channel;
       private String subject;
       private String content;  // May be truncated for list view
       private NotificationStatus status;
       private String templateName;
       private LocalDateTime sentAt;
       private String failedReason;
       private Integer retryCount;
       private LocalDateTime createdAt;
       private LocalDateTime updatedAt;

       // Helper for truncated content in list views
       public String getTruncatedContent(int maxLength) {
           if (content == null) return null;
           return content.length() > maxLength
               ? content.substring(0, maxLength) + "..."
               : content;
       }
   }

   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public class NotificationStatsResponse {
       private Long totalNotifications;
       private Long pendingCount;
       private Long sentCount;
       private Long failedCount;
       private Map<NotificationChannel, Long> byChannel;
       private Map<NotificationStatus, Long> byStatus;
       private LocalDateTime generatedAt;
   }
   ```

4. **ApiResponse Wrapper**
   ```java
   package com.banking.notification.dto;

   import com.fasterxml.jackson.annotation.jackson.annotation.JsonInclude;
   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;

   import java.time.LocalDateTime;
   import java.util.Map;

   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public class ApiResponse<T> {

       private boolean success;
       private String message;
       private T data;
       private String errorCode;
       private Map<String, String> errors;  // Field-level validation errors
       private LocalDateTime timestamp;

       public static <T> ApiResponse<T> success(T data) {
           return ApiResponse.<T>builder()
               .success(true)
               .data(data)
               .timestamp(LocalDateTime.now())
               .build();
       }

       public static <T> ApiResponse<T> success(T data, String message) {
           return ApiResponse.<T>builder()
               .success(true)
               .message(message)
               .data(data)
               .timestamp(LocalDateTime.now())
               .build();
       }

       public static <T> ApiResponse<T> error(String message) {
           return ApiResponse.<T>builder()
               .success(false)
               .message(message)
               .timestamp(LocalDateTime.now())
               .build();
       }

       public static <T> ApiResponse<T> error(String message, String errorCode) {
           return ApiResponse.<T>builder()
               .success(false)
               .message(message)
               .errorCode(errorCode)
               .timestamp(LocalDateTime.now())
               .build();
       }

       public static <T> ApiResponse<T> validationError(
           String message,
           Map<String, String> errors
       ) {
           return ApiResponse.<T>builder()
               .success(false)
               .message(message)
               .errors(errors)
               .errorCode("VALIDATION_ERROR")
               .timestamp(LocalDateTime.now())
               .build();
       }
   }
   ```

5. **Global Exception Handler**
   ```java
   package com.banking.notification.exception;

   import com.banking.notification.dto.ApiResponse;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.http.HttpStatus;
   import org.springframework.http.ResponseEntity;
   import org.springframework.validation.FieldError;
   import org.springframework.web.bind.MethodArgumentNotValidException;
   import org.springframework.web.bind.annotation.ExceptionHandler;
   import org.springframework.web.bind.annotation.RestControllerAdvice;

   import java.util.HashMap;
   import java.util.Map;
   import java.util.stream.Collectors;

   @RestControllerAdvice
   @Slf4j
   public class GlobalExceptionHandler {

       @ExceptionHandler(NotificationNotFoundException.class)
       public ResponseEntity<ApiResponse<Void>> handleNotificationNotFound(
           NotificationNotFoundException ex
       ) {
           log.error("Notification not found: {}", ex.getMessage());
           return ResponseEntity
               .status(HttpStatus.NOT_FOUND)
               .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
       }

       @ExceptionHandler(MethodArgumentNotValidException.class)
       public ResponseEntity<ApiResponse<Void>> handleValidationException(
           MethodArgumentNotValidException ex
       ) {
           Map<String, String> errors = ex.getBindingResult()
               .getFieldErrors()
               .stream()
               .collect(Collectors.toMap(
                   FieldError::getField,
                   error -> error.getDefaultMessage() != null
                       ? error.getDefaultMessage()
                       : "Invalid value",
                   (existing, replacement) -> existing + "; " + replacement
               ));

           log.warn("Validation failed: {}", errors);

           return ResponseEntity
               .status(HttpStatus.BAD_REQUEST)
               .body(ApiResponse.validationError("Validation failed", errors));
       }

       @ExceptionHandler(Exception.class)
       public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
           log.error("Unexpected error", ex);
           return ResponseEntity
               .status(HttpStatus.INTERNAL_SERVER_ERROR)
               .body(ApiResponse.error(
                   "An unexpected error occurred",
                   "INTERNAL_SERVER_ERROR"
               ));
       }
   }
   ```

### Usage Examples

#### Example 1: Account API

**Prompt:**
```
Generate REST API for Account Service with:
- Create account (POST)
- Get account by number (GET)
- Get account by IBAN (GET)
- Update balance (PUT)
- List accounts with filters (GET with query params)
- Delete account (soft delete, admin only)

Include validation, authorization, and error handling.
```

**Expected Output:** (Controller + DTOs + Validation + Exception handling)

#### Example 2: Transfer API with SAGA Status

**Prompt:**
```
Generate Transfer API with:
- Initiate transfer (POST with idempotency)
- Get transfer status (GET)
- Cancel transfer (DELETE, only if PENDING)
- List transfers for account (GET with pagination)

Include:
- Idempotency key validation
- SAGA status in response
- Compensation details if failed
```

**Expected Output:** (Complete API implementation)

### Best Practices

#### API Design Principles

**The agent follows:**
- ✅ RESTful conventions (resources, HTTP methods)
- ✅ Consistent URL structure (/api/v1/{resource})
- ✅ Proper HTTP status codes (201 for creation, 204 for delete)
- ✅ ApiResponse wrapper for all endpoints
- ✅ @PathVariable explicit names
- ✅ Validation on all inputs
- ✅ @PreAuthorize for role-based access
- ✅ Logging with context (no sensitive data)

#### HTTP Status Codes

```
200 OK - Successful GET, PUT
201 CREATED - Successful POST (resource created)
204 NO CONTENT - Successful DELETE
400 BAD REQUEST - Validation error
401 UNAUTHORIZED - Missing/invalid JWT
403 FORBIDDEN - Insufficient permissions
404 NOT FOUND - Resource not found
409 CONFLICT - Duplicate (e.g., idempotency key)
500 INTERNAL SERVER ERROR - Server error
```

---

## 3. SAGAImplementationAgent 🔄

### Specification

**ID:** `AGENT-CG-003`
**Name:** SAGAImplementationAgent
**Category:** Code Generation
**Scope:** Distributed transaction orchestration
**Automation Level:** High (80%)

### Objective

Generate SAGA orchestrators with validation, execution, and compensation steps following the orchestration pattern established in Transfer Service.

### Capabilities

#### Input Parameters
- Transaction name (e.g., "Transfer", "Account Closure")
- Steps (validation, execution phases)
- Compensation logic
- Timeout configuration
- Idempotency requirements

#### Processing Logic

1. **SAGA Design**
   - Define steps (sequential execution)
   - Define compensation (reverse order)
   - State machine design
   - Event publishing

2. **Orchestrator Generation**
   - Main orchestrator class
   - Step interfaces and implementations
   - Compensation methods
   - State tracking
   - Error handling

3. **Status Enum Generation**
   - All possible states
   - Transition rules
   - Final states (success/failure)

4. **Integration Points**
   - Feign clients for remote calls
   - Circuit breakers
   - Timeout handling
   - Retry logic

#### Output Artifacts

1. **SAGA Status Enum**
   ```java
   package com.banking.transfer.model;

   public enum TransferStatus {
       // Initial
       PENDING,

       // Validation phase
       VALIDATING,
       VALIDATED,

       // Execution phase
       DEBIT_PENDING,
       DEBIT_COMPLETED,
       CREDIT_PENDING,
       CREDIT_COMPLETED,

       // Success
       COMPLETED,

       // Failure & Compensation
       VALIDATION_FAILED,
       COMPENSATING,
       COMPENSATED,

       // Final states
       FAILED;

       public boolean isFinal() {
           return this == COMPLETED ||
                  this == FAILED ||
                  this == COMPENSATED;
       }

       public boolean isCompensatable() {
           return this != PENDING &&
                  this != VALIDATING &&
                  !isFinal();
       }
   }
   ```

2. **SAGA Step Interface**
   ```java
   package com.banking.transfer.saga;

   import com.banking.transfer.model.Transfer;

   public interface SagaStep {

       /**
        * Execute the step
        * @param transfer The transfer object
        * @throws SagaStepException if step fails
        */
       void execute(Transfer transfer) throws SagaStepException;

       /**
        * Compensate (rollback) the step
        * @param transfer The transfer object
        */
       void compensate(Transfer transfer);

       /**
        * Get step name for logging
        */
       String getStepName();
   }
   ```

3. **Validation Step**
   ```java
   package com.banking.transfer.saga.steps;

   import com.banking.transfer.client.AccountServiceClient;
   import com.banking.transfer.model.Transfer;
   import com.banking.transfer.model.TransferStatus;
   import com.banking.transfer.saga.SagaStep;
   import com.banking.transfer.saga.SagaStepException;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.stereotype.Component;

   import java.math.BigDecimal;

   @Component
   @Slf4j
   public class ValidationStep implements SagaStep {

       private final AccountServiceClient accountServiceClient;

       public ValidationStep(AccountServiceClient accountServiceClient) {
           this.accountServiceClient = accountServiceClient;
       }

       @Override
       public void execute(Transfer transfer) throws SagaStepException {
           log.info("Executing validation step for transfer: {}",
               transfer.getTransferReference());

           try {
               // 1. Validate source account exists and is active
               var sourceAccount = accountServiceClient
                   .getAccount(transfer.getFromAccountNumber());

               if (!sourceAccount.isActive()) {
                   throw new SagaStepException(
                       "Source account is not active: " +
                       transfer.getFromAccountNumber()
                   );
               }

               // 2. Validate destination account exists and is active
               var destAccount = accountServiceClient
                   .getAccount(transfer.getToAccountNumber());

               if (!destAccount.isActive()) {
                   throw new SagaStepException(
                       "Destination account is not active: " +
                       transfer.getToAccountNumber()
                   );
               }

               // 3. Validate sufficient balance
               BigDecimal requiredAmount = transfer.getAmount()
                   .add(transfer.getFeeAmount() != null
                       ? transfer.getFeeAmount()
                       : BigDecimal.ZERO);

               if (sourceAccount.getBalance().compareTo(requiredAmount) < 0) {
                   throw new SagaStepException(
                       String.format(
                           "Insufficient balance. Required: %s, Available: %s",
                           requiredAmount,
                           sourceAccount.getBalance()
                       )
                   );
               }

               // 4. Validate currency match
               if (!sourceAccount.getCurrency().equals(transfer.getCurrency())) {
                   throw new SagaStepException(
                       "Currency mismatch: account uses " +
                       sourceAccount.getCurrency() +
                       ", transfer uses " + transfer.getCurrency()
                   );
               }

               transfer.setStatus(TransferStatus.VALIDATED);
               log.info("Validation step completed successfully: {}",
                   transfer.getTransferReference());

           } catch (Exception e) {
               transfer.setStatus(TransferStatus.VALIDATION_FAILED);
               transfer.setFailureReason(e.getMessage());
               throw new SagaStepException("Validation failed: " + e.getMessage(), e);
           }
       }

       @Override
       public void compensate(Transfer transfer) {
           // No compensation needed for validation
           log.debug("No compensation needed for validation step");
       }

       @Override
       public String getStepName() {
           return "ValidationStep";
       }
   }
   ```

4. **Debit Step**
   ```java
   package com.banking.transfer.saga.steps;

   import com.banking.transfer.client.AccountServiceClient;
   import com.banking.transfer.dto.UpdateBalanceRequest;
   import com.banking.transfer.model.Transfer;
   import com.banking.transfer.model.TransferStatus;
   import com.banking.transfer.saga.SagaStep;
   import com.banking.transfer.saga.SagaStepException;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.stereotype.Component;

   @Component
   @Slf4j
   public class DebitStep implements SagaStep {

       private final AccountServiceClient accountServiceClient;

       public DebitStep(AccountServiceClient accountServiceClient) {
           this.accountServiceClient = accountServiceClient;
       }

       @Override
       public void execute(Transfer transfer) throws SagaStepException {
           log.info("Executing debit step for transfer: {}",
               transfer.getTransferReference());

           try {
               transfer.setStatus(TransferStatus.DEBIT_PENDING);

               UpdateBalanceRequest debitRequest = UpdateBalanceRequest.builder()
                   .amount(transfer.getAmount().negate())  // Negative for debit
                   .description("Transfer to " + transfer.getToAccountNumber())
                   .transactionReference(transfer.getTransferReference())
                   .build();

               var response = accountServiceClient.updateBalance(
                   transfer.getFromAccountNumber(),
                   debitRequest
               );

               transfer.setDebitTransactionId(response.getTransactionId());
               transfer.setStatus(TransferStatus.DEBIT_COMPLETED);

               log.info("Debit step completed: transfer={}, txId={}",
                   transfer.getTransferReference(),
                   response.getTransactionId());

           } catch (Exception e) {
               log.error("Debit step failed: {}", e.getMessage());
               throw new SagaStepException("Debit failed: " + e.getMessage(), e);
           }
       }

       @Override
       public void compensate(Transfer transfer) {
           log.warn("Compensating debit step for transfer: {}",
               transfer.getTransferReference());

           try {
               // Credit back the debited amount
               UpdateBalanceRequest creditRequest = UpdateBalanceRequest.builder()
                   .amount(transfer.getAmount())  // Positive for credit
                   .description("Compensation for failed transfer " +
                       transfer.getTransferReference())
                   .transactionReference(transfer.getTransferReference() + "-COMP")
                   .build();

               accountServiceClient.updateBalance(
                   transfer.getFromAccountNumber(),
                   creditRequest
               );

               log.info("Debit compensation completed: {}",
                   transfer.getTransferReference());

           } catch (Exception e) {
               log.error("Debit compensation failed - MANUAL INTERVENTION REQUIRED", e);
               // Alert operations team
               // Store in compensation failure queue
           }
       }

       @Override
       public String getStepName() {
           return "DebitStep";
       }
   }
   ```

5. **SAGA Orchestrator**
   ```java
   package com.banking.transfer.saga;

   import com.banking.transfer.model.Transfer;
   import com.banking.transfer.model.TransferStatus;
   import com.banking.transfer.repository.TransferRepository;
   import com.banking.transfer.saga.steps.*;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.stereotype.Service;
   import org.springframework.transaction.annotation.Transactional;

   import java.util.ArrayList;
   import java.util.Collections;
   import java.util.List;

   @Service
   @Slf4j
   public class TransferSagaOrchestrator {

       private final List<SagaStep> steps;
       private final TransferRepository transferRepository;

       public TransferSagaOrchestrator(
           ValidationStep validationStep,
           DebitStep debitStep,
           CreditStep creditStep,
           TransferRepository transferRepository
       ) {
           this.steps = List.of(validationStep, debitStep, creditStep);
           this.transferRepository = transferRepository;
       }

       @Transactional
       public Transfer executeTransfer(Transfer transfer) {
           log.info("Starting SAGA execution for transfer: {}",
               transfer.getTransferReference());

           List<SagaStep> executedSteps = new ArrayList<>();

           try {
               // Execute all steps sequentially
               for (SagaStep step : steps) {
                   log.info("Executing step: {}", step.getStepName());
                   step.execute(transfer);
                   executedSteps.add(step);
                   transferRepository.save(transfer);  // Persist state after each step
               }

               // All steps completed successfully
               transfer.setStatus(TransferStatus.COMPLETED);
               transferRepository.save(transfer);

               log.info("SAGA execution completed successfully: {}",
                   transfer.getTransferReference());

               return transfer;

           } catch (SagaStepException e) {
               log.error("SAGA execution failed at step: {}, error: {}",
                   e.getStepName(), e.getMessage());

               // Compensate in reverse order
               compensate(transfer, executedSteps);

               return transfer;
           }
       }

       private void compensate(Transfer transfer, List<SagaStep> executedSteps) {
           log.warn("Starting compensation for transfer: {}",
               transfer.getTransferReference());

           transfer.setStatus(TransferStatus.COMPENSATING);
           transferRepository.save(transfer);

           // Reverse the list to compensate in reverse order
           List<SagaStep> reversed = new ArrayList<>(executedSteps);
           Collections.reverse(reversed);

           for (SagaStep step : reversed) {
               try {
                   log.info("Compensating step: {}", step.getStepName());
                   step.compensate(transfer);
               } catch (Exception e) {
                   log.error("Compensation failed for step: {} - MANUAL INTERVENTION REQUIRED",
                       step.getStepName(), e);
                   // Alert operations team
                   // Add to manual compensation queue
               }
           }

           transfer.setStatus(TransferStatus.COMPENSATED);
           transferRepository.save(transfer);

           log.info("Compensation completed for transfer: {}",
               transfer.getTransferReference());
       }
   }
   ```

### Usage Examples

#### Example 1: Account Closure SAGA

**Prompt:**
```
Generate SAGA orchestrator for account closure with steps:
1. Validation (account exists, no pending transactions, balance is zero)
2. Suspend account (prevent new transactions)
3. Notify customer (send notification)
4. Archive data (move to archive storage)
5. Mark as closed

Include compensation for each step.
```

**Expected Output:** (SAGA orchestrator + steps + compensation logic)

#### Example 2: Multi-Account Transfer SAGA

**Prompt:**
```
Generate SAGA for transferring funds from multiple source accounts to one destination:
1. Validate all source accounts
2. Calculate total and validate against destination limit
3. Debit from all source accounts (parallel)
4. Credit to destination account
5. Distribute fees among sources

Compensation should reverse all debits if any step fails.
```

**Expected Output:** (Complex SAGA with parallel execution + rollback)

### Best Practices

#### SAGA Design Principles

**The agent follows:**
- ✅ Orchestration pattern (centralized control)
- ✅ Sequential execution (not parallel, for simplicity)
- ✅ Compensation in reverse order
- ✅ Idempotency (same request = same result)
- ✅ State persistence after each step
- ✅ Detailed logging
- ✅ Manual intervention alerts on compensation failure
- ✅ Timeout handling

#### State Transitions

```
PENDING → VALIDATING → VALIDATED
  ↓
DEBIT_PENDING → DEBIT_COMPLETED
  ↓
CREDIT_PENDING → CREDIT_COMPLETED
  ↓
COMPLETED ✅

Any failure:
  ↓
COMPENSATING → COMPENSATED ↩️
  ↓
FAILED ❌
```

---

## Summary

Code Generation agents produce production-ready code:

- **EntityDesignAgent:** JPA entities + Liquibase migrations + repositories
- **APIDesignAgent:** REST controllers + DTOs + validation + error handling
- **SAGAImplementationAgent:** SAGA orchestrators + steps + compensation

**(Continued in next file due to length)**

**Next:** See agents 4-5 in this file (EventDrivenAgent, SecurityImplementationAgent) - to be added based on token limits.

For now, continuing with other categories...

**Next Category:** [Testing Agents →](./03-testing.md)
