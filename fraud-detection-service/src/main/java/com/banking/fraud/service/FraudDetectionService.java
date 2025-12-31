package com.banking.fraud.service;

import com.banking.fraud.dto.*;
import com.banking.fraud.model.FraudCheckStatus;

import java.util.List;

public interface FraudDetectionService {

    FraudCheckResponse performFraudCheck(FraudCheckRequest request);

    FraudCheckResponse getFraudCheckById(String checkId);

    List<FraudCheckResponse> getFraudChecksByTransfer(String transferReference);

    List<FraudCheckResponse> getFraudChecksByAccount(String accountNumber);

    List<FraudCheckResponse> getPendingReviews();

    FraudCheckResponse reviewFraudCheck(String checkId, ReviewRequest request);

    RiskScoreResponse getRiskScore(String accountNumber);

    List<RiskScoreResponse> getHighRiskAccounts();

    List<FraudRuleResponse> getAllRules();

    FraudRuleResponse getRuleById(String ruleId);

    FraudRuleResponse updateRule(String ruleId, UpdateRuleRequest request);

    FraudRuleResponse toggleRule(String ruleId);
}
