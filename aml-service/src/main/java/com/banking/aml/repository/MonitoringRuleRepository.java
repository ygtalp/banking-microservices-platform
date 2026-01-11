package com.banking.aml.repository;

import com.banking.aml.model.MonitoringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoringRuleRepository extends JpaRepository<MonitoringRule, String> {

    // Find enabled rules
    List<MonitoringRule> findByEnabledTrue();

    // Find by rule type
    List<MonitoringRule> findByRuleType(String ruleType);

    // Find enabled rules by type
    List<MonitoringRule> findByRuleTypeAndEnabledTrue(String ruleType);

    // Find by rule name
    Optional<MonitoringRule> findByRuleName(String ruleName);
}
