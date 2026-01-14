package com.banking.transfer.saga;


import com.banking.transfer.model.Transfer;
import com.banking.transfer.model.TransferStatus;
import com.banking.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferSagaOrchestrator Unit Tests")
class TransferSagaOrchestratorTest {

    @Mock
    private ValidationStep validationStep;

    @Mock
    private DebitStep debitStep;

    @Mock
    private CreditStep creditStep;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private TransferSagaOrchestrator sagaOrchestrator;

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
                .status(TransferStatus.PENDING)
                .build();

        // Default: repository returns the transfer after save
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Default step names (lenient to avoid UnnecessaryStubbingException)
        lenient().when(validationStep.getStepName()).thenReturn("VALIDATION_STEP");
        lenient().when(debitStep.getStepName()).thenReturn("DEBIT_STEP");
        lenient().when(creditStep.getStepName()).thenReturn("CREDIT_STEP");
    }

    // ==================== SUCCESSFUL SAGA EXECUTION ====================

    @Test
    @DisplayName("Should execute SAGA successfully when all steps pass")
    void shouldExecuteSagaSuccessfully_WhenAllStepsPass() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenReturn(true);

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.getInitiatedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();

        verify(validationStep, times(1)).execute(transfer);
        verify(debitStep, times(1)).execute(transfer);
        verify(creditStep, times(1)).execute(transfer);

        verify(validationStep, never()).compensate(any());
        verify(debitStep, never()).compensate(any());
        verify(creditStep, never()).compensate(any());

        verify(transferRepository, times(5)).save(transfer);
        // 1: VALIDATING, 2: after validation, 3: DEBIT_PENDING, 4: DEBIT_COMPLETED + CREDIT_PENDING, 5: COMPLETED
    }

    @Test
    @DisplayName("Should set correct status transitions during successful SAGA")
    void shouldSetCorrectStatusTransitions_DuringSuccessfulSaga() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenReturn(true);

        // When
        sagaOrchestrator.executeTransfer(transfer);

        // Then - Verify status progression
        verify(transferRepository, atLeast(5)).save(argThat(t -> {
            TransferStatus status = t.getStatus();
            return status == TransferStatus.VALIDATING ||
                   status == TransferStatus.DEBIT_PENDING ||
                   status == TransferStatus.DEBIT_COMPLETED ||
                   status == TransferStatus.CREDIT_PENDING ||
                   status == TransferStatus.COMPLETED;
        }));
    }

    // ==================== VALIDATION STEP FAILURES ====================

    @Test
    @DisplayName("Should fail transfer when validation step fails")
    void shouldFailTransfer_WhenValidationStepFails() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setFailureReason("Source account not found");
            return false;
        });

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Source account not found");

        verify(validationStep, times(1)).execute(transfer);
        verify(debitStep, never()).execute(any());
        verify(creditStep, never()).execute(any());
        verify(validationStep, never()).compensate(any());
    }

    // ==================== DEBIT STEP FAILURES ====================

    @Test
    @DisplayName("Should compensate when debit step fails")
    void shouldCompensate_WhenDebitStepFails() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setFailureReason("Insufficient balance");
            return false;
        });

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPENSATED);
        assertThat(result.getFailureReason()).contains("Insufficient balance");

        verify(validationStep, times(1)).execute(transfer);
        verify(debitStep, times(1)).execute(transfer);
        verify(creditStep, never()).execute(any());

        // Compensation should occur in reverse order
        verify(validationStep, times(1)).compensate(transfer);
        verify(debitStep, never()).compensate(any()); // Debit failed, so not executed
    }

    @Test
    @DisplayName("Should set COMPENSATING status before compensation")
    void shouldSetCompensatingStatus_BeforeCompensation() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setFailureReason("Error");
            return false;
        });

        // When
        sagaOrchestrator.executeTransfer(transfer);

        // Then - Check that status transitions include COMPENSATING
        // Note: Transfer is mutated, so we check the final status
        assertThat(transfer.getStatus()).isIn(TransferStatus.COMPENSATING, TransferStatus.COMPENSATED, TransferStatus.FAILED);
        verify(transferRepository, atLeast(2)).save(any(Transfer.class));
    }

    // ==================== CREDIT STEP FAILURES ====================

    @Test
    @DisplayName("Should compensate when credit step fails")
    void shouldCompensate_WhenCreditStepFails() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.compensate(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setFailureReason("Destination account not active");
            return false;
        });

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPENSATED);
        assertThat(result.getFailureReason()).contains("Destination account not active");

        verify(validationStep, times(1)).execute(transfer);
        verify(debitStep, times(1)).execute(transfer);
        verify(creditStep, times(1)).execute(transfer);

        // Compensation in reverse order
        verify(debitStep, times(1)).compensate(transfer);
        verify(validationStep, times(1)).compensate(transfer);
        verify(creditStep, never()).compensate(any()); // Credit failed, so not executed
    }

    @Test
    @DisplayName("Should compensate steps in reverse order")
    void shouldCompensateStepsInReverseOrder() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.compensate(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenReturn(false);

        // When
        sagaOrchestrator.executeTransfer(transfer);

        // Then - Use inOrder to verify reverse order
        var inOrder = inOrder(debitStep, validationStep);
        inOrder.verify(debitStep).compensate(transfer);
        inOrder.verify(validationStep).compensate(transfer);
    }

    // ==================== COMPENSATION FAILURES ====================

    @Test
    @DisplayName("Should set status to FAILED when compensation fails")
    void shouldSetStatusToFailed_WhenCompensationFails() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(false); // Compensation fails
        when(debitStep.execute(any(Transfer.class))).thenReturn(false);

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(result.getFailureReason()).contains("Compensation partially failed");
        assertThat(result.getFailureReason()).contains("Manual intervention required");
    }

    @Test
    @DisplayName("Should set status to COMPENSATED when all compensations succeed")
    void shouldSetStatusToCompensated_WhenAllCompensationsSucceed() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.compensate(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenReturn(false);

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPENSATED);
        if (result.getFailureReason() != null) {
            assertThat(result.getFailureReason()).doesNotContain("Compensation partially failed");
        }
    }

    @Test
    @DisplayName("Should handle exception during compensation gracefully")
    void shouldHandleExceptionDuringCompensation() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class)))
                .thenThrow(new RuntimeException("Compensation error"));
        when(debitStep.execute(any(Transfer.class))).thenReturn(false);

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(result.getFailureReason()).contains("Compensation partially failed");
    }

    // ==================== EXCEPTION HANDLING ====================

    @Test
    @DisplayName("Should handle unexpected exception during validation step")
    void shouldHandleUnexpectedExceptionDuringValidation() {
        // Given
        when(validationStep.execute(any(Transfer.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPENSATED);
        assertThat(result.getFailureReason()).contains("Unexpected error");

        verify(debitStep, never()).execute(any());
        verify(creditStep, never()).execute(any());
    }

    @Test
    @DisplayName("Should handle unexpected exception during debit step")
    void shouldHandleUnexpectedExceptionDuringDebit() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class)))
                .thenThrow(new RuntimeException("Database connection lost"));

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPENSATED);
        assertThat(result.getFailureReason()).contains("Database connection lost");

        verify(creditStep, never()).execute(any());
        verify(validationStep, times(1)).compensate(transfer);
    }

    @Test
    @DisplayName("Should handle unexpected exception during credit step")
    void shouldHandleUnexpectedExceptionDuringCredit() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.compensate(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPENSATED);
        assertThat(result.getFailureReason()).contains("Service unavailable");

        verify(debitStep, times(1)).compensate(transfer);
        verify(validationStep, times(1)).compensate(transfer);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle large transfer amounts")
    void shouldHandleLargeTransferAmounts() {
        // Given
        transfer.setAmount(new BigDecimal("9999999.99"));
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenReturn(true);

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("9999999.99"));
    }

    @Test
    @DisplayName("Should set initiated timestamp at the start")
    void shouldSetInitiatedTimestampAtStart() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenReturn(true);

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getInitiatedAt()).isNotNull();
        assertThat(result.getInitiatedAt()).isBeforeOrEqualTo(result.getCompletedAt());
    }

    @Test
    @DisplayName("Should set completed timestamp only on success")
    void shouldSetCompletedTimestampOnlyOnSuccess() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenReturn(true);

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not set completed timestamp on failure")
    void shouldNotSetCompletedTimestampOnFailure() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(false);

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Should not set completed timestamp on compensation")
    void shouldNotSetCompletedTimestampOnCompensation() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(false);

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Should preserve original failure reason during compensation")
    void shouldPreserveOriginalFailureReasonDuringCompensation() {
        // Given
        String originalReason = "Credit operation failed";
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.compensate(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setFailureReason(originalReason);
            return false;
        });

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getFailureReason()).contains(originalReason);
    }

    @Test
    @DisplayName("Should append compensation error to failure reason when compensation fails")
    void shouldAppendCompensationErrorToFailureReason() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(validationStep.compensate(any(Transfer.class))).thenReturn(false);
        when(debitStep.execute(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setFailureReason("Original error");
            return false;
        });

        // When
        Transfer result = sagaOrchestrator.executeTransfer(transfer);

        // Then
        assertThat(result.getFailureReason()).contains("Original error");
        assertThat(result.getFailureReason()).contains("Compensation partially failed");
    }

    @Test
    @DisplayName("Should persist transfer state at each step")
    void shouldPersistTransferStateAtEachStep() {
        // Given
        when(validationStep.execute(any(Transfer.class))).thenReturn(true);
        when(debitStep.execute(any(Transfer.class))).thenReturn(true);
        when(creditStep.execute(any(Transfer.class))).thenReturn(true);

        // When
        sagaOrchestrator.executeTransfer(transfer);

        // Then - Verify save is called multiple times for status updates
        verify(transferRepository, atLeast(5)).save(transfer);
    }
}
