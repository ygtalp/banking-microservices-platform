package com.banking.account.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("IBAN Generator Tests")
class IbanGeneratorTest {

    private IbanGenerator ibanGenerator;

    @BeforeEach
    void setUp() {
        ibanGenerator = new IbanGenerator();
    }

    @Test
    @DisplayName("Should generate IBAN with correct length")
    void shouldGenerateIbanWithCorrectLength() {
        // When
        String iban = ibanGenerator.generateIban();

        // Then
        assertThat(iban).hasSize(26); // TR IBAN format: 26 characters
    }

    @Test
    @DisplayName("Should generate IBAN starting with TR")
    void shouldGenerateIbanStartingWithTR() {
        // When
        String iban = ibanGenerator.generateIban();

        // Then
        assertThat(iban).startsWith("TR");
    }

    @Test
    @DisplayName("Should generate IBAN with valid check digits")
    void shouldGenerateIbanWithValidCheckDigits() {
        // When
        String iban = ibanGenerator.generateIban();

        // Then
        assertThat(iban).hasSize(26);
        String checkDigits = iban.substring(2, 4);
        assertThat(checkDigits).matches("\\d{2}"); // Two digits
        assertThat(Integer.parseInt(checkDigits)).isBetween(2, 98); // Valid check digit range (02-98)
    }

    @Test
    @DisplayName("Should generate IBAN with correct bank code")
    void shouldGenerateIbanWithCorrectBankCode() {
        // When
        String iban = ibanGenerator.generateIban();

        // Then
        String bankCode = iban.substring(4, 9); // After country code and check digits
        assertThat(bankCode).isEqualTo("00001");
    }

    @Test
    @DisplayName("Should generate IBAN with valid format")
    void shouldGenerateIbanWithValidFormat() {
        // When
        String iban = ibanGenerator.generateIban();

        // Then
        // Format: TR + 2 check digits + 5 bank code + 1 reserved + 16 account number
        assertThat(iban).matches("TR\\d{24}");
    }

    @Test
    @DisplayName("Should generate unique IBANs")
    void shouldGenerateUniqueIbans() {
        // Given
        Set<String> ibans = new HashSet<>();
        int iterations = 100;

        // When
        for (int i = 0; i < iterations; i++) {
            ibans.add(ibanGenerator.generateIban());
        }

        // Then
        assertThat(ibans).hasSize(iterations); // All IBANs should be unique
    }

    @Test
    @DisplayName("Should generate IBAN with valid MOD-97 checksum")
    void shouldGenerateIbanWithValidMod97Checksum() {
        // When
        String iban = ibanGenerator.generateIban();

        // Then
        assertThat(isValidIbanChecksum(iban)).isTrue();
    }

    @Test
    @DisplayName("Should generate multiple valid IBANs")
    void shouldGenerateMultipleValidIbans() {
        // When & Then
        for (int i = 0; i < 50; i++) {
            String iban = ibanGenerator.generateIban();

            assertThat(iban)
                .hasSize(26)
                .startsWith("TR")
                .matches("TR\\d{24}");

            assertThat(isValidIbanChecksum(iban))
                .as("IBAN %s should have valid checksum", iban)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Should generate IBAN with numeric account number")
    void shouldGenerateIbanWithNumericAccountNumber() {
        // When
        String iban = ibanGenerator.generateIban();

        // Then
        String accountNumber = iban.substring(10); // After country, check digits, bank code, and reserved
        assertThat(accountNumber).matches("\\d{16}");
    }

    @Test
    @DisplayName("Should generate IBAN with reserved digit")
    void shouldGenerateIbanWithReservedDigit() {
        // When
        String iban = ibanGenerator.generateIban();

        // Then
        String reserved = iban.substring(9, 10); // After bank code
        assertThat(reserved).matches("\\d");
    }

    @Test
    @DisplayName("Should generate IBAN with all numeric BBAN")
    void shouldGenerateIbanWithAllNumericBban() {
        // When
        String iban = ibanGenerator.generateIban();

        // Then
        String bban = iban.substring(4); // After country code and check digits
        assertThat(bban).matches("\\d{22}"); // 5 bank + 1 reserved + 16 account = 22 digits
    }

    @Test
    @DisplayName("Should generate different IBANs on consecutive calls")
    void shouldGenerateDifferentIbansOnConsecutiveCalls() {
        // When
        String iban1 = ibanGenerator.generateIban();
        String iban2 = ibanGenerator.generateIban();
        String iban3 = ibanGenerator.generateIban();

        // Then
        assertThat(iban1).isNotEqualTo(iban2);
        assertThat(iban2).isNotEqualTo(iban3);
        assertThat(iban1).isNotEqualTo(iban3);
    }

    @Test
    @DisplayName("Should generate IBAN with check digits between 02 and 98")
    void shouldGenerateIbanWithCheckDigitsBetween02And98() {
        // When & Then
        for (int i = 0; i < 20; i++) {
            String iban = ibanGenerator.generateIban();
            int checkDigits = Integer.parseInt(iban.substring(2, 4));

            assertThat(checkDigits)
                .as("Check digits should be between 02 and 98 for IBAN: %s", iban)
                .isBetween(2, 98);
        }
    }

    // Helper method to validate IBAN checksum using MOD-97 algorithm
    private boolean isValidIbanChecksum(String iban) {
        // Move first 4 characters to end
        String rearranged = iban.substring(4) + iban.substring(0, 4);

        // Replace letters with numbers (A=10, B=11, etc.)
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append((int) c - 55);
            } else {
                numeric.append(c);
            }
        }

        // Calculate MOD-97
        String number = numeric.toString();
        int mod = 0;
        for (int i = 0; i < number.length(); i++) {
            mod = (mod * 10 + Character.getNumericValue(number.charAt(i))) % 97;
        }

        return mod == 1; // Valid IBAN has MOD-97 result of 1
    }
}
