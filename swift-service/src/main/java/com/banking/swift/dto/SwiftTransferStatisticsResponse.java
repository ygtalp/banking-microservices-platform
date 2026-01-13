package com.banking.swift.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for SWIFT transfer statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwiftTransferStatisticsResponse {

    // Count statistics
    private Long totalTransfers;
    private Long pendingCount;
    private Long processingCount;
    private Long completedCount;
    private Long failedCount;
    private Long rejectedCount;
    
    // Amount statistics (30-day rolling window)
    private BigDecimal totalVolume30Days;
    private BigDecimal averageAmount;
    private BigDecimal largestTransfer;
    private BigDecimal totalFees;
    
    // Compliance statistics
    private Long ofacFlagged;
    private Long sanctionsFlagged;
    private Long complianceCleared;
    
    // Today's statistics
    private Long todayCount;
    private BigDecimal todayVolume;
}
