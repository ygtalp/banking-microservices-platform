package com.banking.aml.service;

import com.banking.aml.model.*;
import com.banking.aml.repository.MonitoringRuleRepository;
import com.banking.aml.repository.TransactionMonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionMonitoringService {

    private final TransactionMonitoringRepository transactionMonitoringRepository;
    private final MonitoringRuleRepository monitoringRuleRepository;
    private final AmlScreeningService amlScreeningService;
    private final CustomerRiskScoringService customerRiskScoringService;

    @Transactional
    public TransactionMonitoring monitorTransaction(String accountNumber, String transferReference,
                                                     BigDecimal amount, String currency,
                                                     LocalDateTime transactionDate) {
        log.info("Monitoring transaction: {}, account: {}, amount: {} {}",
                 transferReference, accountNumber, amount, currency);

        // Get enabled rules
        List<MonitoringRule> enabledRules = monitoringRuleRepository.findByEnabledTrue();

        int totalRiskScore = 0;
        List<String> triggeredRules = new ArrayList<>();
        List<String> alertReasons = new ArrayList<>();

        // Apply each rule
        for (MonitoringRule rule : enabledRules) {
            boolean triggered = false;

            switch (rule.getRuleType()) {
                case "VELOCITY":
                    triggered = checkVelocityRule(rule, accountNumber, transactionDate);
                    if (triggered) {
                        alertReasons.add("Velocity rule triggered: " + rule.getDescription());
                    }
                    break;

                case "AMOUNT":
                    triggered = checkAmountRule(rule, amount);
                    if (triggered) {
                        alertReasons.add("Amount threshold exceeded: " + rule.getThresholdAmount());
                    }
                    break;

                case "DAILY_LIMIT":
                    triggered = checkDailyLimitRule(rule, accountNumber, amount, currency, transactionDate);
                    if (triggered) {
                        alertReasons.add("Daily limit exceeded: " + rule.getThresholdAmount());
                    }
                    break;

                case "TIME_BASED":
                    triggered = checkTimeBasedRule(rule, amount, transactionDate);
                    if (triggered) {
                        alertReasons.add("Suspicious time-based activity");
                    }
                    break;

                case "STRUCTURING":
                    triggered = checkStructuringRule(rule, amount);
                    if (triggered) {
                        alertReasons.add("Potential structuring detected");
                    }
                    break;

                case "ROUND_AMOUNT":
                    triggered = checkRoundAmountRule(amount);
                    if (triggered) {
                        alertReasons.add("Unusual round amount pattern");
                    }
                    break;
            }

            if (triggered) {
                totalRiskScore += rule.getRiskPoints();
                triggeredRules.add(rule.getRuleId());
            }
        }

        // Create monitoring record
        boolean flagged = totalRiskScore >= 30; // Medium risk threshold

        TransactionMonitoring monitoring = TransactionMonitoring.builder()
                .accountNumber(accountNumber)
                .transferReference(transferReference)
                .amount(amount)
                .currency(currency)
                .transactionDate(transactionDate)
                .riskScore(totalRiskScore)
                .triggeredRules(String.join(",", triggeredRules))
                .flagged(flagged)
                .build();

        monitoring = transactionMonitoringRepository.save(monitoring);

        // Update customer risk profile with transaction data
        try {
            // Extract customer ID from account number or use account number as fallback
            String customerId = "CUS-" + accountNumber.replace("ACC-", "");
            customerRiskScoringService.updateAfterTransaction(
                    customerId, accountNumber, amount, flagged);
        } catch (Exception e) {
            log.warn("Failed to update customer risk profile for account {}: {}",
                    accountNumber, e.getMessage());
        }

        // Create alert if flagged
        if (flagged) {
            AlertType alertType = determineAlertType(triggeredRules, enabledRules);
            AmlAlert alert = amlScreeningService.createAlert(
                accountNumber, transferReference, amount, currency,
                alertType, totalRiskScore, alertReasons);

            monitoring.setAlertId(alert.getAlertId());
            monitoring = transactionMonitoringRepository.save(monitoring);
        }

        log.info("Transaction monitoring completed: {}, risk score: {}, flagged: {}",
                 transferReference, totalRiskScore, flagged);

        return monitoring;
    }

    private boolean checkVelocityRule(MonitoringRule rule, String accountNumber,
                                      LocalDateTime transactionDate) {
        LocalDateTime since = transactionDate.minusMinutes(rule.getTimeWindowMinutes());
        Long count = transactionMonitoringRepository.countRecentTransactions(accountNumber, since);
        return count >= rule.getThresholdCount();
    }

    private boolean checkAmountRule(MonitoringRule rule, BigDecimal amount) {
        return amount.compareTo(rule.getThresholdAmount()) > 0;
    }

    private boolean checkDailyLimitRule(MonitoringRule rule, String accountNumber,
                                       BigDecimal amount, String currency,
                                       LocalDateTime transactionDate) {
        LocalDateTime startOfDay = transactionDate.toLocalDate().atStartOfDay();
        BigDecimal dailyTotal = transactionMonitoringRepository.sumRecentAmounts(
            accountNumber, startOfDay, currency);
        BigDecimal newTotal = dailyTotal.add(amount);
        return newTotal.compareTo(rule.getThresholdAmount()) > 0;
    }

    private boolean checkTimeBasedRule(MonitoringRule rule, BigDecimal amount,
                                       LocalDateTime transactionDate) {
        // Check if transaction is during night hours (00:00 - 06:00) and above threshold
        LocalTime time = transactionDate.toLocalTime();
        boolean isNightTime = time.isBefore(LocalTime.of(6, 0));
        return isNightTime && amount.compareTo(rule.getThresholdAmount()) > 0;
    }

    private boolean checkStructuringRule(MonitoringRule rule, BigDecimal amount) {
        // Check if amount is just below a threshold (e.g., 9,500 when threshold is 10,000)
        BigDecimal threshold = rule.getThresholdAmount();
        BigDecimal lowerBound = threshold.multiply(new BigDecimal("0.90"));
        return amount.compareTo(lowerBound) >= 0 && amount.compareTo(threshold) < 0;
    }

    private boolean checkRoundAmountRule(BigDecimal amount) {
        // Check if amount is a round number (e.g., 10000.00, 5000.00)
        BigDecimal remainder = amount.remainder(new BigDecimal("1000"));
        return remainder.compareTo(BigDecimal.ZERO) == 0 && amount.compareTo(new BigDecimal("1000")) >= 0;
    }

    private AlertType determineAlertType(List<String> triggeredRules, List<MonitoringRule> allRules) {
        for (String ruleId : triggeredRules) {
            MonitoringRule rule = allRules.stream()
                    .filter(r -> r.getRuleId().equals(ruleId))
                    .findFirst()
                    .orElse(null);

            if (rule != null) {
                switch (rule.getRuleType()) {
                    case "VELOCITY": return AlertType.VELOCITY_ALERT;
                    case "STRUCTURING": return AlertType.STRUCTURING;
                    case "DAILY_LIMIT": return AlertType.DAILY_LIMIT_EXCEEDED;
                    case "ROUND_AMOUNT": return AlertType.ROUND_AMOUNT;
                }
            }
        }
        return AlertType.PATTERN_ANOMALY;
    }

    public List<TransactionMonitoring> getTransactionsByAccount(String accountNumber) {
        return transactionMonitoringRepository.findByAccountNumberOrderByTransactionDateDesc(accountNumber);
    }

    public List<TransactionMonitoring> getFlaggedTransactions() {
        return transactionMonitoringRepository.findByFlaggedTrueOrderByRiskScoreDesc();
    }

    public List<TransactionMonitoring> getHighRiskUnalerted(int minScore) {
        return transactionMonitoringRepository.findHighRiskUnalerted(minScore);
    }
}
