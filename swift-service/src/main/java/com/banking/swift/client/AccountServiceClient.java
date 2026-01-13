package com.banking.swift.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @PutMapping("/accounts/{accountNumber}/balance")
    String debitAccount(@PathVariable("accountNumber") String accountNumber,
                       @RequestParam BigDecimal amount,
                       @RequestParam String description);

    @PutMapping("/accounts/{accountNumber}/balance")
    String creditAccount(@PathVariable("accountNumber") String accountNumber,
                        @RequestParam BigDecimal amount,
                        @RequestParam String description);

    @GetMapping("/accounts/{accountNumber}")
    Object getAccount(@PathVariable("accountNumber") String accountNumber);
}
