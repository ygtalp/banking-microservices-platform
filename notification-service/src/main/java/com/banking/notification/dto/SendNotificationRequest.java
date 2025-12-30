package com.banking.notification.dto;

import com.banking.notification.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Recipient is required")
    private String recipient;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @NotBlank(message = "Template code is required")
    private String templateCode;

    private Map<String, String> parameters;
}
