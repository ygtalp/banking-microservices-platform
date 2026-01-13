package com.banking.swift.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * BIC (Bank Identifier Code) Validation Service
 *
 * BIC Format: AAAABBCCXXX
 * - AAAA: Bank code (4 letters)
 * - BB: Country code (2 letters, ISO 3166-1)
 * - CC: Location code (2 letters or digits)
 * - XXX: Branch code (3 letters or digits, optional)
 */
@Service
@Slf4j
public class BicValidationService {

    private static final Pattern BIC8_PATTERN = Pattern.compile("^[A-Z]{6}[A-Z0-9]{2}$");
    private static final Pattern BIC11_PATTERN = Pattern.compile("^[A-Z]{6}[A-Z0-9]{2}[A-Z0-9]{3}$");

    // Major correspondent banking countries
    private static final Set<String> SUPPORTED_COUNTRIES = Set.of(
        "US", "GB", "DE", "FR", "CH", "NL", "BE", "LU", "IT", "ES",
        "AT", "IE", "SE", "DK", "NO", "FI", "PT", "GR", "PL", "CZ",
        "HU", "RO", "BG", "HR", "SI", "SK", "EE", "LV", "LT", "MT",
        "CY", "IS", "TR", "JP", "CN", "HK", "SG", "AU", "CA", "AE",
        "SA", "IN", "BR", "MX", "AR", "CL", "ZA", "KE", "NG", "EG"
    );

    /**
     * Validate BIC format
     */
    public boolean isValidBic(String bic) {
        if (bic == null || bic.isEmpty()) {
            return false;
        }

        bic = bic.toUpperCase().trim();

        // BIC must be 8 or 11 characters
        if (bic.length() != 8 && bic.length() != 11) {
            log.warn("Invalid BIC length: {}", bic.length());
            return false;
        }

        // Validate format
        boolean validFormat = (bic.length() == 8 && BIC8_PATTERN.matcher(bic).matches()) ||
                             (bic.length() == 11 && BIC11_PATTERN.matcher(bic).matches());

        if (!validFormat) {
            log.warn("Invalid BIC format: {}", bic);
            return false;
        }

        // Validate country code
        String countryCode = bic.substring(4, 6);
        if (!SUPPORTED_COUNTRIES.contains(countryCode)) {
            log.warn("Unsupported country code in BIC: {}", countryCode);
            return false;
        }

        return true;
    }

    /**
     * Extract country code from BIC
     */
    public String extractCountryCode(String bic) {
        if (bic == null || bic.length() < 6) {
            throw new IllegalArgumentException("Invalid BIC format");
        }
        return bic.substring(4, 6);
    }

    /**
     * Extract bank code from BIC
     */
    public String extractBankCode(String bic) {
        if (bic == null || bic.length() < 4) {
            throw new IllegalArgumentException("Invalid BIC format");
        }
        return bic.substring(0, 4);
    }

    /**
     * Check if BIC is in supported countries
     */
    public boolean isSupportedCountry(String bic) {
        String countryCode = extractCountryCode(bic);
        return SUPPORTED_COUNTRIES.contains(countryCode);
    }

    /**
     * Normalize BIC to 11 characters (add XXX if 8-char BIC)
     */
    public String normalizeBic(String bic) {
        if (bic == null) {
            throw new IllegalArgumentException("BIC cannot be null");
        }

        bic = bic.toUpperCase().trim();

        if (bic.length() == 8) {
            return bic + "XXX";
        } else if (bic.length() == 11) {
            return bic;
        } else {
            throw new IllegalArgumentException("Invalid BIC length: " + bic.length());
        }
    }
}