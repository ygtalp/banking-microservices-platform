package com.banking.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserPreferenceRequest {

    private String email;
    private String phoneNumber;
    private String deviceToken;

    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;

    private Boolean accountNotifications;
    private Boolean transferNotifications;
    private Boolean securityNotifications;
    private Boolean marketingNotifications;

    private String language;
    private String timezone;
}
