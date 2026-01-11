package com.banking.aml.repository;

import com.banking.aml.model.SanctionList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SanctionListRepository extends JpaRepository<SanctionList, String> {

    // Find active sanctions
    List<SanctionList> findByIsActiveTrue();

    // Find by list name
    List<SanctionList> findByListNameAndIsActiveTrue(SanctionList.SanctionListName listName);

    // Find by entity type
    List<SanctionList> findByEntityTypeAndIsActiveTrue(SanctionList.SanctionEntityType entityType);

    // Find by country
    List<SanctionList> findByCountryAndIsActiveTrue(String country);

    // Find by nationality
    List<SanctionList> findByNationalityAndIsActiveTrue(String nationality);

    // Find by program (e.g., IRAN, SYRIA)
    List<SanctionList> findByProgramAndIsActiveTrue(String program);

    // Fuzzy name search
    @Query("SELECT s FROM SanctionList s WHERE s.isActive = true AND " +
           "(LOWER(s.sanctionedName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(s.aliasNames) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<SanctionList> findByNameFuzzyMatch(@Param("name") String name);

    // Find by national ID
    Optional<SanctionList> findByNationalIdAndIsActiveTrue(String nationalId);

    // Find by passport
    Optional<SanctionList> findByPassportNumberAndIsActiveTrue(String passportNumber);

    // Find by date of birth
    List<SanctionList> findByDateOfBirthAndIsActiveTrue(LocalDate dateOfBirth);

    // Find by entity ID
    Optional<SanctionList> findByEntityIdAndIsActiveTrue(String entityId);

    // Find recently added (last N days)
    @Query("SELECT s FROM SanctionList s WHERE s.isActive = true AND " +
           "s.listedDate >= :sinceDate ORDER BY s.listedDate DESC")
    List<SanctionList> findRecentlyListed(@Param("sinceDate") LocalDate sinceDate);

    // Find recently delisted
    @Query("SELECT s FROM SanctionList s WHERE s.isActive = false AND " +
           "s.delistedDate >= :sinceDate ORDER BY s.delistedDate DESC")
    List<SanctionList> findRecentlyDelisted(@Param("sinceDate") LocalDate sinceDate);

    // Count by list name
    Long countByListNameAndIsActiveTrue(SanctionList.SanctionListName listName);

    // Count total active sanctions
    Long countByIsActiveTrue();

    // Find stale entries (not refreshed in X days)
    @Query("SELECT s FROM SanctionList s WHERE s.isActive = true AND " +
           "(s.lastRefreshedAt IS NULL OR s.lastRefreshedAt < :cutoffDate)")
    List<SanctionList> findStaleEntries(@Param("cutoffDate") LocalDate cutoffDate);

    // Advanced search: name + DOB + nationality
    @Query("SELECT s FROM SanctionList s WHERE s.isActive = true AND " +
           "LOWER(s.sanctionedName) LIKE LOWER(CONCAT('%', :name, '%')) AND " +
           "(:dob IS NULL OR s.dateOfBirth = :dob) AND " +
           "(:nationality IS NULL OR LOWER(s.nationality) = LOWER(:nationality))")
    List<SanctionList> advancedSearch(@Param("name") String name,
                                      @Param("dob") LocalDate dob,
                                      @Param("nationality") String nationality);
}
