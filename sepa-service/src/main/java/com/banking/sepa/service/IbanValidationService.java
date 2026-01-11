package com.banking.sepa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
@Slf4j
public class IbanValidationService {

    public boolean isValidIban(String iban) {
        if (iban == null || iban.isEmpty()) {
            return false;
        }

        // Remove spaces and convert to uppercase
        iban = iban.replaceAll("\\s+", "").toUpperCase();

        // Check length (IBAN should be 15-34 characters)
        if (iban.length() < 15 || iban.length() > 34) {
            log.warn("Invalid IBAN length: {}", iban.length());
            return false;
        }

        // Check format: first 2 chars must be letters, next 2 must be digits
        if (!iban.substring(0, 2).matches("[A-Z]{2}") ||
            !iban.substring(2, 4).matches("\\d{2}")) {
            log.warn("Invalid IBAN format");
            return false;
        }

        // Validate using MOD-97 algorithm
        return validateMod97(iban);
    }

    private boolean validateMod97(String iban) {
        try {
            // Move first 4 characters to end
            String rearranged = iban.substring(4) + iban.substring(0, 4);

            // Replace letters with numbers (A=10, B=11, ..., Z=35)
            StringBuilder numericIban = new StringBuilder();
            for (char c : rearranged.toCharArray()) {
                if (Character.isLetter(c)) {
                    numericIban.append(c - 'A' + 10);
                } else {
                    numericIban.append(c);
                }
            }

            // Calculate MOD 97
            BigInteger ibanNumber = new BigInteger(numericIban.toString());
            int remainder = ibanNumber.mod(BigInteger.valueOf(97)).intValue();

            return remainder == 1;

        } catch (Exception e) {
            log.error("Error validating IBAN", e);
            return false;
        }
    }

    public String getIbanCountryCode(String iban) {
        if (iban == null || iban.length() < 2) {
            return null;
        }
        return iban.substring(0, 2).toUpperCase();
    }

    public boolean isSepaCountry(String iban) {
        String countryCode = getIbanCountryCode(iban);
        // SEPA countries (simplified list - 36 countries in total)
        String[] sepaCountries = {
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU",
            "MT", "MC", "NL", "NO", "PL", "PT", "RO", "SM", "SK", "SI",
            "ES", "SE", "CH", "GB"
        };

        for (String country : sepaCountries) {
            if (country.equals(countryCode)) {
                return true;
            }
        }
        return false;
    }
}
