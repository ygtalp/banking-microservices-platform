package com.banking.transfer.saga;

import com.banking.transfer.client.AccountServiceClient;
import com.banking.transfer.dto.AccountBalanceResponse;
import com.banking.transfer.dto.ApiResponse;

import com.banking.transfer.model.Transfer;
import com.banking.transfer.model.TransferStatus;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationStep Unit Tests")
class ValidationStepTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private ValidationStep validationStep;

    private Transfer transfer;
    private ApiResponse<AccountBalanceResponse> fromAccountResponse;
    private ApiResponse<AccountBalanceResponse> toAccountResponse;
    private AccountBalanceResponse fromAccount;
    private AccountBalanceResponse toAccount;

    @BeforeEach
    void setUp() {
        transfer = Transfer.builder()
                .transferReference("TXF-123456789012")
                .fromAccountNumber("ACC001")
                .toAccountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .status(TransferStatus.VALIDATING)
                .build();

        fromAccount = AccountBalanceResponse.builder()
                .accountNumber("ACC001")
                .balance(new BigDecimal("500.00"))
                .currency("TRY")
                .status("ACTIVE")
                .build();

        toAccount = AccountBalanceResponse.builder()
                .accountNumber("ACC002")
                .balance(new BigDecimal("200.00"))
                .currency("TRY")
                .status("ACTIVE")
                .build();

        fromAccountResponse = ApiResponse.<AccountBalanceResponse>builder()
                .success(true)
                .data(fromAccount)
                .message("Success")
                .build();

        toAccountResponse = ApiResponse.<AccountBalanceResponse>builder()
                .success(true)
                .data(toAccount)
                .message("Success")
                .build();
    }

    // ==================== SUCCESSFUL VALIDATION ====================

    @Test
    @DisplayName("Should validate transfer successfully with all conditions met")
    void shouldValidateTransferSuccessfully() {
        // Given
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isTrue();
        assertThat(transfer.getFailureReason()).isNull();

        verify(accountServiceClient, times(1)).getAccountByNumber("ACC001");
        verify(accountServiceClient, times(1)).getAccountByNumber("ACC002");
    }

    @Test
    @DisplayName("Should return correct step name")
    void shouldReturnCorrectStepName() {
        // When
        String stepName = validationStep.getStepName();

        // Then
        assertThat(stepName).isEqualTo("VALIDATION_STEP");
    }

    // ==================== VALIDATION FAILURES ====================

    @Test
    @DisplayName("Should fail validation when transferring to same account")
    void shouldFailValidation_WhenSameAccount() {
        // Given
        transfer.setToAccountNumber("ACC001");

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).isEqualTo("Cannot transfer to the same account");

        verify(accountServiceClient, never()).getAccountByNumber(anyString());
    }

    @Test
    @DisplayName("Should fail validation when source account not found")
    void shouldFailValidation_WhenSourceAccountNotFound() {
        // Given
        ApiResponse<AccountBalanceResponse> notFoundResponse = ApiResponse.<AccountBalanceResponse>builder()
                .success(false)
                .data(null)
                .message("Account not found")
                .build();

        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(notFoundResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Source account not found");
        assertThat(transfer.getFailureReason()).contains("ACC001");

        verify(accountServiceClient, times(1)).getAccountByNumber("ACC001");
        verify(accountServiceClient, never()).getAccountByNumber("ACC002");
    }

    @Test
    @DisplayName("Should fail validation when destination account not found")
    void shouldFailValidation_WhenDestinationAccountNotFound() {
        // Given
        ApiResponse<AccountBalanceResponse> notFoundResponse = ApiResponse.<AccountBalanceResponse>builder()
                .success(false)
                .data(null)
                .message("Account not found")
                .build();

        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(notFoundResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Destination account not found");
        assertThat(transfer.getFailureReason()).contains("ACC002");

        verify(accountServiceClient, times(1)).getAccountByNumber("ACC001");
        verify(accountServiceClient, times(1)).getAccountByNumber("ACC002");
    }

    @Test
    @DisplayName("Should fail validation when source account is not active")
    void shouldFailValidation_WhenSourceAccountNotActive() {
        // Given
        fromAccount.setStatus("SUSPENDED");
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).isEqualTo("Source account is not active");

        verify(accountServiceClient, times(1)).getAccountByNumber("ACC001");
        verify(accountServiceClient, never()).getAccountByNumber("ACC002");
    }

    @Test
    @DisplayName("Should fail validation when destination account is not active")
    void shouldFailValidation_WhenDestinationAccountNotActive() {
        // Given
        toAccount.setStatus("CLOSED");
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).isEqualTo("Destination account is not active");

        verify(accountServiceClient, times(1)).getAccountByNumber("ACC001");
        verify(accountServiceClient, times(1)).getAccountByNumber("ACC002");
    }

    @Test
    @DisplayName("Should fail validation when source account currency mismatch")
    void shouldFailValidation_WhenSourceAccountCurrencyMismatch() {
        // Given
        fromAccount.setCurrency("USD");
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Currency mismatch - Source account");
        assertThat(transfer.getFailureReason()).contains("USD");
        assertThat(transfer.getFailureReason()).contains("TRY");

        verify(accountServiceClient, times(1)).getAccountByNumber("ACC001");
        verify(accountServiceClient, times(1)).getAccountByNumber("ACC002");
    }

    @Test
    @DisplayName("Should fail validation when destination account currency mismatch")
    void shouldFailValidation_WhenDestinationAccountCurrencyMismatch() {
        // Given
        toAccount.setCurrency("EUR");
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Currency mismatch - Destination account");
        assertThat(transfer.getFailureReason()).contains("EUR");
        assertThat(transfer.getFailureReason()).contains("TRY");
    }

    @Test
    @DisplayName("Should fail validation when insufficient balance")
    void shouldFailValidation_WhenInsufficientBalance() {
        // Given
        fromAccount.setBalance(new BigDecimal("50.00"));
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Insufficient balance");
        assertThat(transfer.getFailureReason()).contains("50.00");
        assertThat(transfer.getFailureReason()).contains("100.00");
    }

    @Test
    @DisplayName("Should fail validation when transfer amount is zero")
    void shouldFailValidation_WhenTransferAmountIsZero() {
        // Given
        transfer.setAmount(BigDecimal.ZERO);
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).isEqualTo("Transfer amount must be greater than zero");
    }

    @Test
    @DisplayName("Should fail validation when transfer amount is negative")
    void shouldFailValidation_WhenTransferAmountIsNegative() {
        // Given
        transfer.setAmount(new BigDecimal("-100.00"));
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).isEqualTo("Transfer amount must be greater than zero");
    }

    // ==================== EXCEPTION HANDLING ====================

    @Test
    @DisplayName("Should handle exception during validation gracefully")
    void shouldHandleExceptionDuringValidation() {
        // Given
        when(accountServiceClient.getAccountByNumber("ACC001"))
                .thenThrow(new RuntimeException("Network error"));

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Validation error");
        assertThat(transfer.getFailureReason()).contains("Network error");
    }

    // ==================== COMPENSATION ====================

    @Test
    @DisplayName("Should compensate successfully (no-op for validation)")
    void shouldCompensateSuccessfully() {
        // When
        boolean result = validationStep.compensate(transfer);

        // Then
        assertThat(result).isTrue();

        verify(accountServiceClient, never()).getAccountByNumber(anyString());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should validate transfer with exact balance match")
    void shouldValidateTransfer_WithExactBalanceMatch() {
        // Given
        fromAccount.setBalance(new BigDecimal("100.00"));
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isTrue();
        assertThat(transfer.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Should validate transfer with large amounts")
    void shouldValidateTransfer_WithLargeAmounts() {
        // Given
        transfer.setAmount(new BigDecimal("999999.99"));
        fromAccount.setBalance(new BigDecimal("1000000.00"));
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate transfer with different currencies (USD)")
    void shouldValidateTransfer_WithUSD() {
        // Given
        transfer.setCurrency("USD");
        fromAccount.setCurrency("USD");
        toAccount.setCurrency("USD");
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate transfer with different currencies (EUR)")
    void shouldValidateTransfer_WithEUR() {
        // Given
        transfer.setCurrency("EUR");
        fromAccount.setCurrency("EUR");
        toAccount.setCurrency("EUR");
        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(fromAccountResponse);
        when(accountServiceClient.getAccountByNumber("ACC002")).thenReturn(toAccountResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle null data in API response")
    void shouldHandleNullDataInApiResponse() {
        // Given
        ApiResponse<AccountBalanceResponse> nullDataResponse = ApiResponse.<AccountBalanceResponse>builder()
                .success(true)
                .data(null)
                .message("Success")
                .build();

        when(accountServiceClient.getAccountByNumber("ACC001")).thenReturn(nullDataResponse);

        // When
        boolean result = validationStep.execute(transfer);

        // Then
        assertThat(result).isFalse();
        assertThat(transfer.getFailureReason()).contains("Source account not found");
    }
}