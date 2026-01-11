package com.banking.auth.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * TOTP (Time-based One-Time Password) Service
 * Implements Google Authenticator compatible TOTP generation and verification
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TotpService {

    private static final String ISSUER = "Banking Platform";
    private static final int QR_CODE_SIZE = 250;

    private final SecretGenerator secretGenerator = new dev.samstevens.totp.secret.DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    /**
     * Generate a new TOTP secret
     *
     * @return Base32 encoded secret
     */
    public String generateSecret() {
        String secret = secretGenerator.generate();
        log.info("Generated new TOTP secret");
        return secret;
    }

    /**
     * Generate QR code data URL for TOTP setup
     *
     * @param email  User's email
     * @param secret TOTP secret
     * @return QR code as Base64 data URL
     */
    public String generateQrCodeDataUrl(String email, String secret) {
        try {
            QrData data = new QrData.Builder()
                    .label(email)
                    .secret(secret)
                    .issuer(ISSUER)
                    .algorithm(HashingAlgorithm.SHA1)
                    .digits(6)
                    .period(30)
                    .build();

            String qrCodeUri = getQrDataUri(data);
            byte[] qrCodeImage = generateQrCodeImage(qrCodeUri);
            String base64Image = Base64.getEncoder().encodeToString(qrCodeImage);

            log.info("Generated QR code for user: {}", email);
            return "data:image/png;base64," + base64Image;

        } catch (Exception e) {
            log.error("Failed to generate QR code for user: {}", email, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Verify TOTP code
     *
     * @param secret TOTP secret
     * @param code   6-digit TOTP code
     * @return true if code is valid
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }

        try {
            boolean isValid = verifier.isValidCode(secret, code);
            log.info("TOTP verification result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
            return false;
        }
    }

    /**
     * Generate current TOTP code (for testing purposes)
     *
     * @param secret TOTP secret
     * @return 6-digit TOTP code
     */
    public String getCurrentCode(String secret) {
        try {
            long currentBucket = Math.floorDiv(timeProvider.getTime(), 30);
            return codeGenerator.generate(secret, currentBucket);
        } catch (Exception e) {
            log.error("Error generating current TOTP code", e);
            throw new RuntimeException("Failed to generate TOTP code", e);
        }
    }

    /**
     * Build QR code data URI (otpauth:// format)
     */
    private String getQrDataUri(QrData data) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
                data.getIssuer(),
                data.getLabel(),
                data.getSecret(),
                data.getIssuer(),
                data.getAlgorithm(),
                data.getDigits(),
                data.getPeriod()
        );
    }

    /**
     * Generate QR code image from URI
     */
    private byte[] generateQrCodeImage(String qrCodeUri) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(
                qrCodeUri,
                BarcodeFormat.QR_CODE,
                QR_CODE_SIZE,
                QR_CODE_SIZE,
                hints
        );

        BufferedImage image = new BufferedImage(QR_CODE_SIZE, QR_CODE_SIZE, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < QR_CODE_SIZE; x++) {
            for (int y = 0; y < QR_CODE_SIZE; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }
}
