package com.banking.swift.repository;

import com.banking.swift.model.ChargeType;
import com.banking.swift.model.SwiftTransfer;
import com.banking.swift.model.SwiftTransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Swift Transfer Repository Database Tests")
class SwiftTransferRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("banking_swift_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SwiftTransferRepository swiftTransferRepository;

    private SwiftTransfer sampleTransfer1;
    private SwiftTransfer sampleTransfer2;

    @BeforeEach
    void setUp() {
        swiftTransferRepository.deleteAll();

        sampleTransfer1 = SwiftTransfer.builder()
                .transactionReference("SWFT1A2B3C4D5E6F")
                .internalAccountId("ACC123456")
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("USD")
                .amount(new BigDecimal("10000.00"))
                .orderingCustomerName("John Doe")
                .orderingCustomerAccount("US1234567890")
                .senderBic("BNPAFRPPXXX")
                .senderName("BNP Paribas")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryBankName("Deutsche Bank AG")
                .beneficiaryName("Max Mustermann")
                .beneficiaryAccount("DE89370400440532013000")
                .remittanceInfo("INVOICE 12345")
                .chargeType(ChargeType.SHA)
                .status(SwiftTransferStatus.PENDING)
                .build();

        sampleTransfer2 = SwiftTransfer.builder()
                .transactionReference("SWFT9Z8Y7X6W5V4U")
                .internalAccountId("ACC789012")
                .valueDate(LocalDate.of(2026, 1, 16))
                .currency("EUR")
                .amount(new BigDecimal("5000.00"))
                .orderingCustomerName("Jane Smith")
                .orderingCustomerAccount("FR7630006000011234567890189")
                .senderBic("HSBCGB2LXXX")
                .senderName("HSBC Bank")
                .beneficiaryBankBic("CITIUS33XXX")
                .beneficiaryBankName("Citibank")
                .beneficiaryName("Robert Johnson")
                .beneficiaryAccount("US1234567890")
                .remittanceInfo("PAYMENT 67890")
                .chargeType(ChargeType.OUR)
                .status(SwiftTransferStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve SWIFT transfer")
    void shouldSaveAndRetrieveSwiftTransfer() {
        // When
        SwiftTransfer saved = swiftTransferRepository.save(sampleTransfer1);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTransactionReference()).isEqualTo("SWFT1A2B3C4D5E6F");
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<SwiftTransfer> retrieved = swiftTransferRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTransactionReference()).isEqualTo("SWFT1A2B3C4D5E6F");
    }

    @Test
    @DisplayName("Should find transfer by transaction reference")
    void shouldFindTransferByTransactionReference() {
        // Given
        swiftTransferRepository.save(sampleTransfer1);

        // When
        Optional<SwiftTransfer> found = swiftTransferRepository
                .findByTransactionReference("SWFT1A2B3C4D5E6F");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTransactionReference()).isEqualTo("SWFT1A2B3C4D5E6F");
        assertThat(found.get().getInternalAccountId()).isEqualTo("ACC123456");
    }

    @Test
    @DisplayName("Should return empty when transaction reference not found")
    void shouldReturnEmptyWhenTransactionReferenceNotFound() {
        // When
        Optional<SwiftTransfer> found = swiftTransferRepository
                .findByTransactionReference("SWFTNONEXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find transfers by internal account ID")
    void shouldFindTransfersByInternalAccountId() {
        // Given
        swiftTransferRepository.save(sampleTransfer1);
        SwiftTransfer anotherTransfer = SwiftTransfer.builder()
                .transactionReference("SWFTTEST12345678")
                .internalAccountId("ACC123456") // Same account
                .valueDate(LocalDate.now())
                .currency("USD")
                .amount(new BigDecimal("2000.00"))
                .orderingCustomerName("Test")
                .senderBic("BNPAFRPPXXX")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryName("Test Beneficiary")
                .beneficiaryAccount("DE89370400440532013000")
                .chargeType(ChargeType.SHA)
                .status(SwiftTransferStatus.PENDING)
                .build();
        swiftTransferRepository.save(anotherTransfer);

        // When
        List<SwiftTransfer> transfers = swiftTransferRepository
                .findByInternalAccountId("ACC123456");

        // Then
        assertThat(transfers).hasSize(2);
        assertThat(transfers).extracting(SwiftTransfer::getInternalAccountId)
                .containsOnly("ACC123456");
    }

    @Test
    @DisplayName("Should find transfers by status")
    void shouldFindTransfersByStatus() {
        // Given
        swiftTransferRepository.save(sampleTransfer1); // PENDING
        swiftTransferRepository.save(sampleTransfer2); // COMPLETED

        // When
        Page<SwiftTransfer> pendingTransfers = swiftTransferRepository
                .findByStatus(SwiftTransferStatus.PENDING, PageRequest.of(0, 10));

        // Then
        assertThat(pendingTransfers.getContent()).hasSize(1);
        assertThat(pendingTransfers.getContent().get(0).getStatus())
                .isEqualTo(SwiftTransferStatus.PENDING);
    }

    @Test
    @DisplayName("Should count transfers by status")
    void shouldCountTransfersByStatus() {
        // Given
        swiftTransferRepository.save(sampleTransfer1); // PENDING
        swiftTransferRepository.save(sampleTransfer2); // COMPLETED

        // When
        long pendingCount = swiftTransferRepository.countByStatus(SwiftTransferStatus.PENDING);
        long completedCount = swiftTransferRepository.countByStatus(SwiftTransferStatus.COMPLETED);
        long failedCount = swiftTransferRepository.countByStatus(SwiftTransferStatus.FAILED);

        // Then
        assertThat(pendingCount).isEqualTo(1);
        assertThat(completedCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should sum completed transfers after date")
    void shouldSumCompletedTransfersAfterDate() {
        // Given
        sampleTransfer2.setCompletedAt(LocalDateTime.now());
        swiftTransferRepository.save(sampleTransfer2); // 5000.00 EUR

        SwiftTransfer anotherCompleted = SwiftTransfer.builder()
                .transactionReference("SWFTCOMPLETED123")
                .internalAccountId("ACC999999")
                .valueDate(LocalDate.now())
                .currency("EUR")
                .amount(new BigDecimal("3000.00"))
                .orderingCustomerName("Test")
                .senderBic("BNPAFRPPXXX")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryName("Test")
                .beneficiaryAccount("DE89370400440532013000")
                .chargeType(ChargeType.SHA)
                .status(SwiftTransferStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        swiftTransferRepository.save(anotherCompleted); // 3000.00 EUR

        // When
        BigDecimal totalVolume = swiftTransferRepository
                .sumCompletedTransfersAfterDate(LocalDateTime.now().minusDays(1));

        // Then
        assertThat(totalVolume).isNotNull();
        assertThat(totalVolume).isEqualByComparingTo(new BigDecimal("8000.00")); // 5000 + 3000
    }

    @Test
    @DisplayName("Should return null when no completed transfers exist")
    void shouldReturnNullWhenNoCompletedTransfersExist() {
        // Given
        swiftTransferRepository.save(sampleTransfer1); // PENDING only

        // When
        BigDecimal totalVolume = swiftTransferRepository
                .sumCompletedTransfersAfterDate(LocalDateTime.now().minusDays(1));

        // Then
        assertThat(totalVolume).isNull();
    }

    @Test
    @DisplayName("Should find transfers by sender BIC")
    void shouldFindTransfersBySenderBic() {
        // Given
        swiftTransferRepository.save(sampleTransfer1); // BNPAFRPPXXX
        swiftTransferRepository.save(sampleTransfer2); // HSBCGB2LXXX

        // When
        List<SwiftTransfer> bnpTransfers = swiftTransferRepository
                .findBySenderBic("BNPAFRPPXXX");

        // Then
        assertThat(bnpTransfers).hasSize(1);
        assertThat(bnpTransfers.get(0).getSenderBic()).isEqualTo("BNPAFRPPXXX");
    }

    @Test
    @DisplayName("Should find transfers by beneficiary bank BIC")
    void shouldFindTransfersByBeneficiaryBankBic() {
        // Given
        swiftTransferRepository.save(sampleTransfer1); // DEUTDEFFXXX
        swiftTransferRepository.save(sampleTransfer2); // CITIUS33XXX

        // When
        List<SwiftTransfer> deutscheBankTransfers = swiftTransferRepository
                .findByBeneficiaryBankBic("DEUTDEFFXXX");

        // Then
        assertThat(deutscheBankTransfers).hasSize(1);
        assertThat(deutscheBankTransfers.get(0).getBeneficiaryBankBic())
                .isEqualTo("DEUTDEFFXXX");
    }

    @Test
    @DisplayName("Should find transfers by value date range")
    void shouldFindTransfersByValueDateRange() {
        // Given
        swiftTransferRepository.save(sampleTransfer1); // 2026-01-15
        swiftTransferRepository.save(sampleTransfer2); // 2026-01-16

        // When
        List<SwiftTransfer> transfers = swiftTransferRepository
                .findByValueDateBetween(
                        LocalDate.of(2026, 1, 15),
                        LocalDate.of(2026, 1, 15)
                );

        // Then
        assertThat(transfers).hasSize(1);
        assertThat(transfers.get(0).getValueDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("Should update transfer status")
    void shouldUpdateTransferStatus() {
        // Given
        SwiftTransfer saved = swiftTransferRepository.save(sampleTransfer1);
        assertThat(saved.getStatus()).isEqualTo(SwiftTransferStatus.PENDING);

        // When
        saved.setStatus(SwiftTransferStatus.COMPLETED);
        saved.setCompletedAt(LocalDateTime.now());
        SwiftTransfer updated = swiftTransferRepository.save(saved);

        // Then
        assertThat(updated.getStatus()).isEqualTo(SwiftTransferStatus.COMPLETED);
        assertThat(updated.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete transfer")
    void shouldDeleteTransfer() {
        // Given
        SwiftTransfer saved = swiftTransferRepository.save(sampleTransfer1);
        Long id = saved.getId();

        // When
        swiftTransferRepository.deleteById(id);

        // Then
        Optional<SwiftTransfer> deleted = swiftTransferRepository.findById(id);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple currencies")
    void shouldHandleMultipleCurrencies() {
        // Given
        swiftTransferRepository.save(sampleTransfer1); // USD
        swiftTransferRepository.save(sampleTransfer2); // EUR

        SwiftTransfer gbpTransfer = SwiftTransfer.builder()
                .transactionReference("SWFTGBP123456789")
                .internalAccountId("ACC555555")
                .valueDate(LocalDate.now())
                .currency("GBP")
                .amount(new BigDecimal("8000.00"))
                .orderingCustomerName("Test")
                .senderBic("BNPAFRPPXXX")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryName("Test")
                .beneficiaryAccount("GB29NWBK60161331926819")
                .chargeType(ChargeType.SHA)
                .status(SwiftTransferStatus.PENDING)
                .build();
        swiftTransferRepository.save(gbpTransfer);

        // When
        List<SwiftTransfer> allTransfers = swiftTransferRepository.findAll();

        // Then
        assertThat(allTransfers).hasSize(3);
        assertThat(allTransfers).extracting(SwiftTransfer::getCurrency)
                .containsExactlyInAnyOrder("USD", "EUR", "GBP");
    }

    @Test
    @DisplayName("Should persist all SWIFT transfer fields correctly")
    void shouldPersistAllSwiftTransferFieldsCorrectly() {
        // Given
        sampleTransfer1.setCorrespondentBic("CHASUS33XXX");
        sampleTransfer1.setCorrespondentName("JP Morgan Chase Bank");
        sampleTransfer1.setCorrespondentAccount("NOSTRO123456");
        sampleTransfer1.setFixedFee(new BigDecimal("25.00"));
        sampleTransfer1.setPercentageFee(new BigDecimal("10.00"));
        sampleTransfer1.setTotalFee(new BigDecimal("35.00"));
        sampleTransfer1.setMt103Message("MT103_MESSAGE_CONTENT");

        // When
        SwiftTransfer saved = swiftTransferRepository.save(sampleTransfer1);

        // Then
        SwiftTransfer retrieved = swiftTransferRepository.findById(saved.getId()).orElseThrow();
        assertThat(retrieved.getCorrespondentBic()).isEqualTo("CHASUS33XXX");
        assertThat(retrieved.getCorrespondentName()).isEqualTo("JP Morgan Chase Bank");
        assertThat(retrieved.getCorrespondentAccount()).isEqualTo("NOSTRO123456");
        assertThat(retrieved.getFixedFee()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(retrieved.getPercentageFee()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(retrieved.getTotalFee()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(retrieved.getMt103Message()).isEqualTo("MT103_MESSAGE_CONTENT");
    }

    @Test
    @DisplayName("Should maintain audit timestamps")
    void shouldMaintainAuditTimestamps() {
        // When
        SwiftTransfer saved = swiftTransferRepository.save(sampleTransfer1);

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        // When - Update
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();
        saved.setStatus(SwiftTransferStatus.PROCESSING);
        SwiftTransfer updated = swiftTransferRepository.save(saved);

        // Then
        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }
}
