package com.banking.swift.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

/**
 * Feign client for Fraud Detection Service integration.
 * Enables SEPA service to perform fraud checks before processing transfers.
 */
@FeignClient(name = "fraud-detection-service", path = "/fraud-checks")
public interface FraudDetectionClient {

    /**
     * Performs a fraud check for a SEPA transfer.
     *
     * @param request The fraud check request
     * @return The fraud check response
     */
    @PostMapping
    FraudCheckResponse performFraudCheck(@RequestBody FraudCheckRequest request);

    /**
     * Fraud check request DTO.
     */
    class FraudCheckRequest {
        private String accountNumber;
        private String transferReference;
        private BigDecimal amount;
        private String currency;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;

        public FraudCheckRequest() {
        }

        public FraudCheckRequest(String accountNumber, String transferReference, BigDecimal amount,
                                String currency, BigDecimal balanceBefore, BigDecimal balanceAfter) {
            this.accountNumber = accountNumber;
            this.transferReference = transferReference;
            this.amount = amount;
            this.currency = currency;
            this.balanceBefore = balanceBefore;
            this.balanceAfter = balanceAfter;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getTransferReference() {
            return transferReference;
        }

        public void setTransferReference(String transferReference) {
            this.transferReference = transferReference;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public BigDecimal getBalanceBefore() {
            return balanceBefore;
        }

        public void setBalanceBefore(BigDecimal balanceBefore) {
            this.balanceBefore = balanceBefore;
        }

        public BigDecimal getBalanceAfter() {
            return balanceAfter;
        }

        public void setBalanceAfter(BigDecimal balanceAfter) {
            this.balanceAfter = balanceAfter;
        }
    }

    /**
     * Fraud check response DTO.
     */
    class FraudCheckResponse {
        private String checkId;
        private String status;
        private String riskLevel;
        private Integer riskScore;
        private String[] reasons;

        public FraudCheckResponse() {
        }

        public String getCheckId() {
            return checkId;
        }

        public void setCheckId(String checkId) {
            this.checkId = checkId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public Integer getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(Integer riskScore) {
            this.riskScore = riskScore;
        }

        public String[] getReasons() {
            return reasons;
        }

        public void setReasons(String[] reasons) {
            this.reasons = reasons;
        }

        public boolean isBlocked() {
            return "BLOCKED".equals(status);
        }

        public boolean isFlagged() {
            return "FLAGGED".equals(status);
        }

        public boolean isPassed() {
            return "PASSED".equals(status);
        }
    }
}
