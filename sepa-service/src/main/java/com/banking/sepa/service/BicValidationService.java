package com.banking.sepa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for validating SWIFT BIC (Bank Identifier Code) codes.
 * BIC codes are used to identify banks in SEPA and international transactions.
 *
 * BIC Format:
 * - 4 letters: Bank code
 * - 2 letters: Country code (ISO 3166-1 alpha-2)
 * - 2 letters or digits: Location code
 * - 3 letters or digits (optional): Branch code
 * Total: 8 or 11 characters
 */
@Service
@Slf4j
public class BicValidationService {

    // BIC format: 4 letters (bank) + 2 letters (country) + 2 alphanumeric (location) + optional 3 alphanumeric (branch)
    private static final Pattern BIC_PATTERN = Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$");

    // SEPA country codes (SEPA member countries)
    private static final Set<String> SEPA_COUNTRY_CODES = new HashSet<>(Arrays.asList(
        // Eurozone countries
        "AT", "BE", "CY", "DE", "EE", "ES", "FI", "FR", "GR", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PT", "SI", "SK",
        // Non-Eurozone EU countries
        "BG", "HR", "CZ", "DK", "HU", "PL", "RO", "SE",
        // Non-EU SEPA countries
        "CH", "GB", "IS", "LI", "NO", "MC", "SM", "VA"
    ));

    /**
     * Validates a BIC code format.
     *
     * @param bic The BIC code to validate
     * @return ValidationResult containing validation status and error message
     */
    public ValidationResult validate(String bic) {
        if (bic == null || bic.trim().isEmpty()) {
            return ValidationResult.invalid("BIC code is required");
        }

        String normalizedBic = bic.trim().toUpperCase();

        // Check length
        if (normalizedBic.length() != 8 && normalizedBic.length() != 11) {
            return ValidationResult.invalid("BIC must be 8 or 11 characters long");
        }

        // Check format
        if (!BIC_PATTERN.matcher(normalizedBic).matches()) {
            return ValidationResult.invalid("BIC format is invalid. Expected: AAAABBCCXXX (A=letter, B=country, C=location, X=branch optional)");
        }

        // Extract and validate country code
        String countryCode = normalizedBic.substring(4, 6);
        if (!isValidCountryCode(countryCode)) {
            return ValidationResult.invalid("Invalid country code in BIC: " + countryCode);
        }

        log.debug("BIC validation successful: {}", normalizedBic);
        return ValidationResult.valid();
    }

    /**
     * Validates a BIC code and checks if it's from a SEPA country.
     *
     * @param bic The BIC code to validate
     * @return ValidationResult containing validation status and error message
     */
    public ValidationResult validateForSepa(String bic) {
        ValidationResult basicValidation = validate(bic);
        if (!basicValidation.isValid()) {
            return basicValidation;
        }

        String normalizedBic = bic.trim().toUpperCase();
        String countryCode = normalizedBic.substring(4, 6);

        if (!SEPA_COUNTRY_CODES.contains(countryCode)) {
            return ValidationResult.invalid("BIC country '" + countryCode + "' is not part of SEPA");
        }

        log.debug("SEPA BIC validation successful: {}", normalizedBic);
        return ValidationResult.valid();
    }

    /**
     * Validates consistency between BIC and IBAN.
     * Both should have the same country code.
     *
     * @param bic The BIC code
     * @param iban The IBAN
     * @return ValidationResult containing validation status and error message
     */
    public ValidationResult validateBicIbanConsistency(String bic, String iban) {
        if (bic == null || iban == null) {
            return ValidationResult.invalid("BIC and IBAN are required for consistency check");
        }

        String normalizedBic = bic.trim().toUpperCase();
        String normalizedIban = iban.trim().toUpperCase().replace(" ", "");

        // Validate BIC first
        ValidationResult bicValidation = validate(normalizedBic);
        if (!bicValidation.isValid()) {
            return bicValidation;
        }

        // Extract country codes
        String bicCountry = normalizedBic.substring(4, 6);

        if (normalizedIban.length() < 2) {
            return ValidationResult.invalid("IBAN is too short to extract country code");
        }

        String ibanCountry = normalizedIban.substring(0, 2);

        // Check consistency
        if (!bicCountry.equals(ibanCountry)) {
            return ValidationResult.invalid("BIC country '" + bicCountry + "' does not match IBAN country '" + ibanCountry + "'");
        }

        log.debug("BIC-IBAN consistency validation successful: BIC={}, IBAN={}", normalizedBic, normalizedIban);
        return ValidationResult.valid();
    }

    /**
     * Extracts the country code from a BIC.
     *
     * @param bic The BIC code
     * @return The 2-letter country code, or null if BIC is invalid
     */
    public String extractCountryCode(String bic) {
        if (bic == null || bic.trim().length() < 6) {
            return null;
        }
        return bic.trim().toUpperCase().substring(4, 6);
    }

    /**
     * Checks if a BIC has a branch code (11 characters).
     *
     * @param bic The BIC code
     * @return true if BIC has branch code, false otherwise
     */
    public boolean hasBranchCode(String bic) {
        if (bic == null) {
            return false;
        }
        return bic.trim().length() == 11;
    }

    /**
     * Normalizes a BIC code (uppercase, trim).
     *
     * @param bic The BIC code
     * @return Normalized BIC code
     */
    public String normalize(String bic) {
        if (bic == null) {
            return null;
        }
        return bic.trim().toUpperCase();
    }

    /**
     * Checks if a country code is valid (basic ISO 3166-1 alpha-2 check).
     * This is a simplified check - in production, use a comprehensive list.
     *
     * @param countryCode The 2-letter country code
     * @return true if valid, false otherwise
     */
    private boolean isValidCountryCode(String countryCode) {
        // Basic check: 2 uppercase letters
        // In production, validate against full ISO 3166-1 alpha-2 list
        return countryCode != null &&
               countryCode.length() == 2 &&
               countryCode.matches("[A-Z]{2}");
    }

    /**
     * Validation result class.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
