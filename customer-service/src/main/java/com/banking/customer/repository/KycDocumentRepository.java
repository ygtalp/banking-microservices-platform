package com.banking.customer.repository;

import com.banking.customer.model.DocumentStatus;
import com.banking.customer.model.DocumentType;
import com.banking.customer.model.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<KycDocument> findByCustomerIdAndStatus(Long customerId, DocumentStatus status);

    Optional<KycDocument> findByCustomerIdAndDocumentType(Long customerId, DocumentType documentType);

    List<KycDocument> findByStatus(DocumentStatus status);

    boolean existsByCustomerIdAndDocumentType(Long customerId, DocumentType documentType);

    // Count documents by customer and status
    long countByCustomerIdAndStatus(Long customerId, DocumentStatus status);

    // Get all verified documents for a customer
    @Query("SELECT d FROM KycDocument d WHERE d.customerId = :customerId AND d.status = 'VERIFIED' ORDER BY d.createdAt DESC")
    List<KycDocument> findVerifiedDocumentsByCustomerId(@Param("customerId") Long customerId);

    // Check if customer has all required documents verified
    @Query("SELECT COUNT(d) FROM KycDocument d WHERE d.customerId = :customerId AND d.status = 'VERIFIED' AND d.documentType IN :requiredTypes")
    long countVerifiedRequiredDocuments(@Param("customerId") Long customerId, @Param("requiredTypes") List<DocumentType> requiredTypes);
}
