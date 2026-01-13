package com.banking.account.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Token Blacklist Service Tests")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private String sampleToken;

    @BeforeEach
    void setUp() {
        sampleToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature";
    }

    @Test
    @DisplayName("Should return true when token is blacklisted")
    void shouldReturnTrueWhenTokenIsBlacklisted() {
        // Given
        String key = "token:blacklist:" + sampleToken;
        when(redisTemplate.hasKey(key)).thenReturn(true);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(sampleToken);

        // Then
        assertThat(isBlacklisted).isTrue();
        verify(redisTemplate).hasKey(key);
    }

    @Test
    @DisplayName("Should return false when token is not blacklisted")
    void shouldReturnFalseWhenTokenIsNotBlacklisted() {
        // Given
        String key = "token:blacklist:" + sampleToken;
        when(redisTemplate.hasKey(key)).thenReturn(false);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(sampleToken);

        // Then
        assertThat(isBlacklisted).isFalse();
        verify(redisTemplate).hasKey(key);
    }

    @Test
    @DisplayName("Should return false when token key does not exist in Redis")
    void shouldReturnFalseWhenTokenKeyDoesNotExist() {
        // Given
        String key = "token:blacklist:" + sampleToken;
        when(redisTemplate.hasKey(key)).thenReturn(null); // Redis returns null for non-existent keys

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(sampleToken);

        // Then
        assertThat(isBlacklisted).isFalse();
        verify(redisTemplate).hasKey(key);
    }

    @Test
    @DisplayName("Should return false when Redis is unavailable (graceful degradation)")
    void shouldReturnFalseWhenRedisIsUnavailable() {
        // Given
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection error"));

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(sampleToken);

        // Then
        assertThat(isBlacklisted).isFalse(); // Graceful degradation: fail-open
        verify(redisTemplate).hasKey(anyString());
    }

    @Test
    @DisplayName("Should use correct key prefix for blacklist check")
    void shouldUseCorrectKeyPrefixForBlacklistCheck() {
        // Given
        String expectedKey = "token:blacklist:" + sampleToken;
        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

        // When
        tokenBlacklistService.isTokenBlacklisted(sampleToken);

        // Then
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("Should remove token from blacklist")
    void shouldRemoveTokenFromBlacklist() {
        // Given
        String key = "token:blacklist:" + sampleToken;
        when(redisTemplate.delete(key)).thenReturn(true);

        // When
        tokenBlacklistService.removeFromBlacklist(sampleToken);

        // Then
        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("Should handle exception when removing token from blacklist")
    void shouldHandleExceptionWhenRemovingTokenFromBlacklist() {
        // Given
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis error"));

        // When & Then
        assertThatCode(() -> tokenBlacklistService.removeFromBlacklist(sampleToken))
            .doesNotThrowAnyException(); // Should not throw exception, just log error
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("Should get token blacklist TTL")
    void shouldGetTokenBlacklistTtl() {
        // Given
        String key = "token:blacklist:" + sampleToken;
        long expectedTtl = 900000L; // 15 minutes in milliseconds
        when(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS)).thenReturn(expectedTtl);

        // When
        Long ttl = tokenBlacklistService.getTokenBlacklistTTL(sampleToken);

        // Then
        assertThat(ttl).isEqualTo(expectedTtl);
        verify(redisTemplate).getExpire(key, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("Should return -1 when getting TTL fails")
    void shouldReturnMinusOneWhenGettingTtlFails() {
        // Given
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.MILLISECONDS)))
            .thenThrow(new RuntimeException("Redis error"));

        // When
        Long ttl = tokenBlacklistService.getTokenBlacklistTTL(sampleToken);

        // Then
        assertThat(ttl).isEqualTo(-1L); // Error case returns -1
        verify(redisTemplate).getExpire(anyString(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should handle null token gracefully in isTokenBlacklisted")
    void shouldHandleNullTokenGracefullyInIsTokenBlacklisted() {
        // Given
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(null);

        // Then
        assertThat(isBlacklisted).isFalse();
    }

    @Test
    @DisplayName("Should handle empty token gracefully in isTokenBlacklisted")
    void shouldHandleEmptyTokenGracefullyInIsTokenBlacklisted() {
        // Given
        String key = "token:blacklist:";
        when(redisTemplate.hasKey(key)).thenReturn(false);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted("");

        // Then
        assertThat(isBlacklisted).isFalse();
        verify(redisTemplate).hasKey(key);
    }

    @Test
    @DisplayName("Should check multiple tokens independently")
    void shouldCheckMultipleTokensIndependently() {
        // Given
        String token1 = "token1";
        String token2 = "token2";
        String token3 = "token3";

        when(redisTemplate.hasKey("token:blacklist:token1")).thenReturn(true);
        when(redisTemplate.hasKey("token:blacklist:token2")).thenReturn(false);
        when(redisTemplate.hasKey("token:blacklist:token3")).thenReturn(true);

        // When
        boolean isBlacklisted1 = tokenBlacklistService.isTokenBlacklisted(token1);
        boolean isBlacklisted2 = tokenBlacklistService.isTokenBlacklisted(token2);
        boolean isBlacklisted3 = tokenBlacklistService.isTokenBlacklisted(token3);

        // Then
        assertThat(isBlacklisted1).isTrue();
        assertThat(isBlacklisted2).isFalse();
        assertThat(isBlacklisted3).isTrue();

        verify(redisTemplate, times(3)).hasKey(anyString());
    }

    @Test
    @DisplayName("Should get TTL for token with remaining time")
    void shouldGetTtlForTokenWithRemainingTime() {
        // Given
        String key = "token:blacklist:" + sampleToken;
        long ttl = 600000L; // 10 minutes
        when(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS)).thenReturn(ttl);

        // When
        Long actualTtl = tokenBlacklistService.getTokenBlacklistTTL(sampleToken);

        // Then
        assertThat(actualTtl).isEqualTo(ttl);
    }

    @Test
    @DisplayName("Should handle Redis timeout exception gracefully")
    void shouldHandleRedisTimeoutExceptionGracefully() {
        // Given
        when(redisTemplate.hasKey(anyString()))
            .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Connection timeout"));

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(sampleToken);

        // Then
        assertThat(isBlacklisted).isFalse(); // Graceful degradation
    }
}
