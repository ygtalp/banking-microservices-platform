package com.banking.notification.event;

import com.banking.notification.model.Notification;
import com.banking.notification.model.NotificationChannel;
import com.banking.notification.service.NotificationService;
import com.banking.notification.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final UserPreferenceService userPreferenceService;

    @KafkaListener(topics = "account.created", groupId = "notification-service")
    public void handleAccountCreated(AccountCreatedEvent event) {
        log.info("Received AccountCreatedEvent for account: {}", event.getAccountNumber());

        try {
            // Get user preference to find email
            var preference = userPreferenceService.getOrCreateUserPreference(event.getCustomerId());

            if (preference.getEmail() != null && preference.isChannelEnabled(NotificationChannel.EMAIL)) {
                Map<String, String> parameters = new HashMap<>();
                parameters.put("customerName", event.getCustomerName());
                parameters.put("accountNumber", event.getAccountNumber());
                parameters.put("iban", event.getIban());
                parameters.put("accountType", event.getAccountType());
                parameters.put("currency", event.getCurrency());

                Notification notification = notificationService.createNotification(
                        event.getCustomerId(),
                        preference.getEmail(),
                        NotificationChannel.EMAIL,
                        "ACCOUNT_CREATED",
                        parameters
                );

                notificationService.sendNotification(notification);
                log.info("Account creation notification sent for: {}", event.getAccountNumber());
            }
        } catch (Exception e) {
            log.error("Failed to send account creation notification", e);
        }
    }

    @KafkaListener(topics = "transfer.completed", groupId = "notification-service")
    public void handleTransferCompleted(TransferCompletedEvent event) {
        log.info("Received TransferCompletedEvent for transfer: {}", event.getTransferReference());

        try {
            // This would require customer lookup by account number
            // For now, log the event
            log.info("Transfer completed notification would be sent for: {}", event.getTransferReference());

            // In production: lookup customer by account, send notification
            Map<String, String> parameters = new HashMap<>();
            parameters.put("transferReference", event.getTransferReference());
            parameters.put("amount", event.getAmount().toString());
            parameters.put("currency", event.getCurrency());
            parameters.put("status", event.getStatus());

            // Would send notification to both sender and receiver
            log.info("Transfer notification prepared with parameters: {}", parameters);

        } catch (Exception e) {
            log.error("Failed to send transfer notification", e);
        }
    }

    @KafkaListener(topics = "customer.verified", groupId = "notification-service")
    public void handleCustomerVerified(CustomerVerifiedEvent event) {
        log.info("Received CustomerVerifiedEvent for customer: {}", event.getCustomerId());

        try {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("firstName", event.getFirstName());
            parameters.put("lastName", event.getLastName());
            parameters.put("status", event.getStatus());

            Notification notification = notificationService.createNotification(
                    event.getCustomerId(),
                    event.getEmail(),
                    NotificationChannel.EMAIL,
                    "CUSTOMER_VERIFIED",
                    parameters
            );

            notificationService.sendNotification(notification);
            log.info("Customer verification notification sent for: {}", event.getCustomerId());

        } catch (Exception e) {
            log.error("Failed to send customer verification notification", e);
        }
    }
}
