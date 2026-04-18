package dk.unievent.app.application.service;

import dk.unievent.app.api.dto.FbLongLivedTokenResponse;
import dk.unievent.app.api.dto.FbPageResponse;
import dk.unievent.app.api.dto.FbShortLivedTokenResponse;
import dk.unievent.app.infrastructure.config.FacebookConfig;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class FacebookGraphApiServiceTests {

    private MockRestServiceServer server;
    private FacebookGraphApiService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        FacebookConfig config = new FacebookConfig();
        config.setGraphApiBaseUrl("http://localhost");
        config.setGraphApiVersion("v22.0");
        config.setAppId("app-id");
        config.setAppSecret("app-secret");
        config.setRedirectUri("http://localhost/callback");

        service = new FacebookGraphApiService(builder, config);
    }

    @Test
    void getShortLivedTokenShouldParseSuccessResponse() {
        server.expect(requestTo(containsString("/v22.0/oauth/access_token")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"short-tok\",\"token_type\":\"bearer\",\"expires_in\":7200}",
                        MediaType.APPLICATION_JSON));

        FbShortLivedTokenResponse response = service.getShortLivedToken("auth-code");

        assertEquals("short-tok", response.getAccessToken());
        assertEquals("bearer", response.getTokenType());
        server.verify();
    }

    @Test
    void getShortLivedTokenShouldThrowFacebookApiExceptionOn4xx() {
        server.expect(requestTo(containsString("/oauth/access_token")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        FacebookApiException ex = assertThrows(FacebookApiException.class,
                () -> service.getShortLivedToken("bad-code"));

        assertEquals("SHORT_LIVED_TOKEN_ERROR", ex.getErrorType());
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    void getLongLivedTokenShouldParseSuccessResponse() {
        server.expect(requestTo(containsString("/oauth/access_token")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"long-tok\",\"token_type\":\"bearer\",\"expires_in\":5183944}",
                        MediaType.APPLICATION_JSON));

        FbLongLivedTokenResponse response = service.getLongLivedToken("short-tok");

        assertEquals("long-tok", response.getAccessToken());
        server.verify();
    }

    @Test
    void getLongLivedTokenShouldThrowOn4xx() {
        server.expect(requestTo(containsString("/oauth/access_token")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        FacebookApiException ex = assertThrows(FacebookApiException.class,
                () -> service.getLongLivedToken("bad-tok"));

        assertEquals("LONG_LIVED_TOKEN_ERROR", ex.getErrorType());
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void getPagesFromUserShouldReturnMappedPages() {
        server.expect(requestTo(containsString("/me/accounts")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer user-token"))
                .andRespond(withSuccess(
                        "{\"data\":[{\"id\":\"p1\",\"name\":\"Page One\",\"access_token\":\"pg-tok\"}]}",
                        MediaType.APPLICATION_JSON));

        List<FbPageResponse> pages = service.getPagesFromUser("user-token");

        assertEquals(1, pages.size());
        assertEquals("p1", pages.get(0).getId());
        assertEquals("Page One", pages.get(0).getName());
        assertEquals("pg-tok", pages.get(0).getAccessToken());
        server.verify();
    }

    @Test
    void getPagesFromUserShouldReturnEmptyWhenDataKeyAbsent() {
        server.expect(requestTo(containsString("/me/accounts")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<FbPageResponse> pages = service.getPagesFromUser("user-token");

        assertTrue(pages.isEmpty());
    }

    @Test
    void getPagesFromUserShouldReturnEmptyWhenDataArrayEmpty() {
        server.expect(requestTo(containsString("/me/accounts")))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        List<FbPageResponse> pages = service.getPagesFromUser("user-token");

        assertTrue(pages.isEmpty());
    }

    @Test
    void getPagesFromUserShouldThrowOnHttpError() {
        server.expect(requestTo(containsString("/me/accounts")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        FacebookApiException ex = assertThrows(FacebookApiException.class,
                () -> service.getPagesFromUser("bad-token"));

        assertEquals("PAGES_FETCH_ERROR", ex.getErrorType());
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void getPageEventsShouldReturnMappedEvents() {
        server.expect(requestTo(containsString("/page-1/events")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer pg-tok"))
                .andRespond(withSuccess(
                        "{\"data\":[{\"id\":\"e1\",\"name\":\"Event One\"}]}",
                        MediaType.APPLICATION_JSON));

        var events = service.getPageEvents("page-1", "pg-tok");

        assertEquals(1, events.size());
        assertEquals("e1", events.get(0).getId());
        server.verify();
    }

    @Test
    void getPageEventsShouldReturnEmptyWhenDataEmpty() {
        server.expect(requestTo(containsString("/page-1/events")))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        var events = service.getPageEvents("page-1", "pg-tok");

        assertTrue(events.isEmpty());
    }

    @Test
    void getPageEventsShouldThrowOn5xx() {
        server.expect(requestTo(containsString("/page-1/events")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        FacebookApiException ex = assertThrows(FacebookApiException.class,
                () -> service.getPageEvents("page-1", "pg-tok"));

        assertEquals("EVENTS_FETCH_ERROR", ex.getErrorType());
        assertEquals(500, ex.getStatusCode());
    }

    @Test
    void refreshPageTokenShouldReturnNewToken() {
        server.expect(requestTo(containsString("/oauth/access_token")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"refreshed-tok\",\"token_type\":\"bearer\"}",
                        MediaType.APPLICATION_JSON));

        FbLongLivedTokenResponse response = service.refreshPageToken("old-tok");

        assertEquals("refreshed-tok", response.getAccessToken());
        server.verify();
    }

    @Test
    void refreshPageTokenShouldThrowOnHttpError() {
        server.expect(requestTo(containsString("/oauth/access_token")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        FacebookApiException ex = assertThrows(FacebookApiException.class,
                () -> service.refreshPageToken("expired-tok"));

        assertEquals("TOKEN_REFRESH_ERROR", ex.getErrorType());
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void validatePageTokenShouldReturnTrueWhenIdMatches() {
        server.expect(requestTo(containsString("/page-1")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer valid-tok"))
                .andRespond(withSuccess("{\"id\":\"page-1\"}", MediaType.APPLICATION_JSON));

        assertTrue(service.validatePageToken("page-1", "valid-tok"));
        server.verify();
    }

    @Test
    void validatePageTokenShouldReturnFalseOn401() {
        server.expect(requestTo(containsString("/page-1")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertFalse(service.validatePageToken("page-1", "invalid-tok"));
    }

    @Test
    void validatePageTokenShouldReturnFalseOn403() {
        server.expect(requestTo(containsString("/page-1")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertFalse(service.validatePageToken("page-1", "invalid-tok"));
    }

    @Test
    void validatePageTokenShouldThrowOn500() {
        server.expect(requestTo(containsString("/page-1")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        FacebookApiException ex = assertThrows(FacebookApiException.class,
                () -> service.validatePageToken("page-1", "tok"));

        assertEquals("TOKEN_VALIDATION_ERROR", ex.getErrorType());
        assertEquals(500, ex.getStatusCode());
    }
}
