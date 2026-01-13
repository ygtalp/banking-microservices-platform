package com.banking.swift.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BIC Validation Service Tests")
class BicValidationServiceTest {

    private BicValidationService bicValidationService;

    @BeforeEach
    void setUp() {
        bicValidationService = new BicValidationService();
    }

    @Test
    @DisplayName("Should validate 8-character BIC code")
    void shouldValidate8CharacterBic() {
        assertThat(bicValidationService.isValidBic("CHASUS33")).isTrue();
    }

    @Test
    @DisplayName("Should validate 11-character BIC code")
    void shouldValidate11CharacterBic() {
        assertThat(bicValidationService.isValidBic("CHASUS33XXX")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "DEUTDEFF", "DEUTDEFFXXX", "BNPAFRPP", "BNPAFRPPXXX",
        "HSBCGB2L", "HSBCGB2LXXX", "CITIUS33", "CITIUS33XXX"
    })
    @DisplayName("Should validate multiple valid BIC codes")
    void shouldValidateMultipleValidBicCodes(String bic) {
        assertThat(bicValidationService.isValidBic(bic)).isTrue();
    }

    @Test
    @DisplayName("Should validate BIC with lowercase and trim whitespace")
    void shouldValidateBicWithLowercaseAndWhitespace() {
        assertThat(bicValidationService.isValidBic("  chasus33xxx  ")).isTrue();
    }

    @Test
    @DisplayName("Should reject null BIC")
    void shouldRejectNullBic() {
        assertThat(bicValidationService.isValidBic(null)).isFalse();
    }

    @Test
    @DisplayName("Should reject empty BIC")
    void shouldRejectEmptyBic() {
        assertThat(bicValidationService.isValidBic("")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "CHAS", "CHASUSXX", "CHASUS33X", "CHASUS33XXXX"
    })
    @DisplayName("Should reject BIC with invalid length")
    void shouldRejectBicWithInvalidLength(String bic) {
        assertThat(bicValidationService.isValidBic(bic)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "12345678", "CHAS1234", "CHASUSXX", "CHASUS3!", "CHAS US33"
    })
    @DisplayName("Should reject BIC with invalid format")
    void shouldRejectBicWithInvalidFormat(String bic) {
        assertThat(bicValidationService.isValidBic(bic)).isFalse();
    }

    @Test
    @DisplayName("Should reject BIC with invalid country code")
    void shouldRejectBicWithInvalidCountryCode() {
        assertThat(bicValidationService.isValidBic("CHASXX33XXX")).isFalse();
    }

    @Test
    @DisplayName("Should extract country code from BIC")
    void shouldExtractCountryCode() {
        assertThat(bicValidationService.extractCountryCode("CHASUS33XXX")).isEqualTo("US");
    }

    @Test
    @DisplayName("Should extract bank code from BIC")
    void shouldExtractBankCode() {
        assertThat(bicValidationService.extractBankCode("CHASUS33XXX")).isEqualTo("CHAS");
    }

    @Test
    @DisplayName("Should normalize 8-character BIC to 11 characters")
    void shouldNormalize8CharBicTo11() {
        assertThat(bicValidationService.normalizeBic("CHASUS33")).isEqualTo("CHASUS33XXX");
    }

    @Test
    @DisplayName("Should normalize 11-character BIC unchanged")
    void shouldNormalize11CharBicUnchanged() {
        assertThat(bicValidationService.normalizeBic("CHASUS33YYY")).isEqualTo("CHASUS33YYY");
    }

    @Test
    @DisplayName("Should check if country is supported")
    void shouldCheckIfCountryIsSupported() {
        assertThat(bicValidationService.isSupportedCountry("CHASUS33XXX")).isTrue();
        assertThat(bicValidationService.isSupportedCountry("DEUTDEFFXXX")).isTrue();
    }

    @Test
    @DisplayName("Should throw exception for null BIC in extractCountryCode")
    void shouldThrowExceptionForNullBicInExtractCountryCode() {
        assertThatThrownBy(() -> bicValidationService.extractCountryCode(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid BIC format");
    }

    @Test
    @DisplayName("Should throw exception for invalid BIC in normalizeBic")
    void shouldThrowExceptionForInvalidBicInNormalizeBic() {
        assertThatThrownBy(() -> bicValidationService.normalizeBic("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid BIC length");
    }

    @Test
    @DisplayName("Should normalize BIC with lowercase to uppercase")
    void shouldNormalizeBicWithLowercaseToUppercase() {
        assertThat(bicValidationService.normalizeBic("  chasus33  ")).isEqualTo("CHASUS33XXX");
    }
}
