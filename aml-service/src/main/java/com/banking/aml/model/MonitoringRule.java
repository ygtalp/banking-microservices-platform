package com.banking.aml.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "monitoring_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoringRule implements Serializable {

    @Id
    @Column(name = "rule_id", nullable = false, length = 50)
    private String ruleId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType; // VELOCITY, AMOUNT, PATTERN, DAILY_LIMIT, STRUCTURING

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "threshold_amount", precision = 19, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "threshold_count")
    private Integer thresholdCount;

    @Column(name = "time_window_minutes")
    private Integer timeWindowMinutes;

    @Column(name = "risk_points", nullable = false)
    private Integer riskPoints;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @PrePersist
    public void prePersist() {
        if (this.enabled == null) {
            this.enabled = true;
        }
    }
}
