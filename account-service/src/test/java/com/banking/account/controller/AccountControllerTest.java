package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.BalanceUpdateRequest;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.exception.AccountNotFoundException;
import com.banking.account.exception.InsufficientBalanceException;
import com.banking.account.exception.InvalidAccountStateException;
import com.banking.account.model.AccountHistory;
import com.banking.account.model.AccountStatus;
import com.banking.account.model.AccountType;
import com.banking.account.model.Currency;
import com.banking.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Account Controller Integration Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private CreateAccountRequest createAccountRequest;
    private AccountResponse accountResponse;
    private BalanceUpdateRequest balanceUpdateRequest;
    private List<AccountHistory> accountHistoryList;

    @BeforeEach
    void setUp() {
        createAccountRequest = CreateAccountRequest.builder()
                .customerId("CUS-123456")
                .customerName("John Doe")
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("1000.00"))
                .build();

        accountResponse = AccountResponse.builder()
                .id(1L)
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.CHECKING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        balanceUpdateRequest = BalanceUpdateRequest.builder()
                .amount(new BigDecimal("500.00"))
                .referenceId("REF-123")
                .description("Test transaction")
                .build();

        AccountHistory history1 = new AccountHistory();
        history1.setId(1L);
        history1.setAccountNumber("TR330006100519786457841326");
        history1.setOperation("ACCOUNT_CREATED");
        history1.setDescription("Account created with initial balance: 1000.00 TRY");

        AccountHistory history2 = new AccountHistory();
        history2.setId(2L);
        history2.setAccountNumber("TR330006100519786457841326");
        history2.setOperation("BALANCE_CREDIT");
        history2.setDescription("Account credited with 500.00 TRY");

        accountHistoryList = Arrays.asList(history1, history2);
    }

    // CREATE ACCOUNT TESTS

    @Test
    @DisplayName("Should create account successfully with ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateAccountSuccessfullyWithAdminRole() throws Exception {
        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(accountResponse);

        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account created successfully"))
                .andExpect(jsonPath("$.data.accountNumber").value("TR330006100519786457841326"))
                .andExpect(jsonPath("$.data.customerId").value("CUS-123456"))
                .andExpect(jsonPath("$.data.customerName").value("John Doe"))
                .andExpect(jsonPath("$.data.balance").value(1000.00))
                .andExpect(jsonPath("$.data.currency").value("TRY"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accountType").value("CHECKING"));
    }

    @Test
    @DisplayName("Should create account successfully with MANAGER role")
    @WithMockUser(roles = "MANAGER")
    void shouldCreateAccountSuccessfullyWithManagerRole() throws Exception {
        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(accountResponse);

        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").value("TR330006100519786457841326"));
    }

    @Test
    @DisplayName("Should return 403 when creating account with CUSTOMER role")
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturn403WhenCreatingAccountWithCustomerRole() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 400 when creating account with invalid request")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenCreatingAccountWithInvalidRequest() throws Exception {
        CreateAccountRequest invalidRequest = CreateAccountRequest.builder()
                .customerId("")  // Invalid: empty
                .customerName("")  // Invalid: empty
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // GET ACCOUNT BY ID TESTS

    @Test
    @DisplayName("Should get account by ID successfully")
    @WithMockUser
    void shouldGetAccountByIdSuccessfully() throws Exception {
        when(accountService.getAccountById(1L)).thenReturn(accountResponse);

        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.accountNumber").value("TR330006100519786457841326"));
    }

    @Test
    @DisplayName("Should return 404 when account not found by ID")
    @WithMockUser
    void shouldReturn404WhenAccountNotFoundById() throws Exception {
        when(accountService.getAccountById(999L))
                .thenThrow(new AccountNotFoundException("Account not found with id: 999"));

        mockMvc.perform(get("/api/v1/accounts/999"))
                .andExpect(status().isNotFound());
    }

    // GET ACCOUNT BY ACCOUNT NUMBER TESTS

    @Test
    @DisplayName("Should get account by account number successfully")
    @WithMockUser
    void shouldGetAccountByAccountNumberSuccessfully() throws Exception {
        when(accountService.getAccountByAccountNumber("TR330006100519786457841326")).thenReturn(accountResponse);

        mockMvc.perform(get("/api/v1/accounts/number/TR330006100519786457841326"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").value("TR330006100519786457841326"));
    }

    @Test
    @DisplayName("Should return 404 when account not found by account number")
    @WithMockUser
    void shouldReturn404WhenAccountNotFoundByAccountNumber() throws Exception {
        when(accountService.getAccountByAccountNumber("TR000000000000000000000000"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(get("/api/v1/accounts/number/TR000000000000000000000000"))
                .andExpect(status().isNotFound());
    }

    // GET ACCOUNTS BY CUSTOMER ID TESTS

    @Test
    @DisplayName("Should get accounts by customer ID successfully")
    @WithMockUser
    void shouldGetAccountsByCustomerIdSuccessfully() throws Exception {
        AccountResponse account2 = AccountResponse.builder()
                .id(2L)
                .accountNumber("TR330006100519786457841327")
                .customerId("CUS-123456")
                .customerName("John Doe")
                .balance(new BigDecimal("2000.00"))
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.SAVINGS)
                .build();

        List<AccountResponse> accounts = Arrays.asList(accountResponse, account2);
        when(accountService.getAccountsByCustomerId("CUS-123456")).thenReturn(accounts);

        mockMvc.perform(get("/api/v1/accounts/customer/CUS-123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].accountNumber").value("TR330006100519786457841326"))
                .andExpect(jsonPath("$.data[1].accountNumber").value("TR330006100519786457841327"));
    }

    @Test
    @DisplayName("Should return empty list when customer has no accounts")
    @WithMockUser
    void shouldReturnEmptyListWhenCustomerHasNoAccounts() throws Exception {
        when(accountService.getAccountsByCustomerId("CUS-NOACCOUNTS")).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/accounts/customer/CUS-NOACCOUNTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // CREDIT ACCOUNT TESTS

    @Test
    @DisplayName("Should credit account successfully")
    @WithMockUser
    void shouldCreditAccountSuccessfully() throws Exception {
        AccountResponse creditedAccount = AccountResponse.builder()
                .id(1L)
                .accountNumber("TR330006100519786457841326")
                .balance(new BigDecimal("1500.00"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.creditAccount(eq("TR330006100519786457841326"), any(BalanceUpdateRequest.class)))
                .thenReturn(creditedAccount);

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/credit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(balanceUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account credited successfully"))
                .andExpect(jsonPath("$.data.balance").value(1500.00));
    }

    @Test
    @DisplayName("Should return 400 when crediting with invalid amount")
    @WithMockUser
    void shouldReturn400WhenCreditingWithInvalidAmount() throws Exception {
        BalanceUpdateRequest invalidRequest = BalanceUpdateRequest.builder()
                .amount(new BigDecimal("-100.00"))  // Invalid: negative
                .build();

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/credit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when crediting inactive account")
    @WithMockUser
    void shouldReturn400WhenCreditingInactiveAccount() throws Exception {
        when(accountService.creditAccount(eq("TR330006100519786457841326"), any(BalanceUpdateRequest.class)))
                .thenThrow(new InvalidAccountStateException("Account is not active"));

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/credit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(balanceUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    // DEBIT ACCOUNT TESTS

    @Test
    @DisplayName("Should debit account successfully")
    @WithMockUser
    void shouldDebitAccountSuccessfully() throws Exception {
        AccountResponse debitedAccount = AccountResponse.builder()
                .id(1L)
                .accountNumber("TR330006100519786457841326")
                .balance(new BigDecimal("500.00"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.debitAccount(eq("TR330006100519786457841326"), any(BalanceUpdateRequest.class)))
                .thenReturn(debitedAccount);

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/debit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(balanceUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account debited successfully"))
                .andExpect(jsonPath("$.data.balance").value(500.00));
    }

    @Test
    @DisplayName("Should return 400 when debiting with insufficient balance")
    @WithMockUser
    void shouldReturn400WhenDebitingWithInsufficientBalance() throws Exception {
        when(accountService.debitAccount(eq("TR330006100519786457841326"), any(BalanceUpdateRequest.class)))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/debit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(balanceUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    // FREEZE ACCOUNT TESTS

    @Test
    @DisplayName("Should freeze account successfully with ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void shouldFreezeAccountSuccessfullyWithAdminRole() throws Exception {
        AccountResponse frozenAccount = AccountResponse.builder()
                .id(1L)
                .accountNumber("TR330006100519786457841326")
                .status(AccountStatus.FROZEN)
                .build();

        when(accountService.freezeAccount("TR330006100519786457841326")).thenReturn(frozenAccount);

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account frozen successfully"))
                .andExpect(jsonPath("$.data.status").value("FROZEN"));
    }

    @Test
    @DisplayName("Should return 403 when freezing account without ADMIN role")
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturn403WhenFreezingAccountWithoutAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 404 when freezing non-existent account")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenFreezingNonExistentAccount() throws Exception {
        when(accountService.freezeAccount("TR000000000000000000000000"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(post("/api/v1/accounts/TR000000000000000000000000/freeze")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ACTIVATE ACCOUNT TESTS

    @Test
    @DisplayName("Should activate account successfully")
    @WithMockUser
    void shouldActivateAccountSuccessfully() throws Exception {
        AccountResponse activatedAccount = AccountResponse.builder()
                .id(1L)
                .accountNumber("TR330006100519786457841326")
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.activateAccount("TR330006100519786457841326")).thenReturn(activatedAccount);

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/activate")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account activated successfully"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    // CLOSE ACCOUNT TESTS

    @Test
    @DisplayName("Should close account successfully with ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void shouldCloseAccountSuccessfullyWithAdminRole() throws Exception {
        AccountResponse closedAccount = AccountResponse.builder()
                .id(1L)
                .accountNumber("TR330006100519786457841326")
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.CLOSED)
                .build();

        when(accountService.closeAccount("TR330006100519786457841326")).thenReturn(closedAccount);

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account closed successfully"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    @DisplayName("Should return 403 when closing account without ADMIN role")
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturn403WhenClosingAccountWithoutAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 400 when closing account with non-zero balance")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenClosingAccountWithNonZeroBalance() throws Exception {
        when(accountService.closeAccount("TR330006100519786457841326"))
                .thenThrow(new InvalidAccountStateException("Cannot close account with non-zero balance"));

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // GET ACCOUNT HISTORY TESTS

    @Test
    @DisplayName("Should get account history successfully")
    @WithMockUser
    void shouldGetAccountHistorySuccessfully() throws Exception {
        when(accountService.getAccountHistory("TR330006100519786457841326")).thenReturn(accountHistoryList);

        mockMvc.perform(get("/api/v1/accounts/TR330006100519786457841326/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account history retrieved successfully"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].action").value("ACCOUNT_CREATED"))
                .andExpect(jsonPath("$.data[1].action").value("BALANCE_CREDIT"));
    }

    @Test
    @DisplayName("Should return 404 when getting history of non-existent account")
    @WithMockUser
    void shouldReturn404WhenGettingHistoryOfNonExistentAccount() throws Exception {
        when(accountService.getAccountHistory("TR000000000000000000000000"))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(get("/api/v1/accounts/TR000000000000000000000000/history"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return empty history for account with no transactions")
    @WithMockUser
    void shouldReturnEmptyHistoryForAccountWithNoTransactions() throws Exception {
        when(accountService.getAccountHistory("TR330006100519786457841326")).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/accounts/TR330006100519786457841326/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
