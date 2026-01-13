package com.banking.swift.controller;

import com.banking.swift.dto.CreateSwiftTransferRequest;
import com.banking.swift.model.ChargeType;
import com.banking.swift.model.SwiftTransfer;
import com.banking.swift.model.SwiftTransferStatus;
import com.banking.swift.repository.SwiftTransferRepository;
import com.banking.swift.service.BicValidationService;
import com.banking.swift.service.Mt103MessageGenerator;
import com.banking.swift.service.SwiftTransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SwiftController.class)
@DisplayName("Swift Controller Integration Tests")
class SwiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SwiftTransferService swiftTransferService;

    @MockBean
    private SwiftTransferRepository swiftTransferRepository;

    @MockBean
    private Mt103MessageGenerator mt103MessageGenerator;

    @MockBean
    private BicValidationService bicValidationService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private CreateSwiftTransferRequest validRequest;
    private SwiftTransfer sampleTransfer;

    @BeforeEach
    void setUp() {
        validRequest = CreateSwiftTransferRequest.builder()
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

        sampleTransfer = SwiftTransfer.builder()
                .id(1L)
                .transactionReference("SWFT1A2B3C4D5E6F")
                .internalAccountId("ACC123456")
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("USD")
                .amount(new BigDecimal("10000.00"))
                .orderingCustomerName("John Doe")
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
    }

    @Test
    @WithMockUser
    @DisplayName("Should create SWIFT transfer successfully")
    void shouldCreateSwiftTransferSuccessfully() throws Exception {
        // Given
        when(swiftTransferService.createSwiftTransfer(any(SwiftTransfer.class)))
                .thenReturn(sampleTransfer);

        // When & Then
        mockMvc.perform(post("/swift/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference", is("SWFT1A2B3C4D5E6F")))
                .andExpect(jsonPath("$.internalAccountId", is("ACC123456")))
                .andExpect(jsonPath("$.amount", is(10000.00)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.senderBic", is("BNPAFRPPXXX")))
                .andExpect(jsonPath("$.beneficiaryBankBic", is("DEUTDEFFXXX")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when creating transfer with invalid data")
    void shouldReturn400WhenCreatingTransferWithInvalidData() throws Exception {
        // Given - Invalid request with null required fields
        CreateSwiftTransferRequest invalidRequest = CreateSwiftTransferRequest.builder()
                .amount(new BigDecimal("-100.00")) // Invalid negative amount
                .build();

        // When & Then
        mockMvc.perform(post("/swift/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when creating transfer without authentication")
    void shouldReturn401WhenCreatingTransferWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(post("/swift/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should get transfer by reference successfully")
    void shouldGetTransferByReferenceSuccessfully() throws Exception {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        when(swiftTransferService.getTransferByReference(reference))
                .thenReturn(sampleTransfer);

        // When & Then
        mockMvc.perform(get("/swift/transfers/{reference}", reference)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference", is(reference)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when transfer not found")
    void shouldReturn404WhenTransferNotFound() throws Exception {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        when(swiftTransferService.getTransferByReference(reference))
                .thenThrow(new RuntimeException("Transfer not found: " + reference));

        // When & Then
        mockMvc.perform(get("/swift/transfers/{reference}", reference)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Should get transfers by account successfully")
    void shouldGetTransfersByAccountSuccessfully() throws Exception {
        // Given
        String accountId = "ACC123456";
        List<SwiftTransfer> transfers = Arrays.asList(sampleTransfer, sampleTransfer);
        when(swiftTransferService.getTransfersByAccount(accountId))
                .thenReturn(transfers);

        // When & Then
        mockMvc.perform(get("/swift/transfers/account/{accountId}", accountId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].internalAccountId", is(accountId)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should get transfers by status with pagination")
    void shouldGetTransfersByStatusWithPagination() throws Exception {
        // Given
        SwiftTransferStatus status = SwiftTransferStatus.PENDING;
        Page<SwiftTransfer> page = new PageImpl<>(Arrays.asList(sampleTransfer));
        when(swiftTransferService.getTransfersByStatus(eq(status), any(PageRequest.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/swift/transfers/status/{status}", status.name())
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should process transfer successfully")
    void shouldProcessTransferSuccessfully() throws Exception {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        sampleTransfer.setStatus(SwiftTransferStatus.SUBMITTED);
        sampleTransfer.setMt103Message("MT103_MESSAGE");

        when(swiftTransferService.processTransfer(reference))
                .thenReturn(sampleTransfer);

        // When & Then
        mockMvc.perform(post("/swift/transfers/{reference}/process", reference)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference", is(reference)))
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.mt103Message", is("MT103_MESSAGE")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should complete transfer successfully")
    void shouldCompleteTransferSuccessfully() throws Exception {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        String settlementReference = "SETTLE123";
        sampleTransfer.setStatus(SwiftTransferStatus.COMPLETED);
        sampleTransfer.setSettlementReference(settlementReference);

        when(swiftTransferService.completeTransfer(reference, settlementReference))
                .thenReturn(sampleTransfer);

        // When & Then
        mockMvc.perform(post("/swift/transfers/{reference}/complete", reference)
                        .with(csrf())
                        .param("settlementReference", settlementReference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference", is(reference)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.settlementReference", is(settlementReference)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should fail transfer successfully")
    void shouldFailTransferSuccessfully() throws Exception {
        // Given
        String reference = "SWFT1A2B3C4D5E6F";
        String reason = "Compliance check failed";
        sampleTransfer.setStatus(SwiftTransferStatus.FAILED);
        sampleTransfer.setStatusReason(reason);

        when(swiftTransferService.failTransfer(reference, reason))
                .thenReturn(sampleTransfer);

        // When & Then
        mockMvc.perform(post("/swift/transfers/{reference}/fail", reference)
                        .with(csrf())
                        .param("reason", reason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference", is(reference)))
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.statusReason", is(reason)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should get statistics successfully")
    void shouldGetStatisticsSuccessfully() throws Exception {
        // Given
        SwiftTransferService.SwiftTransferStatistics stats =
                SwiftTransferService.SwiftTransferStatistics.builder()
                        .pendingCount(5L)
                        .processingCount(3L)
                        .completedCount(100L)
                        .failedCount(2L)
                        .totalVolumeL30D(new BigDecimal("500000.00"))
                        .build();

        when(swiftTransferService.getStatistics()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/swift/transfers/statistics")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount", is(5)))
                .andExpect(jsonPath("$.processingCount", is(3)))
                .andExpect(jsonPath("$.completedCount", is(100)))
                .andExpect(jsonPath("$.failedCount", is(2)))
                .andExpect(jsonPath("$.totalVolumeL30D", is(500000.00)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should validate required fields in create request")
    void shouldValidateRequiredFieldsInCreateRequest() throws Exception {
        // Given - Request with missing required fields
        CreateSwiftTransferRequest invalidRequest = CreateSwiftTransferRequest.builder()
                .amount(new BigDecimal("1000.00"))
                // Missing required fields: currency, senderBic, beneficiaryBankBic, etc.
                .build();

        // When & Then
        mockMvc.perform(post("/swift/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("Should validate amount constraints")
    void shouldValidateAmountConstraints() throws Exception {
        // Given - Request with invalid amount
        validRequest.setAmount(new BigDecimal("0.00")); // Below minimum

        // When & Then
        mockMvc.perform(post("/swift/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("Should validate BIC format")
    void shouldValidateBicFormat() throws Exception {
        // Given - Request with invalid BIC format
        validRequest.setSenderBic("INVALID"); // Too short

        // When & Then
        mockMvc.perform(post("/swift/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() throws Exception {
        // Given
        when(swiftTransferService.createSwiftTransfer(any(SwiftTransfer.class)))
                .thenThrow(new RuntimeException("Internal service error"));

        // When & Then
        mockMvc.perform(post("/swift/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return empty list when account has no transfers")
    void shouldReturnEmptyListWhenAccountHasNoTransfers() throws Exception {
        // Given
        String accountId = "ACC999999";
        when(swiftTransferService.getTransfersByAccount(accountId))
                .thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/swift/transfers/account/{accountId}", accountId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should validate date format in request")
    void shouldValidateDateFormatInRequest() throws Exception {
        // Given - Valid request with proper date
        String jsonRequest = objectMapper.writeValueAsString(validRequest);

        // When & Then
        mockMvc.perform(post("/swift/transfers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk());
    }
}
