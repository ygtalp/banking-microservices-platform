package com.banking.customer.service;

import com.banking.customer.dto.request.*;
import com.banking.customer.dto.response.CustomerResponse;
import com.banking.customer.model.CustomerHistory;

import java.util.List;

public interface CustomerService {

    // Customer Operations
    CustomerResponse registerCustomer(RegisterCustomerRequest request);

    CustomerResponse getCustomer(String customerId);

    CustomerResponse getCustomerByEmail(String email);

    CustomerResponse getCustomerByNationalId(String nationalId);

    CustomerResponse updateCustomer(String customerId, UpdateCustomerRequest request);

    // Status Management
    CustomerResponse verifyCustomer(String customerId, VerifyCustomerRequest request);

    CustomerResponse approveCustomer(String customerId, ApproveCustomerRequest request);

    CustomerResponse suspendCustomer(String customerId, SuspendCustomerRequest request);

    CustomerResponse reactivateCustomer(String customerId);

    CustomerResponse closeCustomer(String customerId);

    // History
    List<CustomerHistory> getCustomerHistory(String customerId);
}
