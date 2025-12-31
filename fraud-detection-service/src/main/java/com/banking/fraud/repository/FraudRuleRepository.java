package com.banking.fraud.repository;

import com.banking.fraud.model.FraudRule;
import com.banking.fraud.model.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {

    Optional<FraudRule> findByRuleId(String ruleId);

    Optional<FraudRule> findByRuleName(String ruleName);

    List<FraudRule> findByEnabled(Boolean enabled);

    List<FraudRule> findByRuleType(RuleType ruleType);

    List<FraudRule> findByEnabledAndRuleType(Boolean enabled, RuleType ruleType);

    boolean existsByRuleId(String ruleId);
}
