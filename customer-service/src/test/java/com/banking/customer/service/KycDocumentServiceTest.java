package com.banking.customer.service;

import com.banking.customer.dto.request.RejectDocumentRequest;
import com.banking.customer.dto.request.UploadDocumentRequest;
import com.banking.customer.dto.request.VerifyDocumentRequest;
import com.banking.customer.dto.response.KycDocumentResponse;
import com.banking.customer.exception.CustomerNotFoundException;
import com.banking.customer.exception.DocumentNotFoundException;
import com.banking.customer.exception.InvalidDocumentException;
import com.banking.customer.model.*;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.repository.KycDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KycDocumentService Unit Tests")
class KycDocumentServiceTest {

    @Mock
    private KycDocumentRepository documentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private KycDocumentService kycDocumentService;

    private Customer testCustomer;
    private KycDocument testDocument;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .customerId("CUS-123456789ABC")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .status(CustomerStatus.PENDING_VERIFICATION)
                .build();

        testDocument = KycDocument.builder()
                .id(1L)
                .customerId(1L)
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
    @DisplayName("Should upload document successfully")
    void shouldUploadDocumentSuccessfully() {
        // Given
        String customerId = "CUS-123456789ABC";
        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .documentType(DocumentType.PASSPORT)
                .documentNumber("P12345678")
                .issuingAuthority("Netherlands Government")
                .issueDate(LocalDate.of(2020, 1, 1))
                .expiryDate(LocalDate.of(2030, 1, 1))
                .documentUrl("/documents/passport_1.pdf")
                .build();

        when(customerRepository.findByCustomerId(customerId))
                .thenReturn(Optional.of(testCustomer));
        when(documentRepository.save(any(KycDocument.class))).thenReturn(testDocument);

        // When
        KycDocumentResponse response = kycDocumentService.uploadDocument(customerId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.PASSPORT);
        assertThat(response.getDocumentNumber()).isEqualTo("P12345678");
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.PENDING);

        verify(customerRepository).findByCustomerId(customerId);
        verify(documentRepository).save(any(KycDocument.class));
        verify(eventPublisher).publishKycDocumentUploaded(any(KycDocument.class));
    }

    @Test
    @DisplayName("Should throw exception when uploading document for non-existent customer")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        String customerId = "CUS-NOTFOUND";
        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .documentType(DocumentType.PASSPORT)
                .documentNumber("P12345678")
                .build();

        when(customerRepository.findByCustomerId(customerId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> kycDocumentService.uploadDocument(customerId, request))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("CUS-NOTFOUND");

        verify(documentRepository, never()).save(any());
        verify(eventPublisher, never()).publishKycDocumentUploaded(any());
    }

    @Test
    @DisplayName("Should throw exception when uploading expired document")
    void shouldThrowExceptionWhenDocumentExpired() {
        // Given
        String customerId = "CUS-123456789ABC";
        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .documentType(DocumentType.PASSPORT)
                .documentNumber("P12345678")
                .issueDate(LocalDate.of(2010, 1, 1))
                .expiryDate(LocalDate.of(2020, 1, 1)) // Expired
                .documentUrl("/documents/passport_1.pdf")
                .build();

        when(customerRepository.findByCustomerId(customerId))
                .thenReturn(Optional.of(testCustomer));

        // When & Then
        assertThatThrownBy(() -> kycDocumentService.uploadDocument(customerId, request))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("expired");

        verify(documentRepository, never()).save(any());
        verify(eventPublisher, never()).publishKycDocumentUploaded(any());
    }

    @Test
    @DisplayName("Should verify document successfully")
    void shouldVerifyDocumentSuccessfully() {
        // Given
        VerifyDocumentRequest request = new VerifyDocumentRequest("admin@bank.com", null);
        testDocument.setStatus(DocumentStatus.PENDING);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(KycDocument.class))).thenAnswer(invocation -> {
            KycDocument doc = invocation.getArgument(0);
            doc.setStatus(DocumentStatus.VERIFIED);
            doc.setVerifiedAt(LocalDateTime.now());
            doc.setVerifiedBy("admin@bank.com");
            return doc;
        });

        // When
        KycDocumentResponse response = kycDocumentService.verifyDocument(1L, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.VERIFIED);
        assertThat(response.getVerifiedBy()).isEqualTo("admin@bank.com");
        assertThat(response.getVerifiedAt()).isNotNull();

        verify(documentRepository).findById(1L);
        verify(documentRepository).save(any(KycDocument.class));
        verify(eventPublisher).publishKycDocumentVerified(any(KycDocument.class));
    }

    @Test
    @DisplayName("Should throw exception when verifying non-existent document")
    void shouldThrowExceptionWhenDocumentNotFound() {
        // Given
        VerifyDocumentRequest request = new VerifyDocumentRequest("admin@bank.com", null);
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> kycDocumentService.verifyDocument(999L, request))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("999");

        verify(documentRepository, never()).save(any());
        verify(eventPublisher, never()).publishKycDocumentVerified(any());
    }

    @Test
    @DisplayName("Should throw exception when verifying already verified document")
    void shouldThrowExceptionWhenDocumentAlreadyVerified() {
        // Given
        VerifyDocumentRequest request = new VerifyDocumentRequest("admin@bank.com", null);
        testDocument.setStatus(DocumentStatus.VERIFIED); // Already verified

        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        // When & Then
        assertThatThrownBy(() -> kycDocumentService.verifyDocument(1L, request))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("PENDING");

        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject document successfully")
    void shouldRejectDocumentSuccessfully() {
        // Given
        RejectDocumentRequest request = RejectDocumentRequest.builder()
                .rejectionReason("Document is not clear")
                .build();

        testDocument.setStatus(DocumentStatus.PENDING);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(KycDocument.class))).thenAnswer(invocation -> {
            KycDocument doc = invocation.getArgument(0);
            doc.setStatus(DocumentStatus.REJECTED);
            doc.setRejectionReason("Document is not clear");
            return doc;
        });

        // When
        KycDocumentResponse response = kycDocumentService.rejectDocument(1L, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("Document is not clear");

        verify(documentRepository).findById(1L);
        verify(documentRepository).save(any(KycDocument.class));
    }

    @Test
    @DisplayName("Should list documents for customer successfully")
    void shouldListDocumentsForCustomerSuccessfully() {
        // Given
        KycDocument document2 = KycDocument.builder()
                .id(2L)
                .customerId(1L)
                .documentType(DocumentType.NATIONAL_ID)
                .documentNumber("NID123456")
                .status(DocumentStatus.VERIFIED)
                .build();

        when(customerRepository.findByCustomerId("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));
        when(documentRepository.findByCustomerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(document2, testDocument));

        // When
        List<KycDocumentResponse> documents = kycDocumentService.getCustomerDocuments("CUS-123456789ABC");

        // Then
        assertThat(documents).hasSize(2);
        assertThat(documents.get(0).getDocumentType()).isEqualTo(DocumentType.NATIONAL_ID);
        assertThat(documents.get(1).getDocumentType()).isEqualTo(DocumentType.PASSPORT);

        verify(customerRepository).findByCustomerId("CUS-123456789ABC");
        verify(documentRepository).findByCustomerIdOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("Should get document by ID successfully")
    void shouldGetDocumentByIdSuccessfully() {
        // Given
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        // When
        KycDocumentResponse response = kycDocumentService.getDocument(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.PASSPORT);

        verify(documentRepository).findById(1L);
    }

    @Test
    @DisplayName("Should check if document is expired")
    void shouldCheckIfDocumentIsExpired() {
        // Given
        KycDocument expiredDoc = KycDocument.builder()
                .documentType(DocumentType.PASSPORT)
                .expiryDate(LocalDate.now().minusDays(1)) // Expired yesterday
                .build();

        KycDocument validDoc = KycDocument.builder()
                .documentType(DocumentType.PASSPORT)
                .expiryDate(LocalDate.now().plusYears(5)) // Valid for 5 more years
                .build();

        // When & Then
        assertThat(expiredDoc.isExpired()).isTrue();
        assertThat(validDoc.isExpired()).isFalse();
    }
}
