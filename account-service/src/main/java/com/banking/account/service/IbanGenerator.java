package com.banking.account.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
public class IbanGenerator {

    private static final String COUNTRY_CODE = "TR";
    private static final String BANK_CODE = "00001"; // Fake bank code
    private final Random random = new SecureRandom();

    /**
     * Generate a valid Turkish IBAN
     * Format: TR + 2 check digits + 5 bank code + 1 reserved + 16 account number = 26 characters
     * Example: TR330006100519786457841326
     */
    public String generateIban() {
        // Generate random 16-digit account number
        String accountNumber = generateRandomDigits(16);

        // Generate random 1-digit reserved field
        String reserved = String.valueOf(random.nextInt(10));

        // Combine: bank code + reserved + account number
        String bban = BANK_CODE + reserved + accountNumber;

        // Calculate check digits
        String checkDigits = calculateCheckDigits(COUNTRY_CODE, bban);

        // Complete IBAN
        return COUNTRY_CODE + checkDigits + bban;
    }

    private String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String calculateCheckDigits(String countryCode, String bban) {
        // Move country code to end and replace letters with numbers (A=10, B=11, etc.)
        String rearranged = bban + letterToNumber(countryCode) + "00";

        // Calculate mod 97
        int checksum = 98 - mod97(rearranged);

        // Return as 2-digit string
        return String.format("%02d", checksum);
    }

    private String letterToNumber(String letters) {
        StringBuilder sb = new StringBuilder();
        for (char c : letters.toCharArray()) {
            sb.append((int) c - 55); // A=10, B=11, ..., Z=35
        }
        return sb.toString();
    }

    private int mod97(String number) {
        String remainder = number;
        while (remainder.length() > 2) {
            int len = Math.min(9, remainder.length());
            String part = remainder.substring(0, len);
            remainder = (Integer.parseInt(part) % 97) + remainder.substring(len);
        }
        return Integer.parseInt(remainder) % 97;
    }
}