package com.banking.card.repository;

import com.banking.card.model.Card;
import com.banking.card.model.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardNumber(String cardNumber);
    List<Card> findByCustomerId(String customerId);
    List<Card> findByAccountNumber(String accountNumber);
    List<Card> findByStatus(CardStatus status);
    List<Card> findByCustomerIdAndStatus(String customerId, CardStatus status);
    boolean existsByCardNumber(String cardNumber);
}
