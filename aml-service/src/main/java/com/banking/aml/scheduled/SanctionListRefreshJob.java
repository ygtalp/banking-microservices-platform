package com.banking.aml.scheduled;

import com.banking.aml.service.SanctionListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SanctionListRefreshJob {

    private final SanctionListService sanctionListService;

    /**
     * Refresh sanction lists daily at 2:00 AM
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void refreshSanctionListsDaily() {
        log.info("Starting daily sanction list refresh job");

        try {
            int refreshedCount = sanctionListService.refreshAllSanctions();
            log.info("Daily sanction list refresh completed. Refreshed {} entries", refreshedCount);
        } catch (Exception e) {
            log.error("Error during daily sanction list refresh: {}", e.getMessage(), e);
        }
    }

    /**
     * Optional: Refresh every 6 hours for high-frequency updates
     * Uncomment if needed for more frequent updates
     */
    // @Scheduled(cron = "0 0 */6 * * *")
    public void refreshSanctionListsHourly() {
        log.info("Starting 6-hourly sanction list refresh job");

        try {
            int refreshedCount = sanctionListService.refreshAllSanctions();
            log.info("6-hourly sanction list refresh completed. Refreshed {} entries", refreshedCount);
        } catch (Exception e) {
            log.error("Error during 6-hourly sanction list refresh: {}", e.getMessage(), e);
        }
    }
}
