package com.banking.account.security;

import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.model.AccountType;
import com.banking.account.model.Currency;
import com.banking.account.repository.AccountRepository;
import com.banking.account.service.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private CreateAccountRequest createAccountRequest;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        when(tokenBlacklistService.isTokenBlacklisted(anyString())).thenReturn(false);

        createAccountRequest = CreateAccountRequest.builder()
                .customerId("CUS-123456")
                .customerName("John Doe")
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("1000.00"))
                .build();
    }

    // AUTHENTICATION TESTS

    @Test
    @DisplayName("Should return 401 when accessing protected endpoint without authentication")
    void shouldReturn401WhenAccessingProtectedEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow access to protected endpoint with valid authentication")
    @WithMockUser
    void shouldAllowAccessToProtectedEndpointWithValidAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isNotFound());  // Account doesn't exist, but auth passed
    }

    @Test
    @DisplayName("Should return 401 for all GET endpoints without authentication")
    void shouldReturn401ForAllGetEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/accounts/number/TR330006100519786457841326"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/accounts/customer/CUS-123456"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 for all POST endpoints without authentication")
    void shouldReturn401ForAllPostEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/credit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/debit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // AUTHORIZATION TESTS - ADMIN ROLE

    @Test
    @DisplayName("Should allow ADMIN to create account")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should allow ADMIN to freeze account")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToFreezeAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isNotFound());  // Account doesn't exist, but authorization passed
    }

    @Test
    @DisplayName("Should allow ADMIN to close account")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToCloseAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isNotFound());  // Account doesn't exist, but authorization passed
    }

    // AUTHORIZATION TESTS - MANAGER ROLE

    @Test
    @DisplayName("Should allow MANAGER to create account")
    @WithMockUser(roles = "MANAGER")
    void shouldAllowManagerToCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should deny MANAGER access to freeze account")
    @WithMockUser(roles = "MANAGER")
    void shouldDenyManagerAccessToFreezeAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny MANAGER access to close account")
    @WithMockUser(roles = "MANAGER")
    void shouldDenyManagerAccessToCloseAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // AUTHORIZATION TESTS - CUSTOMER ROLE

    @Test
    @DisplayName("Should deny CUSTOMER access to create account")
    @WithMockUser(roles = "CUSTOMER")
    void shouldDenyCustomerAccessToCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny CUSTOMER access to freeze account")
    @WithMockUser(roles = "CUSTOMER")
    void shouldDenyCustomerAccessToFreezeAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny CUSTOMER access to close account")
    @WithMockUser(roles = "CUSTOMER")
    void shouldDenyCustomerAccessToCloseAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow CUSTOMER to view account details")
    @WithMockUser(roles = "CUSTOMER")
    void shouldAllowCustomerToViewAccountDetails() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isNotFound());  // Account doesn't exist, but authorization passed
    }

    @Test
    @DisplayName("Should allow CUSTOMER to credit account")
    @WithMockUser(roles = "CUSTOMER")
    void shouldAllowCustomerToCreditAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/credit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00}"))
                .andExpect(status().isNotFound());  // Account doesn't exist, but authorization passed
    }

    // AUTHORIZATION TESTS - NO ROLE

    @Test
    @DisplayName("Should deny access when user has no roles")
    @WithMockUser(roles = {})
    void shouldDenyAccessWhenUserHasNoRoles() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // CSRF TESTS

    @Test
    @DisplayName("Should reject POST request without CSRF token")
    @WithMockUser(roles = "ADMIN")
    void shouldRejectPostRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should accept POST request with CSRF token")
    @WithMockUser(roles = "ADMIN")
    void shouldAcceptPostRequestWithCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated());
    }

    // MULTIPLE ROLES TESTS

    @Test
    @DisplayName("Should allow user with multiple roles including ADMIN")
    @WithMockUser(roles = {"CUSTOMER", "ADMIN"})
    void shouldAllowUserWithMultipleRolesIncludingAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isNotFound());  // Authorization passed
    }

    @Test
    @DisplayName("Should allow user with multiple roles including MANAGER for create account")
    @WithMockUser(roles = {"CUSTOMER", "MANAGER"})
    void shouldAllowUserWithMultipleRolesIncludingManagerForCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated());
    }

    // TOKEN BLACKLIST TESTS

    @Test
    @DisplayName("Should deny access when token is blacklisted")
    @WithMockUser(roles = "ADMIN")
    void shouldDenyAccessWhenTokenIsBlacklisted() throws Exception {
        when(tokenBlacklistService.isTokenBlacklisted(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/v1/accounts/1")
                        .header("Authorization", "Bearer blacklisted-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow access when token is not blacklisted")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAccessWhenTokenIsNotBlacklisted() throws Exception {
        when(tokenBlacklistService.isTokenBlacklisted(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isNotFound());  // Token is valid, account not found
    }

    // METHOD SECURITY TESTS

    @Test
    @DisplayName("Should enforce method-level security on create account")
    @WithMockUser(roles = "CUSTOMER")
    void shouldEnforceMethodLevelSecurityOnCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should enforce method-level security on freeze account")
    @WithMockUser(roles = "CUSTOMER")
    void shouldEnforceMethodLevelSecurityOnFreezeAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should enforce method-level security on close account")
    @WithMockUser(roles = "MANAGER")
    void shouldEnforceMethodLevelSecurityOnCloseAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // COMPREHENSIVE AUTHORIZATION MATRIX TESTS

    @Test
    @DisplayName("ADMIN should have access to all operations")
    @WithMockUser(roles = "ADMIN")
    void adminShouldHaveAccessToAllOperations() throws Exception {
        // Create - allowed
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated());

        // Freeze - allowed
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isNotFound());  // Authorization passed

        // Close - allowed
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isNotFound());  // Authorization passed

        // View - allowed
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isNotFound());  // Authorization passed
    }

    @Test
    @DisplayName("MANAGER should have limited access")
    @WithMockUser(roles = "MANAGER")
    void managerShouldHaveLimitedAccess() throws Exception {
        // Create - allowed
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isCreated());

        // Freeze - denied
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Close - denied
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // View - allowed
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isNotFound());  // Authorization passed
    }

    @Test
    @DisplayName("CUSTOMER should have minimal access")
    @WithMockUser(roles = "CUSTOMER")
    void customerShouldHaveMinimalAccess() throws Exception {
        // Create - denied
        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                .andExpect(status().isForbidden());

        // Freeze - denied
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/freeze")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Close - denied
        mockMvc.perform(post("/api/v1/accounts/TR330006100519786457841326/close")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // View - allowed
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isNotFound());  // Authorization passed
    }
}
