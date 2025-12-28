package com.banking.customer.service;

import com.banking.customer.event.*;
import com.banking.customer.model.Customer;
import com.banking.customer.model.CustomerStatus;
import com.banking.customer.model.KycDocument;
import com.banking.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private static final String TOPIC = "customer.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CustomerRepository customerRepository;

    public void publishCustomerCreated(Customer customer) {
        log.info("Publishing CustomerCreatedEvent for customer: {}", customer.getCustomerId());

        CustomerCreatedEvent event = CustomerCreatedEvent.builder()
                .eventType("CUSTOMER_CREATED")
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .nationalId(customer.getNationalId())
                .status(customer.getStatus())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC, customer.getCustomerId(), event);
        log.debug("CustomerCreatedEvent published successfully");
    }

    public void publishCustomerVerified(Customer customer, String verifiedBy) {
        log.info("Publishing CustomerVerifiedEvent for customer: {}", customer.getCustomerId());

        CustomerVerifiedEvent event = CustomerVerifiedEvent.builder()
                .eventType("CUSTOMER_VERIFIED")
                .customerId(customer.getCustomerId())
                .email(customer.getEmail())
                .verifiedBy(verifiedBy)
                .verifiedAt(customer.getVerifiedAt())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC, customer.getCustomerId(), event);
        log.debug("CustomerVerifiedEvent published successfully");
    }

    public void publishCustomerApproved(Customer customer, String approvedBy) {
        log.info("Publishing CustomerApprovedEvent for customer: {}", customer.getCustomerId());

        CustomerApprovedEvent event = CustomerApprovedEvent.builder()
                .eventType("CUSTOMER_APPROVED")
                .customerId(customer.getCustomerId())
                .email(customer.getEmail())
                .approvedBy(approvedBy)
                .riskLevel(customer.getRiskLevel())
                .approvedAt(customer.getApprovedAt())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC, customer.getCustomerId(), event);
        log.debug("CustomerApprovedEvent published successfully");
    }

    public void publishCustomerStatusChanged(Customer customer, String reason) {
        log.info("Publishing CustomerStatusChangedEvent for customer: {}", customer.getCustomerId());

        CustomerStatusChangedEvent event = CustomerStatusChangedEvent.builder()
                .eventType("CUSTOMER_STATUS_CHANGED")
                .customerId(customer.getCustomerId())
                .previousStatus(CustomerStatus.APPROVED) // Could be improved to track actual previous status
                .newStatus(customer.getStatus())
                .reason(reason)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC, customer.getCustomerId(), event);
        log.debug("CustomerStatusChangedEvent published successfully");
    }

    public void publishKycDocumentUploaded(KycDocument document) {
        log.info("Publishing KycDocumentUploadedEvent for document: {}", document.getId());

        // Get customer to include customerId in event
        Customer customer = customerRepository.findById(document.getCustomerId())
                .orElse(null);

        if (customer == null) {
            log.warn("Customer not found for document: {}", document.getId());
            return;
        }

        KycDocumentUploadedEvent event = KycDocumentUploadedEvent.builder()
                .eventType("KYC_DOCUMENT_UPLOADED")
                .customerId(customer.getCustomerId())
                .documentId(document.getId())
                .documentType(document.getDocumentType())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC, customer.getCustomerId(), event);
        log.debug("KycDocumentUploadedEvent published successfully");
    }

    public void publishKycDocumentVerified(KycDocument document) {
        log.info("Publishing KycDocumentVerifiedEvent for document: {}", document.getId());

        // Get customer to include customerId in event
        Customer customer = customerRepository.findById(document.getCustomerId())
                .orElse(null);

        if (customer == null) {
            log.warn("Customer not found for document: {}", document.getId());
            return;
        }

        KycDocumentVerifiedEvent event = KycDocumentVerifiedEvent.builder()
                .eventType("KYC_DOCUMENT_VERIFIED")
                .customerId(customer.getCustomerId())
                .documentId(document.getId())
                .documentType(document.getDocumentType())
                .verifiedBy(document.getVerifiedBy())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC, customer.getCustomerId(), event);
        log.debug("KycDocumentVerifiedEvent published successfully");
    }
}
