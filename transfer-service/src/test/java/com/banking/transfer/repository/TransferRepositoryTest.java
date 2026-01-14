package com.banking.transfer.repository;


import com.banking.transfer.model.Transfer;
import com.banking.transfer.model.TransferStatus;
import com.banking.transfer.model.TransferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("TransferRepository Database Tests")
class TransferRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("transfer_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransferRepository transferRepository;

    private Transfer transfer1;
    private Transfer transfer2;
    private Transfer transfer3;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();

        transfer1 = Transfer.builder()
                .transferReference("TXF-111111111111")
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .description("Test transfer 1")
                .status(TransferStatus.COMPLETED)
                .transferType(TransferType.INTERNAL)
                .idempotencyKey("IDEMPOTENCY-001")
                .debitTransactionId("DEBIT-001")
                .creditTransactionId("CREDIT-001")
                .initiatedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        transfer2 = Transfer.builder()
                .transferReference("TXF-222222222222")
                .fromAccountNumber("ACC002")
                .toAccountNumber("ACC003")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .description("Test transfer 2")
                .status(TransferStatus.PENDING)
                .transferType(TransferType.EXTERNAL)
                .idempotencyKey("IDEMPOTENCY-002")
                .initiatedAt(LocalDateTime.now())
                .build();

        transfer3 = Transfer.builder()
                .transferReference("TXF-333333333333")
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC003")
                .amount(new BigDecimal("300.00"))
                .currency("EUR")
                .description("Test transfer 3")
                .status(TransferStatus.FAILED)
                .transferType(TransferType.INTERNAL)
                .idempotencyKey("IDEMPOTENCY-003")
                .failureReason("Insufficient balance")
                .initiatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== BASIC CRUD OPERATIONS ====================

    @Test
    @DisplayName("Should save transfer successfully")
    void shouldSaveTransferSuccessfully() {
        // When
        Transfer saved = transferRepository.save(transfer1);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTransferReference()).isEqualTo("TXF-111111111111");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find transfer by ID")
    void shouldFindTransferById() {
        // Given
        Transfer saved = transferRepository.save(transfer1);

        // When
        Optional<Transfer> found = transferRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTransferReference()).isEqualTo("TXF-111111111111");
    }

    @Test
    @DisplayName("Should update transfer")
    void shouldUpdateTransfer() {
        // Given
        Transfer saved = transferRepository.save(transfer1);
        saved.setStatus(TransferStatus.COMPLETED);
        saved.setCompletedAt(LocalDateTime.now());

        // When
        Transfer updated = transferRepository.save(saved);

        // Then
        assertThat(updated.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getUpdatedAt()).isAfter(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Should delete transfer")
    void shouldDeleteTransfer() {
        // Given
        Transfer saved = transferRepository.save(transfer1);

        // When
        transferRepository.deleteById(saved.getId());

        // Then
        Optional<Transfer> found = transferRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    // ==================== FIND BY TRANSFER REFERENCE ====================

    @Test
    @DisplayName("Should find transfer by transfer reference")
    void shouldFindTransferByTransferReference() {
        // Given
        transferRepository.save(transfer1);

        // When
        Optional<Transfer> found = transferRepository.findByTransferReference("TXF-111111111111");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should return empty when transfer reference not found")
    void shouldReturnEmpty_WhenTransferReferenceNotFound() {
        // When
        Optional<Transfer> found = transferRepository.findByTransferReference("TXF-NOTFOUND");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should enforce unique constraint on transfer reference")
    void shouldEnforceUniqueConstraintOnTransferReference() {
        // Given
        transferRepository.save(transfer1);

        Transfer duplicate = Transfer.builder()
                .transferReference("TXF-111111111111") // Same reference
                .fromAccountNumber("ACC999")
                .toAccountNumber("ACC998")
                .amount(new BigDecimal("50.00"))
                .currency("TRY")
                .status(TransferStatus.PENDING)
                .transferType(TransferType.INTERNAL)
                .idempotencyKey("IDEMPOTENCY-999")
                .build();

        // When & Then
        assertThatThrownBy(() -> transferRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ==================== FIND BY IDEMPOTENCY KEY ====================

    @Test
    @DisplayName("Should find transfer by idempotency key")
    void shouldFindTransferByIdempotencyKey() {
        // Given
        transferRepository.save(transfer1);

        // When
        Optional<Transfer> found = transferRepository.findByIdempotencyKey("IDEMPOTENCY-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTransferReference()).isEqualTo("TXF-111111111111");
    }

    @Test
    @DisplayName("Should check if idempotency key exists")
    void shouldCheckIfIdempotencyKeyExists() {
        // Given
        transferRepository.save(transfer1);

        // When
        boolean exists = transferRepository.existsByIdempotencyKey("IDEMPOTENCY-001");
        boolean notExists = transferRepository.existsByIdempotencyKey("NON-EXISTENT");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should enforce unique constraint on idempotency key")
    void shouldEnforceUniqueConstraintOnIdempotencyKey() {
        // Given
        transferRepository.save(transfer1);

        Transfer duplicate = Transfer.builder()
                .transferReference("TXF-999999999999")
                .fromAccountNumber("ACC999")
                .toAccountNumber("ACC998")
                .amount(new BigDecimal("50.00"))
                .currency("TRY")
                .status(TransferStatus.PENDING)
                .transferType(TransferType.INTERNAL)
                .idempotencyKey("IDEMPOTENCY-001") // Same idempotency key
                .build();

        // When & Then
        assertThatThrownBy(() -> transferRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ==================== FIND BY ACCOUNT NUMBER ====================

    @Test
    @DisplayName("Should find transfers from account ordered by created date")
    void shouldFindTransfersFromAccount_OrderedByCreatedDate() {
        // Given
        transferRepository.save(transfer1);
        transferRepository.save(transfer3); // Both from ACC001

        // When
        List<Transfer> transfers = transferRepository.findByFromAccountNumberOrderByCreatedAtDesc("ACC001");

        // Then
        assertThat(transfers).hasSize(2);
        assertThat(transfers).extracting(Transfer::getTransferReference)
                .containsExactly("TXF-333333333333", "TXF-111111111111");
    }

    @Test
    @DisplayName("Should find transfers to account ordered by created date")
    void shouldFindTransfersToAccount_OrderedByCreatedDate() {
        // Given
        transferRepository.save(transfer1);
        transferRepository.save(transfer2);
        transferRepository.save(transfer3); // All to ACC002 or ACC003

        // When
        List<Transfer> transfers = transferRepository.findByToAccountNumberOrderByCreatedAtDesc("ACC003");

        // Then
        assertThat(transfers).hasSize(2);
        assertThat(transfers.get(0).getToAccountNumber()).isEqualTo("ACC003");
        assertThat(transfers.get(1).getToAccountNumber()).isEqualTo("ACC003");
    }

    @Test
    @DisplayName("Should find transfers by account number (both from and to)")
    void shouldFindTransfersByAccountNumber_BothFromAndTo() {
        // Given
        transferRepository.save(transfer1); // from ACC001 to ACC002
        transferRepository.save(transfer2); // from ACC002 to ACC003
        transferRepository.save(transfer3); // from ACC001 to ACC003

        // When
        List<Transfer> transfers = transferRepository.findByAccountNumber("ACC002");

        // Then - ACC002 is in 2 transfers (from in one, to in one)
        assertThat(transfers).hasSize(2);
        assertThat(transfers).extracting(Transfer::getTransferReference)
                .contains("TXF-111111111111", "TXF-222222222222");
    }

    @Test
    @DisplayName("Should return empty list when no transfers found for account")
    void shouldReturnEmptyList_WhenNoTransfersFoundForAccount() {
        // Given
        transferRepository.save(transfer1);

        // When
        List<Transfer> transfers = transferRepository.findByAccountNumber("ACC999");

        // Then
        assertThat(transfers).isEmpty();
    }

    // ==================== FIND BY STATUS ====================

    @Test
    @DisplayName("Should find transfers by status list")
    void shouldFindTransfersByStatusList() {
        // Given
        transferRepository.save(transfer1); // COMPLETED
        transferRepository.save(transfer2); // PENDING
        transferRepository.save(transfer3); // FAILED

        // When
        List<Transfer> pendingOrFailed = transferRepository.findByStatusIn(
                Arrays.asList(TransferStatus.PENDING, TransferStatus.FAILED)
        );

        // Then
        assertThat(pendingOrFailed).hasSize(2);
        assertThat(pendingOrFailed).extracting(Transfer::getStatus)
                .containsExactlyInAnyOrder(TransferStatus.PENDING, TransferStatus.FAILED);
    }

    @Test
    @DisplayName("Should find stuck transfers older than threshold")
    void shouldFindStuckTransfers_OlderThanThreshold() {
        // Given
        Transfer stuckTransfer = Transfer.builder()
                .transferReference("TXF-444444444444")
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .status(TransferStatus.DEBIT_PENDING)
                .transferType(TransferType.INTERNAL)
                .build();
        stuckTransfer = transferRepository.save(stuckTransfer);

        // Manually set old timestamp
        stuckTransfer.setInitiatedAt(LocalDateTime.now().minusHours(2));
        transferRepository.save(stuckTransfer);

        // When
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<Transfer> stuckTransfers = transferRepository.findStuckTransfers(
                Arrays.asList(TransferStatus.DEBIT_PENDING, TransferStatus.CREDIT_PENDING),
                threshold
        );

        // Then
        assertThat(stuckTransfers).hasSize(1);
        assertThat(stuckTransfers.get(0).getTransferReference()).isEqualTo("TXF-444444444444");
    }

    // ==================== BIG DECIMAL PRECISION ====================

    @Test
    @DisplayName("Should preserve BigDecimal precision")
    void shouldPreserveBigDecimalPrecision() {
        // Given
        BigDecimal preciseAmount = new BigDecimal("123.45");
        transfer1.setAmount(preciseAmount);

        // When
        Transfer saved = transferRepository.save(transfer1);
        Transfer found = transferRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getAmount()).isEqualByComparingTo(preciseAmount);
        assertThat(found.getAmount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle large amounts")
    void shouldHandleLargeAmounts() {
        // Given
        BigDecimal largeAmount = new BigDecimal("99999999999999999.99");
        transfer1.setAmount(largeAmount);

        // When
        Transfer saved = transferRepository.save(transfer1);

        // Then
        assertThat(saved.getAmount()).isEqualByComparingTo(largeAmount);
    }

    @Test
    @DisplayName("Should handle zero amount")
    void shouldHandleZeroAmount() {
        // Given
        transfer1.setAmount(BigDecimal.ZERO);

        // When
        Transfer saved = transferRepository.save(transfer1);

        // Then
        assertThat(saved.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ==================== ENUM HANDLING ====================

    @Test
    @DisplayName("Should persist all transfer status values")
    void shouldPersistAllTransferStatusValues() {
        // Given
        transfer1.setStatus(TransferStatus.PENDING);
        transfer2.setStatus(TransferStatus.VALIDATING);
        transfer3.setStatus(TransferStatus.DEBIT_PENDING);

        // When
        transferRepository.save(transfer1);
        transferRepository.save(transfer2);
        transferRepository.save(transfer3);

        // Then
        assertThat(transferRepository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("Should persist all transfer type values")
    void shouldPersistAllTransferTypeValues() {
        // Given
        transfer1.setTransferType(TransferType.INTERNAL);
        transfer2.setTransferType(TransferType.EXTERNAL);

        // When
        transferRepository.save(transfer1);
        transferRepository.save(transfer2);

        // Then
        List<Transfer> all = transferRepository.findAll();
        assertThat(all).extracting(Transfer::getTransferType)
                .containsExactlyInAnyOrder(TransferType.INTERNAL, TransferType.EXTERNAL);
    }

    // ==================== TIMESTAMP AUTO-GENERATION ====================

    @Test
    @DisplayName("Should auto-generate createdAt timestamp")
    void shouldAutoGenerateCreatedAtTimestamp() {
        // When
        Transfer saved = transferRepository.save(transfer1);

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should auto-generate updatedAt timestamp")
    void shouldAutoGenerateUpdatedAtTimestamp() {
        // When
        Transfer saved = transferRepository.save(transfer1);

        // Then
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Should update updatedAt timestamp on modification")
    void shouldUpdateUpdatedAtTimestamp_OnModification() throws InterruptedException {
        // Given
        Transfer saved = transferRepository.save(transfer1);
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(10); // Ensure time difference

        // When
        saved.setStatus(TransferStatus.COMPLETED);
        Transfer updated = transferRepository.save(saved);

        // Then
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    // ==================== OPTIMISTIC LOCKING ====================

    @Test
    @DisplayName("Should initialize version field")
    void shouldInitializeVersionField() {
        // When
        Transfer saved = transferRepository.save(transfer1);

        // Then
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should increment version on update")
    void shouldIncrementVersion_OnUpdate() {
        // Given
        Transfer saved = transferRepository.save(transfer1);
        Long originalVersion = saved.getVersion();

        // When
        saved.setStatus(TransferStatus.COMPLETED);
        Transfer updated = transferRepository.save(saved);

        // Then
        assertThat(updated.getVersion()).isGreaterThan(originalVersion);
    }

    // ==================== HELPER METHODS ====================

    @Test
    @DisplayName("Should correctly identify completed transfers")
    void shouldCorrectlyIdentifyCompletedTransfers() {
        // Given
        transfer1.setStatus(TransferStatus.COMPLETED);
        transfer2.setStatus(TransferStatus.PENDING);

        // Then
        assertThat(transfer1.isCompleted()).isTrue();
        assertThat(transfer2.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify failed transfers")
    void shouldCorrectlyIdentifyFailedTransfers() {
        // Given
        transfer1.setStatus(TransferStatus.FAILED);
        transfer2.setStatus(TransferStatus.COMPLETED);

        // Then
        assertThat(transfer1.isFailed()).isTrue();
        assertThat(transfer2.isFailed()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify pending transfers")
    void shouldCorrectlyIdentifyPendingTransfers() {
        // Given
        transfer1.setStatus(TransferStatus.PENDING);
        transfer2.setStatus(TransferStatus.DEBIT_PENDING);
        transfer3.setStatus(TransferStatus.CREDIT_PENDING);
        Transfer transfer4 = Transfer.builder()
                .transferReference("TXF-444444444444")
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC002")
                .amount(BigDecimal.TEN)
                .currency("TRY")
                .status(TransferStatus.COMPLETED)
                .transferType(TransferType.INTERNAL)
                .build();

        // Then
        assertThat(transfer1.isPending()).isTrue();
        assertThat(transfer2.isPending()).isTrue();
        assertThat(transfer3.isPending()).isTrue();
        assertThat(transfer4.isPending()).isFalse();
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
        // Given
        transfer1.setDescription(null);
        transfer1.setFailureReason(null);
        transfer1.setIdempotencyKey(null);
        transfer1.setDebitTransactionId(null);
        transfer1.setCreditTransactionId(null);
        transfer1.setCompletedAt(null);

        // When
        Transfer saved = transferRepository.save(transfer1);

        // Then
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getFailureReason()).isNull();
        assertThat(saved.getIdempotencyKey()).isNull();
        assertThat(saved.getDebitTransactionId()).isNull();
        assertThat(saved.getCreditTransactionId()).isNull();
        assertThat(saved.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Should handle long descriptions")
    void shouldHandleLongDescriptions() {
        // Given
        String longDescription = "A".repeat(500);
        transfer1.setDescription(longDescription);

        // When
        Transfer saved = transferRepository.save(transfer1);

        // Then
        assertThat(saved.getDescription()).hasSize(500);
    }

    @Test
    @DisplayName("Should handle long failure reasons")
    void shouldHandleLongFailureReasons() {
        // Given
        String longFailureReason = "Error: " + "X".repeat(990);
        transfer1.setFailureReason(longFailureReason);

        // When
        Transfer saved = transferRepository.save(transfer1);

        // Then
        assertThat(saved.getFailureReason()).hasSize(997);
    }

    @Test
    @DisplayName("Should handle all currency types")
    void shouldHandleAllCurrencyTypes() {
        // Given
        transfer1.setCurrency("TRY");
        transfer2.setCurrency("USD");
        transfer3.setCurrency("EUR");

        // When
        transferRepository.saveAll(Arrays.asList(transfer1, transfer2, transfer3));

        // Then
        List<Transfer> all = transferRepository.findAll();
        assertThat(all).extracting(Transfer::getCurrency)
                .containsExactlyInAnyOrder("TRY", "USD", "EUR");
    }
}
