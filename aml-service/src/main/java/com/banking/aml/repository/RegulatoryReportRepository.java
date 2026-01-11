package com.banking.aml.repository;

import com.banking.aml.model.RegulatoryReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegulatoryReportRepository extends JpaRepository<RegulatoryReport, String> {

    // Find by report number
    Optional<RegulatoryReport> findByReportNumber(String reportNumber);

    // Find by type and status
    List<RegulatoryReport> findByReportTypeAndStatus(RegulatoryReport.ReportType reportType,
                                                      RegulatoryReport.ReportStatus status);

    // Find by customer ID
    List<RegulatoryReport> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    // Find by account number
    List<RegulatoryReport> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    // Find by alert ID
    Optional<RegulatoryReport> findByAlertId(String alertId);

    // Find by case ID
    List<RegulatoryReport> findByCaseId(String caseId);

    // Find pending review
    @Query("SELECT r FROM RegulatoryReport r WHERE r.status IN ('DRAFT', 'PENDING_REVIEW') " +
           "ORDER BY r.createdAt ASC")
    List<RegulatoryReport> findPendingReview();

    // Find pending approval
    List<RegulatoryReport> findByStatusOrderByCreatedAtAsc(RegulatoryReport.ReportStatus status);

    // Find filed reports
    @Query("SELECT r FROM RegulatoryReport r WHERE r.status IN ('FILED', 'ACKNOWLEDGED') " +
           "ORDER BY r.filedAt DESC")
    List<RegulatoryReport> findFiledReports();

    // Find by suspicion category
    List<RegulatoryReport> findBySuspicionCategoryOrderByCreatedAtDesc(
            RegulatoryReport.SuspicionCategory category);

    // Find reports filed to specific authority
    List<RegulatoryReport> findByFiledToAuthorityOrderByFiledAtDesc(String authority);

    // Find by date range
    @Query("SELECT r FROM RegulatoryReport r WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY r.createdAt DESC")
    List<RegulatoryReport> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    // Find reports needing acknowledgment
    @Query("SELECT r FROM RegulatoryReport r WHERE r.status = 'FILED' AND " +
           "r.acknowledgmentReceivedAt IS NULL AND " +
           "r.filedAt < :cutoffDate")
    List<RegulatoryReport> findAwaitingAcknowledgment(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find by prepared by
    List<RegulatoryReport> findByPreparedByOrderByCreatedAtDesc(String preparedBy);

    // Find by reviewed by
    List<RegulatoryReport> findByReviewedByOrderByReviewedAtDesc(String reviewedBy);

    // Find by approved by
    List<RegulatoryReport> findByApprovedByOrderByApprovedAtDesc(String approvedBy);

    // Count by report type
    Long countByReportType(RegulatoryReport.ReportType reportType);

    // Count by status
    Long countByStatus(RegulatoryReport.ReportStatus status);

    // Count filed reports by date range
    @Query("SELECT COUNT(r) FROM RegulatoryReport r WHERE r.status IN ('FILED', 'ACKNOWLEDGED') " +
           "AND r.filedAt BETWEEN :startDate AND :endDate")
    Long countFiledReportsByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // Find overdue drafts (created more than X days ago)
    @Query("SELECT r FROM RegulatoryReport r WHERE r.status = 'DRAFT' AND " +
           "r.createdAt < :cutoffDate")
    List<RegulatoryReport> findOverdueDrafts(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Statistics query
    @Query("SELECT r.reportType as reportType, r.status as status, COUNT(r) as count " +
           "FROM RegulatoryReport r GROUP BY r.reportType, r.status")
    List<Object[]> getReportStatistics();

    // Advanced search
    @Query("SELECT r FROM RegulatoryReport r WHERE " +
           "(:reportType IS NULL OR r.reportType = :reportType) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:customerId IS NULL OR r.customerId = :customerId) AND " +
           "(:suspicionCategory IS NULL OR r.suspicionCategory = :suspicionCategory) AND " +
           "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR r.createdAt <= :endDate) " +
           "ORDER BY r.createdAt DESC")
    List<RegulatoryReport> advancedSearch(@Param("reportType") RegulatoryReport.ReportType reportType,
                                          @Param("status") RegulatoryReport.ReportStatus status,
                                          @Param("customerId") String customerId,
                                          @Param("suspicionCategory") RegulatoryReport.SuspicionCategory suspicionCategory,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
}
