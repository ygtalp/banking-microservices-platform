package com.banking.aml.repository;

import com.banking.aml.model.SanctionMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanctionMatchRepository extends JpaRepository<SanctionMatch, String> {

    // Find by account number
    List<SanctionMatch> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    // Find by match status
    List<SanctionMatch> findByMatchStatusOrderByCreatedAtDesc(String matchStatus);

    // Find by sanction list
    List<SanctionMatch> findBySanctionListOrderByCreatedAtDesc(String sanctionList);

    // Find potential matches (high score, not yet reviewed)
    @Query("SELECT s FROM SanctionMatch s WHERE s.matchScore >= :minScore AND s.reviewedAt IS NULL ORDER BY s.matchScore DESC")
    List<SanctionMatch> findPotentialMatches(@Param("minScore") Integer minScore);

    // Find matches for customer
    List<SanctionMatch> findByNationalIdOrderByCreatedAtDesc(String nationalId);

    // Count matches by status
    Long countByMatchStatus(String matchStatus);

    // Find unreviewed matches
    @Query("SELECT s FROM SanctionMatch s WHERE s.reviewedAt IS NULL ORDER BY s.matchScore DESC, s.createdAt ASC")
    List<SanctionMatch> findUnreviewedMatches();
}
