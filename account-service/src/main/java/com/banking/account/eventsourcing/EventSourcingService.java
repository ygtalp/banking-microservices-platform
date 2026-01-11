package com.banking.account.eventsourcing;

import com.banking.account.model.Account;
import com.banking.account.model.AccountStatus;
import com.banking.account.model.AccountType;
import com.banking.account.model.Currency;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4f;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Event Sourcing Service
 * Handles event persistence and state reconstruction
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventSourcingService {

    private final AccountEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save an event to the event store
     *
     * @param accountNumber Account number (aggregate ID)
     * @param eventType     Type of event
     * @param eventData     Event payload (will be serialized to JSON)
     * @param userId        User who triggered the event
     * @param correlationId Correlation ID for tracking
     * @return Saved event
     */
    @Transactional
    public AccountEvent saveEvent(
            String accountNumber,
            EventType eventType,
            Map<String, Object> eventData,
            String userId,
            String correlationId) {

        // Get current version
        Long currentVersion = eventRepository.findLatestVersion(accountNumber);
        Long nextVersion = (currentVersion == null) ? 1L : currentVersion + 1;

        try {
            String jsonData = objectMapper.writeValueAsString(eventData);

            AccountEvent event = AccountEvent.builder()
                    .accountNumber(accountNumber)
                    .eventType(eventType)
                    .aggregateVersion(nextVersion)
                    .eventData(jsonData)
                    .userId(userId)
                    .correlationId(correlationId)
                    .build();

            AccountEvent savedEvent = eventRepository.save(event);
            log.info("Event saved: type={}, account={}, version={}",
                    eventType, accountNumber, nextVersion);

            return savedEvent;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event data", e);
            throw new RuntimeException("Failed to save event", e);
        }
    }

    /**
     * Replay all events to reconstruct account state
     *
     * @param accountNumber Account number
     * @return Reconstructed account state
     */
    @Transactional(readOnly = true)
    public Account replayEvents(String accountNumber) {
        List<AccountEvent> events = eventRepository.findByAccountNumberOrderByAggregateVersionAsc(accountNumber);

        if (events.isEmpty()) {
            throw new IllegalArgumentException("No events found for account: " + accountNumber);
        }

        Account account = new Account();
        account.setAccountNumber(accountNumber);

        for (AccountEvent event : events) {
            applyEvent(account, event);
        }

        log.info("Replayed {} events for account: {}", events.size(), accountNumber);
        return account;
    }

    /**
     * Replay events from a specific version
     *
     * @param accountNumber Account number
     * @param fromVersion   Starting version (exclusive)
     * @param initialState  Initial account state
     * @return Updated account state
     */
    @Transactional(readOnly = true)
    public Account replayEventsFrom(String accountNumber, Long fromVersion, Account initialState) {
        List<AccountEvent> events = eventRepository
                .findByAccountNumberAndAggregateVersionGreaterThanOrderByAggregateVersionAsc(
                        accountNumber, fromVersion);

        Account account = initialState;

        for (AccountEvent event : events) {
            applyEvent(account, event);
        }

        log.info("Replayed {} events from version {} for account: {}",
                events.size(), fromVersion, accountNumber);
        return account;
    }

    /**
     * Get event history for an account
     *
     * @param accountNumber Account number
     * @return List of events
     */
    public List<AccountEvent> getEventHistory(String accountNumber) {
        return eventRepository.findByAccountNumberOrderByAggregateVersionAsc(accountNumber);
    }

    /**
     * Get event count for an account
     *
     * @param accountNumber Account number
     * @return Event count
     */
    public Long getEventCount(String accountNumber) {
        return eventRepository.countByAccountNumber(accountNumber);
    }

    /**
     * Apply an event to an account (state mutation)
     */
    private void applyEvent(Account account, AccountEvent event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(event.getEventData(), Map.class);

            switch (event.getEventType()) {
                case ACCOUNT_CREATED:
                    applyAccountCreated(account, data);
                    break;
                case BALANCE_CREDITED:
                    applyBalanceCredited(account, data);
                    break;
                case BALANCE_DEBITED:
                    applyBalanceDebited(account, data);
                    break;
                case BALANCE_UPDATED:
                    applyBalanceUpdated(account, data);
                    break;
                case ACCOUNT_SUSPENDED:
                    account.setStatus(AccountStatus.SUSPENDED);
                    break;
                case ACCOUNT_ACTIVATED:
                    account.setStatus(AccountStatus.ACTIVE);
                    break;
                case ACCOUNT_CLOSED:
                    account.setStatus(AccountStatus.CLOSED);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event data for event ID: {}", event.getId(), e);
            throw new RuntimeException("Failed to apply event", e);
        }
    }

    private void applyAccountCreated(Account account, Map<String, Object> data) {
        account.setCustomerName((String) data.get("customerName"));
        account.setIban((String) data.get("iban"));
        account.setAccountType(AccountType.valueOf((String) data.get("accountType")));
        account.setCurrency(Currency.valueOf((String) data.get("currency")));
        account.setBalance(new BigDecimal(data.get("balance").toString()));
        account.setStatus(AccountStatus.ACTIVE);
    }

    private void applyBalanceCredited(Account account, Map<String, Object> data) {
        BigDecimal amount = new BigDecimal(data.get("amount").toString());
        account.setBalance(account.getBalance().add(amount));
    }

    private void applyBalanceDebited(Account account, Map<String, Object> data) {
        BigDecimal amount = new BigDecimal(data.get("amount").toString());
        account.setBalance(account.getBalance().subtract(amount));
    }

    private void applyBalanceUpdated(Account account, Map<String, Object> data) {
        account.setBalance(new BigDecimal(data.get("newBalance").toString()));
    }
}
