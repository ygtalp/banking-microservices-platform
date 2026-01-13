package com.banking.swift.service;

import com.banking.swift.model.ChargeType;
import com.banking.swift.model.SwiftTransfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MT103 Message Generator Tests")
class Mt103MessageGeneratorTest {

    private Mt103MessageGenerator mt103MessageGenerator;

    @BeforeEach
    void setUp() {
        mt103MessageGenerator = new Mt103MessageGenerator();
    }

    @Test
    @DisplayName("Should generate valid MT103 message for complete transfer")
    void shouldGenerateValidMt103MessageForCompleteTransfer() {
        // Given
        SwiftTransfer transfer = SwiftTransfer.builder()
                .transactionReference("SWFT1A2B3C4D5E6F")
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("USD")
                .amount(new BigDecimal("10000.00"))
                .orderingCustomerName("John Doe")
                .orderingCustomerAddress("123 Main St, New York")
                .orderingCustomerAccount("US1234567890")
                .senderBic("CHASUS33XXX")
                .senderName("JP Morgan Chase Bank")
                .correspondentBic("CHASUS33XXX")
                .correspondentName("JP Morgan Chase Bank")
                .correspondentAccount("NOSTRO123456")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryBankName("Deutsche Bank AG")
                .beneficiaryName("Max Mustermann")
                .beneficiaryAddress("456 Berlin Str, Germany")
                .beneficiaryAccount("DE89370400440532013000")
                .remittanceInfo("INVOICE 12345")
                .chargeType(ChargeType.SHA)
                .build();

        // When
        String mt103 = mt103MessageGenerator.generateMt103Message(transfer);

        // Then
        assertThat(mt103).isNotNull();
        assertThat(mt103).isNotEmpty();

        // Verify Block 4 content
        assertThat(mt103).contains(":20:SWFT1A2B3C4D5E6F");
        assertThat(mt103).contains(":23B:CRED");
        assertThat(mt103).contains(":32A:260115USD10000,00");
        assertThat(mt103).contains(":50K:/US1234567890");
        assertThat(mt103).contains("JOHN DOE");
        assertThat(mt103).contains(":52A:CHASUS33XXX");
        assertThat(mt103).contains(":53A:/NOSTRO123456");
        assertThat(mt103).contains(":57A:DEUTDEFFXXX");
        assertThat(mt103).contains(":59:/DE89370400440532013000");
        assertThat(mt103).contains("MAX MUSTERMANN");
        assertThat(mt103).contains(":70:INVOICE 12345");
        assertThat(mt103).contains(":71A:SHA");
    }

    @Test
    @DisplayName("Should generate MT103 message without optional fields")
    void shouldGenerateMt103MessageWithoutOptionalFields() {
        // Given
        SwiftTransfer transfer = SwiftTransfer.builder()
                .transactionReference("SWFT9Z8Y7X6W5V4U")
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("EUR")
                .amount(new BigDecimal("5000.00"))
                .orderingCustomerName("Jane Smith")
                .senderBic("BNPAFRPPXXX")
                .senderName("BNP Paribas")
                .beneficiaryBankBic("HSBCGB2LXXX")
                .beneficiaryName("Robert Johnson")
                .beneficiaryAccount("GB29NWBK60161331926819")
                .chargeType(ChargeType.OUR)
                .build();

        // When
        String mt103 = mt103MessageGenerator.generateMt103Message(transfer);

        // Then
        assertThat(mt103).isNotNull();
        assertThat(mt103).contains(":20:SWFT9Z8Y7X6W5V4U");
        assertThat(mt103).contains(":32A:260115EUR5000,00");
        assertThat(mt103).contains(":71A:OUR");
    }

    @Test
    @DisplayName("Should format currency amount correctly")
    void shouldFormatCurrencyAmountCorrectly() {
        // Given
        SwiftTransfer transfer = SwiftTransfer.builder()
                .transactionReference("SWFTTEST12345678")
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("GBP")
                .amount(new BigDecimal("123456.78"))
                .orderingCustomerName("Test Customer")
                .senderBic("CITIUS33XXX")
                .senderName("Citibank")
                .beneficiaryBankBic("UBSWCHZHXXX")
                .beneficiaryName("Test Beneficiary")
                .beneficiaryAccount("CH9300762011623852957")
                .chargeType(ChargeType.BEN)
                .build();

        // When
        String mt103 = mt103MessageGenerator.generateMt103Message(transfer);

        // Then
        assertThat(mt103).contains(":32A:260115GBP123456,78");
    }

    @Test
    @DisplayName("Should validate generated MT103 message")
    void shouldValidateGeneratedMt103Message() {
        // Given
        SwiftTransfer transfer = SwiftTransfer.builder()
                .transactionReference("SWFT1A2B3C4D5E6F")
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("USD")
                .amount(new BigDecimal("10000.00"))
                .orderingCustomerName("John Doe")
                .senderBic("CHASUS33XXX")
                .senderName("JP Morgan Chase Bank")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryName("Max Mustermann")
                .beneficiaryAccount("DE89370400440532013000")
                .chargeType(ChargeType.SHA)
                .build();

        // When
        String mt103 = mt103MessageGenerator.generateMt103Message(transfer);

        // Then - Validation should pass
        assertThat(mt103MessageGenerator.validateMt103Message(mt103)).isTrue();
    }

    @Test
    @DisplayName("Should handle special characters in names")
    void shouldHandleSpecialCharactersInNames() {
        // Given
        SwiftTransfer transfer = SwiftTransfer.builder()
                .transactionReference("SWFTSPECIAL12345")
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("EUR")
                .amount(new BigDecimal("1000.00"))
                .orderingCustomerName("François O'Brien")
                .senderBic("BNPAFRPPXXX")
                .senderName("BNP Paribas")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryName("José García")
                .beneficiaryAccount("DE89370400440532013000")
                .chargeType(ChargeType.SHA)
                .build();

        // When
        String mt103 = mt103MessageGenerator.generateMt103Message(transfer);

        // Then
        assertThat(mt103).isNotNull();
        assertThat(mt103).contains("FRANCOIS O BRIEN");
        assertThat(mt103).contains("JOSE GARCIA");
    }

    @Test
    @DisplayName("Should truncate long fields to SWIFT limits")
    void shouldTruncateLongFieldsToSwiftLimits() {
        // Given - Names longer than 140 characters (SWIFT limit)
        String longName = "A".repeat(200);
        SwiftTransfer transfer = SwiftTransfer.builder()
                .transactionReference("SWFTLONG12345678")
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("USD")
                .amount(new BigDecimal("1000.00"))
                .orderingCustomerName(longName)
                .senderBic("CHASUS33XXX")
                .senderName("JP Morgan Chase Bank")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryName(longName)
                .beneficiaryAccount("DE89370400440532013000")
                .chargeType(ChargeType.SHA)
                .build();

        // When
        String mt103 = mt103MessageGenerator.generateMt103Message(transfer);

        // Then - Should not throw exception and should be valid
        assertThat(mt103).isNotNull();
        assertThat(mt103MessageGenerator.validateMt103Message(mt103)).isTrue();
    }

    @Test
    @DisplayName("Should handle different charge types")
    void shouldHandleDifferentChargeTypes() {
        // Test OUR charge type
        SwiftTransfer ourTransfer = createBasicTransfer(ChargeType.OUR);
        assertThat(mt103MessageGenerator.generateMt103Message(ourTransfer)).contains(":71A:OUR");

        // Test BEN charge type
        SwiftTransfer benTransfer = createBasicTransfer(ChargeType.BEN);
        assertThat(mt103MessageGenerator.generateMt103Message(benTransfer)).contains(":71A:BEN");

        // Test SHA charge type
        SwiftTransfer shaTransfer = createBasicTransfer(ChargeType.SHA);
        assertThat(mt103MessageGenerator.generateMt103Message(shaTransfer)).contains(":71A:SHA");
    }

    @Test
    @DisplayName("Should generate different messages for different transfers")
    void shouldGenerateDifferentMessagesForDifferentTransfers() {
        // Given
        SwiftTransfer transfer1 = createBasicTransfer(ChargeType.SHA);
        transfer1.setTransactionReference("SWFT111111111111");

        SwiftTransfer transfer2 = createBasicTransfer(ChargeType.SHA);
        transfer2.setTransactionReference("SWFT222222222222");

        // When
        String mt1031 = mt103MessageGenerator.generateMt103Message(transfer1);
        String mt1032 = mt103MessageGenerator.generateMt103Message(transfer2);

        // Then
        assertThat(mt1031).isNotEqualTo(mt1032);
        assertThat(mt1031).contains("SWFT111111111111");
        assertThat(mt1032).contains("SWFT222222222222");
    }

    // Helper method
    private SwiftTransfer createBasicTransfer(ChargeType chargeType) {
        return SwiftTransfer.builder()
                .transactionReference("SWFTTEST12345678")
                .valueDate(LocalDate.of(2026, 1, 15))
                .currency("USD")
                .amount(new BigDecimal("1000.00"))
                .orderingCustomerName("Test Customer")
                .senderBic("CHASUS33XXX")
                .senderName("JP Morgan Chase Bank")
                .beneficiaryBankBic("DEUTDEFFXXX")
                .beneficiaryName("Test Beneficiary")
                .beneficiaryAccount("DE89370400440532013000")
                .chargeType(chargeType)
                .build();
    }
}
