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
@DisplayName("CreditStep Unit Tests")
class CreditStepTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private CreditStep creditStep;

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
                .status(TransferStatus.CREDIT_PENDING)
                .debitTransactionId("TXN-DEBIT-001")
                .build();

        transactionResponse = TransactionResponse.builder()
                .transactionId("TXN-CREDIT-001")
                .accountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("300.00"))
                .build();

        successResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(transactionResponse)
                .message("Credit successful")
                .build();
    }

    // ==================== SUCCESSFUL EXECUTION ====================

    @Test
    @DisplayName("Should execute credit step successfully")
    void shouldExecuteCreditStepSuccessfully() {
        // Given
        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        boolean result = creditStep.execute(transfer);

        // Then
        assertThat(result).isTrue();
        assertThat(transfer.getCreditTransactionId()).isEqualTo("TXN-CREDIT-001");
        assertThat(transfer.getFailureReason()).isNull();

        verify(accountServiceClient, times(1)).creditAccount(eq("ACC002"), any(TransactionRequest.class));
    }

    @Test
    @DisplayName("Should create correct credit request with description")
    void shouldCreateCorrectCreditRequestWithDescription() {
        // Given
        ArgumentCaptor<TransactionRequest> requestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        creditStep.execute(transfer);

        // Then
        verify(accountServiceClient).creditAccount(eq("ACC002"), requestCaptor.capture());

        TransactionRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedRequest.getDescription()).contains("Transfer from ACC001");
        assertThat(capturedRequest.getDescription()).contains("TXF-123456789012");
        assertThat(capturedRequest.getReferenceId()).isEqualTo("TXF-123456789012");
    }

    @Test
    @DisplayName("Should return correct step name")
    void shouldReturnCorrectStepName() {
        // When
        String stepName = creditStep.getStepName();

        // Then
        assertThat(stepName).isEqualTo("CREDIT_STEP");
    }

    // ==================== EXECUTION FAILURES ====================

    @Test
    @DisplayName("Should fail when credit operation returns unsuccessful response")
    void shouldFail_WhenCreditOperationReturnsUnsuccessfulResponse() {
        // Given
        ApiResponse<TransactionResponse> failureResponse = ApiResponse.<TransactionResponse>builder()
                .success(false)
                .data(null)
                .message("Account not found")
                .build();

        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(failureResponse);

        // When
        boolean result = creditStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Credit failed");
        assertThat(transfer.getFailureReason()).contains("Account not found");
        assertThat(transfer.getCreditTransactionId()).isNull();
    }

    @Test
    @DisplayName("Should fail when credit operation returns null data")
    void shouldFail_WhenCreditOperationReturnsNullData() {
        // Given
        ApiResponse<TransactionResponse> nullDataResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(null)
                .message("Success but no data")
                .build();

        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(nullDataResponse);

        // When
        boolean result = creditStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Credit failed");
    }

    @Test
    @DisplayName("Should handle exception during credit operation")
    void shouldHandleExceptionDuringCreditOperation() {
        // Given
        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When
        boolean result = creditStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Credit error");
        assertThat(transfer.getFailureReason()).contains("Network error");
        assertThat(transfer.getCreditTransactionId()).isNull();
    }

    // ==================== SUCCESSFUL COMPENSATION ====================

    @Test
    @DisplayName("Should compensate successfully by debiting back the amount")
    void shouldCompensateSuccessfully() {
        // Given
        transfer.setCreditTransactionId("TXN-CREDIT-001");

        TransactionResponse debitResponse = TransactionResponse.builder()
                .transactionId("TXN-DEBIT-REVERSAL-001")
                .accountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .build();

        ApiResponse<TransactionResponse> debitSuccessResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(debitResponse)
                .message("Debit successful")
                .build();

        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(debitSuccessResponse);

        // When
        boolean result = creditStep.compensate(transfer);

        // Then
        assertThat(result).isTrue();

        verify(accountServiceClient, times(1)).debitAccount(eq("ACC002"), any(TransactionRequest.class));
    }

    @Test
    @DisplayName("Should create correct compensation request with reversal description")
    void shouldCreateCorrectCompensationRequest() {
        // Given
        transfer.setCreditTransactionId("TXN-CREDIT-001");
        ArgumentCaptor<TransactionRequest> requestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);

        TransactionResponse debitResponse = TransactionResponse.builder()
                .transactionId("TXN-DEBIT-REVERSAL-001")
                .build();

        ApiResponse<TransactionResponse> debitSuccessResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(debitResponse)
                .build();

        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(debitSuccessResponse);

        // When
        creditStep.compensate(transfer);

        // Then
        verify(accountServiceClient).debitAccount(eq("ACC002"), requestCaptor.capture());

        TransactionRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedRequest.getDescription()).contains("Reversal");
        assertThat(capturedRequest.getDescription()).contains("Failed transfer");
        assertThat(capturedRequest.getDescription()).contains("ACC001");
        assertThat(capturedRequest.getDescription()).contains("TXF-123456789012");
        assertThat(capturedRequest.getReferenceId()).isEqualTo("TXF-123456789012-REVERSAL");
    }

    @Test
    @DisplayName("Should skip compensation when no credit transaction exists")
    void shouldSkipCompensation_WhenNoCreditTransactionExists() {
        // Given
        transfer.setCreditTransactionId(null);

        // When
        boolean result = creditStep.compensate(transfer);

        // Then
        assertThat(result).isTrue();

        verify(accountServiceClient, never()).debitAccount(anyString(), any(TransactionRequest.class));
    }

    // ==================== COMPENSATION FAILURES ====================

    @Test
    @DisplayName("Should fail compensation when debit operation returns unsuccessful response")
    void shouldFailCompensation_WhenDebitOperationFails() {
        // Given
        transfer.setCreditTransactionId("TXN-CREDIT-001");

        ApiResponse<TransactionResponse> failureResponse = ApiResponse.<TransactionResponse>builder()
                .success(false)
                .data(null)
                .message("Insufficient balance")
                .build();

        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(failureResponse);

        // When
        boolean result = creditStep.compensate(transfer);

        // Then
        assertThat(result).isFalse();

        verify(accountServiceClient, times(1)).debitAccount(eq("ACC002"), any(TransactionRequest.class));
    }

    @Test
    @DisplayName("Should handle exception during compensation")
    void shouldHandleExceptionDuringCompensation() {
        // Given
        transfer.setCreditTransactionId("TXN-CREDIT-001");

        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When
        boolean result = creditStep.compensate(transfer);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle large amounts in credit step")
    void shouldHandleLargeAmountsInCreditStep() {
        // Given
        BigDecimal largeAmount = new BigDecimal("1000000.50");
        transfer.setAmount(largeAmount);

        transactionResponse.setAmount(largeAmount);
        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        boolean result = creditStep.execute(transfer);

        // Then
        assertThat(result).isTrue();

        ArgumentCaptor<TransactionRequest> requestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(accountServiceClient).creditAccount(anyString(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAmount()).isEqualByComparingTo(largeAmount);
    }

    @Test
    @DisplayName("Should handle decimal amounts with precision")
    void shouldHandleDecimalAmountsWithPrecision() {
        // Given
        BigDecimal preciseAmount = new BigDecimal("99.99");
        transfer.setAmount(preciseAmount);

        transactionResponse.setAmount(preciseAmount);
        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        boolean result = creditStep.execute(transfer);

        // Then
        assertThat(result).isTrue();

        ArgumentCaptor<TransactionRequest> requestCaptor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(accountServiceClient).creditAccount(anyString(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAmount()).isEqualByComparingTo(preciseAmount);
    }

    @Test
    @DisplayName("Should store credit transaction ID after successful execution")
    void shouldStoreCreditTransactionId() {
        // Given
        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        creditStep.execute(transfer);

        // Then
        assertThat(transfer.getCreditTransactionId()).isNotNull();
        assertThat(transfer.getCreditTransactionId()).isEqualTo("TXN-CREDIT-001");
    }

    @Test
    @DisplayName("Should not store credit transaction ID on failure")
    void shouldNotStoreCreditTransactionId_OnFailure() {
        // Given
        ApiResponse<TransactionResponse> failureResponse = ApiResponse.<TransactionResponse>builder()
                .success(false)
                .message("Error")
                .build();

        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(failureResponse);

        // When
        creditStep.execute(transfer);

        // Then
        assertThat(transfer.getCreditTransactionId()).isNull();
    }

    @Test
    @DisplayName("Should use correct account number for credit operation")
    void shouldUseCorrectAccountNumberForCredit() {
        // Given
        when(accountServiceClient.creditAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(successResponse);

        // When
        creditStep.execute(transfer);

        // Then
        verify(accountServiceClient).creditAccount(eq("ACC002"), any(TransactionRequest.class));
    }

    @Test
    @DisplayName("Should use correct account number for compensation")
    void shouldUseCorrectAccountNumberForCompensation() {
        // Given
        transfer.setCreditTransactionId("TXN-CREDIT-001");

        TransactionResponse debitResponse = TransactionResponse.builder()
                .transactionId("TXN-DEBIT-REVERSAL-001")
                .build();

        ApiResponse<TransactionResponse> debitSuccessResponse = ApiResponse.<TransactionResponse>builder()
                .success(true)
                .data(debitResponse)
                .build();

        when(accountServiceClient.debitAccount(anyString(), any(TransactionRequest.class)))
                .thenReturn(debitSuccessResponse);

        // When
        creditStep.compensate(transfer);

        // Then
        verify(accountServiceClient).debitAccount(eq("ACC002"), any(TransactionRequest.class));
    }
}