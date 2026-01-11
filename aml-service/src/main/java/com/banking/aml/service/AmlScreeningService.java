package com.banking.aml.service;

import com.banking.aml.event.AmlAlertCreatedEvent;
import com.banking.aml.model.*;
import com.banking.aml.repository.AmlAlertRepository;
import com.banking.aml.repository.SanctionMatchRepository;
import com.banking.aml.repository.TransactionMonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmlScreeningService {

    private final AmlAlertRepository alertRepository;
    private final SanctionMatchRepository sanctionMatchRepository;
    private final TransactionMonitoringRepository transactionMonitoringRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CustomerRiskScoringService customerRiskScoringService;

    @Transactional
    public AmlAlert createAlert(String accountNumber, String transferReference,
                                 BigDecimal amount, String currency,
                                 AlertType alertType, Integer riskScore, List<String> reasons) {
        log.info("Creating AML alert for account: {}, type: {}, risk: {}",
                 accountNumber, alertType, riskScore);

        AmlAlert alert = AmlAlert.builder()
                .alertType(alertType)
                .status(AlertStatus.OPEN)
                .accountNumber(accountNumber)
                .transferReference(transferReference)
                .amount(amount)
                .currency(currency)
                .riskScore(riskScore)
                .riskLevel(RiskLevel.fromScore(riskScore))
                .reasons(reasons)
                .build();

        alert = alertRepository.save(alert);

        // Publish alert created event
        publishAlertEvent(alert);

        // If critical risk, publish escalation event
        if (alert.getRiskLevel() == RiskLevel.CRITICAL) {
            publishEscalationEvent(alert);
        }

        log.info("AML alert created: {}, risk level: {}", alert.getAlertId(), alert.getRiskLevel());
        return alert;
    }

    @Transactional
    @CacheEvict(value = "amlAlerts", key = "#alertId")
    public AmlAlert updateAlertStatus(String alertId, AlertStatus newStatus,
                                       String reviewedBy, String notes) {
        log.info("Updating alert {} status to: {}", alertId, newStatus);

        AmlAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        alert.setStatus(newStatus);
        alert.setReviewedBy(reviewedBy);
        alert.setReviewedAt(LocalDateTime.now());
        if (notes != null) {
            alert.setNotes(notes);
        }

        alert = alertRepository.save(alert);
        log.info("Alert {} updated to status: {}", alertId, newStatus);

        return alert;
    }

    @Cacheable(value = "amlAlerts", key = "#alertId")
    public Optional<AmlAlert> getAlert(String alertId) {
        return alertRepository.findById(alertId);
    }

    public List<AmlAlert> getAlertsByAccount(String accountNumber) {
        return alertRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public Page<AmlAlert> getAlertsByAccount(String accountNumber, Pageable pageable) {
        return alertRepository.findByAccountNumber(accountNumber, pageable);
    }

    public List<AmlAlert> getAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<AmlAlert> getPendingReview() {
        return alertRepository.findPendingReview();
    }

    public List<AmlAlert> getHighRiskAlerts() {
        return alertRepository.findHighRiskOpenAlerts();
    }

    public List<AmlAlert> getRecentAlerts(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return alertRepository.findRecentAlerts(since);
    }

    @Transactional
    public SanctionMatch createSanctionMatch(String accountNumber, String customerName,
                                             String nationalId, String sanctionedName,
                                             String sanctionedEntityId, String sanctionList,
                                             Integer matchScore, String matchReason) {
        log.info("Creating sanction match for account: {}, list: {}, score: {}",
                 accountNumber, sanctionList, matchScore);

        SanctionMatch match = SanctionMatch.builder()
                .accountNumber(accountNumber)
                .customerName(customerName)
                .nationalId(nationalId)
                .sanctionedName(sanctionedName)
                .sanctionedEntityId(sanctionedEntityId)
                .sanctionList(sanctionList)
                .matchScore(matchScore)
                .matchReason(matchReason)
                .build();

        match = sanctionMatchRepository.save(match);

        // Create alert if match score is high
        if (matchScore >= 70) {
            createAlert(accountNumber, null, null, null,
                       AlertType.SANCTIONS_MATCH, matchScore,
                       List.of("Sanctions match detected: " + sanctionList + " - " + sanctionedName));
        }

        log.info("Sanction match created: {}", match.getMatchId());
        return match;
    }

    public List<SanctionMatch> getSanctionMatchesByAccount(String accountNumber) {
        return sanctionMatchRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public List<SanctionMatch> getPotentialMatches(int minScore) {
        return sanctionMatchRepository.findPotentialMatches(minScore);
    }

    public List<SanctionMatch> getUnreviewedMatches() {
        return sanctionMatchRepository.findUnreviewedMatches();
    }

    @Transactional
    public SanctionMatch reviewMatch(String matchId, String matchStatus, String reviewedBy) {
        log.info("Reviewing sanction match: {}, status: {}", matchId, matchStatus);

        SanctionMatch match = sanctionMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

        match.setMatchStatus(matchStatus);
        match.setReviewedBy(reviewedBy);
        match.setReviewedAt(LocalDateTime.now());

        return sanctionMatchRepository.save(match);
    }

    public Long getAlertCount(AlertStatus status) {
        return alertRepository.countByStatus(status);
    }

    public Long getAlertCountByRisk(RiskLevel riskLevel) {
        return alertRepository.countByRiskLevel(riskLevel);
    }

    private void publishAlertEvent(AmlAlert alert) {
        try {
            AmlAlertCreatedEvent event = new AmlAlertCreatedEvent(
                    alert.getAlertId(),
                    alert.getAlertType().name(),
                    alert.getStatus().name(),
                    alert.getAccountNumber(),
                    alert.getCustomerId(),
                    alert.getCustomerName(),
                    alert.getAmount(),
                    alert.getCurrency(),
                    alert.getRiskScore(),
                    alert.getRiskLevel().name(),
                    alert.getReasons(),
                    alert.getTransferReference(),
                    LocalDateTime.now()
            );
            kafkaTemplate.send("aml.alert.created", alert.getAlertId(), event);
            log.debug("Published alert created event for: {}", alert.getAlertId());

            // Update customer risk profile
            if (alert.getCustomerId() != null && alert.getAccountNumber() != null) {
                customerRiskScoringService.updateAfterAlert(
                        alert.getCustomerId(),
                        alert.getAccountNumber(),
                        alert.getStatus().name()
                );
            }
        } catch (Exception e) {
            log.error("Failed to publish alert event for: {}", alert.getAlertId(), e);
        }
    }

    private void publishEscalationEvent(AmlAlert alert) {
        try {
            AmlAlertCreatedEvent event = new AmlAlertCreatedEvent(
                    alert.getAlertId(),
                    alert.getAlertType().name(),
                    "ESCALATED",
                    alert.getAccountNumber(),
                    alert.getCustomerId(),
                    alert.getCustomerName(),
                    alert.getAmount(),
                    alert.getCurrency(),
                    alert.getRiskScore(),
                    alert.getRiskLevel().name(),
                    alert.getReasons(),
                    alert.getTransferReference(),
                    LocalDateTime.now()
            );
            kafkaTemplate.send("aml.alert.escalated", alert.getAlertId(), event);
            log.warn("Published escalation event for critical alert: {}", alert.getAlertId());
        } catch (Exception e) {
            log.error("Failed to publish escalation event for: {}", alert.getAlertId(), e);
        }
    }
}
