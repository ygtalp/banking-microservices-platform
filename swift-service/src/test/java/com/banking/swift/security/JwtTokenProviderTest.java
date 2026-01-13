package com.banking.swift.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private String secret;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        // Use a 512-bit secret for HS512 algorithm
        secret = "ThisIsAVerySecureSecretKeyForHS512AlgorithmThatMustBeAtLeast512BitsLongForSecurity" +
                "AndThisIsExtendedToMeetTheMinimumLengthRequirementOf512BitsOrMore";
        when(jwtConfig.getSecret()).thenReturn(secret);
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsernameFromValidToken() {
        // Given
        String username = "john.doe@example.com";
        String token = createValidToken(username, Arrays.asList("ROLE_USER"), "access", 60000);

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should extract roles from valid token")
    void shouldExtractRolesFromValidToken() {
        // Given
        List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");
        String token = createValidToken("user@example.com", roles, "access", 60000);

        // When
        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);

        // Then
        assertThat(extractedRoles).containsExactlyInAnyOrderElementsOf(roles);
    }

    @Test
    @DisplayName("Should extract token type from valid token")
    void shouldExtractTokenTypeFromValidToken() {
        // Given
        String tokenType = "access";
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), tokenType, 60000);

        // When
        String extractedType = jwtTokenProvider.getTokenType(token);

        // Then
        assertThat(extractedType).isEqualTo(tokenType);
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {
        // Given
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), "access", 60000);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Given - Token expired 1 hour ago
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), "access", -3600000);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void shouldRejectMalformedToken() {
        // Given
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void shouldRejectTokenWithInvalidSignature() {
        // Given - Token signed with different secret
        SecretKey differentKey = Keys.hmacShaKeyFor(
                ("DifferentSecretKeyThatIsAlsoLongEnoughForHS512AlgorithmSecurity" +
                        "ExtendedToMeetMinimumLength").getBytes(StandardCharsets.UTF_8)
        );
        String token = Jwts.builder()
                .setSubject("user@example.com")
                .claim("roles", Arrays.asList("ROLE_USER"))
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(differentKey)
                .compact();

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        // Given - Token expired 1 hour ago
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), "access", -3600000);

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Should detect non-expired token")
    void shouldDetectNonExpiredToken() {
        // Given
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), "access", 60000);

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should get expiration date from token")
    void shouldGetExpirationDateFromToken() {
        // Given
        long expirationMillis = 60000;
        long expectedExpiration = System.currentTimeMillis() + expirationMillis;
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), "access", expirationMillis);

        // When
        Date expirationDate = jwtTokenProvider.getExpirationDate(token);

        // Then
        assertThat(expirationDate).isNotNull();
        assertThat(expirationDate.getTime()).isCloseTo(expectedExpiration, within(1000L));
    }

    @Test
    @DisplayName("Should handle token with single role")
    void shouldHandleTokenWithSingleRole() {
        // Given
        List<String> roles = Arrays.asList("ROLE_USER");
        String token = createValidToken("user@example.com", roles, "access", 60000);

        // When
        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);

        // Then
        assertThat(extractedRoles).hasSize(1);
        assertThat(extractedRoles).contains("ROLE_USER");
    }

    @Test
    @DisplayName("Should handle token with multiple roles")
    void shouldHandleTokenWithMultipleRoles() {
        // Given
        List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN", "ROLE_MANAGER");
        String token = createValidToken("user@example.com", roles, "access", 60000);

        // When
        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);

        // Then
        assertThat(extractedRoles).hasSize(3);
        assertThat(extractedRoles).containsExactlyInAnyOrderElementsOf(roles);
    }

    @Test
    @DisplayName("Should handle access token type")
    void shouldHandleAccessTokenType() {
        // Given
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), "access", 60000);

        // When
        String tokenType = jwtTokenProvider.getTokenType(token);

        // Then
        assertThat(tokenType).isEqualTo("access");
    }

    @Test
    @DisplayName("Should handle refresh token type")
    void shouldHandleRefreshTokenType() {
        // Given
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), "refresh", 60000);

        // When
        String tokenType = jwtTokenProvider.getTokenType(token);

        // Then
        assertThat(tokenType).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Should validate token with long expiration")
    void shouldValidateTokenWithLongExpiration() {
        // Given - Token valid for 7 days
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), "refresh", 7 * 24 * 60 * 60 * 1000);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Then
        assertThat(isValid).isTrue();
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should extract all information from complex token")
    void shouldExtractAllInformationFromComplexToken() {
        // Given
        String username = "admin@banking.com";
        List<String> roles = Arrays.asList("ROLE_ADMIN", "ROLE_SUPER_USER", "ROLE_AUDITOR");
        String tokenType = "access";
        String token = createValidToken(username, roles, tokenType, 60000);

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);
        String extractedType = jwtTokenProvider.getTokenType(token);
        Date expirationDate = jwtTokenProvider.getExpirationDate(token);
        boolean isValid = jwtTokenProvider.validateToken(token);
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
        assertThat(extractedRoles).containsExactlyInAnyOrderElementsOf(roles);
        assertThat(extractedType).isEqualTo(tokenType);
        assertThat(expirationDate).isNotNull();
        assertThat(isValid).isTrue();
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when extracting username from expired token")
    void shouldThrowExceptionWhenExtractingUsernameFromExpiredToken() {
        // Given
        String token = createValidToken("user@example.com", Arrays.asList("ROLE_USER"), "access", -3600000);

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should throw exception when extracting roles from malformed token")
    void shouldThrowExceptionWhenExtractingRolesFromMalformedToken() {
        // Given
        String malformedToken = "invalid.token.format";

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getRolesFromToken(malformedToken))
                .isInstanceOf(Exception.class);
    }

    // Helper method to create valid JWT tokens for testing
    private String createValidToken(String username, List<String> roles, String type, long expirationMillis) {
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
