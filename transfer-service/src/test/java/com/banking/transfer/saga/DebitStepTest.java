package com.banking.transfer.saga;

import com.banking.transfer.client.AccountServiceClient;
import com.banking.transfer.dto.ApiResponse;
import com.banking.transfer.dto.TransactionRequest;
import com.banking.transfer.dto.TransactionResponse;

import com.banking.transfer.model.Transfer;
import com.banking.transfer.model.TransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DebitStep Unit Tests")
class DebitStepTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private DebitStep debitStep;

    private Transfer transfer;
    private ApiResponse<TransactionResponse> successResponse;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        transfer = Transfer.builder()
                .transferReference("TXF-123456789012")
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .status(TransferStatus.DEBIT_PENDING)
                .build();

        transactionResponse = TransactionResponse.builder()
                .transactionId("TXN-DEBIT-001")
                .accountNumber("ACC001")
                .amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("400.00"))
                .build();

        successResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(transactionResponse)
                .message("Debit successful")
                .build();
    }

    // ==================== SUCCESSFUL EXECUTION ====================

    @Test
    @DisplayName("Should execute debit step successfully")
    void shouldExecuteDebitStepSuccessfully() {
        // Given
        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        boolean result = debitStep.execute(transfer);

        // Then
        assertThat(result).isTrue();
        assertThat(transfer.getDebitTransactionId()).isEqualTo("TXN-DEBIT-001");
        assertThat(transfer.getFailureReason()).isNull();

        verify(accountServiceClient, times(1)).debitAccount(eq("ACC001"), any(TransactionRequest.class));
    }

    @Test
    @DisplayName("Should create correct debit request with description")
    void shouldCreateCorrectDebitRequestWithDescription() {
        // Given
        ArgumentCaptor<TransactionRequest> requestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        debitStep.execute(transfer);

        // Then
        verify(accountServiceClient).debitAccount(eq("ACC001"), requestCaptor.capture());

        TransactionRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedRequest.getDescription()).contains("Transfer to ACC002");
        assertThat(capturedRequest.getDescription()).contains("TXF-123456789012");
        assertThat(capturedRequest.getReferenceId()).isEqualTo("TXF-123456789012");
    }

    @Test
    @DisplayName("Should return correct step name")
    void shouldReturnCorrectStepName() {
        // When
        String stepName = debitStep.getStepName();

        // Then
        assertThat(stepName).isEqualTo("DEBIT_STEP");
    }

    // ==================== EXECUTION FAILURES ====================

    @Test
    @DisplayName("Should fail when debit operation returns unsuccessful response")
    void shouldFail_WhenDebitOperationReturnsUnsuccessfulResponse() {
        // Given
        ApiResponse<TransactionResponse> failureResponse = ApiResponse.<TransactionResponse>builder()
                .success(false)
                .data(null)
                .message("Insufficient balance")
                .build();

        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(failureResponse);

        // When
        boolean result = debitStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Debit failed");
        assertThat(transfer.getFailureReason()).contains("Insufficient balance");
        assertThat(transfer.getDebitTransactionId()).isNull();
    }

    @Test
    @DisplayName("Should fail when debit operation returns null data")
    void shouldFail_WhenDebitOperationReturnsNullData() {
        // Given
        ApiResponse<TransactionResponse> nullDataResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(null)
                .message("Success but no data")
                .build();

        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(nullDataResponse);

        // When
        boolean result = debitStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Debit failed");
    }

    @Test
    @DisplayName("Should handle exception during debit operation")
    void shouldHandleExceptionDuringDebitOperation() {
        // Given
        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When
        boolean result = debitStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Debit error");
        assertThat(transfer.getFailureReason()).contains("Network error");
        assertThat(transfer.getDebitTransactionId()).isNull();
    }

    // ==================== SUCCESSFUL COMPENSATION ====================

    @Test
    @DisplayName("Should compensate successfully by crediting back the amount")
    void shouldCompensateSuccessfully() {
        // Given
        transfer.setDebitTransactionId("TXN-DEBIT-001");

        TransactionResponse creditResponse = TransactionResponse.builder()
                .transactionId("TXN-CREDIT-REVERSAL-001")
                .accountNumber("ACC001")
                .amount(new BigDecimal("100.00"))
                .build();

        ApiResponse<TransactionResponse> creditSuccessResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(creditResponse)
                .message("Credit successful")
                .build();

        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(creditSuccessResponse);

        // When
        boolean result = debitStep.compensate(transfer);

        // Then
        assertThat(result).isTrue();

        verify(accountServiceClient, times(1)).creditAccount(eq("ACC001"), any(TransactionRequest.class));
    }

    @Test
    @DisplayName("Should create correct compensation request with reversal description")
    void shouldCreateCorrectCompensationRequest() {
        // Given
        transfer.setDebitTransactionId("TXN-DEBIT-001");
        ArgumentCaptor<TransactionRequest> requestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);

        TransactionResponse creditResponse = TransactionResponse.builder()
                .transactionId("TXN-CREDIT-REVERSAL-001")
                .build();

        ApiResponse<TransactionResponse> creditSuccessResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(creditResponse)
                .build();

        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(creditSuccessResponse);

        // When
        debitStep.compensate(transfer);

        // Then
        verify(accountServiceClient).creditAccount(eq("ACC001"), requestCaptor.capture());

        TransactionRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedRequest.getDescription()).contains("Reversal");
        assertThat(capturedRequest.getDescription()).contains("Failed transfer");
        assertThat(capturedRequest.getDescription()).contains("ACC002");
        assertThat(capturedRequest.getDescription()).contains("TXF-123456789012");
        assertThat(capturedRequest.getReferenceId()).isEqualTo("TXF-123456789012-REVERSAL");
    }

    @Test
    @DisplayName("Should skip compensation when no debit transaction exists")
    void shouldSkipCompensation_WhenNoDebitTransactionExists() {
        // Given
        transfer.setDebitTransactionId(null);

        // When
        boolean result = debitStep.compensate(transfer);

        // Then
        assertThat(result).isTrue();

        verify(accountServiceClient, never()).creditAccount(anyString(), any(TransactionRequest.class));
    }

    // ==================== COMPENSATION FAILURES ====================

    @Test
    @DisplayName("Should fail compensation when credit operation returns unsuccessful response")
    void shouldFailCompensation_WhenCreditOperationFails() {
        // Given
        transfer.setDebitTransactionId("TXN-DEBIT-001");

        ApiResponse<TransactionResponse> failureResponse = ApiResponse.<TransactionResponse>builder()
                .success(false)
                .data(null)
                .message("Account not found")
                .build();

        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(failureResponse);

        // When
        boolean result = debitStep.compensate(transfer);

        // Then
        assertThat(result).isFalse();

        verify(accountServiceClient, times(1)).creditAccount(eq("ACC001"), any(TransactionRequest.class));
    }

    @Test
    @DisplayName("Should handle exception during compensation")
    void shouldHandleExceptionDuringCompensation() {
        // Given
        transfer.setDebitTransactionId("TXN-DEBIT-001");

        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When
        boolean result = debitStep.compensate(transfer);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle large amounts in debit step")
    void shouldHandleLargeAmountsInDebitStep() {
        // Given
        BigDecimal largeAmount = new BigDecimal("1000000.50");
        transfer.setAmount(largeAmount);

        transactionResponse.setAmount(largeAmount);
        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        boolean result = debitStep.execute(transfer);

        // Then
        assertThat(result).isTrue();

        ArgumentCaptor<TransactionRequest> requestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(accountServiceClient).debitAccount(anyString(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAmount()).isEqualByComparingTo(largeAmount);
    }

    @Test
    @DisplayName("Should handle decimal amounts with precision")
    void shouldHandleDecimalAmountsWithPrecision() {
        // Given
        BigDecimal preciseAmount = new BigDecimal("99.99");
        transfer.setAmount(preciseAmount);

        transactionResponse.setAmount(preciseAmount);
        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        boolean result = debitStep.execute(transfer);

        // Then
        assertThat(result).isTrue();

        ArgumentCaptor<TransactionRequest> requestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(accountServiceClient).debitAccount(anyString(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAmount()).isEqualByComparingTo(preciseAmount);
    }

    @Test
    @DisplayName("Should store debit transaction ID after successful execution")
    void shouldStoreDebitTransactionId() {
        // Given
        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        debitStep.execute(transfer);

        // Then
        assertThat(transfer.getDebitTransactionId()).isNotNull();
        assertThat(transfer.getDebitTransactionId()).isEqualTo("TXN-DEBIT-001");
    }

    @Test
    @DisplayName("Should not store debit transaction ID on failure")
    void shouldNotStoreDebitTransactionId_OnFailure() {
        // Given
        ApiResponse<TransactionResponse> failureResponse = ApiResponse.<TransactionResponse>builder()
                .success(false)
                .message("Error")
                .build();

        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(failureResponse);

        // When
        debitStep.execute(transfer);

        // Then
        assertThat(transfer.getDebitTransactionId()).isNull();
    }

    @Test
    @DisplayName("Should use correct account number for debit operation")
    void shouldUseCorrectAccountNumberForDebit() {
        // Given
        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        debitStep.execute(transfer);

        // Then
        verify(accountServiceClient).debitAccount(eq("ACC001"), any(TransactionRequest.class));
    }

    @Test
    @DisplayName("Should use correct account number for compensation")
    void shouldUseCorrectAccountNumberForCompensation() {
        // Given
        transfer.setDebitTransactionId("TXN-DEBIT-001");

        TransactionResponse creditResponse = TransactionResponse.builder()
                .transactionId("TXN-CREDIT-REVERSAL-001")
                .build();

        ApiResponse<TransactionResponse> creditSuccessResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(creditResponse)
                .build();

        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(creditSuccessResponse);

        // When
        debitStep.compensate(transfer);

        // Then
        verify(accountServiceClient).creditAccount(eq("ACC001"), any(TransactionRequest.class));
    }
}