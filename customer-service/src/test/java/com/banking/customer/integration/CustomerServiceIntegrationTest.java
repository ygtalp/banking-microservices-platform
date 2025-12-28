package com.banking.customer.integration;

import com.banking.customer.dto.request.*;
import com.banking.customer.dto.response.CustomerResponse;
import com.banking.customer.exception.CustomerAlreadyExistsException;
import com.banking.customer.exception.CustomerNotFoundException;
import com.banking.customer.exception.InvalidCustomerStateException;
import com.banking.customer.model.Customer;
import com.banking.customer.model.CustomerHistory;
import com.banking.customer.model.CustomerStatus;
import com.banking.customer.model.RiskLevel;
import com.banking.customer.repository.CustomerHistoryRepository;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("CustomerService Integration Tests")
class CustomerServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("banking_customers_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");

        // Disable Kafka for integration tests
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9999");
        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");

        // Disable Eureka for integration tests
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerHistoryRepository historyRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        historyRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Should persist customer to database and create history")
    void shouldPersistCustomerToDatabase() {
        // Given
        RegisterCustomerRequest request = RegisterCustomerRequest.builder()
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

        // When
        CustomerResponse response = customerService.registerCustomer(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);

        // Verify database persistence
        Customer savedCustomer = customerRepository.findByCustomerId(response.getCustomerId()).orElseThrow();
        assertThat(savedCustomer.getFirstName()).isEqualTo("John");
        assertThat(savedCustomer.getLastName()).isEqualTo("Doe");
        assertThat(savedCustomer.getEmail()).isEqualTo("john.doe@test.com");
        assertThat(savedCustomer.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);

        // Verify history was created
        List<CustomerHistory> history = historyRepository.findByCustomerIdOrderByTimestampDesc(savedCustomer.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getOperation()).isEqualTo("REGISTER");
        assertThat(history.get(0).getNewStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void shouldEnforceUniqueEmailConstraint() {
        // Given
        RegisterCustomerRequest request1 = RegisterCustomerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("duplicate@test.com")
                .phoneNumber("+31612345678")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .nationalId("12345678901")
                .address("Test Street 123")
                .city("Amsterdam")
                .country("Netherlands")
                .build();

        RegisterCustomerRequest request2 = RegisterCustomerRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("duplicate@test.com") // Same email
                .phoneNumber("+31687654321")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .nationalId("09876543210")
                .address("Other Street 456")
                .city("Rotterdam")
                .country("Netherlands")
                .build();

        // When
        customerService.registerCustomer(request1);

        // Then
        assertThatThrownBy(() -> customerService.registerCustomer(request2))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining("email");

        // Verify only one customer was created
        List<Customer> customers = customerRepository.findAll();
        assertThat(customers).hasSize(1);
    }

    @Test
    @DisplayName("Should complete full verification workflow")
    void shouldCompleteFullVerificationWorkflow() {
        // Given - Register customer
        RegisterCustomerRequest registerRequest = RegisterCustomerRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@test.com")
                .phoneNumber("+31687654321")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .nationalId("09876543210")
                .address("Test Avenue 789")
                .city("Rotterdam")
                .country("Netherlands")
                .build();

        CustomerResponse registered = customerService.registerCustomer(registerRequest);
        assertThat(registered.getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);

        // When - Verify customer
        VerifyCustomerRequest verifyRequest = new VerifyCustomerRequest("admin@bank.com", null);
        CustomerResponse verified = customerService.verifyCustomer(registered.getCustomerId(), verifyRequest);

        // Then
        assertThat(verified.getStatus()).isEqualTo(CustomerStatus.VERIFIED);
        assertThat(verified.getVerifiedBy()).isEqualTo("admin@bank.com");
        assertThat(verified.getVerifiedAt()).isNotNull();

        // Verify database state
        Customer customer = customerRepository.findByCustomerId(registered.getCustomerId()).orElseThrow();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.VERIFIED);
        assertThat(customer.getVerifiedBy()).isEqualTo("admin@bank.com");

        // Verify history
        List<CustomerHistory> history = historyRepository.findByCustomerIdOrderByTimestampDesc(customer.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getOperation()).isEqualTo("VERIFY");
        assertThat(history.get(1).getOperation()).isEqualTo("REGISTER");
    }

    @Test
    @DisplayName("Should complete full approval workflow")
    void shouldCompleteFullApprovalWorkflow() {
        // Given - Register and verify customer
        RegisterCustomerRequest registerRequest = RegisterCustomerRequest.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@test.com")
                .phoneNumber("+31698765432")
                .dateOfBirth(LocalDate.of(1985, 8, 10))
                .nationalId("11223344556")
                .address("Central Street 100")
                .city("Utrecht")
                .country("Netherlands")
                .build();

        CustomerResponse registered = customerService.registerCustomer(registerRequest);
        VerifyCustomerRequest verifyRequest = new VerifyCustomerRequest("admin@bank.com", null);
        CustomerResponse verified = customerService.verifyCustomer(registered.getCustomerId(), verifyRequest);

        assertThat(verified.getStatus()).isEqualTo(CustomerStatus.VERIFIED);

        // When - Approve customer
        ApproveCustomerRequest approveRequest = ApproveCustomerRequest.builder()
                .approvedBy("manager@bank.com")
                .riskLevel(RiskLevel.LOW)
                .build();

        CustomerResponse approved = customerService.approveCustomer(verified.getCustomerId(), approveRequest);

        // Then
        assertThat(approved.getStatus()).isEqualTo(CustomerStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo("manager@bank.com");
        assertThat(approved.getApprovedAt()).isNotNull();
        assertThat(approved.getRiskLevel()).isEqualTo(RiskLevel.LOW);

        // Verify database state
        Customer customer = customerRepository.findByCustomerId(registered.getCustomerId()).orElseThrow();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.APPROVED);
        assertThat(customer.getRiskLevel()).isEqualTo(RiskLevel.LOW);

        // Verify complete history
        List<CustomerHistory> history = historyRepository.findByCustomerIdOrderByTimestampDesc(customer.getId());
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getOperation()).isEqualTo("APPROVE");
        assertThat(history.get(1).getOperation()).isEqualTo("VERIFY");
        assertThat(history.get(2).getOperation()).isEqualTo("REGISTER");
    }

    @Test
    @DisplayName("Should handle suspend and reactivate workflow")
    void shouldHandleSuspendAndReactivateWorkflow() {
        // Given - Create approved customer
        RegisterCustomerRequest registerRequest = RegisterCustomerRequest.builder()
                .firstName("Alice")
                .lastName("Williams")
                .email("alice.williams@test.com")
                .phoneNumber("+31611223344")
                .dateOfBirth(LocalDate.of(1992, 3, 25))
                .nationalId("99887766554")
                .address("Park Lane 200")
                .city("The Hague")
                .country("Netherlands")
                .build();

        CustomerResponse registered = customerService.registerCustomer(registerRequest);
        customerService.verifyCustomer(registered.getCustomerId(), new VerifyCustomerRequest("admin@bank.com", null));
        customerService.approveCustomer(registered.getCustomerId(),
                ApproveCustomerRequest.builder()
                        .approvedBy("manager@bank.com")
                        .riskLevel(RiskLevel.LOW)
                        .build());

        // When - Suspend customer
        SuspendCustomerRequest suspendRequest = SuspendCustomerRequest.builder()
                .reason("Suspicious activity detected")
                .suspendedBy("compliance@bank.com")
                .build();

        CustomerResponse suspended = customerService.suspendCustomer(registered.getCustomerId(), suspendRequest);

        // Then
        assertThat(suspended.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(suspended.getStatusReason()).isEqualTo("Suspicious activity detected");

        // When - Reactivate customer
        CustomerResponse reactivated = customerService.reactivateCustomer(registered.getCustomerId());

        // Then
        assertThat(reactivated.getStatus()).isEqualTo(CustomerStatus.APPROVED);
        assertThat(reactivated.getStatusReason()).isNull();

        // Verify history
        Customer customer = customerRepository.findByCustomerId(registered.getCustomerId()).orElseThrow();
        List<CustomerHistory> history = historyRepository.findByCustomerIdOrderByTimestampDesc(customer.getId());
        assertThat(history).hasSize(5); // REGISTER, VERIFY, APPROVE, SUSPEND, REACTIVATE
    }

    @Test
    @DisplayName("Should update customer information")
    void shouldUpdateCustomerInformation() {
        // Given
        RegisterCustomerRequest registerRequest = RegisterCustomerRequest.builder()
                .firstName("Tom")
                .lastName("Brown")
                .email("tom.brown@test.com")
                .phoneNumber("+31655443322")
                .dateOfBirth(LocalDate.of(1988, 11, 5))
                .nationalId("55667788990")
                .address("Old Street 50")
                .city("Eindhoven")
                .country("Netherlands")
                .postalCode("5600 AB")
                .build();

        CustomerResponse registered = customerService.registerCustomer(registerRequest);

        // When
        UpdateCustomerRequest updateRequest = UpdateCustomerRequest.builder()
                .phoneNumber("+31699887766")
                .address("New Avenue 100")
                .city("Maastricht")
                .postalCode("6200 CD")
                .build();

        CustomerResponse updated = customerService.updateCustomer(registered.getCustomerId(), updateRequest);

        // Then
        assertThat(updated.getPhoneNumber()).isEqualTo("+31699887766");
        assertThat(updated.getAddress()).isEqualTo("New Avenue 100");
        assertThat(updated.getCity()).isEqualTo("Maastricht");
        assertThat(updated.getPostalCode()).isEqualTo("6200 CD");

        // Verify database
        Customer customer = customerRepository.findByCustomerId(registered.getCustomerId()).orElseThrow();
        assertThat(customer.getPhoneNumber()).isEqualTo("+31699887766");
        assertThat(customer.getAddress()).isEqualTo("New Avenue 100");
    }

    @Test
    @DisplayName("Should enforce state transitions")
    void shouldEnforceStateTransitions() {
        // Given - Customer in PENDING_VERIFICATION
        RegisterCustomerRequest registerRequest = RegisterCustomerRequest.builder()
                .firstName("Sarah")
                .lastName("Davis")
                .email("sarah.davis@test.com")
                .phoneNumber("+31644556677")
                .dateOfBirth(LocalDate.of(1993, 7, 15))
                .nationalId("11222333444")
                .address("Main Road 75")
                .city("Groningen")
                .country("Netherlands")
                .build();

        CustomerResponse registered = customerService.registerCustomer(registerRequest);

        // Then - Cannot approve without verification
        ApproveCustomerRequest approveRequest = ApproveCustomerRequest.builder()
                .approvedBy("manager@bank.com")
                .riskLevel(RiskLevel.MEDIUM)
                .build();

        assertThatThrownBy(() -> customerService.approveCustomer(registered.getCustomerId(), approveRequest))
                .isInstanceOf(InvalidCustomerStateException.class)
                .hasMessageContaining("VERIFIED");
    }

    @Test
    @DisplayName("Should retrieve customer by email and national ID")
    void shouldRetrieveCustomerByEmailAndNationalId() {
        // Given
        RegisterCustomerRequest registerRequest = RegisterCustomerRequest.builder()
                .firstName("Mike")
                .lastName("Wilson")
                .email("mike.wilson@test.com")
                .phoneNumber("+31633445566")
                .dateOfBirth(LocalDate.of(1987, 4, 20))
                .nationalId("66778899001")
                .address("River Street 300")
                .city("Leiden")
                .country("Netherlands")
                .build();

        CustomerResponse registered = customerService.registerCustomer(registerRequest);

        // When
        CustomerResponse byEmail = customerService.getCustomerByEmail("mike.wilson@test.com");
        CustomerResponse byNationalId = customerService.getCustomerByNationalId("66778899001");

        // Then
        assertThat(byEmail.getCustomerId()).isEqualTo(registered.getCustomerId());
        assertThat(byNationalId.getCustomerId()).isEqualTo(registered.getCustomerId());
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // When & Then
        assertThatThrownBy(() -> customerService.getCustomer("CUS-NOTEXIST"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("CUS-NOTEXIST");
    }
}
