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

            // Process all active pages that have Facebook tokens
            // Pageable with page = 0, size = PAGE_SIZE for first batch
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
                                page.getId(), e.getStatusCode());
                        } else {
                            log.error("Facebook API error ingesting events for page: {} - {} ({})",
                                page.getId(), e.getErrorType(), e.getStatusCode());
                        }
                        // An OAuthException means the token is dead - mark it invalid in the registry
                        if ("OAuthException".equals(e.getErrorType())) {
                            vaultService.ifPresent(v -> v.markPageTokenInvalid(page.getId()));
                        }
                    } catch (Exception e) {
                        failureCount++;
                        log.error("Error ingesting events for page: {}", page.getId(), e);
                        // Continue with next page even if this one fails
                    }
                }

                // Check if there are more pages
                hasMorePages = activePagesPage.hasNext();
                pageNumber++;
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Facebook event ingestion completed. Success: {}, Failure: {}, Duration: {}ms",
                successCount, failureCount, duration);

        } catch (Exception e) {
            log.error("Unexpected error in Facebook event ingestion scheduler", e);
            // Log but don't rethrow - scheduler should continue running even after errors
        }
    }
}
