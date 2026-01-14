package com.banking.auth.service;

import com.banking.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Token Blacklist Service Unit Tests")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private String validToken;
    private String expiredToken;
    private Date futureExpirationDate;
    private Date pastExpirationDate;

    @BeforeEach
    void setUp() {
        validToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSJ9.signature";
        expiredToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqYW5lLmRvZUBleGFtcGxlLmNvbSJ9.expired";

        futureExpirationDate = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
        pastExpirationDate = new Date(System.currentTimeMillis() - 3600000); // 1 hour ago

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== BLACKLIST TOKEN TESTS ====================

    @Test
    @DisplayName("Should blacklist token with correct TTL")
    void shouldBlacklistTokenWithCorrectTTL() {
        // Given
        when(jwtTokenProvider.getExpirationDate(validToken)).thenReturn(futureExpirationDate);

        // When
        tokenBlacklistService.blacklistToken(validToken);

        // Then
        verify(jwtTokenProvider, times(1)).getExpirationDate(validToken);
        verify(valueOperations, times(1)).set(
                eq("token:blacklist:" + validToken),
                eq("blacklisted"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Should not blacklist already expired token")
    void shouldNotBlacklistAlreadyExpiredToken() {
        // Given
        when(jwtTokenProvider.getExpirationDate(expiredToken)).thenReturn(pastExpirationDate);

        // When
        tokenBlacklistService.blacklistToken(expiredToken);

        // Then
        verify(jwtTokenProvider, times(1)).getExpirationDate(expiredToken);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Should throw exception when Redis fails during blacklist")
    void shouldThrowExceptionWhenRedisFailsDuringBlacklist() {
        // Given
        when(jwtTokenProvider.getExpirationDate(validToken)).thenReturn(futureExpirationDate);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // When & Then
        assertThatThrownBy(() -> tokenBlacklistService.blacklistToken(validToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to blacklist token");
    }

    @Test
    @DisplayName("Should throw exception when JwtTokenProvider fails")
    void shouldThrowExceptionWhenJwtTokenProviderFails() {
        // Given
        when(jwtTokenProvider.getExpirationDate(validToken))
                .thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> tokenBlacklistService.blacklistToken(validToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to blacklist token");
    }

    @Test
    @DisplayName("Should blacklist token with very short TTL")
    void shouldBlacklistTokenWithVeryShortTTL() {
        // Given
        Date nearFutureDate = new Date(System.currentTimeMillis() + 1000); // 1 second from now
        when(jwtTokenProvider.getExpirationDate(validToken)).thenReturn(nearFutureDate);

        // When
        tokenBlacklistService.blacklistToken(validToken);

        // Then
        verify(valueOperations, times(1)).set(
                eq("token:blacklist:" + validToken),
                eq("blacklisted"),
                longThat(ttl -> ttl > 0 && ttl <= 1000),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Should blacklist token with long TTL")
    void shouldBlacklistTokenWithLongTTL() {
        // Given
        Date farFutureDate = new Date(System.currentTimeMillis() + 86400000); // 24 hours from now
        when(jwtTokenProvider.getExpirationDate(validToken)).thenReturn(farFutureDate);

        // When
        tokenBlacklistService.blacklistToken(validToken);

        // Then
        verify(valueOperations, times(1)).set(
                eq("token:blacklist:" + validToken),
                eq("blacklisted"),
                longThat(ttl -> ttl > 86000000), // Close to 24 hours in milliseconds
                eq(TimeUnit.MILLISECONDS)
        );
    }

    // ==================== CHECK BLACKLIST STATUS TESTS ====================

    @Test
    @DisplayName("Should return true when token is blacklisted")
    void shouldReturnTrueWhenTokenIsBlacklisted() {
        // Given
        when(redisTemplate.hasKey("token:blacklist:" + validToken)).thenReturn(true);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(validToken);

        // Then
        assertThat(isBlacklisted).isTrue();
        verify(redisTemplate, times(1)).hasKey("token:blacklist:" + validToken);
    }

    @Test
    @DisplayName("Should return false when token is not blacklisted")
    void shouldReturnFalseWhenTokenIsNotBlacklisted() {
        // Given
        when(redisTemplate.hasKey("token:blacklist:" + validToken)).thenReturn(false);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(validToken);

        // Then
        assertThat(isBlacklisted).isFalse();
        verify(redisTemplate, times(1)).hasKey("token:blacklist:" + validToken);
    }

    @Test
    @DisplayName("Should return false when Redis returns null")
    void shouldReturnFalseWhenRedisReturnsNull() {
        // Given
        when(redisTemplate.hasKey("token:blacklist:" + validToken)).thenReturn(null);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(validToken);

        // Then
        assertThat(isBlacklisted).isFalse();
        verify(redisTemplate, times(1)).hasKey("token:blacklist:" + validToken);
    }

    @Test
    @DisplayName("Should return true (fail-safe) when Redis fails during check")
    void shouldReturnTrueWhenRedisFailsDuringCheck() {
        // Given
        when(redisTemplate.hasKey("token:blacklist:" + validToken))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(validToken);

        // Then
        assertThat(isBlacklisted).isTrue(); // Fail-safe: deny access if Redis is down
        verify(redisTemplate, times(1)).hasKey("token:blacklist:" + validToken);
    }

    // ==================== REMOVE FROM BLACKLIST TESTS ====================

    @Test
    @DisplayName("Should remove token from blacklist successfully")
    void shouldRemoveTokenFromBlacklistSuccessfully() {
        // Given
        when(redisTemplate.delete("token:blacklist:" + validToken)).thenReturn(true);

        // When
        tokenBlacklistService.removeFromBlacklist(validToken);

        // Then
        verify(redisTemplate, times(1)).delete("token:blacklist:" + validToken);
    }

    @Test
    @DisplayName("Should handle removal when token not in blacklist")
    void shouldHandleRemovalWhenTokenNotInBlacklist() {
        // Given
        when(redisTemplate.delete("token:blacklist:" + validToken)).thenReturn(false);

        // When
        tokenBlacklistService.removeFromBlacklist(validToken);

        // Then
        verify(redisTemplate, times(1)).delete("token:blacklist:" + validToken);
    }

    @Test
    @DisplayName("Should handle Redis error during removal gracefully")
    void shouldHandleRedisErrorDuringRemovalGracefully() {
        // Given
        when(redisTemplate.delete("token:blacklist:" + validToken))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then - Should not throw exception
        assertThatCode(() -> tokenBlacklistService.removeFromBlacklist(validToken))
                .doesNotThrowAnyException();

        verify(redisTemplate, times(1)).delete("token:blacklist:" + validToken);
    }

    // ==================== GET TOKEN TTL TESTS ====================

    @Test
    @DisplayName("Should get token blacklist TTL successfully")
    void shouldGetTokenBlacklistTTLSuccessfully() {
        // Given
        long expectedTTL = 3600000L; // 1 hour in milliseconds
        when(redisTemplate.getExpire("token:blacklist:" + validToken, TimeUnit.MILLISECONDS))
                .thenReturn(expectedTTL);

        // When
        Long ttl = tokenBlacklistService.getTokenBlacklistTTL(validToken);

        // Then
        assertThat(ttl).isEqualTo(expectedTTL);
        verify(redisTemplate, times(1)).getExpire("token:blacklist:" + validToken, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("Should return -1 when Redis fails to get TTL")
    void shouldReturnMinusOneWhenRedisFailsToGetTTL() {
        // Given
        when(redisTemplate.getExpire("token:blacklist:" + validToken, TimeUnit.MILLISECONDS))
                .thenThrow(new RuntimeException("Redis connection failed"));

        // When
        Long ttl = tokenBlacklistService.getTokenBlacklistTTL(validToken);

        // Then
        assertThat(ttl).isEqualTo(-1L);
        verify(redisTemplate, times(1)).getExpire("token:blacklist:" + validToken, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("Should return -2 when token does not exist in Redis")
    void shouldReturnMinusTwoWhenTokenDoesNotExist() {
        // Given
        when(redisTemplate.getExpire("token:blacklist:" + validToken, TimeUnit.MILLISECONDS))
                .thenReturn(-2L); // Redis returns -2 if key does not exist

        // When
        Long ttl = tokenBlacklistService.getTokenBlacklistTTL(validToken);

        // Then
        assertThat(ttl).isEqualTo(-2L);
        verify(redisTemplate, times(1)).getExpire("token:blacklist:" + validToken, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("Should return -1 when token has no expiration")
    void shouldReturnMinusOneWhenTokenHasNoExpiration() {
        // Given
        when(redisTemplate.getExpire("token:blacklist:" + validToken, TimeUnit.MILLISECONDS))
                .thenReturn(-1L); // Redis returns -1 if key exists but has no expiration

        // When
        Long ttl = tokenBlacklistService.getTokenBlacklistTTL(validToken);

        // Then
        assertThat(ttl).isEqualTo(-1L);
        verify(redisTemplate, times(1)).getExpire("token:blacklist:" + validToken, TimeUnit.MILLISECONDS);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle multiple blacklist operations on same token")
    void shouldHandleMultipleBlacklistOperationsOnSameToken() {
        // Given
        when(jwtTokenProvider.getExpirationDate(validToken)).thenReturn(futureExpirationDate);

        // When
        tokenBlacklistService.blacklistToken(validToken);
        tokenBlacklistService.blacklistToken(validToken); // Second blacklist

        // Then
        verify(jwtTokenProvider, times(2)).getExpirationDate(validToken);
        verify(valueOperations, times(2)).set(
                eq("token:blacklist:" + validToken),
                eq("blacklisted"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Should handle blacklist and check operations in sequence")
    void shouldHandleBlacklistAndCheckOperationsInSequence() {
        // Given
        when(jwtTokenProvider.getExpirationDate(validToken)).thenReturn(futureExpirationDate);
        when(redisTemplate.hasKey("token:blacklist:" + validToken)).thenReturn(true);

        // When
        tokenBlacklistService.blacklistToken(validToken);
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(validToken);

        // Then
        assertThat(isBlacklisted).isTrue();
        verify(valueOperations, times(1)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        verify(redisTemplate, times(1)).hasKey("token:blacklist:" + validToken);
    }

    @Test
    @DisplayName("Should handle blacklist, remove, and check operations in sequence")
    void shouldHandleBlacklistRemoveAndCheckOperationsInSequence() {
        // Given
        when(jwtTokenProvider.getExpirationDate(validToken)).thenReturn(futureExpirationDate);
        // After removal, token should not be in blacklist
        when(redisTemplate.hasKey("token:blacklist:" + validToken)).thenReturn(false);

        // When
        tokenBlacklistService.blacklistToken(validToken);
        tokenBlacklistService.removeFromBlacklist(validToken);
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(validToken);

        // Then
        assertThat(isBlacklisted).isFalse();
        verify(valueOperations, times(1)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        verify(redisTemplate, times(1)).delete("token:blacklist:" + validToken);
        verify(redisTemplate, times(1)).hasKey("token:blacklist:" + validToken);
    }

    @Test
    @DisplayName("Should use correct Redis key prefix for all operations")
    void shouldUseCorrectRedisKeyPrefixForAllOperations() {
        // Given
        when(jwtTokenProvider.getExpirationDate(validToken)).thenReturn(futureExpirationDate);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // When
        tokenBlacklistService.blacklistToken(validToken);
        tokenBlacklistService.isTokenBlacklisted(validToken);
        tokenBlacklistService.removeFromBlacklist(validToken);
        tokenBlacklistService.getTokenBlacklistTTL(validToken);

        // Then
        String expectedKey = "token:blacklist:" + validToken;
        verify(valueOperations, times(1)).set(eq(expectedKey), anyString(), anyLong(), any(TimeUnit.class));
        verify(redisTemplate, times(1)).hasKey(expectedKey);
        verify(redisTemplate, times(1)).delete(expectedKey);
        verify(redisTemplate, times(1)).getExpire(expectedKey, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("Should handle different token formats")
    void shouldHandleDifferentTokenFormats() {
        // Given
        String shortToken = "short.token.abc";
        String longToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzA0MDY3MjAwLCJleHAiOjE3MDQwNzA4MDB9.VGhpcyBpcyBhIHZlcnkgbG9uZyB0b2tlbiB3aXRoIG1hbnkgY2hhcmFjdGVycyB0byB0ZXN0IGhhbmRsaW5n";

        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // When
        boolean isShortTokenBlacklisted = tokenBlacklistService.isTokenBlacklisted(shortToken);
        boolean isLongTokenBlacklisted = tokenBlacklistService.isTokenBlacklisted(longToken);

        // Then
        assertThat(isShortTokenBlacklisted).isTrue();
        assertThat(isLongTokenBlacklisted).isTrue();
        verify(redisTemplate, times(1)).hasKey("token:blacklist:" + shortToken);
        verify(redisTemplate, times(1)).hasKey("token:blacklist:" + longToken);
    }
}
