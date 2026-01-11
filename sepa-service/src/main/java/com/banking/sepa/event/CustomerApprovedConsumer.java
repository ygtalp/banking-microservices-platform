package com.banking.sepa.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for customer.approved events.
 * Listens for customer approval to enable SEPA services for approved customers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerApprovedConsumer {

    private final ObjectMapper objectMapper;

    /**
     * Handles customer.approved events from Kafka.
     * This allows SEPA service to enable full SEPA functionality for approved customers.
     *
     * @param message The event message
     */
    @KafkaListener(topics = "customer.approved", groupId = "sepa-service-group")
    public void handleCustomerApproved(String message) {
        log.info("Received customer.approved event");

        try {
            JsonNode event = objectMapper.readTree(message);

            String customerId = event.get("customerId").asText();
            String firstName = event.has("firstName") ? event.get("firstName").asText() : null;
            String lastName = event.has("lastName") ? event.get("lastName").asText() : null;
            String email = event.has("email") ? event.get("email").asText() : null;
            String status = event.has("status") ? event.get("status").asText() : null;
            String approvedBy = event.has("approvedBy") ? event.get("approvedBy").asText() : null;

            String fullName = (firstName != null && lastName != null) ? firstName + " " + lastName : "Unknown";

            log.info("Customer approved - ID: {}, Name: {}, Email: {}, Status: {}, Approved by: {}",
                    customerId, fullName, email, status, approvedBy);

            // In a production system, we might:
            // 1. Enable SEPA transfer limits based on customer tier
            // 2. Update customer registry for SEPA compliance checks
            // 3. Send notification about SEPA services availability
            // 4. Create default SEPA mandate templates if applicable
            // 5. Update AML screening whitelist if customer is low-risk

            // For now, we just log the event
            log.debug("SEPA service is now aware of approved customer: {}", customerId);

            // If customer has high-value status, we might enable instant transfers
            if (status != null && status.equals("APPROVED")) {
                log.info("Customer {} is approved for SEPA services", customerId);
            }

        } catch (Exception e) {
            log.error("Error processing customer.approved event", e);
            // In production, implement proper error handling:
            // - Dead letter queue
            // - Retry mechanism
            // - Alert monitoring
        }
    }
}
