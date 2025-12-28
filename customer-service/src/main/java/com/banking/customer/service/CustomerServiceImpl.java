package com.banking.customer.service;

import com.banking.customer.dto.request.*;
import com.banking.customer.dto.response.CustomerResponse;
import com.banking.customer.exception.CustomerAlreadyExistsException;
import com.banking.customer.exception.CustomerNotFoundException;
import com.banking.customer.exception.InvalidCustomerStateException;
import com.banking.customer.model.Customer;
import com.banking.customer.model.CustomerHistory;
import com.banking.customer.model.CustomerStatus;
import com.banking.customer.repository.CustomerHistoryRepository;
import com.banking.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerHistoryRepository customerHistoryRepository;
    private final EventPublisher eventPublisher;
    private final CustomerIdGenerator customerIdGenerator;

    @Override
    @Transactional
    public CustomerResponse registerCustomer(RegisterCustomerRequest request) {
        log.info("Registering new customer with email: {}", request.getEmail());

        // Check if customer already exists
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer with email already exists: " + request.getEmail());
        }

        if (customerRepository.existsByNationalId(request.getNationalId())) {
            throw new CustomerAlreadyExistsException("Customer with national ID already exists");
        }

        // Generate customer ID
        String customerId = customerIdGenerator.generateCustomerId();

        // Create customer
        Customer customer = Customer.builder()
                .customerId(customerId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .nationalId(request.getNationalId())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .status(CustomerStatus.PENDING_VERIFICATION)
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        // Record history
        recordHistory(savedCustomer, "CREATE", null, CustomerStatus.PENDING_VERIFICATION,
                "Customer registered", "SYSTEM");

        // Publish event
        eventPublisher.publishCustomerCreated(savedCustomer);

        log.info("Customer registered successfully: {}", savedCustomer.getCustomerId());
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(String customerId) {
        log.debug("Fetching customer: {}", customerId);

        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        return mapToResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        log.debug("Fetching customer by email: {}", email);

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + email));

        return mapToResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByNationalId(String nationalId) {
        log.debug("Fetching customer by national ID");

        Customer customer = customerRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with national ID"));

        return mapToResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(String customerId, UpdateCustomerRequest request) {
        log.info("Updating customer: {}", customerId);

        Customer customer = customerRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        // Update fields if provided
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail())) {
            if (customerRepository.existsByEmail(request.getEmail())) {
                throw new CustomerAlreadyExistsException("Email already in use: " + request.getEmail());
            }
            customer.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }

        if (request.getCity() != null) {
            customer.setCity(request.getCity());
        }

        if (request.getCountry() != null) {
            customer.setCountry(request.getCountry());
        }

        if (request.getPostalCode() != null) {
            customer.setPostalCode(request.getPostalCode());
        }

        Customer savedCustomer = customerRepository.save(customer);

        // Record history
        recordHistory(savedCustomer, "UPDATE", customer.getStatus(), customer.getStatus(),
                "Customer information updated", "SYSTEM");

        log.info("Customer updated successfully: {}", customerId);
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse verifyCustomer(String customerId, VerifyCustomerRequest request) {
        log.info("Verifying customer: {}", customerId);

        Customer customer = customerRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        CustomerStatus previousStatus = customer.getStatus();

        try {
            customer.verify(request.getVerifiedBy());
        } catch (IllegalStateException e) {
            throw new InvalidCustomerStateException(e.getMessage());
        }

        Customer savedCustomer = customerRepository.save(customer);

        // Record history
        recordHistory(savedCustomer, "VERIFY", previousStatus, CustomerStatus.VERIFIED,
                "Customer verified: " + (request.getNotes() != null ? request.getNotes() : "KYC verified"),
                request.getVerifiedBy());

        // Publish event
        eventPublisher.publishCustomerVerified(savedCustomer, request.getVerifiedBy());

        log.info("Customer verified successfully: {}", customerId);
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse approveCustomer(String customerId, ApproveCustomerRequest request) {
        log.info("Approving customer: {}", customerId);

        Customer customer = customerRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        CustomerStatus previousStatus = customer.getStatus();

        try {
            customer.approve(request.getApprovedBy(), request.getRiskLevel());
        } catch (IllegalStateException e) {
            throw new InvalidCustomerStateException(e.getMessage());
        }

        Customer savedCustomer = customerRepository.save(customer);

        // Record history
        recordHistory(savedCustomer, "APPROVE", previousStatus, CustomerStatus.APPROVED,
                "Customer approved with risk level: " + request.getRiskLevel() +
                (request.getNotes() != null ? " - " + request.getNotes() : ""),
                request.getApprovedBy());

        // Publish event
        eventPublisher.publishCustomerApproved(savedCustomer, request.getApprovedBy());

        log.info("Customer approved successfully: {}", customerId);
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse suspendCustomer(String customerId, SuspendCustomerRequest request) {
        log.info("Suspending customer: {}", customerId);

        Customer customer = customerRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        CustomerStatus previousStatus = customer.getStatus();

        try {
            customer.suspend(request.getReason());
        } catch (IllegalStateException e) {
            throw new InvalidCustomerStateException(e.getMessage());
        }

        Customer savedCustomer = customerRepository.save(customer);

        // Record history
        recordHistory(savedCustomer, "SUSPEND", previousStatus, CustomerStatus.SUSPENDED,
                "Customer suspended: " + request.getReason(), request.getSuspendedBy());

        // Publish event
        eventPublisher.publishCustomerStatusChanged(savedCustomer, request.getReason());

        log.info("Customer suspended successfully: {}", customerId);
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse reactivateCustomer(String customerId) {
        log.info("Reactivating customer: {}", customerId);

        Customer customer = customerRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        CustomerStatus previousStatus = customer.getStatus();

        try {
            customer.reactivate();
        } catch (IllegalStateException e) {
            throw new InvalidCustomerStateException(e.getMessage());
        }

        Customer savedCustomer = customerRepository.save(customer);

        // Record history
        recordHistory(savedCustomer, "REACTIVATE", previousStatus, savedCustomer.getStatus(),
                "Customer reactivated", "SYSTEM");

        // Publish event
        eventPublisher.publishCustomerStatusChanged(savedCustomer, "Customer reactivated");

        log.info("Customer reactivated successfully: {}", customerId);
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse closeCustomer(String customerId) {
        log.info("Closing customer: {}", customerId);

        Customer customer = customerRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        CustomerStatus previousStatus = customer.getStatus();

        try {
            customer.close();
        } catch (IllegalStateException e) {
            throw new InvalidCustomerStateException(e.getMessage());
        }

        Customer savedCustomer = customerRepository.save(customer);

        // Record history
        recordHistory(savedCustomer, "CLOSE", previousStatus, CustomerStatus.CLOSED,
                "Customer account closed", "SYSTEM");

        // Publish event
        eventPublisher.publishCustomerStatusChanged(savedCustomer, "Customer closed");

        log.info("Customer closed successfully: {}", customerId);
        return mapToResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerHistory> getCustomerHistory(String customerId) {
        log.debug("Fetching history for customer: {}", customerId);

        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        return customerHistoryRepository.findByCustomerIdOrderByTimestampDesc(customer.getId());
    }

    private void recordHistory(Customer customer, String operation, CustomerStatus previousStatus,
                               CustomerStatus newStatus, String description, String performedBy) {
        CustomerHistory history = CustomerHistory.builder()
                .customerId(customer.getId())
                .operation(operation)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .description(description)
                .performedBy(performedBy)
                .build();

        customerHistoryRepository.save(history);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .dateOfBirth(customer.getDateOfBirth())
                .nationalId(customer.getMaskedNationalId()) // Masked for security
                .address(customer.getAddress())
                .city(customer.getCity())
                .country(customer.getCountry())
                .postalCode(customer.getPostalCode())
                .status(customer.getStatus())
                .statusReason(customer.getStatusReason())
                .riskLevel(customer.getRiskLevel())
                .verifiedAt(customer.getVerifiedAt())
                .verifiedBy(customer.getVerifiedBy())
                .approvedAt(customer.getApprovedAt())
                .approvedBy(customer.getApprovedBy())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
