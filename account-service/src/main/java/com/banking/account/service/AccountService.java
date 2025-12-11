package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.BalanceUpdateRequest;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.model.AccountHistory;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountById(Long id);

    AccountResponse getAccountByAccountNumber(String accountNumber);

    List<AccountResponse> getAccountsByCustomerId(String customerId);

    AccountResponse creditAccount(String accountNumber, BalanceUpdateRequest request);

    AccountResponse debitAccount(String accountNumber, BalanceUpdateRequest request);

    AccountResponse freezeAccount(String accountNumber);

    AccountResponse activateAccount(String accountNumber);

    AccountResponse closeAccount(String accountNumber);

    List<AccountHistory> getAccountHistory(String accountNumber);
}