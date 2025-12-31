package com.banking.fraud.service;

import com.banking.fraud.dto.*;
import com.banking.fraud.exception.FraudCheckNotFoundException;
import com.banking.fraud.exception.FraudRuleNotFoundException;
import com.banking.fraud.model.*;
import com.banking.fraud.repository.FraudCheckRepository;
import com.banking.fraud.repository.FraudRuleRepository;
import com.banking.fraud.repository.RiskScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final FraudCheckRepository fraudCheckRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final RiskScoreRepository riskScoreRepository;

    @Override
    @Transactional
    public FraudCheckResponse performFraudCheck(FraudCheckRequest request) {
        log.info("Performing fraud check for transfer: {}, account: {}",
                request.getTransferReference(), request.getAccountNumber());

        // Initialize fraud check result
        int totalRiskScore = 0;
        List<String> triggeredReasons = new ArrayList<>();

        // Get all enabled fraud rules
        List<FraudRule> enabledRules = fraudRuleRepository.findByEnabled(true);

        // Execute each rule
        for (FraudRule rule : enabledRules) {
            RuleResult result = executeRule(rule, request);
            if (result.isTriggered()) {
                totalRiskScore += rule.getRiskPoints();
                triggeredReasons.add(result.getReason());
                log.warn("Rule triggered: {} for account: {}",
                        rule.getRuleName(), request.getAccountNumber());
            }
        }

        // Determine risk level and status
        RiskLevel riskLevel = determineRiskLevel(totalRiskScore);
        FraudCheckStatus status = determineStatus(riskLevel);

        // Create fraud check record
        FraudCheck fraudCheck = FraudCheck.builder()
                .checkId(generateCheckId())
                .transferReference(request.getTransferReference())
                .accountNumber(request.getAccountNumber())
                .amount(request.getAmount())
                .riskScore(totalRiskScore)
                .riskLevel(riskLevel)
                .status(status)
                .reasons(triggeredReasons)
                .metadata(request.getMetadata())
                .checkedAt(LocalDateTime.now())
                .build();

        fraudCheck = fraudCheckRepository.save(fraudCheck);

        // Update risk score for account
        updateRiskScore(request.getAccountNumber(), status, totalRiskScore);

        log.info("Fraud check completed: checkId={}, riskScore={}, status={}",
                fraudCheck.getCheckId(), totalRiskScore, status);

        return mapToResponse(fraudCheck);
    }

    private RuleResult executeRule(FraudRule rule, FraudCheckRequest request) {
        switch (rule.getRuleType()) {
            case VELOCITY:
                return checkVelocityRule(rule, request);
            case AMOUNT:
                return checkAmountRule(rule, request);
            case DAILY_LIMIT:
                return checkDailyLimitRule(rule, request);
            case TIME:
                return checkTimeBasedRule(rule, request);
            case PATTERN:
                return checkPatternRule(rule, request);
            default:
                return RuleResult.notTriggered();
        }
    }

    private RuleResult checkVelocityRule(FraudRule rule, FraudCheckRequest request) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(rule.getTimeWindowMinutes());
        Long recentChecks = fraudCheckRepository.countRecentChecksByAccount(
                request.getAccountNumber(), since);

        if (recentChecks >= rule.getMaxCount()) {
            return RuleResult.triggered(String.format(
                    "Velocity check: %d transfers in %d minutes (max: %d)",
                    recentChecks, rule.getTimeWindowMinutes(), rule.getMaxCount()));
        }
        return RuleResult.notTriggered();
    }

    private RuleResult checkAmountRule(FraudRule rule, FraudCheckRequest request) {
        if (request.getAmount().compareTo(rule.getThreshold()) > 0) {
            return RuleResult.triggered(String.format(
                    "High amount: %s exceeds threshold %s",
                    request.getAmount(), rule.getThreshold()));
        }
        return RuleResult.notTriggered();
    }

    private RuleResult checkDailyLimitRule(FraudRule rule, FraudCheckRequest request) {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        List<FraudCheck> todayChecks = fraudCheckRepository
                .findRecentChecksByAccount(request.getAccountNumber(), startOfDay);

        BigDecimal totalToday = todayChecks.stream()
                .map(FraudCheck::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithCurrent = totalToday.add(request.getAmount());

        if (totalWithCurrent.compareTo(rule.getThreshold()) > 0) {
            return RuleResult.triggered(String.format(
                    "Daily limit exceeded: %s (limit: %s)",
                    totalWithCurrent, rule.getThreshold()));
        }
        return RuleResult.notTriggered();
    }

    private RuleResult checkTimeBasedRule(FraudRule rule, FraudCheckRequest request) {
        int currentHour = LocalDateTime.now().getHour();

        if (currentHour >= rule.getStartHour() && currentHour < rule.getEndHour()) {
            // Check if amount is significant during restricted hours
            BigDecimal threshold = rule.getThreshold() != null ?
                    rule.getThreshold() : new BigDecimal("10000");

            if (request.getAmount().compareTo(threshold) > 0) {
                return RuleResult.triggered(String.format(
                        "High amount transfer during restricted hours (%02d:00-%02d:00): %s",
                        rule.getStartHour(), rule.getEndHour(), request.getAmount()));
            }
        }
        return RuleResult.notTriggered();
    }

    private RuleResult checkPatternRule(FraudRule rule, FraudCheckRequest request) {
        // Check for rapid succession transfers
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        Long veryRecentChecks = fraudCheckRepository.countRecentChecksByAccount(
                request.getAccountNumber(), twoMinutesAgo);

        if (veryRecentChecks > 0) {
            return RuleResult.triggered(String.format(
                    "Rapid succession: %d transfers within 2 minutes",
                    veryRecentChecks));
        }

        // Check for unusual amount (3x average)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<FraudCheck> recentHistory = fraudCheckRepository
                .findRecentChecksByAccount(request.getAccountNumber(), thirtyDaysAgo);

        if (!recentHistory.isEmpty()) {
            BigDecimal averageAmount = recentHistory.stream()
                    .map(FraudCheck::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(recentHistory.size()), BigDecimal.ROUND_HALF_UP);

            BigDecimal threshold = averageAmount.multiply(BigDecimal.valueOf(3));

            if (request.getAmount().compareTo(threshold) > 0) {
                return RuleResult.triggered(String.format(
                        "Unusual pattern: Amount %s is 3x average %s",
                        request.getAmount(), averageAmount));
            }
        }

        return RuleResult.notTriggered();
    }

    private RiskLevel determineRiskLevel(int score) {
        if (score >= 76) return RiskLevel.CRITICAL;
        if (score >= 51) return RiskLevel.HIGH;
        if (score >= 26) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private FraudCheckStatus determineStatus(RiskLevel riskLevel) {
        switch (riskLevel) {
            case CRITICAL:
                return FraudCheckStatus.BLOCKED;
            case HIGH:
            case MEDIUM:
                return FraudCheckStatus.FLAGGED;
            default:
                return FraudCheckStatus.PASSED;
        }
    }

    private void updateRiskScore(String accountNumber, FraudCheckStatus status, int riskScore) {
        RiskScore score = riskScoreRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> RiskScore.builder()
                        .accountNumber(accountNumber)
                        .currentScore(0)
                        .riskLevel(RiskLevel.LOW)
                        .totalChecks(0L)
                        .flaggedCount(0L)
                        .blockedCount(0L)
                        .build());

        score.setTotalChecks(score.getTotalChecks() + 1);
        score.setLastCheckAt(LocalDateTime.now());

        // Update current score (decay over time, increase on incidents)
        if (status == FraudCheckStatus.BLOCKED) {
            score.setCurrentScore(Math.min(100, score.getCurrentScore() + riskScore));
            score.setBlockedCount(score.getBlockedCount() + 1);
            score.setLastIncidentAt(LocalDateTime.now());
        } else if (status == FraudCheckStatus.FLAGGED) {
            score.setCurrentScore(Math.min(100, score.getCurrentScore() + (riskScore / 2)));
            score.setFlaggedCount(score.getFlaggedCount() + 1);
            score.setLastIncidentAt(LocalDateTime.now());
        } else {
            // Decay score for passed checks (reduce by 5, minimum 0)
            score.setCurrentScore(Math.max(0, score.getCurrentScore() - 5));
        }

        riskScoreRepository.save(score);
    }

    @Override
    @Cacheable(value = "fraudChecks", key = "#checkId")
    public FraudCheckResponse getFraudCheckById(String checkId) {
        FraudCheck fraudCheck = fraudCheckRepository.findByCheckId(checkId)
                .orElseThrow(() -> new FraudCheckNotFoundException("Fraud check not found: " + checkId));
        return mapToResponse(fraudCheck);
    }

    @Override
    public List<FraudCheckResponse> getFraudChecksByTransfer(String transferReference) {
        return fraudCheckRepository.findByTransferReference(transferReference).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FraudCheckResponse> getFraudChecksByAccount(String accountNumber) {
        return fraudCheckRepository.findByAccountNumberOrderByCheckedAtDesc(accountNumber).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FraudCheckResponse> getPendingReviews() {
        List<FraudCheckStatus> reviewStatuses = Arrays.asList(
                FraudCheckStatus.FLAGGED, FraudCheckStatus.UNDER_REVIEW);
        return fraudCheckRepository.findByStatusIn(reviewStatuses).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FraudCheckResponse reviewFraudCheck(String checkId, ReviewRequest request) {
        FraudCheck fraudCheck = fraudCheckRepository.findByCheckId(checkId)
                .orElseThrow(() -> new FraudCheckNotFoundException("Fraud check not found: " + checkId));

        fraudCheck.setStatus(request.getStatus());
        fraudCheck.setReviewedBy(request.getReviewedBy());
        fraudCheck.setReviewedAt(LocalDateTime.now());
        fraudCheck.setReviewNotes(request.getReviewNotes());

        fraudCheck = fraudCheckRepository.save(fraudCheck);

        log.info("Fraud check reviewed: checkId={}, status={}, reviewedBy={}",
                checkId, request.getStatus(), request.getReviewedBy());

        return mapToResponse(fraudCheck);
    }

    @Override
    @Cacheable(value = "riskScores", key = "#accountNumber")
    public RiskScoreResponse getRiskScore(String accountNumber) {
        RiskScore score = riskScoreRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> RiskScore.builder()
                        .accountNumber(accountNumber)
                        .currentScore(0)
                        .riskLevel(RiskLevel.LOW)
                        .totalChecks(0L)
                        .flaggedCount(0L)
                        .blockedCount(0L)
                        .build());
        return mapToRiskScoreResponse(score);
    }

    @Override
    public List<RiskScoreResponse> getHighRiskAccounts() {
        return riskScoreRepository.findHighRiskAccounts().stream()
                .map(this::mapToRiskScoreResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "fraudRules")
    public List<FraudRuleResponse> getAllRules() {
        return fraudRuleRepository.findAll().stream()
                .map(this::mapToRuleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FraudRuleResponse getRuleById(String ruleId) {
        FraudRule rule = fraudRuleRepository.findByRuleId(ruleId)
                .orElseThrow(() -> new FraudRuleNotFoundException("Fraud rule not found: " + ruleId));
        return mapToRuleResponse(rule);
    }

    @Override
    @Transactional
    public FraudRuleResponse updateRule(String ruleId, UpdateRuleRequest request) {
        FraudRule rule = fraudRuleRepository.findByRuleId(ruleId)
                .orElseThrow(() -> new FraudRuleNotFoundException("Fraud rule not found: " + ruleId));

        if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());
        if (request.getThreshold() != null) rule.setThreshold(request.getThreshold());
        if (request.getTimeWindowMinutes() != null) rule.setTimeWindowMinutes(request.getTimeWindowMinutes());
        if (request.getMaxCount() != null) rule.setMaxCount(request.getMaxCount());
        if (request.getRiskPoints() != null) rule.setRiskPoints(request.getRiskPoints());
        if (request.getStartHour() != null) rule.setStartHour(request.getStartHour());
        if (request.getEndHour() != null) rule.setEndHour(request.getEndHour());

        rule = fraudRuleRepository.save(rule);

        log.info("Fraud rule updated: ruleId={}", ruleId);

        return mapToRuleResponse(rule);
    }

    @Override
    @Transactional
    public FraudRuleResponse toggleRule(String ruleId) {
        FraudRule rule = fraudRuleRepository.findByRuleId(ruleId)
                .orElseThrow(() -> new FraudRuleNotFoundException("Fraud rule not found: " + ruleId));

        rule.setEnabled(!rule.getEnabled());
        rule = fraudRuleRepository.save(rule);

        log.info("Fraud rule toggled: ruleId={}, enabled={}", ruleId, rule.getEnabled());

        return mapToRuleResponse(rule);
    }

    private String generateCheckId() {
        return "FRD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private FraudCheckResponse mapToResponse(FraudCheck fraudCheck) {
        return FraudCheckResponse.builder()
                .checkId(fraudCheck.getCheckId())
                .transferReference(fraudCheck.getTransferReference())
                .accountNumber(fraudCheck.getAccountNumber())
                .amount(fraudCheck.getAmount())
                .riskScore(fraudCheck.getRiskScore())
                .riskLevel(fraudCheck.getRiskLevel())
                .status(fraudCheck.getStatus())
                .reasons(fraudCheck.getReasons())
                .checkedAt(fraudCheck.getCheckedAt())
                .reviewedBy(fraudCheck.getReviewedBy())
                .reviewedAt(fraudCheck.getReviewedAt())
                .reviewNotes(fraudCheck.getReviewNotes())
                .build();
    }

    private RiskScoreResponse mapToRiskScoreResponse(RiskScore score) {
        return RiskScoreResponse.builder()
                .accountNumber(score.getAccountNumber())
                .currentScore(score.getCurrentScore())
                .riskLevel(score.getRiskLevel())
                .totalChecks(score.getTotalChecks())
                .flaggedCount(score.getFlaggedCount())
                .blockedCount(score.getBlockedCount())
                .lastCheckAt(score.getLastCheckAt())
                .lastIncidentAt(score.getLastIncidentAt())
                .build();
    }

    private FraudRuleResponse mapToRuleResponse(FraudRule rule) {
        return FraudRuleResponse.builder()
                .ruleId(rule.getRuleId())
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .ruleType(rule.getRuleType())
                .enabled(rule.getEnabled())
                .threshold(rule.getThreshold())
                .timeWindowMinutes(rule.getTimeWindowMinutes())
                .maxCount(rule.getMaxCount())
                .riskPoints(rule.getRiskPoints())
                .startHour(rule.getStartHour())
                .endHour(rule.getEndHour())
                .build();
    }

    // Helper class for rule execution results
    private static class RuleResult {
        private final boolean triggered;
        private final String reason;

        private RuleResult(boolean triggered, String reason) {
            this.triggered = triggered;
            this.reason = reason;
        }

        static RuleResult triggered(String reason) {
            return new RuleResult(true, reason);
        }

        static RuleResult notTriggered() {
            return new RuleResult(false, null);
        }

        boolean isTriggered() {
            return triggered;
        }

        String getReason() {
            return reason;
        }
    }
}
