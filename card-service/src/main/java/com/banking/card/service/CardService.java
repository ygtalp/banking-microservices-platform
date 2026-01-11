package com.banking.card.service;

import com.banking.card.dto.CardIssueRequest;
import com.banking.card.dto.CardResponse;
import com.banking.card.model.Card;
import com.banking.card.model.CardStatus;
import com.banking.card.model.CardType;
import com.banking.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public CardResponse issueCard(CardIssueRequest request) {
        String cardNumber = generateCardNumber();
        String cvv = generateCVV();
        LocalDate expiryDate = LocalDate.now().plusYears(3);

        Card card = Card.builder()
                .cardNumber(cardNumber)
                .customerId(request.getCustomerId())
                .accountNumber(request.getAccountNumber())
                .cardType(request.getCardType())
                .cardholderName(request.getCardholderName())
                .cvv(cvv)
                .expiryDate(expiryDate)
                .status(CardStatus.PENDING)
                .dailyLimit(request.getDailyLimit())
                .monthlyLimit(request.getMonthlyLimit())
                .transactionLimit(request.getDailyLimit())
                .creditLimit(request.getCreditLimit())
                .availableCredit(request.getCreditLimit())
                .build();

        Card savedCard = cardRepository.save(card);
        log.info("Card issued: {}", maskCardNumber(cardNumber));

        return mapToResponse(savedCard);
    }

    public CardResponse getCard(String cardNumber) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        return mapToResponse(card);
    }

    public List<CardResponse> getCustomerCards(String customerId) {
        return cardRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CardResponse activateCard(String cardNumber, String pin) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (card.getStatus() != CardStatus.PENDING) {
            throw new RuntimeException("Card cannot be activated");
        }

        card.activate();
        // In production: hash PIN with BCrypt
        card.setPinHash(hashPin(pin));

        Card savedCard = cardRepository.save(card);
        log.info("Card activated: {}", maskCardNumber(cardNumber));

        return mapToResponse(savedCard);
    }

    @Transactional
    public CardResponse blockCard(String cardNumber, String reason) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        card.block(reason);
        Card savedCard = cardRepository.save(card);

        log.info("Card blocked: {}, reason: {}", maskCardNumber(cardNumber), reason);
        return mapToResponse(savedCard);
    }

    private String generateCardNumber() {
        StringBuilder cardNumber;
        do {
            cardNumber = new StringBuilder("4"); // Visa starts with 4
            for (int i = 1; i < 16; i++) {
                cardNumber.append(RANDOM.nextInt(10));
            }
        } while (cardRepository.existsByCardNumber(cardNumber.toString()));

        return cardNumber.toString();
    }

    private String generateCVV() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber.length() < 4) return cardNumber;
        return "**** **** **** " + cardNumber.substring(12);
    }

    private String hashPin(String pin) {
        // In production: use BCrypt
        return "hashed_" + pin;
    }

    private CardResponse mapToResponse(Card card) {
        return CardResponse.builder()
                .cardNumber(card.getCardNumber())
                .maskedCardNumber(maskCardNumber(card.getCardNumber()))
                .customerId(card.getCustomerId())
                .accountNumber(card.getAccountNumber())
                .cardType(card.getCardType())
                .cardholderName(card.getCardholderName())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .dailyLimit(card.getDailyLimit())
                .monthlyLimit(card.getMonthlyLimit())
                .dailySpent(card.getDailySpent())
                .monthlySpent(card.getMonthlySpent())
                .creditLimit(card.getCreditLimit())
                .availableCredit(card.getAvailableCredit())
                .isContactlessEnabled(card.getIsContactlessEnabled())
                .isOnlineEnabled(card.getIsOnlineEnabled())
                .isInternationalEnabled(card.getIsInternationalEnabled())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
