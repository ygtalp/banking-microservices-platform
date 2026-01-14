package com.banking.transfer.service;

import com.banking.transfer.model.Transfer;
import com.banking.transfer.model.TransferStatus;
import com.banking.transfer.model.TransferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventPublisher Unit Tests")
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, Object>> sendFuture;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    private Transfer transfer;

    @BeforeEach
    void setUp() {
        transfer = Transfer.builder()
                .id(1L)
                .transferReference("TXF-123456789012")
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .description("Test transfer")
                .status(TransferStatus.COMPLETED)
                .transferType(TransferType.INTERNAL)
                .debitTransactionId("DEBIT-001")
                .creditTransactionId("CREDIT-001")
                .initiatedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        // Mock successful Kafka send
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(sendFuture);
        when(sendFuture.whenComplete(any())).thenReturn(sendFuture);
    }

    // ==================== TRANSFER INITIATED EVENT ====================

    @Test
    @DisplayName("Should publish transfer initiated event with correct topic")
    void shouldPublishTransferInitiatedEvent_WithCorrectTopic() {
        // Given
        transfer.setStatus(TransferStatus.PENDING);

        // When
        kafkaEventPublisher.publishTransferInitiated(transfer);

        // Then
        verify(kafkaTemplate, times(1)).send(
                eq("transfer.initiated"),
                eq("TXF-123456789012"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should publish transfer initiated event with correct data")
    void shouldPublishTransferInitiatedEvent_WithCorrectData() {
        // Given
        transfer.setStatus(TransferStatus.PENDING);
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferInitiated(transfer);

        // Then
        verify(kafkaTemplate).send(
                eq("transfer.initiated"),
                eq("TXF-123456789012"),
                eventCaptor.capture()
        );

        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event.get("transferReference")).isEqualTo("TXF-123456789012");
        assertThat(event.get("fromAccountNumber")).isEqualTo("ACC001");
        assertThat(event.get("toAccountNumber")).isEqualTo("ACC002");
        assertThat(event.get("amount")).isEqualTo(new BigDecimal("100.00"));
        assertThat(event.get("currency")).isEqualTo("TRY");
        assertThat(event.get("status")).isEqualTo("PENDING");
        assertThat(event.get("transferType")).isEqualTo("INTERNAL");
        assertThat(event.get("description")).isEqualTo("Test transfer");
    }

    // ==================== TRANSFER COMPLETED EVENT ====================

    @Test
    @DisplayName("Should publish transfer completed event with correct topic")
    void shouldPublishTransferCompletedEvent_WithCorrectTopic() {
        // When
        kafkaEventPublisher.publishTransferCompleted(transfer);

        // Then
        verify(kafkaTemplate, times(1)).send(
                eq("transfer.completed"),
                eq("TXF-123456789012"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should publish transfer completed event with transaction IDs")
    void shouldPublishTransferCompletedEvent_WithTransactionIds() {
        // Given
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferCompleted(transfer);

        // Then
        verify(kafkaTemplate).send(
                eq("transfer.completed"),
                anyString(),
                eventCaptor.capture()
        );

        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event.get("debitTransactionId")).isEqualTo("DEBIT-001");
        assertThat(event.get("creditTransactionId")).isEqualTo("CREDIT-001");
        assertThat(event.get("initiatedAt")).isNotNull();
        assertThat(event.get("completedAt")).isNotNull();
    }

    // ==================== TRANSFER FAILED EVENT ====================

    @Test
    @DisplayName("Should publish transfer failed event with correct topic")
    void shouldPublishTransferFailedEvent_WithCorrectTopic() {
        // Given
        transfer.setStatus(TransferStatus.FAILED);
        transfer.setFailureReason("Insufficient balance");

        // When
        kafkaEventPublisher.publishTransferFailed(transfer);

        // Then
        verify(kafkaTemplate, times(1)).send(
                eq("transfer.failed"),
                eq("TXF-123456789012"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should publish transfer failed event with failure reason")
    void shouldPublishTransferFailedEvent_WithFailureReason() {
        // Given
        transfer.setStatus(TransferStatus.FAILED);
        transfer.setFailureReason("Source account not found");
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferFailed(transfer);

        // Then
        verify(kafkaTemplate).send(
                eq("transfer.failed"),
                anyString(),
                eventCaptor.capture()
        );

        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event.get("failureReason")).isEqualTo("Source account not found");
        assertThat(event.get("status")).isEqualTo("FAILED");
    }

    // ==================== TRANSFER COMPENSATED EVENT ====================

    @Test
    @DisplayName("Should publish transfer compensated event with correct topic")
    void shouldPublishTransferCompensatedEvent_WithCorrectTopic() {
        // Given
        transfer.setStatus(TransferStatus.COMPENSATED);
        transfer.setFailureReason("Credit step failed, compensated");

        // When
        kafkaEventPublisher.publishTransferCompensated(transfer);

        // Then
        verify(kafkaTemplate, times(1)).send(
                eq("transfer.compensated"),
                eq("TXF-123456789012"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should publish transfer compensated event with failure reason")
    void shouldPublishTransferCompensatedEvent_WithFailureReason() {
        // Given
        transfer.setStatus(TransferStatus.COMPENSATED);
        transfer.setFailureReason("Destination account not active");
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferCompensated(transfer);

        // Then
        verify(kafkaTemplate).send(
                eq("transfer.compensated"),
                anyString(),
                eventCaptor.capture()
        );

        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event.get("failureReason")).isEqualTo("Destination account not active");
        assertThat(event.get("status")).isEqualTo("COMPENSATED");
    }

    // ==================== MESSAGE KEY ====================

    @Test
    @DisplayName("Should use transfer reference as message key for partitioning")
    void shouldUseTransferReferenceAsMessageKey() {
        // When
        kafkaEventPublisher.publishTransferInitiated(transfer);
        kafkaEventPublisher.publishTransferCompleted(transfer);
        kafkaEventPublisher.publishTransferFailed(transfer);
        kafkaEventPublisher.publishTransferCompensated(transfer);

        // Then - All 4 events should use the same key
        verify(kafkaTemplate, times(4)).send(
                anyString(),
                eq("TXF-123456789012"),
                any(Map.class)
        );
    }

    // ==================== EVENT CONTENT VALIDATION ====================

    @Test
    @DisplayName("Should include all transfer fields in event")
    void shouldIncludeAllTransferFieldsInEvent() {
        // Given
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferCompleted(transfer);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event).containsKeys(
                "transferReference",
                "fromAccountNumber",
                "toAccountNumber",
                "amount",
                "currency",
                "status",
                "transferType",
                "description",
                "initiatedAt",
                "completedAt",
                "debitTransactionId",
                "creditTransactionId"
        );
    }

    @Test
    @DisplayName("Should include null values for optional fields")
    void shouldIncludeNullValuesForOptionalFields() {
        // Given
        transfer.setDescription(null);
        transfer.setCompletedAt(null);
        transfer.setDebitTransactionId(null);
        transfer.setCreditTransactionId(null);
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferInitiated(transfer);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event.get("description")).isNull();
        assertThat(event.get("completedAt")).isNull();
        assertThat(event.get("debitTransactionId")).isNull();
        assertThat(event.get("creditTransactionId")).isNull();
    }

    // ==================== DIFFERENT CURRENCIES ====================

    @Test
    @DisplayName("Should publish events for USD transfers")
    void shouldPublishEventsForUSDTransfers() {
        // Given
        transfer.setCurrency("USD");
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferCompleted(transfer);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().get("currency")).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should publish events for EUR transfers")
    void shouldPublishEventsForEURTransfers() {
        // Given
        transfer.setCurrency("EUR");
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferCompleted(transfer);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().get("currency")).isEqualTo("EUR");
    }

    // ==================== DIFFERENT TRANSFER TYPES ====================

    @Test
    @DisplayName("Should publish events for external transfers")
    void shouldPublishEventsForExternalTransfers() {
        // Given
        transfer.setTransferType(TransferType.EXTERNAL);
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferCompleted(transfer);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().get("transferType")).isEqualTo("EXTERNAL");
    }

    // ==================== LARGE AMOUNTS ====================

    @Test
    @DisplayName("Should publish events with large transfer amounts")
    void shouldPublishEventsWithLargeTransferAmounts() {
        // Given
        BigDecimal largeAmount = new BigDecimal("9999999.99");
        transfer.setAmount(largeAmount);
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferCompleted(transfer);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().get("amount")).isEqualTo(largeAmount);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle transfers with long descriptions")
    void shouldHandleTransfersWithLongDescriptions() {
        // Given
        String longDescription = "A".repeat(500);
        transfer.setDescription(longDescription);
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferCompleted(transfer);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().get("description")).isEqualTo(longDescription);
    }

    @Test
    @DisplayName("Should convert enum values to strings in events")
    void shouldConvertEnumValuesToStringsInEvents() {
        // Given
        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);

        // When
        kafkaEventPublisher.publishTransferCompleted(transfer);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());

        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event.get("status")).isInstanceOf(String.class);
        assertThat(event.get("transferType")).isInstanceOf(String.class);
        assertThat(event.get("status")).isEqualTo("COMPLETED");
        assertThat(event.get("transferType")).isEqualTo("INTERNAL");
    }

    @Test
    @DisplayName("Should publish all four event types for different scenarios")
    void shouldPublishAllFourEventTypes() {
        // When
        kafkaEventPublisher.publishTransferInitiated(transfer);
        kafkaEventPublisher.publishTransferCompleted(transfer);
        kafkaEventPublisher.publishTransferFailed(transfer);
        kafkaEventPublisher.publishTransferCompensated(transfer);

        // Then
        verify(kafkaTemplate, times(1)).send(eq("transfer.initiated"), anyString(), any());
        verify(kafkaTemplate, times(1)).send(eq("transfer.completed"), anyString(), any());
        verify(kafkaTemplate, times(1)).send(eq("transfer.failed"), anyString(), any());
        verify(kafkaTemplate, times(1)).send(eq("transfer.compensated"), anyString(), any());
    }
}
