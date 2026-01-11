package com.banking.auth.service;

import com.banking.auth.model.MfaMethod;
import com.banking.auth.model.MfaSecret;
import com.banking.auth.model.User;
import com.banking.auth.repository.MfaSecretRepository;
import com.banking.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Multi-Factor Authentication Service
 * Handles MFA setup, verification, and management
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MfaService {

    private final UserRepository userRepository;
    private final MfaSecretRepository mfaSecretRepository;
    private final TotpService totpService;
    private final OtpService otpService;

    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Setup TOTP (Google Authenticator)
     *
     * @param userId User ID
     * @return TOTP setup data (secret + QR code)
     */
    @Transactional
    public TotpSetupData setupTotp(String userId) {
        User user = findUserByUserId(userId);

        // Generate new TOTP secret
        String secret = totpService.generateSecret();
        String qrCodeDataUrl = totpService.generateQrCodeDataUrl(user.getEmail(), secret);

        // Get or create MFA secret
        MfaSecret mfaSecret = mfaSecretRepository.findByUser(user)
                .orElseGet(() -> {
                    MfaSecret newSecret = MfaSecret.builder()
                            .user(user)
                            .build();
                    return mfaSecretRepository.save(newSecret);
                });

        // Save TOTP secret (not verified yet)
        mfaSecret.setTotpSecret(secret);
        mfaSecret.setTotpVerified(false);
        mfaSecretRepository.save(mfaSecret);

        log.info("TOTP setup initiated for user: {}", userId);

        return new TotpSetupData(secret, qrCodeDataUrl);
    }

    /**
     * Verify and enable TOTP
     *
     * @param userId User ID
     * @param code   TOTP code from authenticator app
     * @return true if verification successful
     */
    @Transactional
    public boolean verifyAndEnableTotp(String userId, String code) {
        User user = findUserByUserId(userId);
        MfaSecret mfaSecret = findMfaSecret(user);

        if (mfaSecret.getTotpSecret() == null) {
            throw new RuntimeException("TOTP not set up for this user");
        }

        // Verify TOTP code
        boolean isValid = totpService.verifyCode(mfaSecret.getTotpSecret(), code);

        if (isValid) {
            mfaSecret.enableTotp(mfaSecret.getTotpSecret());
            user.enableMfa(MfaMethod.TOTP);

            mfaSecretRepository.save(mfaSecret);
            userRepository.save(user);

            log.info("TOTP enabled for user: {}", userId);
            return true;
        }

        log.warn("TOTP verification failed for user: {}", userId);
        return false;
    }

    /**
     * Setup SMS OTP
     *
     * @param userId      User ID
     * @param phoneNumber Phone number
     * @return OTP code (for development/testing)
     */
    @Transactional
    public String setupSms(String userId, String phoneNumber) {
        User user = findUserByUserId(userId);

        // Update phone number
        user.setPhoneNumber(phoneNumber);
        userRepository.save(user);

        // Generate and send OTP
        String otp = otpService.generateOtp(userId, "SMS");

        // TODO: Send SMS via Twilio (for now, return OTP for testing)
        log.info("SMS OTP generated for user: {}, phone: {}", userId, phoneNumber);

        return otp; // In production, don't return this, send via SMS
    }

    /**
     * Verify and enable SMS OTP
     *
     * @param userId User ID
     * @param code   SMS OTP code
     * @return true if verification successful
     */
    @Transactional
    public boolean verifyAndEnableSms(String userId, String code) {
        User user = findUserByUserId(userId);

        // Verify OTP
        boolean isValid = otpService.verifyOtp(userId, "SMS", code);

        if (isValid) {
            MfaSecret mfaSecret = mfaSecretRepository.findByUser(user)
                    .orElseGet(() -> {
                        MfaSecret newSecret = MfaSecret.builder()
                                .user(user)
                                .build();
                        return mfaSecretRepository.save(newSecret);
                    });

            String smsSecret = generateRandomSecret();
            mfaSecret.enableSms(smsSecret);
            user.enableMfa(MfaMethod.SMS);

            mfaSecretRepository.save(mfaSecret);
            userRepository.save(user);

            log.info("SMS MFA enabled for user: {}", userId);
            return true;
        }

        log.warn("SMS OTP verification failed for user: {}", userId);
        return false;
    }

    /**
     * Setup Email OTP
     *
     * @param userId User ID
     * @return OTP code (for development/testing)
     */
    @Transactional
    public String setupEmail(String userId) {
        User user = findUserByUserId(userId);

        // Generate and send OTP
        String otp = otpService.generateOtp(userId, "EMAIL");

        // TODO: Send email via Notification Service (for now, return OTP for testing)
        log.info("Email OTP generated for user: {}, email: {}", userId, user.getEmail());

        return otp; // In production, don't return this, send via email
    }

    /**
     * Verify and enable Email OTP
     *
     * @param userId User ID
     * @param code   Email OTP code
     * @return true if verification successful
     */
    @Transactional
    public boolean verifyAndEnableEmail(String userId, String code) {
        User user = findUserByUserId(userId);

        // Verify OTP
        boolean isValid = otpService.verifyOtp(userId, "EMAIL", code);

        if (isValid) {
            MfaSecret mfaSecret = mfaSecretRepository.findByUser(user)
                    .orElseGet(() -> {
                        MfaSecret newSecret = MfaSecret.builder()
                                .user(user)
                                .build();
                        return mfaSecretRepository.save(newSecret);
                    });

            String emailSecret = generateRandomSecret();
            mfaSecret.enableEmail(emailSecret);
            user.enableMfa(MfaMethod.EMAIL);

            mfaSecretRepository.save(mfaSecret);
            userRepository.save(user);

            log.info("Email MFA enabled for user: {}", userId);
            return true;
        }

        log.warn("Email OTP verification failed for user: {}", userId);
        return false;
    }

    /**
     * Generate backup codes
     *
     * @param userId User ID
     * @return List of backup codes
     */
    @Transactional
    public List<String> generateBackupCodes(String userId) {
        User user = findUserByUserId(userId);
        MfaSecret mfaSecret = findMfaSecret(user);

        List<String> backupCodes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            backupCodes.add(generateBackupCode());
        }

        // Store backup codes (comma-separated)
        mfaSecret.setBackupCodes(String.join(",", backupCodes));
        mfaSecret.setBackupCodesUsed("");
        mfaSecretRepository.save(mfaSecret);

        log.info("Generated {} backup codes for user: {}", BACKUP_CODE_COUNT, userId);
        return backupCodes;
    }

    /**
     * Verify MFA code (during login)
     *
     * @param userId User ID
     * @param code   MFA code
     * @param method MFA method (optional, uses preferred if null)
     * @return true if code is valid
     */
    @Transactional
    public boolean verifyMfaCode(String userId, String code, MfaMethod method) {
        User user = findUserByUserId(userId);

        if (!user.isMfaEnabled()) {
            throw new RuntimeException("MFA is not enabled for this user");
        }

        MfaSecret mfaSecret = findMfaSecret(user);
        MfaMethod verificationMethod = method != null ? method : user.getPreferredMfaMethod();

        boolean isValid = false;

        switch (verificationMethod) {
            case TOTP:
                isValid = totpService.verifyCode(mfaSecret.getTotpSecret(), code);
                break;
            case SMS:
                isValid = otpService.verifyOtp(userId, "SMS", code);
                break;
            case EMAIL:
                isValid = otpService.verifyOtp(userId, "EMAIL", code);
                break;
            case BACKUP_CODE:
                isValid = mfaSecret.isBackupCodeValid(code);
                if (isValid) {
                    mfaSecret.useBackupCode(code);
                    mfaSecretRepository.save(mfaSecret);
                }
                break;
        }

        if (isValid) {
            mfaSecret.markUsed(verificationMethod);
            mfaSecretRepository.save(mfaSecret);
            log.info("MFA verification successful for user: {}, method: {}", userId, verificationMethod);
        } else {
            log.warn("MFA verification failed for user: {}, method: {}", userId, verificationMethod);
        }

        return isValid;
    }

    /**
     * Send OTP for login (SMS or Email)
     *
     * @param userId User ID
     * @param method MFA method (SMS or EMAIL)
     * @return OTP code (for testing)
     */
    @Transactional
    public String sendLoginOtp(String userId, MfaMethod method) {
        User user = findUserByUserId(userId);

        if (!user.isMfaEnabled()) {
            throw new RuntimeException("MFA is not enabled for this user");
        }

        String otp = otpService.generateOtp(userId, method.name());

        // TODO: Send via SMS/Email service
        log.info("Login OTP generated for user: {}, method: {}", userId, method);

        return otp; // In production, don't return this
    }

    /**
     * Disable MFA
     *
     * @param userId User ID
     */
    @Transactional
    public void disableMfa(String userId) {
        User user = findUserByUserId(userId);
        MfaSecret mfaSecret = findMfaSecret(user);

        mfaSecret.disableMfa();
        user.disableMfa();

        mfaSecretRepository.save(mfaSecret);
        userRepository.save(user);

        log.info("MFA disabled for user: {}", userId);
    }

    /**
     * Get MFA status
     *
     * @param userId User ID
     * @return MFA status
     */
    public MfaStatus getMfaStatus(String userId) {
        User user = findUserByUserId(userId);

        if (!user.isMfaEnabled()) {
            return new MfaStatus(false, null, false, false, false, 0);
        }

        MfaSecret mfaSecret = findMfaSecret(user);
        int remainingBackupCodes = mfaSecret.getBackupCodesList().size() -
                                   mfaSecret.getUsedBackupCodesList().size();

        return new MfaStatus(
                true,
                user.getPreferredMfaMethod(),
                mfaSecret.getTotpVerified(),
                mfaSecret.getSmsVerified(),
                mfaSecret.getEmailVerified(),
                remainingBackupCodes
        );
    }

    // Helper methods

    private User findUserByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private MfaSecret findMfaSecret(User user) {
        return mfaSecretRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("MFA not set up for this user"));
    }

    private String generateRandomSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private String generateBackupCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            if (i == BACKUP_CODE_LENGTH / 2) {
                code.append("-");
            }
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // DTOs (inner classes for now)

    public record TotpSetupData(String secret, String qrCodeDataUrl) {}

    public record MfaStatus(
            boolean enabled,
            MfaMethod preferredMethod,
            boolean totpEnabled,
            boolean smsEnabled,
            boolean emailEnabled,
            int remainingBackupCodes
    ) {}
}
