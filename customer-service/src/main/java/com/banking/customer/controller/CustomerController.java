package com.banking.customer.controller;

import com.banking.customer.client.AccountServiceClient;
import com.banking.customer.dto.request.*;
import com.banking.customer.dto.response.AccountResponse;
import com.banking.customer.dto.response.ApiResponse;
import com.banking.customer.dto.response.CustomerResponse;
import com.banking.customer.model.CustomerHistory;
import com.banking.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final AccountServiceClient accountServiceClient;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request) {
        log.info("Received request to register customer with email: {}", request.getEmail());
        CustomerResponse response = customerService.registerCustomer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Customer registered successfully"));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @PathVariable("customerId") String customerId) {
        log.info("Received request to get customer: {}", customerId);
        CustomerResponse response = customerService.getCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer retrieved successfully"));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByEmail(
            @PathVariable("email") String email) {
        log.info("Received request to get customer by email: {}", email);
        CustomerResponse response = customerService.getCustomerByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer retrieved successfully"));
    }

    @GetMapping("/national-id/{nationalId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByNationalId(
            @PathVariable("nationalId") String nationalId) {
        log.info("Received request to get customer by national ID");
        CustomerResponse response = customerService.getCustomerByNationalId(nationalId);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer retrieved successfully"));
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable("customerId") String customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        log.info("Received request to update customer: {}", customerId);
        CustomerResponse response = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer updated successfully"));
    }

    @PostMapping("/{customerId}/verify")
    public ResponseEntity<ApiResponse<CustomerResponse>> verifyCustomer(
            @PathVariable("customerId") String customerId,
            @Valid @RequestBody VerifyCustomerRequest request) {
        log.info("Received request to verify customer: {}", customerId);
        CustomerResponse response = customerService.verifyCustomer(customerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer verified successfully"));
    }

    @PostMapping("/{customerId}/approve")
    public ResponseEntity<ApiResponse<CustomerResponse>> approveCustomer(
            @PathVariable("customerId") String customerId,
            @Valid @RequestBody ApproveCustomerRequest request) {
        log.info("Received request to approve customer: {}", customerId);
        CustomerResponse response = customerService.approveCustomer(customerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer approved successfully"));
    }

    @PostMapping("/{customerId}/suspend")
    public ResponseEntity<ApiResponse<CustomerResponse>> suspendCustomer(
            @PathVariable("customerId") String customerId,
            @Valid @RequestBody SuspendCustomerRequest request) {
        log.info("Received request to suspend customer: {}", customerId);
        CustomerResponse response = customerService.suspendCustomer(customerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer suspended successfully"));
    }

    @PostMapping("/{customerId}/activate")
    public ResponseEntity<ApiResponse<CustomerResponse>> reactivateCustomer(
            @PathVariable("customerId") String customerId) {
        log.info("Received request to reactivate customer: {}", customerId);
        CustomerResponse response = customerService.reactivateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer reactivated successfully"));
    }

    @PostMapping("/{customerId}/close")
    public ResponseEntity<ApiResponse<CustomerResponse>> closeCustomer(
            @PathVariable("customerId") String customerId) {
        log.info("Received request to close customer: {}", customerId);
        CustomerResponse response = customerService.closeCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer closed successfully"));
    }

    @GetMapping("/{customerId}/history")
    public ResponseEntity<ApiResponse<List<CustomerHistory>>> getCustomerHistory(
            @PathVariable("customerId") String customerId) {
        log.info("Received request to get history for customer: {}", customerId);
        List<CustomerHistory> history = customerService.getCustomerHistory(customerId);
        return ResponseEntity.ok(ApiResponse.success(history, "Customer history retrieved successfully"));
    }

    @GetMapping("/{customerId}/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getCustomerAccounts(
            @PathVariable("customerId") String customerId) {
        log.info("Received request to get accounts for customer: {}", customerId);

        // Verify customer exists
        customerService.getCustomer(customerId);

        // Get accounts from Account Service via Feign
        ApiResponse<List<AccountResponse>> accountsResponse = accountServiceClient.getAccountsByCustomerId(customerId);

        return ResponseEntity.ok(ApiResponse.success(
                accountsResponse.getData(),
                "Customer accounts retrieved successfully"
        ));
    }
}
