package com.banking.fraud.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    private String secret;
    private Long accessTokenExpiration = 900000L;
    private Long refreshTokenExpiration = 604800000L;
    private String issuer = "banking-platform";
}
