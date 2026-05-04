package dk.unievent.app.application.service;

import dk.unievent.app.application.dto.TokenRefreshResult;
import dk.unievent.app.application.dto.TokenRefreshSummary;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Shared Facebook page-token refresh logic for scheduled and manual admin flows.
 */
@Slf4j
@Service
public class FacebookTokenRefreshService {

    private static final int PAGE_SIZE = 50;

    private final PageService pageService;
    private final FacebookGraphApiService facebookGraphApiService;
    private final VaultService vaultService;

    public FacebookTokenRefreshService(
        PageService pageService,
        FacebookGraphApiService facebookGraphApiService,
        VaultService vaultService
    ) {
        this.pageService = pageService;
        this.facebookGraphApiService = facebookGraphApiService;
        this.vaultService = vaultService;
    }

    /**
     * Refresh tokens for every page the scheduler would refresh in one pass.
     */
    public TokenRefreshSummary refreshAll() {
        log.info("Starting Facebook page token refresh (all pages)");
        long startTime = System.currentTimeMillis();
        int refreshedCount = 0;
        int failedCount = 0;

        int pageNumber = 0;
        boolean hasMorePages = true;

        while (hasMorePages) {
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            Page<PageEntity> pagesToRefresh = pageService.getPagesToRefresh(pageable);

            for (PageEntity page : pagesToRefresh.getContent()) {
                if (refreshOne(page.getId()).success()) {
                    refreshedCount++;
                } else {
                    failedCount++;
                }
            }

            hasMorePages = pagesToRefresh.hasNext();
            pageNumber++;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Facebook page token refresh completed. Refreshed: {}, Failed: {}, Duration: {}ms",
            refreshedCount, failedCount, duration);
        return new TokenRefreshSummary(refreshedCount, failedCount, duration);
    }

    /**
     * Refresh tokens for every page regardless of expiry - manual admin use only.
     */
    public TokenRefreshSummary refreshAllForce() {
        log.info("Starting forced Facebook page token refresh (all pages)");
        long startTime = System.currentTimeMillis();
        int refreshedCount = 0;
        int failedCount = 0;

        int pageNumber = 0;
        boolean hasMorePages = true;

        while (hasMorePages) {
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            Page<PageEntity> pages = pageService.getAllPageEntities(pageable);

            for (PageEntity page : pages.getContent()) {
                if (refreshOne(page.getId()).success()) {
                    refreshedCount++;
                } else {
                    failedCount++;
                }
            }

            hasMorePages = pages.hasNext();
            pageNumber++;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Forced token refresh completed. Refreshed: {}, Failed: {}, Duration: {}ms",
            refreshedCount, failedCount, duration);
        return new TokenRefreshSummary(refreshedCount, failedCount, duration);
    }

    /**
     * Refresh a single page's token: Vault read -> Graph API refresh -> Vault write -> DB metadata update.
     */
    public TokenRefreshResult refreshOne(String pageId) {
        log.debug("Refreshing token for page: {}", pageId);

        try {
            Optional<String> currentTokenOpt = vaultService.getPageToken(pageId);
            if (currentTokenOpt.isEmpty()) {
                String msg = "No token found in Vault";
                log.warn("{} for page: {}", msg, pageId);
                pageService.logRefreshFailure(pageId, msg);
                return new TokenRefreshResult(pageId, false, msg);
            }

            String currentToken = currentTokenOpt.get();

            try {
                var tokenResponse = facebookGraphApiService.refreshPageToken(currentToken);
                String newToken = tokenResponse.getAccessToken();

                vaultService.updatePageToken(pageId, newToken);
                pageService.refreshToken(pageId);
                log.info("Successfully refreshed token for page: {}", pageId);
                return new TokenRefreshResult(pageId, true, "Token refreshed");

            } catch (FacebookApiException e) {
                String msg = String.format("Facebook API error: %s (status %d)", e.getErrorType(), e.getStatusCode());
                log.error("Facebook API error refreshing token for page: {} - {}", pageId, msg);
                pageService.logRefreshFailure(pageId, msg);
                return new TokenRefreshResult(pageId, false, msg);
            }

        } catch (Exception e) {
            log.error("Error refreshing token for page: {}", pageId, e);
            pageService.logRefreshFailure(pageId, e.getMessage());
            return new TokenRefreshResult(pageId, false, e.getMessage());
        }
    }
}
