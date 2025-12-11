package com.banking.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Account Service Routes
                .route("account-service", r -> r
                        .path("/api/v1/accounts/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c.setName("accountServiceCircuitBreaker"))
                                .retry(retryConfig -> retryConfig.setRetries(3))
                        )
                        .uri("lb://account-service")
                )

                // Transfer Service Routes
                .route("transfer-service", r -> r
                        .path("/api/v1/transfers/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c.setName("transferServiceCircuitBreaker"))
                                .retry(retryConfig -> retryConfig.setRetries(3))
                        )
                        .uri("lb://transfer-service")
                )

                // Transaction Service Routes
                .route("transaction-service", r -> r
                        .path("/api/v1/transactions/**")
                        .uri("lb://transaction-service")
                )

                // Analytics Service Routes
                .route("analytics-service", r -> r
                        .path("/api/v1/analytics/**")
                        .uri("lb://analytics-service")
                )

                .build();
    }
}