package com.banking.customer.dto.response;

import com.banking.customer.model.DocumentStatus;
import com.banking.customer.model.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KycDocumentResponse {

    private Long id;
    private Long customerId;
    private DocumentType documentType;
    private String documentNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String issuingAuthority;
    private String documentUrl;
    private DocumentStatus status;
    private String rejectionReason;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    private LocalDateTime createdAt;
    private Boolean expired;
}
