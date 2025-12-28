package com.banking.auth.security;

import com.banking.auth.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class JwtTokenProviderTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Authentication authentication;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Setup JWT config
        when(jwtConfig.getSecret()).thenReturn("BankingPlatformSecretKeyForTestingPurposes2024MinimumLengthRequired");
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L); // 15 minutes
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L); // 7 days
        when(jwtConfig.getIssuer()).thenReturn("banking-platform-test");

        // Setup user details
        Collection<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                new SimpleGrantedAuthority("ROLE_USER")
        );

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(authorities)
                .build();
    }

    @Test
    @DisplayName("Should generate valid access token")
    void testGenerateAccessToken_Success() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        String token = jwtTokenProvider.generateAccessToken(authentication);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts

        // Verify token claims
        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("test@example.com", username);

        String tokenType = jwtTokenProvider.getTokenType(token);
        assertEquals("access", tokenType);

        List<String> roles = jwtTokenProvider.getRolesFromToken(token);
        assertTrue(roles.contains("ROLE_CUSTOMER"));
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void testGenerateRefreshToken_Success() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        String token = jwtTokenProvider.generateRefreshToken(authentication);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String tokenType = jwtTokenProvider.getTokenType(token);
        assertEquals("refresh", tokenType);
    }

    @Test
    @DisplayName("Should validate valid token")
    void testValidateToken_ValidToken() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtTokenProvider.generateAccessToken(authentication);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void testValidateToken_InvalidToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("invalid.token.here");

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for malformed token")
    void testValidateToken_MalformedToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("malformed-token");

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for empty token")
    void testValidateToken_EmptyToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("");

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for null token")
    void testValidateToken_NullToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract username from token")
    void testGetUsernameFromToken_Success() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtTokenProvider.generateAccessToken(authentication);

        // Act
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Assert
        assertEquals("test@example.com", username);
    }

    @Test
    @DisplayName("Should extract roles from token")
    void testGetRolesFromToken_Success() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtTokenProvider.generateAccessToken(authentication);

        // Act
        List<String> roles = jwtTokenProvider.getRolesFromToken(token);

        // Assert
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_CUSTOMER"));
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Should extract token type from token")
    void testGetTokenType_AccessToken() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtTokenProvider.generateAccessToken(authentication);

        // Act
        String tokenType = jwtTokenProvider.getTokenType(token);

        // Assert
        assertEquals("access", tokenType);
    }

    @Test
    @DisplayName("Should extract refresh token type")
    void testGetTokenType_RefreshToken() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtTokenProvider.generateRefreshToken(authentication);

        // Act
        String tokenType = jwtTokenProvider.getTokenType(token);

        // Assert
        assertEquals("refresh", tokenType);
    }

    @Test
    @DisplayName("Should get expiration date from token")
    void testGetExpirationDate_Success() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtTokenProvider.generateAccessToken(authentication);

        // Act
        Date expirationDate = jwtTokenProvider.getExpirationDate(token);

        // Assert
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    @DisplayName("Should detect non-expired token")
    void testIsTokenExpired_NotExpired() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String token = jwtTokenProvider.generateAccessToken(authentication);

        // Act
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should generate access token from username and roles")
    void testGenerateAccessTokenFromUsername_Success() {
        // Arrange
        String username = "test@example.com";
        List<String> roles = Arrays.asList("ROLE_CUSTOMER", "ROLE_USER");

        // Act
        String token = jwtTokenProvider.generateAccessTokenFromUsername(username, roles);

        // Assert
        assertNotNull(token);
        assertEquals(username, jwtTokenProvider.getUsernameFromToken(token));
        assertEquals("access", jwtTokenProvider.getTokenType(token));

        List<String> extractedRoles = jwtTokenProvider.getRolesFromToken(token);
        assertEquals(roles.size(), extractedRoles.size());
        assertTrue(extractedRoles.containsAll(roles));
    }

    @Test
    @DisplayName("Should generate refresh token from username and roles")
    void testGenerateRefreshTokenFromUsername_Success() {
        // Arrange
        String username = "test@example.com";
        List<String> roles = Arrays.asList("ROLE_CUSTOMER");

        // Act
        String token = jwtTokenProvider.generateRefreshTokenFromUsername(username, roles);

        // Assert
        assertNotNull(token);
        assertEquals(username, jwtTokenProvider.getUsernameFromToken(token));
        assertEquals("refresh", jwtTokenProvider.getTokenType(token));
    }

    @Test
    @DisplayName("Access token should have shorter expiration than refresh token")
    void testTokenExpiration_AccessShorterThanRefresh() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        Date accessExpiration = jwtTokenProvider.getExpirationDate(accessToken);
        Date refreshExpiration = jwtTokenProvider.getExpirationDate(refreshToken);

        // Assert
        assertTrue(accessExpiration.before(refreshExpiration));
    }

    @Test
    @DisplayName("Two tokens generated at different times should be different")
    void testGenerateToken_Uniqueness() throws InterruptedException {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        String token1 = jwtTokenProvider.generateAccessToken(authentication);
        Thread.sleep(1000); // Wait 1 second to ensure different iat
        String token2 = jwtTokenProvider.generateAccessToken(authentication);

        // Assert
        assertNotEquals(token1, token2); // Different tokens (different iat claim)

        // But same username and type
        assertEquals(
            jwtTokenProvider.getUsernameFromToken(token1),
            jwtTokenProvider.getUsernameFromToken(token2)
        );
        assertEquals(
            jwtTokenProvider.getTokenType(token1),
            jwtTokenProvider.getTokenType(token2)
        );
    }
}
