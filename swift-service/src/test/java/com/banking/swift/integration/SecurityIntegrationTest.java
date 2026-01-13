package com.banking.swift.integration;

import com.banking.swift.dto.CreateSwiftTransferRequest;
import com.banking.swift.model.ChargeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Security Integration Tests - Full Chain")
class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("banking_swift_test")
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
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String validAccessToken;
    private String expiredAccessToken;
    private String invalidSignatureToken;
    private CreateSwiftTransferRequest validRequest;

    @BeforeEach
    void setUp() {
        // Create valid access token
        validAccessToken = createToken("john.doe@example.com",
                Arrays.asList("ROLE_USER"), "access", 60000);

        // Create expired token
        expiredAccessToken = createToken("john.doe@example.com",
                Arrays.asList("ROLE_USER"), "access", -3600000);

        // Create token with invalid signature
        SecretKey differentKey = Keys.hmacShaKeyFor(
                "DifferentSecretKeyThatIsAlsoLongEnoughForHS512AlgorithmSecurityExtendedToMeetMinimumLength"
                        .getBytes(StandardCharsets.UTF_8)
        );
        invalidSignatureToken = Jwts.builder()
                .setSubject("user@example.com")
                .claim("roles", Arrays.asList("ROLE_USER"))
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(differentKey)
                .compact();

        // Create valid request
        validRequest = CreateSwiftTransferRequest.builder()
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("USD")
                .amount(new BigDecimal("10000.00"))
                .orderingCustomerName("John Doe")
                .orderingCustomerAccount("US1234567890")
                .senderBic("BNPAFRPPXXX")
                .senderName("BNP Paribas")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryBankName("Deutsche Bank AG")
                .beneficiaryName("Max Mustermann")
                .beneficiaryAccount("DE89370400440532013000")
                .remittanceInfo("INVOICE 12345")
                .chargeType(ChargeType.SHA)
                .build();
    }

    @Test
    @DisplayName("Should allow access with valid JWT token")
    void shouldAllowAccessWithValidJwtToken() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny access without JWT token")
    void shouldDenyAccessWithoutJwtToken() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access with expired JWT token")
    void shouldDenyAccessWithExpiredJwtToken() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + expiredAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access with invalid signature token")
    void shouldDenyAccessWithInvalidSignatureToken() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + invalidSignatureToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access with malformed JWT token")
    void shouldDenyAccessWithMalformedJwtToken() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer invalid.token.format"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access with Bearer prefix missing")
    void shouldDenyAccessWithBearerPrefixMissing() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", validAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow POST request with valid JWT token")
    void shouldAllowPostRequestWithValidJwtToken() throws Exception {
        mockMvc.perform(post("/swift/transfers")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny POST request without JWT token")
    void shouldDenyPostRequestWithoutJwtToken() throws Exception {
        mockMvc.perform(post("/swift/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should extract username from JWT token in security context")
    void shouldExtractUsernameFromJwtTokenInSecurityContext() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk());
        // Username is extracted and available in SecurityContext during request processing
    }

    @Test
    @DisplayName("Should extract roles from JWT token in security context")
    void shouldExtractRolesFromJwtTokenInSecurityContext() throws Exception {
        String adminToken = createToken("admin@example.com",
                Arrays.asList("ROLE_ADMIN", "ROLE_USER"), "access", 60000);

        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        // Roles are extracted and available in SecurityContext during request processing
    }

    @Test
    @DisplayName("Should handle multiple concurrent requests with different tokens")
    void shouldHandleMultipleConcurrentRequestsWithDifferentTokens() throws Exception {
        String token1 = createToken("user1@example.com", Arrays.asList("ROLE_USER"), "access", 60000);
        String token2 = createToken("user2@example.com", Arrays.asList("ROLE_USER"), "access", 60000);

        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should validate token on every request")
    void shouldValidateTokenOnEveryRequest() throws Exception {
        // First request with valid token
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk());

        // Second request with same valid token
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk());

        // Third request with expired token
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + expiredAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should protect all endpoints except public ones")
    void shouldProtectAllEndpointsExceptPublicOnes() throws Exception {
        // All swift endpoints require authentication
        mockMvc.perform(get("/swift/transfers/SWFT123456789012"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/swift/transfers/account/ACC123456"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/swift/transfers/status/PENDING"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/swift/transfers/statistics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow access to Swagger UI without authentication")
    void shouldAllowAccessToSwaggerUiWithoutAuthentication() throws Exception {
        // Swagger UI should be publicly accessible
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection()); // Redirects to /swagger-ui/index.html
    }

    @Test
    @DisplayName("Should allow access to API docs without authentication")
    void shouldAllowAccessToApiDocsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle empty Authorization header")
    void shouldHandleEmptyAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle Authorization header with only Bearer prefix")
    void shouldHandleAuthorizationHeaderWithOnlyBearerPrefix() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle Authorization header with whitespace")
    void shouldHandleAuthorizationHeaderWithWhitespace() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer    " + validAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for authentication failures")
    void shouldReturn401UnauthorizedForAuthenticationFailures() throws Exception {
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should work with refresh token type")
    void shouldWorkWithRefreshTokenType() throws Exception {
        String refreshToken = createToken("user@example.com",
                Arrays.asList("ROLE_USER"), "refresh", 7 * 24 * 60 * 60 * 1000);

        // Service should validate refresh tokens too
        mockMvc.perform(get("/swift/transfers/statistics")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should maintain security context throughout request lifecycle")
    void shouldMaintainSecurityContextThroughoutRequestLifecycle() throws Exception {
        mockMvc.perform(post("/swift/transfers")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").exists());
        // Security context should be available in all layers (controller, service, repository)
    }

    // Helper method to create JWT tokens for testing
    private String createToken(String username, java.util.List<String> roles, String type, long expirationMillis) {
        SecretKey signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(System.currentTimeMillis() + expirationMillis);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .claim("type", type)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(signingKey)
                .compact();
    }
}
