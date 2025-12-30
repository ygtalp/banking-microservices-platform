package com.banking.notification.handler;

import com.banking.notification.exception.NotificationDeliveryException;
import com.banking.notification.model.Notification;
import com.banking.notification.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsNotificationHandler implements NotificationHandler {

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.provider:twilio}")
    private String smsProvider;

    @Value("${notification.sms.account-sid:}")
    private String accountSid;

    @Value("${notification.sms.auth-token:}")
    private String authToken;

    @Value("${notification.sms.from-number:}")
    private String fromNumber;

    @Override
    public String send(Notification notification) {
        if (!smsEnabled) {
            log.warn("SMS notifications are disabled");
            return "SMS_DISABLED";
        }

        try {
            // Mock SMS sending - in production, integrate with Twilio/AWS SNS/etc.
            log.info("Sending SMS to: {} via provider: {}",
                     notification.getRecipient(), smsProvider);
            log.info("SMS Content: {}", notification.getContent());

            // Simulate SMS sending
            String externalId = "SMS-" + System.currentTimeMillis();
            log.info("SMS sent successfully to: {}, externalId: {}",
                     notification.getRecipient(), externalId);

            return externalId;

        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", notification.getRecipient(), e);
            throw new NotificationDeliveryException("Failed to send SMS", e);
        }
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SMS;
    }
}
