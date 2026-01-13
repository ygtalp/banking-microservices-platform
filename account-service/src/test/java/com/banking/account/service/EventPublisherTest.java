package com.banking.account.service;

import com.banking.account.config.KafkaConfig;
import com.banking.account.event.AccountCreatedEvent;
import com.banking.account.event.AccountStatusChangedEvent;
import com.banking.account.event.BalanceChangedEvent;
import com.banking.account.model.AccountStatus;
import com.banking.account.model.AccountType;
import com.banking.account.model.Currency;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event Publisher Tests")
class EventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        // Setup is minimal as we're using mocks
    }

    @Test
    @DisplayName("Should publish AccountCreatedEvent successfully")
    void shouldPublishAccountCreatedEventSuccessfully() throws JsonProcessingException {
        // Given
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .customerName("John Doe")
                .initialBalance(new BigDecimal("1000.00"))
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .build();

        String eventJson = "{\"accountNumber\":\"TR330006100519786457841326\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(eventJson);

        // When
        eventPublisher.publishAccountCreated(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send(
                eq(KafkaConfig.ACCOUNT_CREATED_TOPIC),
                eq("TR330006100519786457841326"),
                eq(eventJson)
        );
    }

    @Test
    @DisplayName("Should handle JsonProcessingException when publishing AccountCreatedEvent")
    void shouldHandleJsonProcessingExceptionWhenPublishingAccountCreatedEvent() throws JsonProcessingException {
        // Given
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .customerName("John Doe")
                .initialBalance(BigDecimal.ZERO)
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .build();

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        // When
        eventPublisher.publishAccountCreated(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should publish BalanceChangedEvent for CREDIT operation")
    void shouldPublishBalanceChangedEventForCreditOperation() throws JsonProcessingException {
        // Given
        BalanceChangedEvent event = BalanceChangedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .operation("CREDIT")
                .amount(new BigDecimal("500.00"))
                .previousBalance(new BigDecimal("1000.00"))
                .newBalance(new BigDecimal("1500.00"))
                .referenceId("REF-123")
                .build();

        String eventJson = "{\"operation\":\"CREDIT\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(eventJson);

        // When
        eventPublisher.publishBalanceChanged(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send(
                eq(KafkaConfig.BALANCE_CHANGED_TOPIC),
                eq("TR330006100519786457841326"),
                eq(eventJson)
        );
    }

    @Test
    @DisplayName("Should publish BalanceChangedEvent for DEBIT operation")
    void shouldPublishBalanceChangedEventForDebitOperation() throws JsonProcessingException {
        // Given
        BalanceChangedEvent event = BalanceChangedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .operation("DEBIT")
                .amount(new BigDecimal("300.00"))
                .previousBalance(new BigDecimal("1500.00"))
                .newBalance(new BigDecimal("1200.00"))
                .referenceId("REF-456")
                .build();

        String eventJson = "{\"operation\":\"DEBIT\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(eventJson);

        // When
        eventPublisher.publishBalanceChanged(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send(
                eq(KafkaConfig.BALANCE_CHANGED_TOPIC),
                eq("TR330006100519786457841326"),
                eq(eventJson)
        );
    }

    @Test
    @DisplayName("Should handle JsonProcessingException when publishing BalanceChangedEvent")
    void shouldHandleJsonProcessingExceptionWhenPublishingBalanceChangedEvent() throws JsonProcessingException {
        // Given
        BalanceChangedEvent event = BalanceChangedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .operation("CREDIT")
                .amount(new BigDecimal("500.00"))
                .previousBalance(BigDecimal.ZERO)
                .newBalance(new BigDecimal("500.00"))
                .build();

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        // When
        eventPublisher.publishBalanceChanged(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should publish AccountStatusChangedEvent to ACCOUNT_FROZEN_TOPIC when status is FROZEN")
    void shouldPublishAccountStatusChangedEventToFrozenTopicWhenFrozen() throws JsonProcessingException {
        // Given
        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .previousStatus(AccountStatus.ACTIVE)
                .newStatus(AccountStatus.FROZEN)
                .reason("Suspicious activity detected")
                .build();

        String eventJson = "{\"newStatus\":\"FROZEN\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(eventJson);

        // When
        eventPublisher.publishAccountStatusChanged(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send(
                eq(KafkaConfig.ACCOUNT_FROZEN_TOPIC),
                eq("TR330006100519786457841326"),
                eq(eventJson)
        );
    }

    @Test
    @DisplayName("Should publish AccountStatusChangedEvent to ACCOUNT_UPDATED_TOPIC when status is ACTIVE")
    void shouldPublishAccountStatusChangedEventToUpdatedTopicWhenActive() throws JsonProcessingException {
        // Given
        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .previousStatus(AccountStatus.FROZEN)
                .newStatus(AccountStatus.ACTIVE)
                .reason("Account activated")
                .build();

        String eventJson = "{\"newStatus\":\"ACTIVE\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(eventJson);

        // When
        eventPublisher.publishAccountStatusChanged(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send(
                eq(KafkaConfig.ACCOUNT_UPDATED_TOPIC),
                eq("TR330006100519786457841326"),
                eq(eventJson)
        );
    }

    @Test
    @DisplayName("Should publish AccountStatusChangedEvent to ACCOUNT_UPDATED_TOPIC when status is CLOSED")
    void shouldPublishAccountStatusChangedEventToUpdatedTopicWhenClosed() throws JsonProcessingException {
        // Given
        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .previousStatus(AccountStatus.ACTIVE)
                .newStatus(AccountStatus.CLOSED)
                .reason("Account closed by customer request")
                .build();

        String eventJson = "{\"newStatus\":\"CLOSED\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(eventJson);

        // When
        eventPublisher.publishAccountStatusChanged(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send(
                eq(KafkaConfig.ACCOUNT_UPDATED_TOPIC),
                eq("TR330006100519786457841326"),
                eq(eventJson)
        );
    }

    @Test
    @DisplayName("Should handle JsonProcessingException when publishing AccountStatusChangedEvent")
    void shouldHandleJsonProcessingExceptionWhenPublishingAccountStatusChangedEvent() throws JsonProcessingException {
        // Given
        AccountStatusChangedEvent event = AccountStatusChangedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123456")
                .previousStatus(AccountStatus.ACTIVE)
                .newStatus(AccountStatus.FROZEN)
                .reason("Test")
                .build();

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        // When
        eventPublisher.publishAccountStatusChanged(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should use account number as Kafka message key for all events")
    void shouldUseAccountNumberAsKafkaMessageKey() throws JsonProcessingException {
        // Given
        String accountNumber = "TR330006100519786457841326";

        AccountCreatedEvent createdEvent = AccountCreatedEvent.builder()
                .accountNumber(accountNumber)
                .customerId("CUS-123")
                .customerName("Test")
                .initialBalance(BigDecimal.ZERO)
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        eventPublisher.publishAccountCreated(createdEvent);

        // Then
        verify(kafkaTemplate).send(
                anyString(),
                eq(accountNumber), // Key should be account number
                anyString()
        );
    }

    @Test
    @DisplayName("Should publish multiple events independently")
    void shouldPublishMultipleEventsIndependently() throws JsonProcessingException {
        // Given
        AccountCreatedEvent createdEvent = AccountCreatedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123")
                .customerName("Test")
                .initialBalance(BigDecimal.ZERO)
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .build();

        BalanceChangedEvent balanceEvent = BalanceChangedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123")
                .operation("CREDIT")
                .amount(new BigDecimal("100.00"))
                .previousBalance(BigDecimal.ZERO)
                .newBalance(new BigDecimal("100.00"))
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        eventPublisher.publishAccountCreated(createdEvent);
        eventPublisher.publishBalanceChanged(balanceEvent);

        // Then
        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), anyString());
        verify(kafkaTemplate).send(eq(KafkaConfig.ACCOUNT_CREATED_TOPIC), anyString(), anyString());
        verify(kafkaTemplate).send(eq(KafkaConfig.BALANCE_CHANGED_TOPIC), anyString(), anyString());
    }

    @Test
    @DisplayName("Should serialize events to JSON before publishing")
    void shouldSerializeEventsToJsonBeforePublishing() throws JsonProcessingException {
        // Given
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountNumber("TR330006100519786457841326")
                .customerId("CUS-123")
                .customerName("Test")
                .initialBalance(new BigDecimal("1000.00"))
                .currency(Currency.TRY)
                .accountType(AccountType.CHECKING)
                .build();

        String expectedJson = "{\"accountNumber\":\"TR330006100519786457841326\",\"customerId\":\"CUS-123\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(expectedJson);

        // When
        eventPublisher.publishAccountCreated(event);

        // Then
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send(anyString(), anyString(), eq(expectedJson));
    }
}
