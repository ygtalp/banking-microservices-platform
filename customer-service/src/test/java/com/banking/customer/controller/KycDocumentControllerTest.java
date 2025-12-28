package com.banking.customer.controller;

import com.banking.customer.dto.request.RejectDocumentRequest;
import com.banking.customer.dto.request.UploadDocumentRequest;
import com.banking.customer.dto.request.VerifyDocumentRequest;
import com.banking.customer.dto.response.KycDocumentResponse;
import com.banking.customer.exception.DocumentNotFoundException;
import com.banking.customer.exception.InvalidDocumentException;
import com.banking.customer.model.DocumentStatus;
import com.banking.customer.model.DocumentType;
import com.banking.customer.service.KycDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KycDocumentController.class)
@DisplayName("KycDocumentController Unit Tests")
class KycDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KycDocumentService kycDocumentService;

    private UploadDocumentRequest uploadRequest;
    private KycDocumentResponse documentResponse;

    @BeforeEach
    void setUp() {
        uploadRequest = UploadDocumentRequest.builder()
                .documentType(DocumentType.PASSPORT)
                .documentNumber("P12345678")
                .issuingAuthority("Netherlands Government")
                .issueDate(LocalDate.of(2020, 1, 1))
                .expiryDate(LocalDate.of(2030, 1, 1))
                .documentUrl("/documents/passport_1.pdf")
                .build();

        documentResponse = KycDocumentResponse.builder()
                .id(1L)
                .documentType(DocumentType.PASSPORT)
                .documentNumber("P12345678")
                .issuingAuthority("Netherlands Government")
                .issueDate(LocalDate.of(2020, 1, 1))
                .expiryDate(LocalDate.of(2030, 1, 1))
                .documentUrl("/documents/passport_1.pdf")
                .status(DocumentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/documents - Should upload document successfully")
    void shouldUploadDocumentSuccessfully() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        when(kycDocumentService.uploadDocument(eq(customerId), any(UploadDocumentRequest.class)))
                .thenReturn(documentResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/documents", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.documentType").value("PASSPORT"))
                .andExpect(jsonPath("$.data.documentNumber").value("P12345678"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(kycDocumentService).uploadDocument(eq(customerId), any(UploadDocumentRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/documents - Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        UploadDocumentRequest invalidRequest = UploadDocumentRequest.builder()
                .documentType(DocumentType.PASSPORT)
                // Missing required fields
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/documents", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(kycDocumentService, never()).uploadDocument(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/documents - Should return 400 when document is expired")
    void shouldReturn400WhenDocumentExpired() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        when(kycDocumentService.uploadDocument(eq(customerId), any(UploadDocumentRequest.class)))
                .thenThrow(new InvalidDocumentException("Document has expired"));

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/documents", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId}/documents - Should list documents successfully")
    void shouldListDocumentsSuccessfully() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        KycDocumentResponse doc2 = KycDocumentResponse.builder()
                .id(2L)
                .documentType(DocumentType.NATIONAL_ID)
                .documentNumber("NID123456")
                .status(DocumentStatus.VERIFIED)
                .build();

        when(kycDocumentService.getCustomerDocuments(customerId))
                .thenReturn(List.of(documentResponse, doc2));

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{customerId}/documents", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].documentType").value("PASSPORT"))
                .andExpect(jsonPath("$.data[1].documentType").value("NATIONAL_ID"));

        verify(kycDocumentService).getCustomerDocuments(customerId);
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId}/documents/{documentId} - Should get document successfully")
    void shouldGetDocumentSuccessfully() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        when(kycDocumentService.getDocument(1L)).thenReturn(documentResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{customerId}/documents/{documentId}", customerId, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.documentType").value("PASSPORT"));

        verify(kycDocumentService).getDocument(1L);
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId}/documents/{documentId} - Should return 404 when not found")
    void shouldReturn404WhenDocumentNotFound() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        when(kycDocumentService.getDocument(999L))
                .thenThrow(new DocumentNotFoundException("Document not found: 999"));

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{customerId}/documents/{documentId}", customerId, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/documents/{documentId}/verify - Should verify document successfully")
    void shouldVerifyDocumentSuccessfully() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        VerifyDocumentRequest verifyRequest = new VerifyDocumentRequest("admin@bank.com", null);
        documentResponse.setStatus(DocumentStatus.VERIFIED);
        documentResponse.setVerifiedBy("admin@bank.com");
        documentResponse.setVerifiedAt(LocalDateTime.now());

        when(kycDocumentService.verifyDocument(eq(1L), any(VerifyDocumentRequest.class)))
                .thenReturn(documentResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/documents/{documentId}/verify", customerId, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.verifiedBy").value("admin@bank.com"));

        verify(kycDocumentService).verifyDocument(eq(1L), any(VerifyDocumentRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/documents/{documentId}/verify - Should return 400 when already verified")
    void shouldReturn400WhenAlreadyVerified() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        VerifyDocumentRequest verifyRequest = new VerifyDocumentRequest("admin@bank.com", null);
        when(kycDocumentService.verifyDocument(eq(1L), any(VerifyDocumentRequest.class)))
                .thenThrow(new InvalidDocumentException("Document must be in PENDING status"));

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/documents/{documentId}/verify", customerId, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("PENDING")));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/documents/{documentId}/reject - Should reject document successfully")
    void shouldRejectDocumentSuccessfully() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        RejectDocumentRequest rejectRequest = RejectDocumentRequest.builder()
                .rejectionReason("Document is not clear")
                .build();

        documentResponse.setStatus(DocumentStatus.REJECTED);
        documentResponse.setRejectionReason("Document is not clear");

        when(kycDocumentService.rejectDocument(eq(1L), any(RejectDocumentRequest.class)))
                .thenReturn(documentResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/documents/{documentId}/reject", customerId, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Document is not clear"));

        verify(kycDocumentService).rejectDocument(eq(1L), any(RejectDocumentRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/documents/{documentId}/reject - Should return 400 when validation fails")
    void shouldReturn400WhenRejectionValidationFails() throws Exception {
        // Given
        String customerId = "CUS-123456789ABC";
        RejectDocumentRequest invalidRequest = RejectDocumentRequest.builder()
                // Missing rejectionReason
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/documents/{documentId}/reject", customerId, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(kycDocumentService, never()).rejectDocument(any(), any());
    }
}
