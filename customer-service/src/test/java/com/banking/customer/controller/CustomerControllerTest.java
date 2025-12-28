package com.banking.customer.controller;

import com.banking.customer.client.AccountServiceClient;
import com.banking.customer.dto.request.*;
import com.banking.customer.dto.response.AccountResponse;
import com.banking.customer.dto.response.ApiResponse;
import com.banking.customer.dto.response.CustomerResponse;
import com.banking.customer.exception.CustomerAlreadyExistsException;
import com.banking.customer.exception.CustomerNotFoundException;
import com.banking.customer.exception.InvalidCustomerStateException;
import com.banking.customer.model.CustomerStatus;
import com.banking.customer.model.RiskLevel;
import com.banking.customer.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@DisplayName("CustomerController Unit Tests")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private AccountServiceClient accountServiceClient;

    private RegisterCustomerRequest registerRequest;
    private CustomerResponse customerResponse;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterCustomerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .phoneNumber("+31612345678")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .nationalId("12345678901")
                .address("Test Street 123")
                .city("Amsterdam")
                .country("Netherlands")
                .postalCode("1015 CJ")
                .build();

        customerResponse = CustomerResponse.builder()
                .id(1L)
                .customerId("CUS-123456789ABC")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .phoneNumber("+31612345678")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .nationalId("123****8901") // Masked
                .address("Test Street 123")
                .city("Amsterdam")
                .country("Netherlands")
                .postalCode("1015 CJ")
                .status(CustomerStatus.PENDING_VERIFICATION)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/customers - Should register customer successfully")
    void shouldRegisterCustomerSuccessfully() throws Exception {
        // Given
        when(customerService.registerCustomer(any(RegisterCustomerRequest.class)))
                .thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer registered successfully"))
                .andExpect(jsonPath("$.data.customerId").value("CUS-123456789ABC"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john.doe@test.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.data.nationalId").value("123****8901")); // Masked

        verify(customerService).registerCustomer(any(RegisterCustomerRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers - Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Given
        RegisterCustomerRequest invalidRequest = RegisterCustomerRequest.builder()
                .firstName("J") // Too short
                .email("invalid-email") // Invalid format
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("POST /api/v1/customers - Should return 409 when email already exists")
    void shouldReturn409WhenEmailExists() throws Exception {
        // Given
        when(customerService.registerCustomer(any(RegisterCustomerRequest.class)))
                .thenThrow(new CustomerAlreadyExistsException("Customer with email already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("email")));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId} - Should get customer successfully")
    void shouldGetCustomerSuccessfully() throws Exception {
        // Given
        when(customerService.getCustomer("CUS-123456789ABC")).thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/customers/CUS-123456789ABC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customerId").value("CUS-123456789ABC"))
                .andExpect(jsonPath("$.data.email").value("john.doe@test.com"));

        verify(customerService).getCustomer("CUS-123456789ABC");
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId} - Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        // Given
        when(customerService.getCustomer("CUS-NOTFOUND"))
                .thenThrow(new CustomerNotFoundException("Customer not found: CUS-NOTFOUND"));

        // When & Then
        mockMvc.perform(get("/api/v1/customers/CUS-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("GET /api/v1/customers/email/{email} - Should get customer by email")
    void shouldGetCustomerByEmail() throws Exception {
        // Given
        when(customerService.getCustomerByEmail("john.doe@test.com")).thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/customers/email/john.doe@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john.doe@test.com"));

        verify(customerService).getCustomerByEmail("john.doe@test.com");
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{customerId} - Should update customer successfully")
    void shouldUpdateCustomerSuccessfully() throws Exception {
        // Given
        UpdateCustomerRequest updateRequest = UpdateCustomerRequest.builder()
                .phoneNumber("+31687654321")
                .address("New Address 456")
                .city("Rotterdam")
                .build();

        CustomerResponse updatedResponse = CustomerResponse.builder()
                .customerId("CUS-123456789ABC")
                .phoneNumber("+31687654321")
                .address("New Address 456")
                .city("Rotterdam")
                .build();

        when(customerService.updateCustomer(eq("CUS-123456789ABC"), any(UpdateCustomerRequest.class)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/customers/CUS-123456789ABC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phoneNumber").value("+31687654321"))
                .andExpect(jsonPath("$.data.address").value("New Address 456"));

        verify(customerService).updateCustomer(eq("CUS-123456789ABC"), any(UpdateCustomerRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/verify - Should verify customer successfully")
    void shouldVerifyCustomerSuccessfully() throws Exception {
        // Given
        VerifyCustomerRequest verifyRequest = new VerifyCustomerRequest("admin@bank.com", null);
        customerResponse.setStatus(CustomerStatus.VERIFIED);
        customerResponse.setVerifiedBy("admin@bank.com");
        customerResponse.setVerifiedAt(LocalDateTime.now());

        when(customerService.verifyCustomer(eq("CUS-123456789ABC"), any(VerifyCustomerRequest.class)))
                .thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/CUS-123456789ABC/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.verifiedBy").value("admin@bank.com"));

        verify(customerService).verifyCustomer(eq("CUS-123456789ABC"), any(VerifyCustomerRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/approve - Should approve customer successfully")
    void shouldApproveCustomerSuccessfully() throws Exception {
        // Given
        ApproveCustomerRequest approveRequest = ApproveCustomerRequest.builder()
                .approvedBy("manager@bank.com")
                .riskLevel(RiskLevel.LOW)
                .build();

        customerResponse.setStatus(CustomerStatus.APPROVED);
        customerResponse.setApprovedBy("manager@bank.com");
        customerResponse.setApprovedAt(LocalDateTime.now());
        customerResponse.setRiskLevel(RiskLevel.LOW);

        when(customerService.approveCustomer(eq("CUS-123456789ABC"), any(ApproveCustomerRequest.class)))
                .thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/CUS-123456789ABC/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedBy").value("manager@bank.com"))
                .andExpect(jsonPath("$.data.riskLevel").value("LOW"));

        verify(customerService).approveCustomer(eq("CUS-123456789ABC"), any(ApproveCustomerRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/approve - Should return 400 when invalid state")
    void shouldReturn400WhenInvalidStateForApproval() throws Exception {
        // Given
        ApproveCustomerRequest approveRequest = ApproveCustomerRequest.builder()
                .approvedBy("manager@bank.com")
                .riskLevel(RiskLevel.LOW)
                .build();

        when(customerService.approveCustomer(eq("CUS-123456789ABC"), any(ApproveCustomerRequest.class)))
                .thenThrow(new InvalidCustomerStateException("Customer must be VERIFIED before approval"));

        // When & Then
        mockMvc.perform(post("/api/v1/customers/CUS-123456789ABC/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("VERIFIED")));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/suspend - Should suspend customer successfully")
    void shouldSuspendCustomerSuccessfully() throws Exception {
        // Given
        SuspendCustomerRequest suspendRequest = SuspendCustomerRequest.builder()
                .reason("Suspicious activity")
                .suspendedBy("compliance@bank.com")
                .build();

        customerResponse.setStatus(CustomerStatus.SUSPENDED);
        customerResponse.setStatusReason("Suspicious activity");

        when(customerService.suspendCustomer(eq("CUS-123456789ABC"), any(SuspendCustomerRequest.class)))
                .thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/CUS-123456789ABC/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.statusReason").value("Suspicious activity"));

        verify(customerService).suspendCustomer(eq("CUS-123456789ABC"), any(SuspendCustomerRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/activate - Should reactivate customer successfully")
    void shouldReactivateCustomerSuccessfully() throws Exception {
        // Given
        customerResponse.setStatus(CustomerStatus.APPROVED);
        customerResponse.setStatusReason(null);

        when(customerService.reactivateCustomer("CUS-123456789ABC")).thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/CUS-123456789ABC/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(customerService).reactivateCustomer("CUS-123456789ABC");
    }

    @Test
    @DisplayName("POST /api/v1/customers/{customerId}/close - Should close customer successfully")
    void shouldCloseCustomerSuccessfully() throws Exception {
        // Given
        customerResponse.setStatus(CustomerStatus.CLOSED);

        when(customerService.closeCustomer("CUS-123456789ABC")).thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/CUS-123456789ABC/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        verify(customerService).closeCustomer("CUS-123456789ABC");
    }

    @Test
    @DisplayName("GET /api/v1/customers/{customerId}/accounts - Should get customer accounts successfully")
    void shouldGetCustomerAccountsSuccessfully() throws Exception {
        // Given
        AccountResponse account1 = AccountResponse.builder()
                .accountNumber("1234567890")
                .customerId("CUS-123456789ABC")
                .balance(new BigDecimal("1000.00"))
                .currency("EUR")
                .status("ACTIVE")
                .build();

        AccountResponse account2 = AccountResponse.builder()
                .accountNumber("0987654321")
                .customerId("CUS-123456789ABC")
                .balance(new BigDecimal("5000.00"))
                .currency("USD")
                .status("ACTIVE")
                .build();

        when(customerService.getCustomer("CUS-123456789ABC")).thenReturn(customerResponse);
        when(accountServiceClient.getAccountsByCustomerId("CUS-123456789ABC"))
                .thenReturn(ApiResponse.success(List.of(account1, account2), "Accounts retrieved"));

        // When & Then
        mockMvc.perform(get("/api/v1/customers/CUS-123456789ABC/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.data[1].accountNumber").value("0987654321"));

        verify(customerService).getCustomer("CUS-123456789ABC");
        verify(accountServiceClient).getAccountsByCustomerId("CUS-123456789ABC");
    }
}
