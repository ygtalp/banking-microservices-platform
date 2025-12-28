package com.banking.auth.controller;

import com.banking.auth.dto.*;
import com.banking.auth.model.Role;
import com.banking.auth.model.User;
import com.banking.auth.model.UserStatus;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.HashSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("AuthController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("banking_auth_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));

        // Disable Eureka for tests
        registry.add("eureka.client.enabled", () -> false);

        // Disable Kafka for tests
        registry.add("spring.kafka.enabled", () -> false);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Clean database
        userRepository.deleteAll();

        // Ensure roles exist
        if (roleRepository.findByRoleName("CUSTOMER").isEmpty()) {
            Role customerRole = Role.builder()
                    .roleName("CUSTOMER")
                    .description("Customer role")
                    .permissions(new HashSet<>())
                    .build();
            roleRepository.save(customerRole);
        }

        if (roleRepository.findByRoleName("ADMIN").isEmpty()) {
            Role adminRole = Role.builder()
                    .roleName("ADMIN")
                    .description("Admin role")
                    .permissions(new HashSet<>())
                    .build();
            roleRepository.save(adminRole);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should register new user successfully")
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Test@1234")
                .firstName("New")
                .lastName("User")
                .phoneNumber("+905551234567")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("New"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"));
    }

    @Test
    @Order(2)
    @DisplayName("Should fail registration with existing email")
    void testRegister_EmailAlreadyExists() throws Exception {
        // Arrange - Create user first
        Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElseThrow();
        User existingUser = User.builder()
                .userId("USR-EXISTING001")
                .email("existing@example.com")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .firstName("Existing")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build();
        userRepository.save(existingUser);

        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("Test@1234")
                .firstName("Another")
                .lastName("User")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @Order(3)
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Success() throws Exception {
        // Arrange - Create user first
        Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElseThrow();
        User user = User.builder()
                .userId("USR-LOGIN001")
                .email("login@example.com")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .firstName("Login")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("login@example.com")
                .password("Test@1234")
                .build();

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        // Save token for later tests
        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<?> response = objectMapper.readValue(responseBody, ApiResponse.class);
        @SuppressWarnings("unchecked")
        var data = (java.util.LinkedHashMap<String, Object>) response.getData();
        userToken = (String) data.get("accessToken");
    }

    @Test
    @Order(4)
    @DisplayName("Should fail login with invalid credentials")
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("invalid@example.com")
                .password("WrongPassword")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    @Order(5)
    @DisplayName("Should get current user profile")
    void testGetCurrentUser_Success() throws Exception {
        // First login to get token
        Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElseThrow();
        User user = User.builder()
                .userId("USR-PROFILE001")
                .email("profile@example.com")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .firstName("Profile")
                .lastName("User")
                .phoneNumber("+905551234567")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("profile@example.com")
                .password("Test@1234")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        ApiResponse<?> response = objectMapper.readValue(responseBody, ApiResponse.class);
        @SuppressWarnings("unchecked")
        var data = (java.util.LinkedHashMap<String, Object>) response.getData();
        String token = (String) data.get("accessToken");

        // Get profile
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("profile@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Profile"))
                .andExpect(jsonPath("$.data.phoneNumber").value("+905551234567"));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to get profile without token")
    void testGetCurrentUser_Unauthorized() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("Should logout successfully")
    void testLogout_Success() throws Exception {
        // First login
        Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElseThrow();
        User user = User.builder()
                .userId("USR-LOGOUT001")
                .email("logout@example.com")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .firstName("Logout")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("logout@example.com")
                .password("Test@1234")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        ApiResponse<?> response = objectMapper.readValue(responseBody, ApiResponse.class);
        @SuppressWarnings("unchecked")
        var data = (java.util.LinkedHashMap<String, Object>) response.getData();
        String token = (String) data.get("accessToken");

        // Logout
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Try to use token after logout - should fail
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("Should validate password requirements")
    void testRegister_PasswordValidation() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("weakpass@example.com")
                .password("weak")  // Too weak
                .firstName("Weak")
                .lastName("Pass")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @Order(9)
    @DisplayName("Should check health endpoint")
    void testHealth_Success() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Auth service is healthy"));
    }
}
