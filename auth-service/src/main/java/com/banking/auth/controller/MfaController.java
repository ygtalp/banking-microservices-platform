package com.banking.auth.controller;

import com.banking.auth.dto.ApiResponse;
import com.banking.auth.dto.mfa.*;
import com.banking.auth.service.MfaService;
import com.banking.auth.service.MfaService.TotpSetupData;
import com.banking.auth.service.MfaService.MfaStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MFA Controller
 * Handles Multi-Factor Authentication setup, verification, and management
 */
@RestController
@RequestMapping("/mfa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MFA", description = "Multi-Factor Authentication APIs")
public class MfaController {

    private final MfaService mfaService;

    /**
     * Setup TOTP (Google Authenticator)
     */
    @PostMapping("/setup/totp")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Setup TOTP", description = "Initialize TOTP setup and get QR code")
    public ResponseEntity<TotpSetupResponse> setupTotp(Authentication authentication) {
        String userId = extractUserId(authentication);
        TotpSetupData setupData = mfaService.setupTotp(userId);

        TotpSetupResponse response = new TotpSetupResponse(
                setupData.secret(),
                setupData.qrCodeDataUrl(),
                "Scan this QR code with Google Authenticator app and enter the 6-digit code to complete setup"
        );

        log.info("TOTP setup initiated for user: {}", userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify and enable TOTP
     */
    @PostMapping("/setup/totp/verify")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Verify TOTP", description = "Verify TOTP code and enable TOTP MFA")
    public ResponseEntity<ApiResponse> verifyTotp(
            @Valid @RequestBody MfaVerifyRequest request,
            Authentication authentication) {

        String userId = extractUserId(authentication);
        boolean isVerified = mfaService.verifyAndEnableTotp(userId, request.getCode());

        if (isVerified) {
            log.info("TOTP enabled for user: {}", userId);
            return ResponseEntity.ok(ApiResponse.success(
                    "TOTP enabled successfully. Your account is now protected with Two-Factor Authentication.",
                    null
            ));
        } else {
            log.warn("TOTP verification failed for user: {}", userId);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Invalid TOTP code. Please try again."
            ));
        }
    }

    /**
     * Setup SMS OTP
     */
    @PostMapping("/setup/sms")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Setup SMS OTP", description = "Setup SMS-based OTP and receive verification code")
    public ResponseEntity<ApiResponse> setupSms(
            @Valid @RequestBody MfaSetupRequest request,
            Authentication authentication) {

        String userId = extractUserId(authentication);

        if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Phone number is required for SMS MFA"
            ));
        }

        String otp = mfaService.setupSms(userId, request.getPhoneNumber());

        log.info("SMS OTP sent to user: {}", userId);

        // In development, return OTP. In production, send via SMS and don't return
        return ResponseEntity.ok(ApiResponse.success(
                "Verification code sent to your phone. (DEV: " + otp + ")",
                null
        ));
    }

    /**
     * Verify and enable SMS OTP
     */
    @PostMapping("/setup/sms/verify")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Verify SMS OTP", description = "Verify SMS code and enable SMS MFA")
    public ResponseEntity<ApiResponse> verifySms(
            @Valid @RequestBody MfaVerifyRequest request,
            Authentication authentication) {

        String userId = extractUserId(authentication);
        boolean isVerified = mfaService.verifyAndEnableSms(userId, request.getCode());

        if (isVerified) {
            log.info("SMS MFA enabled for user: {}", userId);
            return ResponseEntity.ok(ApiResponse.success(
                    "SMS MFA enabled successfully.",
                    null
            ));
        } else {
            log.warn("SMS verification failed for user: {}", userId);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Invalid or expired SMS code. Please try again."
            ));
        }
    }

    /**
     * Setup Email OTP
     */
    @PostMapping("/setup/email")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Setup Email OTP", description = "Setup Email-based OTP and receive verification code")
    public ResponseEntity<ApiResponse> setupEmail(Authentication authentication) {
        String userId = extractUserId(authentication);
        String otp = mfaService.setupEmail(userId);

        log.info("Email OTP sent to user: {}", userId);

        // In development, return OTP. In production, send via email and don't return
        return ResponseEntity.ok(ApiResponse.success(
                "Verification code sent to your email. (DEV: " + otp + ")",
                null
        ));
    }

    /**
     * Verify and enable Email OTP
     */
    @PostMapping("/setup/email/verify")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Verify Email OTP", description = "Verify Email code and enable Email MFA")
    public ResponseEntity<ApiResponse> verifyEmail(
            @Valid @RequestBody MfaVerifyRequest request,
            Authentication authentication) {

        String userId = extractUserId(authentication);
        boolean isVerified = mfaService.verifyAndEnableEmail(userId, request.getCode());

        if (isVerified) {
            log.info("Email MFA enabled for user: {}", userId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Email MFA enabled successfully.",
                    null
            ));
        } else {
            log.warn("Email verification failed for user: {}", userId);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Invalid or expired email code. Please try again."
            ));
        }
    }

    /**
     * Get MFA status
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get MFA Status", description = "Get current MFA configuration status")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(Authentication authentication) {
        String userId = extractUserId(authentication);
        MfaStatus status = mfaService.getMfaStatus(userId);

        MfaStatusResponse response = new MfaStatusResponse(
                status.enabled(),
                status.preferredMethod(),
                status.totpEnabled(),
                status.smsEnabled(),
                status.emailEnabled(),
                status.remainingBackupCodes()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Generate backup codes
     */
    @PostMapping("/backup-codes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate Backup Codes", description = "Generate new backup recovery codes")
    public ResponseEntity<BackupCodesResponse> generateBackupCodes(Authentication authentication) {
        String userId = extractUserId(authentication);
        List<String> codes = mfaService.generateBackupCodes(userId);

        BackupCodesResponse response = new BackupCodesResponse(
                codes,
                "Save these codes in a secure location. Each code can only be used once."
        );

        log.info("Generated backup codes for user: {}", userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Disable MFA
     */
    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Disable MFA", description = "Disable Multi-Factor Authentication")
    public ResponseEntity<ApiResponse> disableMfa(Authentication authentication) {
        String userId = extractUserId(authentication);
        mfaService.disableMfa(userId);

        log.info("MFA disabled for user: {}", userId);
        return ResponseEntity.ok(ApiResponse.success(
                "Multi-Factor Authentication has been disabled.",
                null
        ));
    }

    // Helper method
    private String extractUserId(Authentication authentication) {
        // Extract user ID from authentication principal (email)
        // This assumes the principal is the email address
        String email = authentication.getName();
        // In a real scenario, you'd fetch the userId from UserRepository
        // For now, we'll use email as userId (this should be fixed in production)
        return email;
    }
}
