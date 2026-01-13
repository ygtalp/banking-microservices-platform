package com.banking.swift.service;

import com.banking.swift.model.ChargeType;
import com.banking.swift.model.SwiftTransfer;
import com.banking.swift.model.SwiftTransferStatus;
import com.banking.swift.repository.SwiftTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Swift Transfer Service Tests")
class SwiftTransferServiceTest {

    @Mock
    private SwiftTransferRepository swiftTransferRepository;

    @Mock
    private Mt103MessageGenerator mt103MessageGenerator;

    @Mock
    private BicValidationService bicValidationService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SwiftTransferService swiftTransferService;

    private SwiftTransfer sampleTransfer;

    @BeforeEach
    void setUp() {
        // Set up configuration properties using reflection
        ReflectionTestUtils.setField(swiftTransferService, "fixedFee", new BigDecimal("25.00"));
        ReflectionTestUtils.setField(swiftTransferService, "percentageFee", new BigDecimal("0.001"));
        ReflectionTestUtils.setField(swiftTransferService, "correspondentBic", "CHASUS33XXX");
        ReflectionTestUtils.setField(swiftTransferService, "correspondentName", "JP Morgan Chase Bank");

        // Create sample transfer
        sampleTransfer = SwiftTransfer.builder()
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
                .build();
    }

    @Test
    @DisplayName("Should create SWIFT transfer successfully")
    void shouldCreateSwiftTransferSuccessfully() {
        // Given
        when(bicValidationService.isValidBic("BNPAFRPPXXX")).thenReturn(true);
        when(bicValidationService.isValidBic("DEUTDEFFXXX")).thenReturn(true);
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        SwiftTransfer result = swiftTransferService.createSwiftTransfer(sampleTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransactionReference()).isNotNull();
        assertThat(result.getTransactionReference()).startsWith("SWFT");
        assertThat(result.getStatus()).isEqualTo(SwiftTransferStatus.PENDING);
        assertThat(result.getCorrespondentBic()).isEqualTo("CHASUS33XXX");
        assertThat(result.getCorrespondentName()).isEqualTo("JP Morgan Chase Bank");
        assertThat(result.getFixedFee()).isNotNull();
        assertThat(result.getPercentageFee()).isNotNull();

        verify(bicValidationService).isValidBic("BNPAFRPPXXX");
        verify(bicValidationService).isValidBic("DEUTDEFFXXX");
        verify(swiftTransferRepository).save(any(SwiftTransfer.class));
    }

    @Test
    @DisplayName("Should set value date to next business day if not provided")
    void shouldSetValueDateToNextBusinessDayIfNotProvided() {
        // Given
        sampleTransfer.setValueDate(null);
        when(bicValidationService.isValidBic(any())).thenReturn(true);
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        SwiftTransfer result = swiftTransferService.createSwiftTransfer(sampleTransfer);

        // Then
        assertThat(result.getValueDate()).isNotNull();
        assertThat(result.getValueDate()).isAfter(LocalDate.now());
    }

    @Test
    @DisplayName("Should throw exception when sender BIC is invalid")
    void shouldThrowExceptionWhenSenderBicIsInvalid() {
        // Given
        when(bicValidationService.isValidBic("BNPAFRPPXXX")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> swiftTransferService.createSwiftTransfer(sampleTransfer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid sender BIC");

        verify(swiftTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when beneficiary BIC is invalid")
    void shouldThrowExceptionWhenBeneficiaryBicIsInvalid() {
        // Given
        when(bicValidationService.isValidBic("BNPAFRPPXXX")).thenReturn(true);
        when(bicValidationService.isValidBic("DEUTDEFFXXX")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> swiftTransferService.createSwiftTransfer(sampleTransfer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid beneficiary bank BIC");

        verify(swiftTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should generate unique transaction reference")
    void shouldGenerateUniqueTransactionReference() {
        // Given
        when(bicValidationService.isValidBic(any())).thenReturn(true);
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        SwiftTransfer result1 = swiftTransferService.createSwiftTransfer(sampleTransfer);
        SwiftTransfer result2 = swiftTransferService.createSwiftTransfer(sampleTransfer);

        // Then
        assertThat(result1.getTransactionReference()).isNotEqualTo(result2.getTransactionReference());
        assertThat(result1.getTransactionReference()).matches("SWFT[A-Z0-9]{12}");
        assertThat(result2.getTransactionReference()).matches("SWFT[A-Z0-9]{12}");
    }

    @Test
    @DisplayName("Should process transfer successfully")
    void shouldProcessTransferSuccessfully() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        sampleTransfer.setTransactionReference(reference);
        sampleTransfer.setStatus(SwiftTransferStatus.PENDING);

        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.of(sampleTransfer));
        when(mt103MessageGenerator.generateMt103Message(any())).thenReturn("MT103_MESSAGE");
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        SwiftTransfer result = swiftTransferService.processTransfer(reference);

        // Then
        assertThat(result.getStatus()).isEqualTo(SwiftTransferStatus.SUBMITTED);
        assertThat(result.getMt103Message()).isEqualTo("MT103_MESSAGE");
        assertThat(result.getSettlementDate()).isEqualTo(result.getValueDate());

        verify(mt103MessageGenerator).generateMt103Message(any());
        verify(kafkaTemplate).send(eq("swift.transfer.submitted"), eq(reference), any());
    }

    @Test
    @DisplayName("Should throw exception when processing non-pending transfer")
    void shouldThrowExceptionWhenProcessingNonPendingTransfer() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        sampleTransfer.setTransactionReference(reference);
        sampleTransfer.setStatus(SwiftTransferStatus.COMPLETED);

        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.of(sampleTransfer));

        // When & Then
        assertThatThrownBy(() -> swiftTransferService.processTransfer(reference))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not in PENDING status");

        verify(mt103MessageGenerator, never()).generateMt103Message(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when processing non-existent transfer")
    void shouldThrowExceptionWhenProcessingNonExistentTransfer() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> swiftTransferService.processTransfer(reference))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transfer not found");
    }

    @Test
    @DisplayName("Should complete transfer successfully")
    void shouldCompleteTransferSuccessfully() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        String settlementReference = "SETTLE123";
        sampleTransfer.setTransactionReference(reference);
        sampleTransfer.setStatus(SwiftTransferStatus.SUBMITTED);

        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.of(sampleTransfer));
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        SwiftTransfer result = swiftTransferService.completeTransfer(reference, settlementReference);

        // Then
        assertThat(result.getStatus()).isEqualTo(SwiftTransferStatus.COMPLETED);
        assertThat(result.getSettlementReference()).isEqualTo(settlementReference);

        verify(kafkaTemplate).send(eq("swift.transfer.completed"), eq(reference), any());
    }

    @Test
    @DisplayName("Should fail transfer successfully")
    void shouldFailTransferSuccessfully() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        String reason = "Compliance check failed";
        sampleTransfer.setTransactionReference(reference);
        sampleTransfer.setStatus(SwiftTransferStatus.PROCESSING);

        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.of(sampleTransfer));
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        SwiftTransfer result = swiftTransferService.failTransfer(reference, reason);

        // Then
        assertThat(result.getStatus()).isEqualTo(SwiftTransferStatus.FAILED);
        assertThat(result.getStatusReason()).isEqualTo(reason);

        verify(kafkaTemplate).send(eq("swift.transfer.failed"), eq(reference), any());
    }

    @Test
    @DisplayName("Should get transfer by reference")
    void shouldGetTransferByReference() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        sampleTransfer.setTransactionReference(reference);

        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.of(sampleTransfer));

        // When
        SwiftTransfer result = swiftTransferService.getTransferByReference(reference);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTransactionReference()).isEqualTo(reference);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent transfer by reference")
    void shouldThrowExceptionWhenGettingNonExistentTransferByReference() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> swiftTransferService.getTransferByReference(reference))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transfer not found");
    }

    @Test
    @DisplayName("Should get transfers by account")
    void shouldGetTransfersByAccount() {
        // Given
        String accountId = "ACC123456";
        List<SwiftTransfer> transfers = Arrays.asList(sampleTransfer, sampleTransfer);

        when(swiftTransferRepository.findByInternalAccountId(accountId))
                .thenReturn(transfers);

        // When
        List<SwiftTransfer> result = swiftTransferService.getTransfersByAccount(accountId);

        // Then
        assertThat(result).hasSize(2);
        verify(swiftTransferRepository).findByInternalAccountId(accountId);
    }

    @Test
    @DisplayName("Should get transfers by status with pagination")
    void shouldGetTransfersByStatusWithPagination() {
        // Given
        SwiftTransferStatus status = SwiftTransferStatus.PENDING;
        Pageable pageable = PageRequest.of(0, 10);
        Page<SwiftTransfer> page = new PageImpl<>(Arrays.asList(sampleTransfer));

        when(swiftTransferRepository.findByStatus(status, pageable))
                .thenReturn(page);

        // When
        Page<SwiftTransfer> result = swiftTransferService.getTransfersByStatus(status, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(swiftTransferRepository).findByStatus(status, pageable);
    }

    @Test
    @DisplayName("Should calculate statistics correctly")
    void shouldCalculateStatisticsCorrectly() {
        // Given
        when(swiftTransferRepository.countByStatus(SwiftTransferStatus.PENDING)).thenReturn(5L);
        when(swiftTransferRepository.countByStatus(SwiftTransferStatus.PROCESSING)).thenReturn(3L);
        when(swiftTransferRepository.countByStatus(SwiftTransferStatus.COMPLETED)).thenReturn(100L);
        when(swiftTransferRepository.countByStatus(SwiftTransferStatus.FAILED)).thenReturn(2L);
        when(swiftTransferRepository.sumCompletedTransfersAfterDate(any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500000.00"));

        // When
        SwiftTransferService.SwiftTransferStatistics stats = swiftTransferService.getStatistics();

        // Then
        assertThat(stats.getPendingCount()).isEqualTo(5L);
        assertThat(stats.getProcessingCount()).isEqualTo(3L);
        assertThat(stats.getCompletedCount()).isEqualTo(100L);
        assertThat(stats.getFailedCount()).isEqualTo(2L);
        assertThat(stats.getTotalVolumeL30D()).isEqualByComparingTo(new BigDecimal("500000.00"));
    }

    @Test
    @DisplayName("Should handle null total volume in statistics")
    void shouldHandleNullTotalVolumeInStatistics() {
        // Given
        when(swiftTransferRepository.countByStatus(any())).thenReturn(0L);
        when(swiftTransferRepository.sumCompletedTransfersAfterDate(any(LocalDateTime.class)))
                .thenReturn(null);

        // When
        SwiftTransferService.SwiftTransferStatistics stats = swiftTransferService.getStatistics();

        // Then
        assertThat(stats.getTotalVolumeL30D()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should publish Kafka event when transfer is submitted")
    void shouldPublishKafkaEventWhenTransferIsSubmitted() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        sampleTransfer.setTransactionReference(reference);
        sampleTransfer.setStatus(SwiftTransferStatus.PENDING);

        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.of(sampleTransfer));
        when(mt103MessageGenerator.generateMt103Message(any())).thenReturn("MT103_MESSAGE");
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        swiftTransferService.processTransfer(reference);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("swift.transfer.submitted");
        assertThat(keyCaptor.getValue()).isEqualTo(reference);
        assertThat(valueCaptor.getValue()).isInstanceOf(SwiftTransfer.class);
    }

    @Test
    @DisplayName("Should publish Kafka event when transfer is completed")
    void shouldPublishKafkaEventWhenTransferIsCompleted() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        sampleTransfer.setTransactionReference(reference);

        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.of(sampleTransfer));
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        swiftTransferService.completeTransfer(reference, "SETTLE123");

        // Then
        verify(kafkaTemplate).send(eq("swift.transfer.completed"), eq(reference), any(SwiftTransfer.class));
    }

    @Test
    @DisplayName("Should publish Kafka event when transfer is failed")
    void shouldPublishKafkaEventWhenTransferIsFailed() {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        sampleTransfer.setTransactionReference(reference);

        when(swiftTransferRepository.findByTransactionReference(reference))
                .thenReturn(Optional.of(sampleTransfer));
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        swiftTransferService.failTransfer(reference, "Test failure");

        // Then
        verify(kafkaTemplate).send(eq("swift.transfer.failed"), eq(reference), any(SwiftTransfer.class));
    }

    @Test
    @DisplayName("Should calculate fees correctly")
    void shouldCalculateFeesCorrectly() {
        // Given
        when(bicValidationService.isValidBic(any())).thenReturn(true);
        when(swiftTransferRepository.save(any(SwiftTransfer.class))).thenAnswer(i -> i.getArgument(0));

        // When
        SwiftTransfer result = swiftTransferService.createSwiftTransfer(sampleTransfer);

        // Then
        assertThat(result.getFixedFee()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(result.getPercentageFee()).isEqualByComparingTo(new BigDecimal("10.00")); // 0.001 * 10000
        assertThat(result.getTotalFee()).isEqualByComparingTo(new BigDecimal("35.00"));
    }
}
