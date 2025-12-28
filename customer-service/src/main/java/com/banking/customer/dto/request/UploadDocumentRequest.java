package com.banking.customer.dto.request;

import com.banking.customer.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "Document number is required")
    @Size(max = 50, message = "Document number must not exceed 50 characters")
    private String documentNumber;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Size(max = 100, message = "Issuing authority must not exceed 100 characters")
    private String issuingAuthority;

    @Size(max = 500, message = "Document URL must not exceed 500 characters")
    private String documentUrl;
}
