package com.banking.transfer.service;

import com.banking.transfer.dto.TransferRequest;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.exception.TransferNotFoundException;
import com.banking.transfer.model.Transfer;
import com.banking.transfer.model.TransferStatus;
import com.banking.transfer.model.TransferType;
import com.banking.transfer.repository.TransferRepository;
import com.banking.transfer.saga.TransferSagaOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService Unit Tests")
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferSagaOrchestrator sagaOrchestrator;

    @Mock
    private KafkaEventPublisher eventPublisher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TransferService transferService;

    private TransferRequest transferRequest;
    private Transfer transfer;

    @BeforeEach
    void setUp() {
        // Mock Redis operations (lenient to avoid UnnecessaryStubbingException)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Setup test data
        transferRequest = TransferRequest.builder()
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .description("Test transfer")
                .transferType(TransferType.INTERNAL)
                .idempotencyKey("test-idempotency-key")
                .build();

        transfer = Transfer.builder()
                .id(1L)
                .transferReference("TXF-123456789012")
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .description("Test transfer")
                .status(TransferStatus.PENDING)
                .transferType(TransferType.INTERNAL)
                .idempotencyKey("test-idempotency-key")
                .initiatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== INITIATE TRANSFER TESTS ====================

    @Test
    @DisplayName("Should initiate transfer successfully without idempotency key")
    void shouldInitiateTransferSuccessfully_WithoutIdempotencyKey() {
        // Given
        transferRequest.setIdempotencyKey(null);
        transfer.setIdempotencyKey(null);

        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer t = invocation.getArgument(0);
                    t.setStatus(TransferStatus.COMPLETED);
                    t.setCompletedAt(LocalDateTime.now());
                    return t;
                });

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.getTransferReference()).startsWith("TXF-");
        assertThat(response.getFromAccountNumber()).isEqualTo("ACC001");
        assertThat(response.getToAccountNumber()).isEqualTo("ACC002");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        verify(transferRepository, times(1)).save(any(Transfer.class));
        verify(eventPublisher, times(1)).publishTransferInitiated(any(Transfer.class));
        verify(eventPublisher, times(1)).publishTransferCompleted(any(Transfer.class));
        verify(sagaOrchestrator, times(1)).executeTransfer(any(Transfer.class));
        verify(valueOperations, never()).get(anyString());
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Should initiate transfer successfully with idempotency key")
    void shouldInitiateTransferSuccessfully_WithIdempotencyKey() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(transferRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer t = invocation.getArgument(0);
                    t.setStatus(TransferStatus.COMPLETED);
                    t.setCompletedAt(LocalDateTime.now());
                    return t;
                });

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransferStatus.COMPLETED);

        verify(valueOperations, times(1)).get("transfer:idempotency:test-idempotency-key");
        verify(valueOperations, times(1)).set(
                eq("transfer:idempotency:test-idempotency-key"),
                anyString(),
                eq(24L),
                eq(TimeUnit.HOURS)
        );
        verify(transferRepository, times(1)).save(any(Transfer.class));
        verify(eventPublisher, times(1)).publishTransferInitiated(any(Transfer.class));
        verify(eventPublisher, times(1)).publishTransferCompleted(any(Transfer.class));
    }

    @Test
    @DisplayName("Should return existing transfer when duplicate idempotency key detected (Redis)")
    void shouldReturnExistingTransfer_WhenDuplicateIdempotencyKey_InRedis() {
        // Given
        when(valueOperations.get("transfer:idempotency:test-idempotency-key"))
                .thenReturn("TXF-123456789012");
        when(transferRepository.findByTransferReference("TXF-123456789012"))
                .thenReturn(Optional.of(transfer));

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransferReference()).isEqualTo("TXF-123456789012");

        verify(valueOperations, times(1)).get("transfer:idempotency:test-idempotency-key");
        verify(transferRepository, times(1)).findByTransferReference("TXF-123456789012");
        verify(transferRepository, never()).save(any(Transfer.class));
        verify(sagaOrchestrator, never()).executeTransfer(any(Transfer.class));
        verify(eventPublisher, never()).publishTransferInitiated(any(Transfer.class));
    }

    @Test
    @DisplayName("Should return existing transfer when duplicate idempotency key detected (Database fallback)")
    void shouldReturnExistingTransfer_WhenDuplicateIdempotencyKey_InDatabase() {
        // Given
        when(valueOperations.get("transfer:idempotency:test-idempotency-key")).thenReturn(null);
        when(transferRepository.findByIdempotencyKey("test-idempotency-key"))
                .thenReturn(Optional.of(transfer));

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransferReference()).isEqualTo("TXF-123456789012");

        verify(valueOperations, times(1)).get("transfer:idempotency:test-idempotency-key");
        verify(transferRepository, times(1)).findByIdempotencyKey("test-idempotency-key");
        verify(transferRepository, never()).save(any(Transfer.class));
        verify(sagaOrchestrator, never()).executeTransfer(any(Transfer.class));
    }

    @Test
    @DisplayName("Should publish TransferFailed event when SAGA fails")
    void shouldPublishTransferFailedEvent_WhenSagaFails() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(transferRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer t = invocation.getArgument(0);
                    t.setStatus(TransferStatus.FAILED);
                    t.setFailureReason("Insufficient balance");
                    return t;
                });

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(response.getFailureReason()).isEqualTo("Insufficient balance");

        verify(eventPublisher, times(1)).publishTransferInitiated(any(Transfer.class));
        verify(eventPublisher, times(1)).publishTransferFailed(any(Transfer.class));
        verify(eventPublisher, never()).publishTransferCompleted(any(Transfer.class));
    }

    @Test
    @DisplayName("Should publish TransferCompensated event when SAGA is compensated")
    void shouldPublishTransferCompensatedEvent_WhenSagaIsCompensated() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(transferRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer t = invocation.getArgument(0);
                    t.setStatus(TransferStatus.COMPENSATED);
                    return t;
                });

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransferStatus.COMPENSATED);

        verify(eventPublisher, times(1)).publishTransferInitiated(any(Transfer.class));
        verify(eventPublisher, times(1)).publishTransferCompensated(any(Transfer.class));
        verify(eventPublisher, never()).publishTransferCompleted(any(Transfer.class));
        verify(eventPublisher, never()).publishTransferFailed(any(Transfer.class));
    }

    @Test
    @DisplayName("Should generate unique transfer reference")
    void shouldGenerateUniqueTransferReference() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(transferRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer t = invocation.getArgument(0);
                    t.setStatus(TransferStatus.COMPLETED);
                    return t;
                });

        // When
        TransferResponse response1 = transferService.initiateTransfer(transferRequest);
        TransferResponse response2 = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response1.getTransferReference()).startsWith("TXF-");
        assertThat(response2.getTransferReference()).startsWith("TXF-");
        assertThat(response1.getTransferReference().length()).isEqualTo(16); // TXF- + 12 chars
    }

    // ==================== GET TRANSFER TESTS ====================

    @Test
    @DisplayName("Should get transfer by reference successfully")
    void shouldGetTransferByReference_Successfully() {
        // Given
        when(transferRepository.findByTransferReference("TXF-123456789012"))
                .thenReturn(Optional.of(transfer));

        // When
        TransferResponse response = transferService.getTransferByReference("TXF-123456789012");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransferReference()).isEqualTo("TXF-123456789012");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        verify(transferRepository, times(1)).findByTransferReference("TXF-123456789012");
    }

    @Test
    @DisplayName("Should throw TransferNotFoundException when transfer not found by reference")
    void shouldThrowTransferNotFoundException_WhenTransferNotFoundByReference() {
        // Given
        when(transferRepository.findByTransferReference("TXF-NOTFOUND"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transferService.getTransferByReference("TXF-NOTFOUND"))
                .isInstanceOf(TransferNotFoundException.class)
                .hasMessageContaining("TXF-NOTFOUND");

        verify(transferRepository, times(1)).findByTransferReference("TXF-NOTFOUND");
    }

    @Test
    @DisplayName("Should get transfers by account number")
    void shouldGetTransfersByAccount() {
        // Given
        Transfer transfer2 = Transfer.builder()
                .id(2L)
                .transferReference("TXF-987654321098")
                .fromAccountNumber("ACC002")
                .toAccountNumber("ACC001")
                .amount(new BigDecimal("50.00"))
                .currency("TRY")
                .status(TransferStatus.COMPLETED)
                .build();

        when(transferRepository.findByAccountNumber("ACC001"))
                .thenReturn(Arrays.asList(transfer, transfer2));

        // When
        List<TransferResponse> responses = transferService.getTransfersByAccount("ACC001");

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TransferResponse::getTransferReference)
                .containsExactly("TXF-123456789012", "TXF-987654321098");

        verify(transferRepository, times(1)).findByAccountNumber("ACC001");
    }

    @Test
    @DisplayName("Should return empty list when no transfers found for account")
    void shouldReturnEmptyList_WhenNoTransfersFoundForAccount() {
        // Given
        when(transferRepository.findByAccountNumber("ACC999"))
                .thenReturn(List.of());

        // When
        List<TransferResponse> responses = transferService.getTransfersByAccount("ACC999");

        // Then
        assertThat(responses).isEmpty();

        verify(transferRepository, times(1)).findByAccountNumber("ACC999");
    }

    @Test
    @DisplayName("Should get transfers from account")
    void shouldGetTransfersFrom() {
        // Given
        when(transferRepository.findByFromAccountNumberOrderByCreatedAtDesc("ACC001"))
                .thenReturn(List.of(transfer));

        // When
        List<TransferResponse> responses = transferService.getTransfersFrom("ACC001");

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFromAccountNumber()).isEqualTo("ACC001");

        verify(transferRepository, times(1))
                .findByFromAccountNumberOrderByCreatedAtDesc("ACC001");
    }

    @Test
    @DisplayName("Should get transfers to account")
    void shouldGetTransfersTo() {
        // Given
        when(transferRepository.findByToAccountNumberOrderByCreatedAtDesc("ACC002"))
                .thenReturn(List.of(transfer));

        // When
        List<TransferResponse> responses = transferService.getTransfersTo("ACC002");

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getToAccountNumber()).isEqualTo("ACC002");

        verify(transferRepository, times(1))
                .findByToAccountNumberOrderByCreatedAtDesc("ACC002");
    }

    // ==================== IDEMPOTENCY TESTS ====================

    @Test
    @DisplayName("Should store idempotency key in Redis with 24-hour TTL")
    void shouldStoreIdempotencyKeyInRedis_With24HourTTL() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);
        when(transferRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class))).thenReturn(transfer);

        // When
        transferService.initiateTransfer(transferRequest);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(valueOperations).set(
                keyCaptor.capture(),
                valueCaptor.capture(),
                ttlCaptor.capture(),
                unitCaptor.capture()
        );

        assertThat(keyCaptor.getValue()).isEqualTo("transfer:idempotency:test-idempotency-key");
        assertThat(ttlCaptor.getValue()).isEqualTo(24L);
        assertThat(unitCaptor.getValue()).isEqualTo(TimeUnit.HOURS);
    }

    @Test
    @DisplayName("Should check Redis first before database for idempotency")
    void shouldCheckRedisFirst_BeforeDatabase_ForIdempotency() {
        // Given
        when(valueOperations.get("transfer:idempotency:test-idempotency-key")).thenReturn(null);
        when(transferRepository.findByIdempotencyKey("test-idempotency-key"))
                .thenReturn(Optional.of(transfer));

        // When
        transferService.initiateTransfer(transferRequest);

        // Then
        verify(valueOperations, times(1)).get("transfer:idempotency:test-idempotency-key");
        verify(transferRepository, times(1)).findByIdempotencyKey("test-idempotency-key");
    }

    // ==================== EDGE CASES & VALIDATION ====================

    @Test
    @DisplayName("Should handle different currencies")
    void shouldHandleDifferentCurrencies() {
        // Given
        transferRequest.setCurrency("USD");
        transfer.setCurrency("USD");

        when(valueOperations.get(anyString())).thenReturn(null);
        when(transferRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer t = invocation.getArgument(0);
                    t.setStatus(TransferStatus.COMPLETED);
                    return t;
                });

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response.getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should handle different transfer types")
    void shouldHandleDifferentTransferTypes() {
        // Given
        transferRequest.setTransferType(TransferType.EXTERNAL);
        transfer.setTransferType(TransferType.EXTERNAL);

        when(valueOperations.get(anyString())).thenReturn(null);
        when(transferRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer t = invocation.getArgument(0);
                    t.setStatus(TransferStatus.COMPLETED);
                    return t;
                });

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response.getTransferType()).isEqualTo(TransferType.EXTERNAL);
    }

    @Test
    @DisplayName("Should handle large transfer amounts")
    void shouldHandleLargeTransferAmounts() {
        // Given
        BigDecimal largeAmount = new BigDecimal("1000000.50");
        transferRequest.setAmount(largeAmount);
        transfer.setAmount(largeAmount);

        when(valueOperations.get(anyString())).thenReturn(null);
        when(transferRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer t = invocation.getArgument(0);
                    t.setStatus(TransferStatus.COMPLETED);
                    return t;
                });

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response.getAmount()).isEqualByComparingTo(largeAmount);
    }

    @Test
    @DisplayName("Should handle transfer with description")
    void shouldHandleTransferWithDescription() {
        // Given
        String description = "Payment for invoice #12345";
        transferRequest.setDescription(description);
        transfer.setDescription(description);

        when(valueOperations.get(anyString())).thenReturn(null);
        when(transferRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(sagaOrchestrator.executeTransfer(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer t = invocation.getArgument(0);
                    t.setStatus(TransferStatus.COMPLETED);
                    return t;
                });

        // When
        TransferResponse response = transferService.initiateTransfer(transferRequest);

        // Then
        assertThat(response.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("Should map all transfer fields to response correctly")
    void shouldMapAllTransferFieldsToResponseCorrectly() {
        // Given
        transfer.setCompletedAt(LocalDateTime.now());
        transfer.setFailureReason("Test failure");
        transfer.setDebitTransactionId("DEBIT-123");
        transfer.setCreditTransactionId("CREDIT-456");

        when(transferRepository.findByTransferReference("TXF-123456789012"))
                .thenReturn(Optional.of(transfer));

        // When
        TransferResponse response = transferService.getTransferByReference("TXF-123456789012");

        // Then
        assertThat(response.getId()).isEqualTo(transfer.getId());
        assertThat(response.getTransferReference()).isEqualTo(transfer.getTransferReference());
        assertThat(response.getFromAccountNumber()).isEqualTo(transfer.getFromAccountNumber());
        assertThat(response.getToAccountNumber()).isEqualTo(transfer.getToAccountNumber());
        assertThat(response.getAmount()).isEqualByComparingTo(transfer.getAmount());
        assertThat(response.getCurrency()).isEqualTo(transfer.getCurrency());
        assertThat(response.getDescription()).isEqualTo(transfer.getDescription());
        assertThat(response.getStatus()).isEqualTo(transfer.getStatus());
        assertThat(response.getTransferType()).isEqualTo(transfer.getTransferType());
        assertThat(response.getFailureReason()).isEqualTo(transfer.getFailureReason());
        assertThat(response.getInitiatedAt()).isEqualTo(transfer.getInitiatedAt());
        assertThat(response.getCompletedAt()).isEqualTo(transfer.getCompletedAt());
        assertThat(response.getCreatedAt()).isEqualTo(transfer.getCreatedAt());
    }
}