package dk.unievent.app.application.service;

import dk.unievent.app.api.dto.FbLongLivedTokenResponse;
import dk.unievent.app.api.dto.FbPageResponse;
import dk.unievent.app.api.dto.FbShortLivedTokenResponse;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.infrastructure.constants.FacebookApiConstants;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

// orchestrates the Facebook OAuth flow after the user grants access: 1) exchange the auth code
// for a short-lived token; 2) upgrade to a long-lived token (60+ days); 3) get the user's pages;
// 4) store each page token in Vault and upsert the PageEntity.
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

    public List<PageEntity> processOAuthCallback(String code) {
        log.info("Processing OAuth callback with authorization code");

        try {
            // 1. exchange the auth code for a short-lived user token (expires in ~1 hour)
            FbShortLivedTokenResponse shortLivedResponse = facebookGraphApiService.getShortLivedToken(code);
            String shortLivedToken = shortLivedResponse.getAccessToken();
            log.debug("Short-lived token obtained, expires in {} seconds", shortLivedResponse.getExpiresIn());

            // 2. upgrade to a long-lived token so we're not re-authing every hour
            FbLongLivedTokenResponse longLivedResponse = facebookGraphApiService.getLongLivedToken(shortLivedToken);
            String longLivedToken = longLivedResponse.getAccessToken();
            Integer expiresInObj = longLivedResponse.getExpiresIn();
            int expiresIn = expiresInObj != null ? expiresInObj : FacebookApiConstants.DEFAULT_TOKEN_EXPIRY_SECONDS;
            log.debug("Long-lived token obtained, expires in {} seconds (~{} days)", expiresIn, (expiresIn / 86400));

            // 3. get the pages this user administers
            List<FbPageResponse> fbPages = facebookGraphApiService.getPagesFromUser(longLivedToken);
            log.info("Retrieved {} Facebook pages from user", fbPages.size());

            // 4. for each page: store its token in Vault and upsert the PageEntity
            List<PageEntity> processedPages = fbPages.stream()
                .map(fbPage -> processFacebookPage(fbPage, longLivedToken))
                .collect(Collectors.toList());

            log.info("OAuth callback processed successfully. {} pages stored", processedPages.size());
            return processedPages;

        } catch (FacebookApiException e) {
            log.error("Facebook API error during OAuth flow: {}, {}", e.getStatusCode(), e.getErrorType(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during OAuth flow", e);
            throw new FacebookApiException("OAuth flow failed: " + e.getMessage(), 0, "OAUTH_FLOW_ERROR");
        }
    }

    private PageEntity processFacebookPage(FbPageResponse fbPage, String userToken) {
        log.debug("Processing Facebook page: {} ({})", fbPage.getName(), fbPage.getId());

        if (userToken == null || userToken.isBlank()) {
            log.warn("Processing page {} without a valid user token from the OAuth flow", fbPage.getId());
        }
        try {
            // 1. store the page token in Vault (registry row gets status=active, expiresAt filled in below)
            String pageToken = fbPage.getAccessToken();
            vaultService.storePageToken(fbPage.getId(), pageToken);

            // 2. upsert the PageEntity, which computes the token expiration date
            PageEntity pageEntity = pageService.createOrUpdatePageFromFacebook(fbPage);
            log.info("Page entity created/updated: {} ({})", pageEntity.getName(), pageEntity.getId());

            // 3. now that we know the expiry, sync it back to the secrets registry row
            vaultService.setPageTokenExpiry(fbPage.getId(), pageEntity.getTokenExpiresAt());

            return pageEntity;

        } catch (Exception e) {
            log.error("Error processing Facebook page: {}", fbPage.getId(), e);
            throw new FacebookApiException(
                String.format("Failed to process Facebook page %s: %s", fbPage.getId(), e.getMessage()),
                0,
                "PAGE_PROCESSING_ERROR"
            );
        }
    }

    // checks whether the stored token for a page is still accepted by Facebook
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
