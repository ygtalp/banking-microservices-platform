package com.banking.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MFA Secret Entity
 * Stores user's MFA secrets and backup codes
 */
@Entity
@Table(name = "mfa_secrets", indexes = {
        @Index(name = "idx_mfa_user_id", columnList = "user_id"),
        @Index(name = "idx_mfa_enabled", columnList = "enabled")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaSecret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User Reference
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // TOTP Secret
    @Column(name = "totp_secret", length = 64)
    private String totpSecret;  // Base32 encoded secret for TOTP

    // SMS OTP Secret
    @Column(name = "sms_secret", length = 64)
    private String smsSecret;

    // Email OTP Secret
    @Column(name = "email_secret", length = 64)
    private String emailSecret;

    // Backup Codes (10 codes, comma-separated)
    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes;  // Encrypted backup codes

    @Column(name = "backup_codes_used", columnDefinition = "TEXT")
    private String backupCodesUsed;  // Track used backup codes

    // MFA Status
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_method", length = 20)
    private MfaMethod preferredMethod;

    // Verification Status
    @Column(name = "totp_verified")
    @Builder.Default
    private Boolean totpVerified = false;

    @Column(name = "sms_verified")
    @Builder.Default
    private Boolean smsVerified = false;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    // Last Used
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_used_method", length = 20)
    private MfaMethod lastUsedMethod;

    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper Methods
    public void enableTotp(String secret) {
        this.totpSecret = secret;
        this.totpVerified = true;
        this.enabled = true;
        if (this.preferredMethod == null) {
            this.preferredMethod = MfaMethod.TOTP;
        }
    }

    public void enableSms(String secret) {
        this.smsSecret = secret;
        this.smsVerified = true;
        this.enabled = true;
        if (this.preferredMethod == null) {
            this.preferredMethod = MfaMethod.SMS;
        }
    }

    public void enableEmail(String secret) {
        this.emailSecret = secret;
        this.emailVerified = true;
        this.enabled = true;
        if (this.preferredMethod == null) {
            this.preferredMethod = MfaMethod.EMAIL;
        }
    }

    public void disableMfa() {
        this.enabled = false;
        this.totpSecret = null;
        this.smsSecret = null;
        this.emailSecret = null;
        this.totpVerified = false;
        this.smsVerified = false;
        this.emailVerified = false;
        this.preferredMethod = null;
    }

    public void markUsed(MfaMethod method) {
        this.lastUsedAt = LocalDateTime.now();
        this.lastUsedMethod = method;
    }

    public List<String> getBackupCodesList() {
        if (backupCodes == null || backupCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(backupCodes.split(","));
    }

    public List<String> getUsedBackupCodesList() {
        if (backupCodesUsed == null || backupCodesUsed.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(backupCodesUsed.split(","));
    }

    public void useBackupCode(String code) {
        List<String> usedCodes = new ArrayList<>(getUsedBackupCodesList());
        usedCodes.add(code);
        this.backupCodesUsed = String.join(",", usedCodes);
        this.lastUsedAt = LocalDateTime.now();
        this.lastUsedMethod = MfaMethod.BACKUP_CODE;
    }

    public boolean isBackupCodeValid(String code) {
        List<String> allCodes = getBackupCodesList();
        List<String> usedCodes = getUsedBackupCodesList();
        return allCodes.contains(code) && !usedCodes.contains(code);
    }
}
