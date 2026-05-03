package dk.unievent.app.application.service;

import dk.unievent.app.api.dto.FbLongLivedTokenResponse;
import dk.unievent.app.api.dto.FbPageResponse;
import dk.unievent.app.api.dto.FbShortLivedTokenResponse;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.infrastructure.config.FacebookApiConstants;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates Facebook OAuth workflow:
 * 1. Exchange authorization code for short-lived token
 * 2. Exchange short-lived token for long-lived token (60+ days)
 * 3. Fetch user's admin-controlled pages
 * 4. Store page tokens in Vault
 * 5. Create/update PageEntities
 */
@Slf4j
@Service
public class FacebookOAuthService {

    private final FacebookGraphApiService facebookGraphApiService;
    private final PageService pageService;
    private final VaultService vaultService;

    public FacebookOAuthService(
        FacebookGraphApiService facebookGraphApiService,
        PageService pageService,
        VaultService vaultService
    ) {
        this.facebookGraphApiService = facebookGraphApiService;
        this.pageService = pageService;
        this.vaultService = vaultService;
    }

    /**
     * Process OAuth callback with authorization code.
     * Exchanges code for tokens and stores Facebook pages.
     * @param code Authorization code from Facebook callback
     * @return List of PageEntities created/updated
     * @throws FacebookApiException if any step of the OAuth flow fails
     */
    public List<PageEntity> processOAuthCallback(String code) {
        log.info("Processing OAuth callback with authorization code");

        try {
            // Step 1: Exchange authorization code for short-lived token
            log.debug("Step 1: Exchanging authorization code for short-lived token");
            FbShortLivedTokenResponse shortLivedResponse = facebookGraphApiService.getShortLivedToken(code);
            String shortLivedToken = shortLivedResponse.getAccessToken();
            log.debug("Short-lived token obtained, expires in {} seconds", shortLivedResponse.getExpiresIn());

            // Step 2: Exchange short-lived token for long-lived token (60+ days)
            log.debug("Step 2: Exchanging short-lived token for long-lived token");
            FbLongLivedTokenResponse longLivedResponse = facebookGraphApiService.getLongLivedToken(shortLivedToken);
            String longLivedToken = longLivedResponse.getAccessToken();
            Integer expiresInObj = longLivedResponse.getExpiresIn();
            int expiresIn = expiresInObj != null ? expiresInObj : FacebookApiConstants.DEFAULT_TOKEN_EXPIRY_SECONDS;
            log.debug("Long-lived token obtained, expires in {} seconds (~{} days)",
                expiresIn, (expiresIn / 86400));

            // Step 3: Fetch user's admin-controlled pages
            log.debug("Step 3: Fetching admin-controlled pages for user");
            List<FbPageResponse> fbPages = facebookGraphApiService.getPagesFromUser(longLivedToken);
            log.info("Retrieved {} Facebook pages from user", fbPages.size());

            // Step 4 & 5: Store tokens and create/update PageEntities
            log.debug("Step 4-5: Processing and storing Facebook pages");
            List<PageEntity> processedPages = fbPages.stream()
                .map(fbPage -> processFacebookPage(fbPage, longLivedToken))
                .collect(Collectors.toList());

            log.info("OAuth callback processed successfully. {} pages stored", processedPages.size());
            return processedPages;

        } catch (FacebookApiException e) {
            log.error("Facebook API error during OAuth flow: {} - {}",
                e.getStatusCode(), e.getErrorType(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during OAuth flow", e);
            throw new FacebookApiException("OAuth flow failed: " + e.getMessage(), 0, "OAUTH_FLOW_ERROR");
        }
    }

    /**
     * Process a single Facebook page response from the OAuth process.
     * Stores token in Vault and creates/updates PageEntity.
     * @param fbPage Facebook page response from Graph API
     * @param userToken User's long-lived access token
     * @return Stored PageEntity
     */
    private PageEntity processFacebookPage(FbPageResponse fbPage, String userToken) {
        log.debug("Processing Facebook page: {} ({})", fbPage.getName(), fbPage.getId());

        if (userToken == null || userToken.isBlank()) {
            log.warn("Processing page {} without a valid user token from the OAuth flow", fbPage.getId());
        }
        try {
            // Store page token in Vault (registry gets status=active, expiresAt set below)
            String pageToken = fbPage.getAccessToken();
            vaultService.storePageToken(fbPage.getId(), pageToken);
            log.debug("Page token stored in Vault for page: {}", fbPage.getId());

            // Create or update PageEntity — this computes the token expiration date
            PageEntity pageEntity = pageService.createOrUpdatePageFromFacebook(fbPage);
            log.info("Page entity created/updated: {} ({})", pageEntity.getName(), pageEntity.getId());

            // Now that we know the expiry, sync it to the secrets registry
            vaultService.setPageTokenExpiry(fbPage.getId(), pageEntity.getTokenExpiresAt());

            return pageEntity;

        } catch (Exception e) {
            log.error("Error processing Facebook page: {}", fbPage.getId(), e);
            // Continue processing other pages even if one fails
            throw new FacebookApiException(
                String.format("Failed to process Facebook page %s: %s", fbPage.getId(), e.getMessage()),
                0,
                "PAGE_PROCESSING_ERROR"
            );
        }
    }

    /**
     * Validate that a stored Facebook page token is still valid.
     * Optionally refresh if expired.
     * @param pageId Facebook page ID
     * @return true if token is valid or successfully refreshed
     */
    public boolean validatePageToken(String pageId) {
        log.debug("Validating token for page: {}", pageId);

        return vaultService.getPageToken(pageId)
            .map(token -> {
                try {
                    boolean isValid = facebookGraphApiService.validatePageToken(pageId, token);
                    if (isValid) {
                        log.debug("Token validated for page: {}", pageId);
                    }
                    return isValid;
                } catch (FacebookApiException e) {
                    log.warn("Token validation failed for page: {}", pageId, e);
                    return false;
                }
            })
            .orElse(false);
    }
}
