package com.banking.card.controller;

import com.banking.card.dto.CardIssueRequest;
import com.banking.card.dto.CardResponse;
import com.banking.card.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card Management APIs")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Issue new card")
    public ResponseEntity<CardResponse> issueCard(@Valid @RequestBody CardIssueRequest request) {
        CardResponse response = cardService.issueCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{cardNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get card details")
    public ResponseEntity<CardResponse> getCard(@PathVariable String cardNumber) {
        return ResponseEntity.ok(cardService.getCard(cardNumber));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer cards")
    public ResponseEntity<List<CardResponse>> getCustomerCards(@PathVariable String customerId) {
        return ResponseEntity.ok(cardService.getCustomerCards(customerId));
    }

    @PostMapping("/{cardNumber}/activate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Activate card")
    public ResponseEntity<CardResponse> activateCard(
            @PathVariable String cardNumber,
            @RequestParam String pin) {
        return ResponseEntity.ok(cardService.activateCard(cardNumber, pin));
    }

    @PostMapping("/{cardNumber}/block")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Block card")
    public ResponseEntity<CardResponse> blockCard(
            @PathVariable String cardNumber,
            @RequestParam String reason) {
        return ResponseEntity.ok(cardService.blockCard(cardNumber, reason));
    }
}
