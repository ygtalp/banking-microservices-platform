package com.banking.transfer.client;

import com.banking.transfer.dto.AccountBalanceResponse;
import com.banking.transfer.dto.ApiResponse;
import com.banking.transfer.dto.TransactionRequest;
import com.banking.transfer.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/number/{accountNumber}")
    ApiResponse<AccountBalanceResponse> getAccountByNumber(
            @PathVariable("accountNumber") String accountNumber
    );

    @PostMapping("/api/v1/accounts/{accountNumber}/credit")
    ApiResponse<TransactionResponse> creditAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody TransactionRequest request
    );

    @PostMapping("/api/v1/accounts/{accountNumber}/debit")
    ApiResponse<TransactionResponse> debitAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody TransactionRequest request
    );
}