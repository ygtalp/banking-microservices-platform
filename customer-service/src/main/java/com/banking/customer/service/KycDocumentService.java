package com.banking.customer.service;

import com.banking.customer.dto.request.RejectDocumentRequest;
import com.banking.customer.dto.request.UploadDocumentRequest;
import com.banking.customer.dto.request.VerifyDocumentRequest;
import com.banking.customer.dto.response.KycDocumentResponse;
import com.banking.customer.exception.CustomerNotFoundException;
import com.banking.customer.exception.DocumentNotFoundException;
import com.banking.customer.exception.InvalidDocumentException;
import com.banking.customer.model.Customer;
import com.banking.customer.model.DocumentStatus;
import com.banking.customer.model.KycDocument;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycDocumentService {

    private final KycDocumentRepository kycDocumentRepository;
    private final CustomerRepository customerRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public KycDocumentResponse uploadDocument(String customerId, UploadDocumentRequest request) {
        log.info("Uploading KYC document for customer: {}, type: {}", customerId, request.getDocumentType());

        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        // Check if document type already exists for this customer
        if (kycDocumentRepository.existsByCustomerIdAndDocumentType(customer.getId(), request.getDocumentType())) {
            throw new InvalidDocumentException("Document of type " + request.getDocumentType() + " already exists for this customer");
        }

        KycDocument document = KycDocument.builder()
                .customerId(customer.getId())
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .issuingAuthority(request.getIssuingAuthority())
                .documentUrl(request.getDocumentUrl())
                .status(DocumentStatus.PENDING)
                .build();

        KycDocument savedDocument = kycDocumentRepository.save(document);

        // Publish event
        eventPublisher.publishKycDocumentUploaded(savedDocument);

        log.info("KYC document uploaded successfully: {}", savedDocument.getId());
        return mapToResponse(savedDocument);
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getCustomerDocuments(String customerId) {
        log.debug("Fetching KYC documents for customer: {}", customerId);

        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        return kycDocumentRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KycDocumentResponse getDocument(Long documentId) {
        log.debug("Fetching KYC document: {}", documentId);

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));

        return mapToResponse(document);
    }

    @Transactional
    public KycDocumentResponse verifyDocument(Long documentId, VerifyDocumentRequest request) {
        log.info("Verifying KYC document: {}", documentId);

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));

        try {
            document.verify(request.getVerifiedBy());
        } catch (IllegalStateException e) {
            throw new InvalidDocumentException(e.getMessage());
        }

        KycDocument savedDocument = kycDocumentRepository.save(document);

        // Publish event
        eventPublisher.publishKycDocumentVerified(savedDocument);

        log.info("KYC document verified successfully: {}", documentId);
        return mapToResponse(savedDocument);
    }

    @Transactional
    public KycDocumentResponse rejectDocument(Long documentId, RejectDocumentRequest request) {
        log.info("Rejecting KYC document: {}", documentId);

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));

        try {
            document.reject(request.getRejectionReason());
        } catch (IllegalStateException e) {
            throw new InvalidDocumentException(e.getMessage());
        }

        KycDocument savedDocument = kycDocumentRepository.save(document);

        log.info("KYC document rejected: {}", documentId);
        return mapToResponse(savedDocument);
    }

    private KycDocumentResponse mapToResponse(KycDocument document) {
        return KycDocumentResponse.builder()
                .id(document.getId())
                .customerId(document.getCustomerId())
                .documentType(document.getDocumentType())
                .documentNumber(document.getDocumentNumber())
                .issueDate(document.getIssueDate())
                .expiryDate(document.getExpiryDate())
                .issuingAuthority(document.getIssuingAuthority())
                .documentUrl(document.getDocumentUrl())
                .status(document.getStatus())
                .rejectionReason(document.getRejectionReason())
                .verifiedAt(document.getVerifiedAt())
                .verifiedBy(document.getVerifiedBy())
                .createdAt(document.getCreatedAt())
                .expired(document.isExpired())
                .build();
    }
}
