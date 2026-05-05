package dk.unievent.app.application.service;

import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import dk.unievent.app.tools.models.RefreshResult;
import dk.unievent.app.tools.models.RefreshSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Shared Facebook page-token refresh logic.
 * Consumed by FacebookTokenRefresher (scheduled) and TokenRefreshController (manual).
 */
@Slf4j
@Service
public class TokenRefreshService {

    private static final int PAGE_SIZE = 50;

    private final PageService pageService;
    private final FacebookGraphApiService facebookGraphApiService;
    private final Optional<VaultService> vaultService;

    public TokenRefreshService(
        PageService pageService,
        FacebookGraphApiService facebookGraphApiService,
        Optional<VaultService> vaultService
    ) {
        this.pageService = pageService;
        this.facebookGraphApiService = facebookGraphApiService;
        this.vaultService = vaultService;
    }

    /**
     * Refresh tokens for every page the scheduler would refresh in one pass.
     */
    public RefreshSummary refreshAll() {
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
                if (refreshOne(page.getId()).isSuccess()) {
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
        return new RefreshSummary(refreshedCount, failedCount, duration);
    }

    /**
     * Refresh tokens for every page regardless of expiry - dev/manual use only.
     */
    public RefreshSummary refreshAllForce() {
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
                if (refreshOne(page.getId()).isSuccess()) {
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
        return new RefreshSummary(refreshedCount, failedCount, duration);
    }

    /**
     * Refresh a single page's token: Vault read → Graph API refresh → Vault write → DB metadata update.
     */
    public RefreshResult refreshOne(String pageId) {
        log.debug("Refreshing token for page: {}", pageId);

        if (vaultService.isEmpty()) {
            String msg = "Vault is disabled - token refresh unavailable";
            log.warn("{} for page: {}", msg, pageId);
            return new RefreshResult(pageId, false, msg);
        }

        VaultService vault = vaultService.get();

        try {
            Optional<String> currentTokenOpt = vault.getPageToken(pageId);
            if (currentTokenOpt.isEmpty()) {
                String msg = "No token found in Vault";
                log.warn("{} for page: {}", msg, pageId);
                pageService.logRefreshFailure(pageId, msg);
                return new RefreshResult(pageId, false, msg);
            }

            String currentToken = currentTokenOpt.get();

            try {
                var tokenResponse = facebookGraphApiService.refreshPageToken(currentToken);
                String newToken = tokenResponse.getAccessToken();

                vault.updatePageToken(pageId, newToken);
                pageService.updateTokenMetadata(pageId);
                log.info("Successfully refreshed token for page: {}", pageId);
                return new RefreshResult(pageId, true, "Token refreshed");

            } catch (FacebookApiException e) {
                String msg = String.format("Facebook API error: %s (status %d)", e.getErrorType(), e.getStatusCode());
                log.error("Facebook API error refreshing token for page: {} - {}", pageId, msg);
                pageService.logRefreshFailure(pageId, msg);
                vault.markPageTokenError(pageId);
                return new RefreshResult(pageId, false, msg);
            }

        } catch (Exception e) {
            log.error("Error refreshing token for page: {}", pageId, e);
            pageService.logRefreshFailure(pageId, e.getMessage());
            vaultService.ifPresent(v -> v.markPageTokenError(pageId));
            return new RefreshResult(pageId, false, e.getMessage());
        }
    }
}
