package com.banking.transfer.controller;

import com.banking.transfer.dto.TransferRequest;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.exception.TransferNotFoundException;
import com.banking.transfer.model.TransferStatus;
import com.banking.transfer.model.TransferType;
import com.banking.transfer.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Transfer Controller Integration Tests")
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    @MockBean
    private com.banking.transfer.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.banking.transfer.service.TokenBlacklistService tokenBlacklistService;

    private TransferRequest transferRequest;
    private TransferResponse transferResponse;

    @BeforeEach
    void setUp() {
        transferRequest = TransferRequest.builder()
                .fromAccountNumber("TR330006100519786457841326")
                .toAccountNumber("TR330006200519786457841327")
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .description("Test transfer")
                .transferType(TransferType.INTERNAL)
                .idempotencyKey("test-idempotency-key")
                .build();

        transferResponse = TransferResponse.builder()
                .transferReference("TXF-123456789012")
                .fromAccountNumber("TR330006100519786457841326")
                .toAccountNumber("TR330006200519786457841327")
                .amount(new BigDecimal("100.00"))
                .currency("TRY")
                .description("Test transfer")
                .status(TransferStatus.COMPLETED)
                .transferType(TransferType.INTERNAL)
                .initiatedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
    }

    // ==================== POST /api/v1/transfers ====================

    @Test
    @WithMockUser
    @DisplayName("Should initiate transfer successfully")
    void shouldInitiateTransferSuccessfully() throws Exception {
        // Given
        when(transferService.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(transferResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transfer initiated successfully"))
                .andExpect(jsonPath("$.data.transferReference").value("TXF-123456789012"))
                .andExpect(jsonPath("$.data.fromAccountNumber").value("TR330006100519786457841326"))
                .andExpect(jsonPath("$.data.toAccountNumber").value("TR330006200519786457841327"))
                .andExpect(jsonPath("$.data.amount").value(100.00))
                .andExpect(jsonPath("$.data.currency").value("TRY"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    
    @WithMockUser
    @DisplayName("Should return 400 when required fields are missing")
    void shouldReturn400_WhenRequiredFieldsAreMissing() throws Exception {
        // Given - Empty request
        TransferRequest invalidRequest = TransferRequest.builder().build();

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    
    @DisplayName("Should handle duplicate transfer with idempotency key")
    void shouldHandleDuplicateTransfer_WithIdempotencyKey() throws Exception {
        // Given - Service returns existing transfer
        when(transferService.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(transferResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transferReference").exists());
    }

    @WithMockUser
    @Test
    
    @DisplayName("Should initiate transfer with large amount")
    void shouldInitiateTransfer_WithLargeAmount() throws Exception {
        // Given
        transferRequest.setAmount(new BigDecimal("1000000.00"));
        transferResponse.setAmount(new BigDecimal("1000000.00"));
        when(transferService.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(transferResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(1000000.00));
    }
    @WithMockUser

    @Test
    
    @DisplayName("Should initiate external transfer")
    void shouldInitiateExternalTransfer() throws Exception {
        // Given
        transferRequest.setTransferType(TransferType.EXTERNAL);
        transferResponse.setTransferType(TransferType.EXTERNAL);
        when(transferService.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(transferResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transferType").value("EXTERNAL"));
    }

    // ==================== GET /api/v1/transfers/{transferReference} ====================

    @Test
    
    @WithMockUser
    @DisplayName("Should get transfer by reference successfully")
    void shouldGetTransferByReferenceSuccessfully() throws Exception {
        // Given
        when(transferService.getTransferByReference("TXF-123456789012"))
                .thenReturn(transferResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/transfers/TXF-123456789012")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transfer retrieved successfully"))
                .andExpect(jsonPath("$.data.transferReference").value("TXF-123456789012"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    
    @WithMockUser
    @DisplayName("Should return 404 when transfer not found")
    void shouldReturn404_WhenTransferNotFound() throws Exception {
        // Given
        when(transferService.getTransferByReference(anyString()))
                .thenThrow(new TransferNotFoundException("Transfer not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/transfers/TXF-NOTFOUND")
                        )
                .andExpect(status().isNotFound());
    }

    // ==================== GET /api/v1/transfers/account/{accountNumber} ====================

    @Test
    
    @WithMockUser
    @DisplayName("Should get all transfers for account")
    void shouldGetAllTransfersForAccount() throws Exception {
        // Given
        TransferResponse transfer2 = TransferResponse.builder()
                .transferReference("TXF-222222222222")
                .fromAccountNumber("TR330006100519786457841326")
                .toAccountNumber("TR330006300519786457841328")
                .amount(new BigDecimal("200.00"))
                .currency("TRY")
                .status(TransferStatus.COMPLETED)
                .transferType(TransferType.INTERNAL)
                .build();

        List<TransferResponse> transfers = Arrays.asList(transferResponse, transfer2);
        when(transferService.getTransfersByAccount("TR330006100519786457841326"))
                .thenReturn(transfers);

        // When & Then
        mockMvc.perform(get("/api/v1/transfers/account/TR330006100519786457841326")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Count: 2")))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].transferReference").value("TXF-123456789012"))
                .andExpect(jsonPath("$.data[1].transferReference").value("TXF-222222222222"));
    }

    @Test
    
    @WithMockUser
    @DisplayName("Should return empty list when no transfers found for account")
    void shouldReturnEmptyList_WhenNoTransfersFoundForAccount() throws Exception {
        // Given
        when(transferService.getTransfersByAccount(anyString()))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/transfers/account/TR330009900519786457841329")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.message").value(containsString("Count: 0")));
    }

    // ==================== GET /api/v1/transfers/from/{accountNumber} ====================

    @Test
    
    @WithMockUser
    @DisplayName("Should get outgoing transfers")
    void shouldGetOutgoingTransfers() throws Exception {
        // Given
        when(transferService.getTransfersFrom("TR330006100519786457841326"))
                .thenReturn(Collections.singletonList(transferResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/transfers/from/TR330006100519786457841326")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Outgoing transfers")))
                .andExpect(jsonPath("$.message").value(containsString("Count: 1")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].fromAccountNumber").value("TR330006100519786457841326"));
    }

    @Test
    
    @WithMockUser
    @DisplayName("Should return empty list when no outgoing transfers")
    void shouldReturnEmptyList_WhenNoOutgoingTransfers() throws Exception {
        // Given
        when(transferService.getTransfersFrom(anyString()))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/transfers/from/TR330009900519786457841329")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ==================== GET /api/v1/transfers/to/{accountNumber} ====================

    @Test
    
    @WithMockUser
    @DisplayName("Should get incoming transfers")
    void shouldGetIncomingTransfers() throws Exception {
        // Given
        when(transferService.getTransfersTo("TR330006200519786457841327"))
                .thenReturn(Collections.singletonList(transferResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/transfers/to/TR330006200519786457841327")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Incoming transfers")))
                .andExpect(jsonPath("$.message").value(containsString("Count: 1")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].toAccountNumber").value("TR330006200519786457841327"));
    }

    @Test
    
    @WithMockUser
    @DisplayName("Should return empty list when no incoming transfers")
    void shouldReturnEmptyList_WhenNoIncomingTransfers() throws Exception {
        // Given
        when(transferService.getTransfersTo(anyString()))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/transfers/to/TR330009900519786457841329")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ==================== EDGE CASES ====================

    @Test
    
    @WithMockUser
    @DisplayName("Should handle transfer with different currencies")
    void shouldHandleTransfer_WithDifferentCurrencies() throws Exception {
        // Given
        transferRequest.setCurrency("USD");
        transferResponse.setCurrency("USD");
        when(transferService.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(transferResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.currency").value("USD"));
    }

    @Test
    
    @WithMockUser
    @DisplayName("Should handle transfer with long description")
    void shouldHandleTransfer_WithLongDescription() throws Exception {
        // Given
        String longDescription = "A".repeat(500);
        transferRequest.setDescription(longDescription);
        transferResponse.setDescription(longDescription);
        when(transferService.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(transferResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.description").value(longDescription));
    }

    @Test
    
    @WithMockUser
    @DisplayName("Should handle failed transfer status")
    void shouldHandleFailedTransferStatus() throws Exception {
        // Given
        transferResponse.setStatus(TransferStatus.FAILED);
        transferResponse.setFailureReason("Insufficient balance");
        when(transferService.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(transferResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failureReason").value("Insufficient balance"));
    }

    @Test
    
    @WithMockUser
    @DisplayName("Should handle transfer with special characters in description")
    void shouldHandleTransfer_WithSpecialCharactersInDescription() throws Exception {
        // Given
        transferRequest.setDescription("Transfer with special chars: @#$%^&*()");
        transferResponse.setDescription("Transfer with special chars: @#$%^&*()");
        when(transferService.initiateTransfer(any(TransferRequest.class)))
                .thenReturn(transferResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/transfers")
                        
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.description").value("Transfer with special chars: @#$%^&*()"));
    }
}
