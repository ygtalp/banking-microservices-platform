package com.banking.aml.repository;

import com.banking.aml.model.AmlCase;
import com.banking.aml.model.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmlCaseRepository extends JpaRepository<AmlCase, String> {

    // Find by case number
    Optional<AmlCase> findByCaseNumber(String caseNumber);

    // Find by customer ID
    List<AmlCase> findByCustomerIdOrderByOpenedAtDesc(String customerId);

    // Find by account number
    List<AmlCase> findByAccountNumberOrderByOpenedAtDesc(String accountNumber);

    // Find by status
    List<AmlCase> findByStatusOrderByOpenedAtDesc(AmlCase.CaseStatus status);

    // Find by priority
    List<AmlCase> findByPriorityOrderByOpenedAtDesc(AmlCase.CasePriority priority);

    // Find by risk level
    List<AmlCase> findByRiskLevelOrderByOpenedAtDesc(RiskLevel riskLevel);

    // Find by case type
    List<AmlCase> findByCaseTypeOrderByOpenedAtDesc(AmlCase.CaseType caseType);

    // Find open cases
    @Query("SELECT c FROM AmlCase c WHERE c.status IN ('OPEN', 'INVESTIGATING', 'PENDING_REVIEW', 'ESCALATED') " +
           "ORDER BY c.priority DESC, c.openedAt ASC")
    List<AmlCase> findOpenCases();

    // Find assigned cases
    List<AmlCase> findByAssignedToAndStatusInOrderByPriorityDescOpenedAtAsc(
            String assignedTo, List<AmlCase.CaseStatus> statuses);

    // Find unassigned cases
    @Query("SELECT c FROM AmlCase c WHERE c.assignedTo IS NULL AND " +
           "c.status IN ('OPEN', 'INVESTIGATING') ORDER BY c.priority DESC, c.openedAt ASC")
    List<AmlCase> findUnassignedCases();

    // Find escalated cases
    List<AmlCase> findByEscalatedTrueAndStatusNotOrderByEscalatedAtDesc(AmlCase.CaseStatus excludeStatus);

    // Find overdue cases
    @Query("SELECT c FROM AmlCase c WHERE c.isOverdue = true AND " +
           "c.status NOT IN ('CLOSED', 'PENDING_CLOSURE') ORDER BY c.dueDate ASC")
    List<AmlCase> findOverdueCases();

    // Find cases requiring SAR filing
    @Query("SELECT c FROM AmlCase c WHERE c.requiresSarFiling = true AND " +
           "c.sarFiled = false AND c.status NOT IN ('CLOSED')")
    List<AmlCase> findCasesRequiringSarFiling();

    // Find cases with blocked customers
    List<AmlCase> findByCustomerBlockedTrueOrderByCustomerBlockedAtDesc();

    // Find cases by date range
    @Query("SELECT c FROM AmlCase c WHERE c.openedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.openedAt DESC")
    List<AmlCase> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // Find cases by opened by
    List<AmlCase> findByOpenedByOrderByOpenedAtDesc(String openedBy);

    // Find cases by closed by
    List<AmlCase> findByClosedByOrderByClosedAtDesc(String closedBy);

    // Find cases by resolution
    List<AmlCase> findByResolutionOrderByClosedAtDesc(AmlCase.CaseResolution resolution);

    // Find high-priority open cases
    @Query("SELECT c FROM AmlCase c WHERE c.priority IN ('HIGH', 'CRITICAL') AND " +
           "c.status IN ('OPEN', 'INVESTIGATING', 'ESCALATED') " +
           "ORDER BY c.priority DESC, c.openedAt ASC")
    List<AmlCase> findHighPriorityOpenCases();

    // Find stale cases (open for X days)
    @Query("SELECT c FROM AmlCase c WHERE c.status IN ('OPEN', 'INVESTIGATING') AND " +
           "c.daysOpen > :days ORDER BY c.daysOpen DESC")
    List<AmlCase> findStaleCases(@Param("days") int days);

    // Count by status
    Long countByStatus(AmlCase.CaseStatus status);

    // Count by priority
    Long countByPriority(AmlCase.CasePriority priority);

    // Count by assigned to
    Long countByAssignedToAndStatusIn(String assignedTo, List<AmlCase.CaseStatus> statuses);

    // Count overdue cases
    Long countByIsOverdueTrueAndStatusNot(AmlCase.CaseStatus excludeStatus);

    // Count cases requiring SAR
    @Query("SELECT COUNT(c) FROM AmlCase c WHERE c.requiresSarFiling = true AND c.sarFiled = false")
    Long countCasesRequiringSar();

    // Statistics: Cases by status
    @Query("SELECT c.status as status, COUNT(c) as count FROM AmlCase c GROUP BY c.status")
    List<Object[]> getCaseStatisticsByStatus();

    // Statistics: Cases by priority
    @Query("SELECT c.priority as priority, COUNT(c) as count FROM AmlCase c " +
           "WHERE c.status NOT IN ('CLOSED') GROUP BY c.priority")
    List<Object[]> getOpenCaseStatisticsByPriority();

    // Statistics: Average days to close
    @Query("SELECT AVG(c.daysOpen) FROM AmlCase c WHERE c.status = 'CLOSED' AND " +
           "c.closedAt BETWEEN :startDate AND :endDate")
    Double getAverageDaysToClose(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    // Advanced search
    @Query("SELECT c FROM AmlCase c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:priority IS NULL OR c.priority = :priority) AND " +
           "(:caseType IS NULL OR c.caseType = :caseType) AND " +
           "(:riskLevel IS NULL OR c.riskLevel = :riskLevel) AND " +
           "(:assignedTo IS NULL OR c.assignedTo = :assignedTo) AND " +
           "(:customerId IS NULL OR c.customerId = :customerId) AND " +
           "(:startDate IS NULL OR c.openedAt >= :startDate) AND " +
           "(:endDate IS NULL OR c.openedAt <= :endDate) " +
           "ORDER BY c.priority DESC, c.openedAt DESC")
    List<AmlCase> advancedSearch(@Param("status") AmlCase.CaseStatus status,
                                 @Param("priority") AmlCase.CasePriority priority,
                                 @Param("caseType") AmlCase.CaseType caseType,
                                 @Param("riskLevel") RiskLevel riskLevel,
                                 @Param("assignedTo") String assignedTo,
                                 @Param("customerId") String customerId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
}
