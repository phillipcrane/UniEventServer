package dk.unievent.app.application.service;

import dk.unievent.app.api.dto.*;
import dk.unievent.app.infrastructure.config.FacebookConfig;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import dk.unievent.app.infrastructure.util.FacebookAppSecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates all Facebook Graph API v25 calls with security hardening.
 * 
 * Security measures:
 * - OAuth secrets transmitted in POST body, never in URL query params
 * - Access tokens transmitted in Authorization header, never in query params
 * - All sensitive data masked in logs
 * - HTTPS enforcement via RestClient configuration
 */
@Slf4j
@Service
public class FacebookGraphApiService {
    
    private static final String FB_GRAPH_BASE_URL = "https://graph.facebook.com";
    private final RestClient restClient;
    private final RestClient restClientNoBaseUrl;
    private final FacebookConfig facebookConfig;
    private final ObjectMapper objectMapper;
    
    public FacebookGraphApiService(RestClient.Builder restClientBuilder, FacebookConfig facebookConfig) {
        // Regular RestClient with baseUrl for other Graph API calls
        this.restClient = restClientBuilder.baseUrl(FB_GRAPH_BASE_URL).build();
        // Separate RestClient WITHOUT baseUrl for OAuth endpoints (which need full URIs)
        this.restClientNoBaseUrl = RestClient.builder().build();
        this.facebookConfig = facebookConfig;
        this.objectMapper = new ObjectMapper();
        // Register JSR310 module for Java 8+ date/time types (LocalDateTime, LocalDate, etc.)
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Exchange authorization code for short-lived access token.
     * Uses POST with form body (never GET with query params for security).
     *
     * @param code Authorization code from Facebook callback
     * @return Short-lived token response (valid ~2 hours)
     * @throws FacebookApiException if token exchange fails
     */
    public FbShortLivedTokenResponse getShortLivedToken(String code) {
        try {
            log.info("Exchanging authorization code for short-lived token");
            
            // Build request body with credentials (secure - not in URL)
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", facebookConfig.getAppId());
            body.add("client_secret", facebookConfig.getAppSecret());
            body.add("code", code);
            body.add("redirect_uri", facebookConfig.getRedirectUri());
            
            // Log parameters for debugging (mask sensitive values)
            log.info("OAuth token exchange parameters:");
            log.info("  - API version: {}", facebookConfig.getGraphApiVersion());
            log.info("  - App ID: {}", facebookConfig.getAppId());
            log.info("  - Redirect URI: {}", facebookConfig.getRedirectUri());
            log.info("  - Authorization code: {}", code.substring(0, Math.min(10, code.length())) + "...");
            
            // Build full URI using UriComponentsBuilder to ensure correct parsing
            URI fullUri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("graph.facebook.com")
                    .path("/{version}/oauth/access_token")
                    .buildAndExpand(facebookConfig.getGraphApiVersion())
                    .toUri();
            
            log.info("CONSTRUCTED URI: {}", fullUri.toString());
            log.info("URI scheme: {}", fullUri.getScheme());
            log.info("URI host: {}", fullUri.getHost());
            log.info("URI path: {}", fullUri.getPath());
            log.info("REQUEST BODY PARAMS: client_id={}, client_secret=***, code={}, redirect_uri={}", 
                facebookConfig.getAppId(), code.substring(0, Math.min(10, code.length())) + "...", facebookConfig.getRedirectUri());
            
            // Read as String first due to Facebook's text/javascript content type
            String responseBody = restClientNoBaseUrl.post()
                    .uri(fullUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            
            // Manually deserialize using ObjectMapper to handle text/javascript content type
            FbShortLivedTokenResponse response = objectMapper.readValue(responseBody, FbShortLivedTokenResponse.class);
            
            log.info("SHORT-LIVED TOKEN EXCHANGE SUCCESSFUL");
            return response;
                    
        } catch (RestClientResponseException e) {
            log.error("Failed to get short-lived token. Status: {} - Response: {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new FacebookApiException(
                    "Failed to exchange code for short-lived token",
                    e.getStatusCode().value(),
                    "SHORT_LIVED_TOKEN_ERROR"
            );
        } catch (Exception e) {
            log.error("Unexpected error during token exchange", e);
            throw new FacebookApiException(
                    "Failed to exchange code for short-lived token: " + e.getMessage(),
                    0,
                    "SHORT_LIVED_TOKEN_ERROR"
            );
        }
    }
    
    /**
     * Exchange short-lived token for long-lived token.
     * Uses POST with form body (never GET with query params for security).
     *
     * @param shortLivedToken Short-lived token to exchange
     * @return Long-lived token response (valid ~60 days)
     * @throws FacebookApiException if token exchange fails
     */
    public FbLongLivedTokenResponse getLongLivedToken(String shortLivedToken) {
        try {
            log.debug("Exchanging short-lived token for long-lived token");
            
            // Build request body with credentials (secure - not in URL)
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "fb_exchange_token");
            body.add("client_id", facebookConfig.getAppId());
            body.add("client_secret", facebookConfig.getAppSecret());
            body.add("fb_exchange_token", shortLivedToken);
            
            // Build full URI using UriComponentsBuilder to ensure correct parsing
            URI fullUri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("graph.facebook.com")
                    .path("/{version}/oauth/access_token")
                    .buildAndExpand(facebookConfig.getGraphApiVersion())
                    .toUri();
            
            log.info("LONG-LIVED TOKEN EXCHANGE - CONSTRUCTED URI: {}", fullUri.toString());
            log.info("URI scheme: {}", fullUri.getScheme());
            log.info("URI host: {}", fullUri.getHost());
            log.info("URI path: {}", fullUri.getPath());
            log.info("LONG-LIVED REQUEST BODY PARAMS: grant_type=fb_exchange_token, client_id={}, client_secret=***, fb_exchange_token={}", 
                facebookConfig.getAppId(), "***");
            
            // Get response as String first (Facebook returns text/javascript content type)
            String responseBody = restClientNoBaseUrl.post()
                    .uri(fullUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            
            // Manually deserialize using ObjectMapper to handle text/javascript content type
            FbLongLivedTokenResponse response = objectMapper.readValue(responseBody, FbLongLivedTokenResponse.class);
            
            log.info("LONG-LIVED TOKEN EXCHANGE SUCCESSFUL");
            return response;
                    
        } catch (RestClientResponseException e) {
            log.error("Failed to get long-lived token. Status: {}", e.getStatusCode());
            throw new FacebookApiException(
                    "Failed to exchange token for long-lived token",
                    e.getStatusCode().value(),
                    "LONG_LIVED_TOKEN_ERROR"
            );
        } catch (Exception e) {
            log.error("Unexpected error during long-lived token exchange", e);
            throw new FacebookApiException(
                    "Failed to exchange token for long-lived token: " + e.getMessage(),
                    0,
                    "LONG_LIVED_TOKEN_ERROR"
            );
        }
    }
    
    /**
     * Fetch user's admin-controlled Facebook pages.
     * Token transmitted in Authorization header for security (not query param).
     *
     * @param userToken User's long-lived access token
     * @return List of pages with their data
     * @throws FacebookApiException if API call fails
     */
    @SuppressWarnings("unchecked")
    public List<FbPageResponse> getPagesFromUser(String userToken) {
        try {
            log.debug("Fetching admin-controlled pages for user (token: {})", FacebookAppSecurityUtil.maskToken(userToken));
            
            // Build full URI with scheme and host (Facebook returns text/javascript content type)
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("graph.facebook.com")
                    .path("/{version}/me/accounts")
                    .queryParam("fields", "id,name,access_token")
                    .buildAndExpand(facebookConfig.getGraphApiVersion())
                    .toUri();
            
            log.debug("PAGES REQUEST URI: {}", uri);
            
            // Read as String due to Facebook's text/javascript content type
            String responseBody = restClientNoBaseUrl.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + userToken)
                    .retrieve()
                    .body(String.class);
            
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Empty response from Facebook pages endpoint");
                return List.of();
            }
            
            // Parse JSON manually
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            if (!responseMap.containsKey("data")) {
                log.warn("No 'data' field in Facebook response");
                return List.of();
            }
            
            Object dataObj = responseMap.get("data");
            if (dataObj == null) {
                log.warn("'data' field is null in Facebook response");
                return List.of();
            }
            
            List<Map<String, Object>> pagesData = (List<Map<String, Object>>) dataObj;
            if (pagesData.isEmpty()) {
                log.debug("No pages found for user");
                return List.of();
            }
            
            List<FbPageResponse> pages = pagesData.stream()
                    .map(data -> objectMapper.convertValue(data, FbPageResponse.class))
                    .toList();
            log.debug("Retrieved {} pages from user", pages.size());
            return pages;
            
        } catch (RestClientResponseException e) {
            log.error("Failed to fetch user pages. Status: {}", e.getStatusCode());
            throw new FacebookApiException(
                    "Failed to fetch user pages",
                    e.getStatusCode().value(),
                    "PAGES_FETCH_ERROR"
            );
        } catch (Exception e) {
            log.error("Unexpected error fetching user pages", e);
            throw new FacebookApiException(
                    "Failed to fetch user pages: " + e.getMessage(),
                    0,
                    "PAGES_FETCH_ERROR"
            );
        }
    }
    
    /**
     * Fetch upcoming events for a Facebook page.
     * Token transmitted in Authorization header for security (not query param).
     *
     * @param pageId Facebook page ID
     * @param pageToken Page access token
     * @return List of upcoming events
     * @throws FacebookApiException if API call fails
     */
    @SuppressWarnings("unchecked")
    public List<FbEventResponse> getPageEvents(String pageId, String pageToken) {
        try {
            log.debug("Fetching events for page: {} (token: {})", pageId, FacebookAppSecurityUtil.maskToken(pageToken));
            
            // Build full URI with scheme and host (Facebook returns text/javascript content type)
            // Use /events endpoint with proper fields for page events
            String fields = "id,name,description,start_time,end_time,place,cover,timezone,is_canceled,is_online,type";
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("graph.facebook.com")
                    .path("/{version}/{pageId}/events")
                    .queryParam("fields", fields)
                    .queryParam("limit", 100)
                    .buildAndExpand(facebookConfig.getGraphApiVersion(), pageId)
                    .toUri();
            
            // Read as String due to Facebook's text/javascript content type
            String responseBody = restClientNoBaseUrl.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + pageToken)
                    .retrieve()
                    .body(String.class);
            
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Empty response from Facebook events endpoint for page: {}", pageId);
                return List.of();
            }
            
            // Parse JSON manually
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            if (!responseMap.containsKey("data")) {
                log.warn("No 'data' field in Facebook events response for page: {}", pageId);
                return List.of();
            }
            
            Object dataObj = responseMap.get("data");
            if (dataObj == null) {
                log.warn("'data' field is null in Facebook events response for page: {}", pageId);
                return List.of();
            }
            
            List<Map<String, Object>> eventsData = (List<Map<String, Object>>) dataObj;
            if (eventsData.isEmpty()) {
                log.debug("No events found for page: {}", pageId);
                return List.of();
            }
            
            List<FbEventResponse> events = eventsData.stream()
                    .map(data -> objectMapper.convertValue(data, FbEventResponse.class))
                    .toList();
            log.debug("Retrieved {} events for page: {}", events.size(), pageId);
            return events;
            
        } catch (RestClientResponseException e) {
            log.error("Failed to fetch events for page: {}. Status: {} - Response: {}", pageId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new FacebookApiException(
                    "Failed to fetch page events: " + e.getResponseBodyAsString(),
                    e.getStatusCode().value(),
                    "EVENTS_FETCH_ERROR"
            );
        } catch (Exception e) {
            log.error("Unexpected error fetching events for page: {}", pageId, e);
            throw new FacebookApiException(
                    "Failed to fetch page events: " + e.getMessage(),
                    0,
                    "EVENTS_FETCH_ERROR"
            );
        }
    }
    
    /**
     * Refresh an expired page access token.
     * Uses POST with form body (never GET with query params for security).
     *
     * @param expiredToken Expired page access token to refresh
     * @return New long-lived token response (valid ~60 days)
     * @throws FacebookApiException if token refresh fails
     */
    public FbLongLivedTokenResponse refreshPageToken(String expiredToken) {
        try {
            log.debug("Refreshing page token (token: {})", FacebookAppSecurityUtil.maskToken(expiredToken));
            
            // Build request body with credentials (secure - not in URL)
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "fb_exchange_token");
            body.add("client_id", facebookConfig.getAppId());
            body.add("client_secret", facebookConfig.getAppSecret());
            body.add("fb_exchange_token", expiredToken);
            
            // Build full URI using UriComponentsBuilder
            URI fullUri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("graph.facebook.com")
                    .path("/{version}/oauth/access_token")
                    .buildAndExpand(facebookConfig.getGraphApiVersion())
                    .toUri();
            
            // Read as String first due to Facebook's text/javascript content type
            String responseBody = restClientNoBaseUrl.post()
                    .uri(fullUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            
            // Manually deserialize using ObjectMapper to handle text/javascript content type
            return objectMapper.readValue(responseBody, FbLongLivedTokenResponse.class);
                    
        } catch (RestClientResponseException e) {
            log.error("Failed to refresh page token. Status: {}", e.getStatusCode());
            throw new FacebookApiException(
                    "Failed to refresh page token",
                    e.getStatusCode().value(),
                    "TOKEN_REFRESH_ERROR"
            );
        } catch (Exception e) {
            log.error("Unexpected error refreshing page token", e);
            throw new FacebookApiException(
                    "Failed to refresh page token: " + e.getMessage(),
                    0,
                    "TOKEN_REFRESH_ERROR"
            );
        }
    }

    /**
     * Validate a Facebook page token with a lightweight page lookup.
     *
     * @param pageId Facebook page ID
     * @param pageToken Page access token to validate
     * @return true if token can access the page
     * @throws FacebookApiException if validation call fails
     */
    public boolean validatePageToken(String pageId, String pageToken) {
        try {
            log.debug("Validating token for page: {} (token: {})",
                    pageId, FacebookAppSecurityUtil.maskToken(pageToken));

            URI uri = UriComponentsBuilder
                    .fromPath("/{version}/{pageId}")
                    .queryParam("fields", "id")
                    .buildAndExpand(facebookConfig.getGraphApiVersion(), pageId)
                    .toUri();

            var response = restClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + pageToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) {
                return false;
            }
            Object id = response.get("id");
            return id instanceof String && pageId.equals((String) id);

        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.debug("Page token rejected for page: {}. Status: {}", pageId, e.getStatusCode());
                return false;
            }
            log.warn("Page token validation failed for page: {}. Status: {}", pageId, e.getStatusCode());
            throw new FacebookApiException(
                    "Failed to validate page token",
                    e.getStatusCode().value(),
                    "TOKEN_VALIDATION_ERROR"
            );
        }
    }
}
