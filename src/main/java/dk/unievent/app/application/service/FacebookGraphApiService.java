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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FacebookGraphApiService {

    private final RestClient restClient;
    private final FacebookConfig facebookConfig;
    private final ObjectMapper objectMapper;

    public FacebookGraphApiService(RestClient.Builder restClientBuilder, FacebookConfig facebookConfig) {
        this.restClient = restClientBuilder.baseUrl(facebookConfig.getGraphApiBaseUrl()).build();
        this.facebookConfig = facebookConfig;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public FbShortLivedTokenResponse getShortLivedToken(String code) {
        try {
            log.info("Exchanging authorization code for short-lived token");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", facebookConfig.getAppId());
            body.add("client_secret", facebookConfig.getAppSecret());
            body.add("code", code);
            body.add("redirect_uri", facebookConfig.getRedirectUri());

            String responseBody = restClient.post()
                    .uri("/{version}/oauth/access_token", facebookConfig.getGraphApiVersion())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            FbShortLivedTokenResponse response = objectMapper.readValue(responseBody, FbShortLivedTokenResponse.class);
            log.info("Short-lived token exchange successful");
            return response;

        } catch (RestClientResponseException e) {
            log.error("Failed to get short-lived token. Status: {}", e.getStatusCode());
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

    public FbLongLivedTokenResponse getLongLivedToken(String shortLivedToken) {
        try {
            log.debug("Exchanging short-lived token for long-lived token");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "fb_exchange_token");
            body.add("client_id", facebookConfig.getAppId());
            body.add("client_secret", facebookConfig.getAppSecret());
            body.add("fb_exchange_token", shortLivedToken);

            String responseBody = restClient.post()
                    .uri("/{version}/oauth/access_token", facebookConfig.getGraphApiVersion())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            FbLongLivedTokenResponse response = objectMapper.readValue(responseBody, FbLongLivedTokenResponse.class);
            log.debug("Long-lived token exchange successful");
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

    @SuppressWarnings("unchecked")
    public List<FbPageResponse> getPagesFromUser(String userToken) {
        try {
            log.debug("Fetching admin-controlled pages for user (token: {})", FacebookAppSecurityUtil.maskToken(userToken));

            String responseBody = restClient.get()
                    .uri("/{version}/me/accounts?fields=id,name,access_token", facebookConfig.getGraphApiVersion())
                    .header("Authorization", "Bearer " + userToken)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Empty response from Facebook pages endpoint");
                return List.of();
            }

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

    @SuppressWarnings("unchecked")
    public List<FbEventResponse> getPageEvents(String pageId, String pageToken) {
        try {
            log.debug("Fetching events for page: {} (token: {})", pageId, FacebookAppSecurityUtil.maskToken(pageToken));

            String fields = "id,name,description,start_time,end_time,place,cover,timezone,is_canceled,is_online,type";

            String responseBody = restClient.get()
                    .uri("/{version}/{pageId}/events?fields={fields}&limit=100",
                            facebookConfig.getGraphApiVersion(), pageId, fields)
                    .header("Authorization", "Bearer " + pageToken)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Empty response from Facebook events endpoint for page: {}", pageId);
                return List.of();
            }

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

    public FbLongLivedTokenResponse refreshPageToken(String expiredToken) {
        try {
            log.debug("Refreshing page token (token: {})", FacebookAppSecurityUtil.maskToken(expiredToken));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "fb_exchange_token");
            body.add("client_id", facebookConfig.getAppId());
            body.add("client_secret", facebookConfig.getAppSecret());
            body.add("fb_exchange_token", expiredToken);

            String responseBody = restClient.post()
                    .uri("/{version}/oauth/access_token", facebookConfig.getGraphApiVersion())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

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

    public boolean validatePageToken(String pageId, String pageToken) {
        try {
            log.debug("Validating token for page: {} (token: {})",
                    pageId, FacebookAppSecurityUtil.maskToken(pageToken));

            var response = restClient.get()
                    .uri("/{version}/{pageId}?fields=id", facebookConfig.getGraphApiVersion(), pageId)
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
