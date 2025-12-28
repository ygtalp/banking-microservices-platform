package com.banking.customer.controller;

import com.banking.customer.dto.request.RejectDocumentRequest;
import com.banking.customer.dto.request.UploadDocumentRequest;
import com.banking.customer.dto.request.VerifyDocumentRequest;
import com.banking.customer.dto.response.ApiResponse;
import com.banking.customer.dto.response.KycDocumentResponse;
import com.banking.customer.service.KycDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/documents")
@RequiredArgsConstructor
@Slf4j
public class KycDocumentController {

    private final KycDocumentService kycDocumentService;

    @PostMapping
    public ResponseEntity<ApiResponse<KycDocumentResponse>> uploadDocument(
            @PathVariable("customerId") String customerId,
            @Valid @RequestBody UploadDocumentRequest request) {
        log.info("Received request to upload document for customer: {}, type: {}",
                customerId, request.getDocumentType());
        KycDocumentResponse response = kycDocumentService.uploadDocument(customerId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> getCustomerDocuments(
            @PathVariable("customerId") String customerId) {
        log.info("Received request to get documents for customer: {}", customerId);
        List<KycDocumentResponse> documents = kycDocumentService.getCustomerDocuments(customerId);
        return ResponseEntity.ok(ApiResponse.success(documents, "Documents retrieved successfully"));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> getDocument(
            @PathVariable("customerId") String customerId,
            @PathVariable("documentId") Long documentId) {
        log.info("Received request to get document: {} for customer: {}", documentId, customerId);
        KycDocumentResponse document = kycDocumentService.getDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(document, "Document retrieved successfully"));
    }

    @PostMapping("/{documentId}/verify")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> verifyDocument(
            @PathVariable("customerId") String customerId,
            @PathVariable("documentId") Long documentId,
            @Valid @RequestBody VerifyDocumentRequest request) {
        log.info("Received request to verify document: {} for customer: {}", documentId, customerId);
        KycDocumentResponse response = kycDocumentService.verifyDocument(documentId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Document verified successfully"));
    }

    @PostMapping("/{documentId}/reject")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> rejectDocument(
            @PathVariable("customerId") String customerId,
            @PathVariable("documentId") Long documentId,
            @Valid @RequestBody RejectDocumentRequest request) {
        log.info("Received request to reject document: {} for customer: {}", documentId, customerId);
        KycDocumentResponse response = kycDocumentService.rejectDocument(documentId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Document rejected"));
    }
}
