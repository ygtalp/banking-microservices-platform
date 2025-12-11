package com.banking.transfer.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of requests fail
                .slowCallRateThreshold(50) // Open circuit if 50% of calls are slow
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
                .minimumNumberOfCalls(5) // Need at least 5 calls to calculate failure rate
                .slidingWindowSize(10) // Look at last 10 calls
                .build();
    }

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3) // Retry up to 3 times
                .waitDuration(Duration.ofSeconds(1)) // Wait 1s between retries
                .retryExceptions(Exception.class)
                .build();
    }

    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3)) // Timeout after 3 seconds
                .build();
    }
}