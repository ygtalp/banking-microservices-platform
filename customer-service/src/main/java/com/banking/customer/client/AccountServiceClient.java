package com.banking.customer.client;

import com.banking.customer.dto.response.AccountResponse;
import com.banking.customer.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/customer/{customerId}")
    ApiResponse<List<AccountResponse>> getAccountsByCustomerId(
            @PathVariable("customerId") String customerId
    );
}
