package dk.unievent.app.application.scheduler;

import dk.unievent.app.application.service.EventService;
import dk.unievent.app.application.service.PageService;
import dk.unievent.app.application.service.VaultService;
import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.infrastructure.constants.SchedulingConstants;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

// scheduled job that pulls events from the Facebook Graph API for every active page.
// runs on a fixed delay (not a fixed rate) so each run waits for the previous one to finish before starting.
@Slf4j
@Component
public class FacebookIngestionScheduler {

    private final PageService pageService;
    private final EventService eventService;
    private final Optional<VaultService> vaultService;
    private final int pageSize;

    public FacebookIngestionScheduler(
        PageService pageService,
        EventService eventService,
        Optional<VaultService> vaultService,
        @Value("${unievent.facebook.ingestion.page-size:50}") int pageSize
    ) {
        this.pageService = pageService;
        this.eventService = eventService;
        this.vaultService = vaultService;
        this.pageSize = pageSize;
    }

    @Scheduled(fixedDelay = SchedulingConstants.INGESTION_INTERVAL_MS, initialDelay = SchedulingConstants.INGESTION_INITIAL_DELAY_MS)
    public void ingestFacebookEvents() {
        log.info("Starting scheduled Facebook event ingestion");

        try {
            long startTime = System.currentTimeMillis();
            int successCount = 0;
            int failureCount = 0;

            // 1. walk through all active pages in batches of pageSize so we don't load the whole table at once
            int pageNumber = 0;
            boolean hasMorePages = true;

            while (hasMorePages) {
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                Page<PageDTO> activePagesPage = pageService.getActivePages(pageable);

                for (PageDTO page : activePagesPage.getContent()) {
                    try {
                        log.debug("Ingesting events for page: {} ({})", page.getName(), page.getId());
                        eventService.ingestFacebookEvents(page.getId());
                        successCount++;
                        log.info("Successfully ingested events for page: {}", page.getId());
                    } catch (FacebookApiException e) {
                        failureCount++;
                        if ("TOKEN_NOT_FOUND".equals(e.getErrorType())) {
                            log.warn("Skipping Facebook ingestion for page {} because no token exists in Vault ({})",
                                page.getId(), e.getStatusCode()); // page just hasn't been connected yet
                        } else {
                            log.error("Facebook API error ingesting events for page: {} - {} ({})",
                                page.getId(), e.getErrorType(), e.getStatusCode());
                        }
                        // OAuthException means Facebook rejected the token entirely. Mark it dead so the refresher stops retrying it
                        if ("OAuthException".equals(e.getErrorType())) {
                            vaultService.ifPresent(v -> v.markPageTokenInvalid(page.getId()));
                        }
                    } catch (Exception e) {
                        failureCount++;
                        log.error("Error ingesting events for page: {}", page.getId(), e); // one bad page shouldn't stop the whole run
                    }
                }

                hasMorePages = activePagesPage.hasNext();
                pageNumber++;
            }

            // 2. log a summary with counts and duration so the scheduled run is easy to audit in logs
            long duration = System.currentTimeMillis() - startTime;
            log.info("Facebook event ingestion completed. Success: {}, Failure: {}, Duration: {}ms",
                successCount, failureCount, duration);

        } catch (Exception e) {
            log.error("Unexpected error in Facebook event ingestion scheduler", e); // log but don't rethrow, Spring reschedules the next run regardless
        }
    }
}
