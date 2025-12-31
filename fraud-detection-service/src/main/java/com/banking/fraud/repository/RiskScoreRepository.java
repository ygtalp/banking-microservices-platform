package com.banking.fraud.repository;

import com.banking.fraud.model.RiskLevel;
import com.banking.fraud.model.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {

    Optional<RiskScore> findByAccountNumber(String accountNumber);

    List<RiskScore> findByRiskLevel(RiskLevel riskLevel);

    @Query("SELECT rs FROM RiskScore rs WHERE rs.currentScore >= :minScore ORDER BY rs.currentScore DESC")
    List<RiskScore> findByMinScore(@Param("minScore") Integer minScore);

    @Query("SELECT rs FROM RiskScore rs WHERE rs.riskLevel IN ('HIGH', 'CRITICAL') ORDER BY rs.currentScore DESC")
    List<RiskScore> findHighRiskAccounts();

    boolean existsByAccountNumber(String accountNumber);
}
