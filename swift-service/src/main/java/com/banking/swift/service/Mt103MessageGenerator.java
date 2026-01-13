package com.banking.swift.service;

import com.banking.swift.model.SwiftTransfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * MT103 SWIFT Message Generator
 *
 * Generates SWIFT MT103 (Single Customer Credit Transfer) messages
 * following the ISO 15022 standard format.
 *
 * Message Structure:
 * Block 1: Basic Header (Application ID, Service ID, LT Address, Session, Sequence)
 * Block 2: Application Header (I/O Identifier, Message Type, Destination, Priority)
 * Block 3: User Header (optional - Service Type Code)
 * Block 4: Text Block (actual message content)
 * Block 5: Trailer (checksums and security)
 */
@Service
@Slf4j
public class Mt103MessageGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#0.00");

    /**
     * Generate complete MT103 message
     */
    public String generateMt103Message(SwiftTransfer transfer) {
        log.info("Generating MT103 message for transaction: {}", transfer.getTransactionReference());

        StringBuilder message = new StringBuilder();

        // Block 1: Basic Header
        message.append(generateBlock1(transfer));

        // Block 2: Application Header
        message.append(generateBlock2(transfer));

        // Block 3: User Header
        message.append(generateBlock3(transfer));

        // Block 4: Text Block (main message content)
        message.append(generateBlock4(transfer));

        // Block 5: Trailer
        message.append(generateBlock5(transfer));

        return message.toString();
    }

    /**
     * Block 1: Basic Header
     * Format: {1:F01BANKBEBBAXXX0000000000}
     */
    private String generateBlock1(SwiftTransfer transfer) {
        return String.format("{1:F01%s0000000000}", transfer.getSenderBic());
    }

    /**
     * Block 2: Application Header (Input)
     * Format: {2:I103BANKDEFFXXXXN}
     * I = Input, 103 = MT103, BANKDEFFXXXX = Receiver BIC, N = Normal priority
     */
    private String generateBlock2(SwiftTransfer transfer) {
        String receiverBic = transfer.getCorrespondentBic() != null ?
            transfer.getCorrespondentBic() : transfer.getBeneficiaryBankBic();
        return String.format("{2:I103%sN}", receiverBic);
    }

    /**
     * Block 3: User Header
     * Optional fields for service type, banking priority, etc.
     */
    private String generateBlock3(SwiftTransfer transfer) {
        return "{3:{108:MT103}}"; // Service Type Code
    }

    /**
     * Block 4: Text Block (Main Content)
     * Contains all the mandatory and optional fields
     */
    private String generateBlock4(SwiftTransfer transfer) {
        StringBuilder block4 = new StringBuilder("{4:\n");

        // :20: Transaction Reference (mandatory, max 16 chars)
        block4.append(":20:").append(transfer.getTransactionReference()).append("\n");

        // :23B: Bank Operation Code (mandatory)
        block4.append(":23B:").append(transfer.getBankOperationCode()).append("\n");

        // :32A: Value Date, Currency, Amount (mandatory)
        // Format: YYMMDDCCCAMOUNT
        String valueDate = transfer.getValueDate().format(DATE_FORMAT);
        String formattedAmount = AMOUNT_FORMAT.format(transfer.getAmount()).replace(".", ",");
        block4.append(":32A:").append(valueDate)
              .append(transfer.getCurrency())
              .append(formattedAmount).append("\n");

        // :50K: Ordering Customer (mandatory, max 4x35 chars)
        block4.append(":50K:");
        if (transfer.getOrderingCustomerAccount() != null) {
            block4.append("/").append(transfer.getOrderingCustomerAccount()).append("\n");
        }
        block4.append(transfer.getOrderingCustomerName()).append("\n");
        if (transfer.getOrderingCustomerAddress() != null) {
            block4.append(transfer.getOrderingCustomerAddress()).append("\n");
        }

        // :52A: Ordering Institution (optional)
        if (transfer.getSenderBic() != null) {
            block4.append(":52A:").append(transfer.getSenderBic()).append("\n");
        }

        // :53A: Sender's Correspondent (optional - our correspondent bank)
        if (transfer.getCorrespondentBic() != null) {
            block4.append(":53A:");
            if (transfer.getCorrespondentAccount() != null) {
                block4.append("/").append(transfer.getCorrespondentAccount()).append("\n");
            }
            block4.append(transfer.getCorrespondentBic()).append("\n");
        }

        // :57A: Account With Institution (beneficiary's bank, optional but recommended)
        if (transfer.getBeneficiaryBankBic() != null) {
            block4.append(":57A:").append(transfer.getBeneficiaryBankBic()).append("\n");
        }

        // :59: Beneficiary Customer (mandatory, max 4x35 chars)
        block4.append(":59:");
        if (transfer.getBeneficiaryAccount() != null) {
            block4.append("/").append(transfer.getBeneficiaryAccount()).append("\n");
        }
        block4.append(transfer.getBeneficiaryName()).append("\n");
        if (transfer.getBeneficiaryAddress() != null) {
            block4.append(transfer.getBeneficiaryAddress()).append("\n");
        }

        // :70: Remittance Information (optional, max 4x35 chars)
        if (transfer.getRemittanceInfo() != null) {
            block4.append(":70:").append(transfer.getRemittanceInfo()).append("\n");
        }

        // :71A: Details of Charges (mandatory)
        block4.append(":71A:").append(transfer.getChargeType().name()).append("\n");

        block4.append("-}");
        return block4.toString();
    }

    /**
     * Block 5: Trailer
     * Contains checksums and optional security fields
     */
    private String generateBlock5(SwiftTransfer transfer) {
        // Simplified trailer - in production would include MAC (Message Authentication Code)
        return "{5:{CHK:ABCDEF123456}}";
    }

    /**
     * Validate MT103 message format
     */
    public boolean validateMt103Message(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        // Basic validation - check required blocks
        return message.contains("{1:") &&
               message.contains("{2:") &&
               message.contains("{4:") &&
               message.contains(":20:") &&  // Transaction reference
               message.contains(":32A:") && // Value date/currency/amount
               message.contains(":59:");    // Beneficiary
    }
}