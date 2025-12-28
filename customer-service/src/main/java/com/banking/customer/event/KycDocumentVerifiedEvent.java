package com.banking.customer.event;

import com.banking.customer.model.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocumentVerifiedEvent {

    private String eventType = "KYC_DOCUMENT_VERIFIED";
    private String customerId;
    private Long documentId;
    private DocumentType documentType;
    private String verifiedBy;
    private LocalDateTime timestamp;
}
