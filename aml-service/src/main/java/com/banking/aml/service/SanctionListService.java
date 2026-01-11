package com.banking.aml.service;

import com.banking.aml.model.SanctionList;
import com.banking.aml.repository.SanctionListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SanctionListService {

    private final SanctionListRepository sanctionListRepository;

    /**
     * Get all active sanctions (cached)
     */
    @Cacheable(value = "sanctions", key = "'all-active'")
    public List<SanctionList> getAllActiveSanctions() {
        log.info("Fetching all active sanctions from database");
        return sanctionListRepository.findByIsActiveTrue();
    }

    /**
     * Get sanctions by list name
     */
    @Cacheable(value = "sanctions", key = "#listName")
    public List<SanctionList> getSanctionsByListName(SanctionList.SanctionListName listName) {
        return sanctionListRepository.findByListNameAndIsActiveTrue(listName);
    }

    /**
     * Fuzzy name search for sanctions screening
     */
    @Cacheable(value = "sanctions", key = "'name:' + #name")
    public List<SanctionList> searchByName(String name) {
        return sanctionListRepository.findByNameFuzzyMatch(name);
    }

    /**
     * Screen by national ID
     */
    public Optional<SanctionList> screenByNationalId(String nationalId) {
        return sanctionListRepository.findByNationalIdAndIsActiveTrue(nationalId);
    }

    /**
     * Screen by passport number
     */
    public Optional<SanctionList> screenByPassportNumber(String passportNumber) {
        return sanctionListRepository.findByPassportNumberAndIsActiveTrue(passportNumber);
    }

    /**
     * Advanced search: name + DOB + nationality
     */
    public List<SanctionList> advancedSearch(String name, LocalDate dob, String nationality) {
        return sanctionListRepository.advancedSearch(name, dob, nationality);
    }

    /**
     * Import sanctions from CSV file
     * CSV Format: list_name, entity_type, name, alias_names, national_id, passport, dob, country, nationality, program
     */
    @Transactional
    @CacheEvict(value = "sanctions", allEntries = true)
    public ImportResult importFromCsv(MultipartFile file, SanctionList.SanctionListName listName) {
        log.info("Starting CSV import for sanction list: {}", listName);

        ImportResult result = new ImportResult();
        List<SanctionList> sanctionsToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip header
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                try {
                    SanctionList sanction = parseCsvLine(line, listName);
                    sanctionsToSave.add(sanction);
                    result.incrementSuccess();
                } catch (Exception e) {
                    log.warn("Failed to parse CSV line: {}, error: {}", line, e.getMessage());
                    result.incrementFailure();
                }
            }

            // Batch save
            if (!sanctionsToSave.isEmpty()) {
                sanctionListRepository.saveAll(sanctionsToSave);
                log.info("Imported {} sanctions from CSV for list: {}", sanctionsToSave.size(), listName);
            }

        } catch (Exception e) {
            log.error("Error importing CSV file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to import sanctions from CSV", e);
        }

        return result;
    }

    /**
     * Parse a single CSV line into SanctionList entity
     * Format: entity_type, name, alias_names, national_id, passport, dob(YYYY-MM-DD), country, nationality, program
     */
    private SanctionList parseCsvLine(String line, SanctionList.SanctionListName listName) {
        String[] fields = line.split(",", -1); // -1 to keep empty trailing fields

        SanctionList sanction = new SanctionList();
        sanction.setListName(listName);

        // Entity type (0)
        if (fields.length > 0 && !fields[0].trim().isEmpty()) {
            sanction.setEntityType(SanctionList.SanctionEntityType.valueOf(fields[0].trim().toUpperCase()));
        } else {
            sanction.setEntityType(SanctionList.SanctionEntityType.INDIVIDUAL);
        }

        // Name (1) - Required
        if (fields.length > 1 && !fields[1].trim().isEmpty()) {
            sanction.setSanctionedName(fields[1].trim());
        } else {
            throw new IllegalArgumentException("Sanctioned name is required");
        }

        // Alias names (2)
        if (fields.length > 2 && !fields[2].trim().isEmpty()) {
            sanction.setAliasNames(fields[2].trim());
        }

        // National ID (3)
        if (fields.length > 3 && !fields[3].trim().isEmpty()) {
            sanction.setNationalId(fields[3].trim());
        }

        // Passport (4)
        if (fields.length > 4 && !fields[4].trim().isEmpty()) {
            sanction.setPassportNumber(fields[4].trim());
        }

        // Date of birth (5) - Format: YYYY-MM-DD
        if (fields.length > 5 && !fields[5].trim().isEmpty()) {
            try {
                sanction.setDateOfBirth(LocalDate.parse(fields[5].trim()));
            } catch (Exception e) {
                log.warn("Invalid date format: {}", fields[5]);
            }
        }

        // Country (6)
        if (fields.length > 6 && !fields[6].trim().isEmpty()) {
            sanction.setCountry(fields[6].trim());
        }

        // Nationality (7)
        if (fields.length > 7 && !fields[7].trim().isEmpty()) {
            sanction.setNationality(fields[7].trim());
        }

        // Program (8)
        if (fields.length > 8 && !fields[8].trim().isEmpty()) {
            sanction.setProgram(fields[8].trim());
        }

        sanction.setListedDate(LocalDate.now());
        sanction.setLastRefreshedAt(LocalDateTime.now());
        sanction.setIsActive(true);

        return sanction;
    }

    /**
     * Refresh all sanction lists (mark as updated)
     * Called by scheduled job
     */
    @Transactional
    @CacheEvict(value = "sanctions", allEntries = true)
    public int refreshAllSanctions() {
        log.info("Refreshing all sanction lists");

        List<SanctionList> activeSanctions = sanctionListRepository.findByIsActiveTrue();
        LocalDateTime now = LocalDateTime.now();

        activeSanctions.forEach(sanction -> sanction.setLastRefreshedAt(now));
        sanctionListRepository.saveAll(activeSanctions);

        log.info("Refreshed {} sanction entries", activeSanctions.size());
        return activeSanctions.size();
    }

    /**
     * Delist (deactivate) a sanction
     */
    @Transactional
    @CacheEvict(value = "sanctions", allEntries = true)
    public void delistSanction(String sanctionId, String reason) {
        SanctionList sanction = sanctionListRepository.findById(sanctionId)
                .orElseThrow(() -> new RuntimeException("Sanction not found: " + sanctionId));

        sanction.setIsActive(false);
        sanction.setDelistedDate(LocalDate.now());
        sanction.setRemarks(reason);

        sanctionListRepository.save(sanction);
        log.info("Delisted sanction: {}", sanctionId);
    }

    /**
     * Get statistics
     */
    public SanctionStatistics getStatistics() {
        SanctionStatistics stats = new SanctionStatistics();
        stats.setTotalActive(sanctionListRepository.countByIsActiveTrue());
        stats.setOfacCount(sanctionListRepository.countByListNameAndIsActiveTrue(SanctionList.SanctionListName.OFAC));
        stats.setEuCount(sanctionListRepository.countByListNameAndIsActiveTrue(SanctionList.SanctionListName.EU));
        stats.setUnCount(sanctionListRepository.countByListNameAndIsActiveTrue(SanctionList.SanctionListName.UN));
        return stats;
    }

    /**
     * Import result DTO
     */
    @lombok.Data
    public static class ImportResult {
        private int successCount = 0;
        private int failureCount = 0;

        public void incrementSuccess() {
            successCount++;
        }

        public void incrementFailure() {
            failureCount++;
        }
    }

    /**
     * Statistics DTO
     */
    @lombok.Data
    public static class SanctionStatistics {
        private Long totalActive;
        private Long ofacCount;
        private Long euCount;
        private Long unCount;
    }
}
