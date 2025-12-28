package com.banking.customer.service;

import com.banking.customer.dto.request.*;
import com.banking.customer.dto.response.CustomerResponse;
import com.banking.customer.exception.CustomerAlreadyExistsException;
import com.banking.customer.exception.CustomerNotFoundException;
import com.banking.customer.exception.InvalidCustomerStateException;
import com.banking.customer.model.*;
import com.banking.customer.repository.CustomerHistoryRepository;
import com.banking.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerHistoryRepository historyRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private CustomerIdGenerator customerIdGenerator;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private RegisterCustomerRequest registerRequest;
    private Customer testCustomer;

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

        testCustomer = Customer.builder()
                .id(1L)
                .customerId("CUS-123456789ABC")
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
                .status(CustomerStatus.PENDING_VERIFICATION)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Should register customer successfully")
    void shouldRegisterCustomerSuccessfully() {
        // Given
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByNationalId(anyString())).thenReturn(false);
        when(customerIdGenerator.generateCustomerId()).thenReturn("CUS-123456789ABC");
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // When
        CustomerResponse response = customerService.registerCustomer(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo("CUS-123456789ABC");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("john.doe@test.com");
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);
        assertThat(response.getNationalId()).contains("***"); // Should be masked

        verify(customerRepository).existsByEmail("john.doe@test.com");
        verify(customerRepository).existsByNationalId("12345678901");
        verify(customerRepository).save(any(Customer.class));
        verify(historyRepository).save(any(CustomerHistory.class));
        verify(eventPublisher).publishCustomerCreated(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        when(customerRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> customerService.registerCustomer(registerRequest))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(customerRepository).existsByEmail("john.doe@test.com");
        verify(customerRepository, never()).save(any());
        verify(eventPublisher, never()).publishCustomerCreated(any());
    }

    @Test
    @DisplayName("Should throw exception when national ID already exists")
    void shouldThrowExceptionWhenNationalIdExists() {
        // Given
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByNationalId(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> customerService.registerCustomer(registerRequest))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining("national ID");

        verify(customerRepository).existsByNationalId("12345678901");
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get customer by customerId successfully")
    void shouldGetCustomerSuccessfully() {
        // Given
        when(customerRepository.findByCustomerId("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));

        // When
        CustomerResponse response = customerService.getCustomer("CUS-123456789ABC");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo("CUS-123456789ABC");
        assertThat(response.getNationalId()).contains("***"); // Masked
        verify(customerRepository).findByCustomerId("CUS-123456789ABC");
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        when(customerRepository.findByCustomerId(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomer("CUS-NOTFOUND"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("CUS-NOTFOUND");

        verify(customerRepository).findByCustomerId("CUS-NOTFOUND");
    }

    @Test
    @DisplayName("Should verify customer successfully")
    void shouldVerifyCustomerSuccessfully() {
        // Given
        VerifyCustomerRequest verifyRequest = new VerifyCustomerRequest("admin@bank.com", null);
        testCustomer.setStatus(CustomerStatus.PENDING_VERIFICATION);

        when(customerRepository.findByCustomerIdForUpdate("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setStatus(CustomerStatus.VERIFIED);
            customer.setVerifiedAt(LocalDateTime.now());
            customer.setVerifiedBy("admin@bank.com");
            return customer;
        });

        // When
        CustomerResponse response = customerService.verifyCustomer("CUS-123456789ABC", verifyRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.VERIFIED);
        assertThat(response.getVerifiedBy()).isEqualTo("admin@bank.com");
        assertThat(response.getVerifiedAt()).isNotNull();

        verify(customerRepository).findByCustomerIdForUpdate("CUS-123456789ABC");
        verify(customerRepository).save(any(Customer.class));
        verify(historyRepository).save(any(CustomerHistory.class));
        verify(eventPublisher).publishCustomerVerified(any(Customer.class), eq("admin@bank.com"));
    }

    @Test
    @DisplayName("Should throw exception when verifying non-pending customer")
    void shouldThrowExceptionWhenVerifyingNonPendingCustomer() {
        // Given
        VerifyCustomerRequest verifyRequest = new VerifyCustomerRequest("admin@bank.com", null);
        testCustomer.setStatus(CustomerStatus.VERIFIED); // Already verified

        when(customerRepository.findByCustomerIdForUpdate("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));

        // When & Then
        assertThatThrownBy(() -> customerService.verifyCustomer("CUS-123456789ABC", verifyRequest))
                .isInstanceOf(InvalidCustomerStateException.class)
                .hasMessageContaining("PENDING_VERIFICATION");

        verify(customerRepository, never()).save(any());
        verify(eventPublisher, never()).publishCustomerVerified(any(), any());
    }

    @Test
    @DisplayName("Should approve customer successfully")
    void shouldApproveCustomerSuccessfully() {
        // Given
        ApproveCustomerRequest approveRequest = ApproveCustomerRequest.builder()
                .approvedBy("manager@bank.com")
                .riskLevel(RiskLevel.LOW)
                .build();

        testCustomer.setStatus(CustomerStatus.VERIFIED);
        testCustomer.setVerifiedAt(LocalDateTime.now());
        testCustomer.setVerifiedBy("admin@bank.com");

        when(customerRepository.findByCustomerIdForUpdate("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setStatus(CustomerStatus.APPROVED);
            customer.setApprovedAt(LocalDateTime.now());
            customer.setApprovedBy("manager@bank.com");
            customer.setRiskLevel(RiskLevel.LOW);
            return customer;
        });

        // When
        CustomerResponse response = customerService.approveCustomer("CUS-123456789ABC", approveRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.APPROVED);
        assertThat(response.getApprovedBy()).isEqualTo("manager@bank.com");
        assertThat(response.getApprovedAt()).isNotNull();
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.LOW);

        verify(customerRepository).findByCustomerIdForUpdate("CUS-123456789ABC");
        verify(customerRepository).save(any(Customer.class));
        verify(historyRepository).save(any(CustomerHistory.class));
        verify(eventPublisher).publishCustomerApproved(any(Customer.class), eq("manager@bank.com"));
    }

    @Test
    @DisplayName("Should throw exception when approving non-verified customer")
    void shouldThrowExceptionWhenApprovingNonVerifiedCustomer() {
        // Given
        ApproveCustomerRequest approveRequest = ApproveCustomerRequest.builder()
                .approvedBy("manager@bank.com")
                .riskLevel(RiskLevel.LOW)
                .build();

        testCustomer.setStatus(CustomerStatus.PENDING_VERIFICATION); // Not verified yet

        when(customerRepository.findByCustomerIdForUpdate("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));

        // When & Then
        assertThatThrownBy(() -> customerService.approveCustomer("CUS-123456789ABC", approveRequest))
                .isInstanceOf(InvalidCustomerStateException.class)
                .hasMessageContaining("VERIFIED");

        verify(customerRepository, never()).save(any());
        verify(eventPublisher, never()).publishCustomerApproved(any(), any());
    }

    @Test
    @DisplayName("Should suspend customer successfully")
    void shouldSuspendCustomerSuccessfully() {
        // Given
        SuspendCustomerRequest suspendRequest = SuspendCustomerRequest.builder()
                .reason("Suspicious activity detected")
                .suspendedBy("compliance@bank.com")
                .build();

        testCustomer.setStatus(CustomerStatus.APPROVED);

        when(customerRepository.findByCustomerIdForUpdate("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setStatus(CustomerStatus.SUSPENDED);
            customer.setStatusReason("Suspicious activity detected");
            return customer;
        });

        // When
        CustomerResponse response = customerService.suspendCustomer("CUS-123456789ABC", suspendRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(response.getStatusReason()).isEqualTo("Suspicious activity detected");

        verify(customerRepository).findByCustomerIdForUpdate("CUS-123456789ABC");
        verify(customerRepository).save(any(Customer.class));
        verify(historyRepository).save(any(CustomerHistory.class));
        verify(eventPublisher).publishCustomerStatusChanged(any(Customer.class), eq("Suspicious activity detected"));
    }

    @Test
    @DisplayName("Should reactivate suspended customer successfully")
    void shouldReactivateSuspendedCustomerSuccessfully() {
        // Given
        testCustomer.setStatus(CustomerStatus.SUSPENDED);
        testCustomer.setStatusReason("Previous suspension");

        when(customerRepository.findByCustomerIdForUpdate("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setStatus(CustomerStatus.APPROVED);
            customer.setStatusReason(null);
            return customer;
        });

        // When
        CustomerResponse response = customerService.reactivateCustomer("CUS-123456789ABC");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.APPROVED);
        assertThat(response.getStatusReason()).isNull();

        verify(customerRepository).findByCustomerIdForUpdate("CUS-123456789ABC");
        verify(customerRepository).save(any(Customer.class));
        verify(historyRepository).save(any(CustomerHistory.class));
        verify(eventPublisher).publishCustomerStatusChanged(any(Customer.class), eq("Customer reactivated"));
    }

    @Test
    @DisplayName("Should close customer successfully")
    void shouldCloseCustomerSuccessfully() {
        // Given
        testCustomer.setStatus(CustomerStatus.APPROVED);

        when(customerRepository.findByCustomerIdForUpdate("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setStatus(CustomerStatus.CLOSED);
            return customer;
        });

        // When
        CustomerResponse response = customerService.closeCustomer("CUS-123456789ABC");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.CLOSED);

        verify(customerRepository).findByCustomerIdForUpdate("CUS-123456789ABC");
        verify(customerRepository).save(any(Customer.class));
        verify(historyRepository).save(any(CustomerHistory.class));
        verify(eventPublisher).publishCustomerStatusChanged(any(Customer.class), eq("Customer closed"));
    }

    @Test
    @DisplayName("Should update customer successfully")
    void shouldUpdateCustomerSuccessfully() {
        // Given
        UpdateCustomerRequest updateRequest = UpdateCustomerRequest.builder()
                .phoneNumber("+31687654321")
                .address("New Address 456")
                .city("Rotterdam")
                .postalCode("3011 AD")
                .build();

        when(customerRepository.findByCustomerIdForUpdate("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CustomerResponse response = customerService.updateCustomer("CUS-123456789ABC", updateRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPhoneNumber()).isEqualTo("+31687654321");
        assertThat(response.getAddress()).isEqualTo("New Address 456");
        assertThat(response.getCity()).isEqualTo("Rotterdam");

        verify(customerRepository).findByCustomerIdForUpdate("CUS-123456789ABC");
        verify(customerRepository).save(any(Customer.class));
        verify(historyRepository).save(any(CustomerHistory.class));
    }

    @Test
    @DisplayName("Should get customer history successfully")
    void shouldGetCustomerHistorySuccessfully() {
        // Given
        CustomerHistory history1 = CustomerHistory.builder()
                .customerId(1L)
                .operation("REGISTER")
                .previousStatus(null)
                .newStatus(CustomerStatus.PENDING_VERIFICATION)
                .description("Customer registered")
                .performedBy("system")
                .timestamp(LocalDateTime.now().minusDays(2))
                .build();

        CustomerHistory history2 = CustomerHistory.builder()
                .customerId(1L)
                .operation("VERIFY")
                .previousStatus(CustomerStatus.PENDING_VERIFICATION)
                .newStatus(CustomerStatus.VERIFIED)
                .description("Customer verified")
                .performedBy("admin@bank.com")
                .timestamp(LocalDateTime.now().minusDays(1))
                .build();

        when(customerRepository.findByCustomerId("CUS-123456789ABC"))
                .thenReturn(Optional.of(testCustomer));
        when(historyRepository.findByCustomerIdOrderByTimestampDesc(1L))
                .thenReturn(List.of(history2, history1));

        // When
        List<CustomerHistory> history = customerService.getCustomerHistory("CUS-123456789ABC");

        // Then
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getOperation()).isEqualTo("VERIFY");
        assertThat(history.get(1).getOperation()).isEqualTo("REGISTER");

        verify(customerRepository).findByCustomerId("CUS-123456789ABC");
        verify(historyRepository).findByCustomerIdOrderByTimestampDesc(1L);
    }

    @Test
    @DisplayName("Should get customer by email successfully")
    void shouldGetCustomerByEmailSuccessfully() {
        // Given
        when(customerRepository.findByEmail("john.doe@test.com"))
                .thenReturn(Optional.of(testCustomer));

        // When
        CustomerResponse response = customerService.getCustomerByEmail("john.doe@test.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john.doe@test.com");

        verify(customerRepository).findByEmail("john.doe@test.com");
    }

    @Test
    @DisplayName("Should get customer by national ID successfully")
    void shouldGetCustomerByNationalIdSuccessfully() {
        // Given
        when(customerRepository.findByNationalId("12345678901"))
                .thenReturn(Optional.of(testCustomer));

        // When
        CustomerResponse response = customerService.getCustomerByNationalId("12345678901");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getNationalId()).contains("***"); // Masked

        verify(customerRepository).findByNationalId("12345678901");
    }
}
