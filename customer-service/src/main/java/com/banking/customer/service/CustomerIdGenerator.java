package com.banking.customer.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CustomerIdGenerator {

    private static final String PREFIX = "CUS-";

    /**
     * Generate unique customer ID
     * Format: CUS-{12-char-UUID}
     */
    public String generateCustomerId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return PREFIX + uuid.substring(0, 12).toUpperCase();
    }
}
