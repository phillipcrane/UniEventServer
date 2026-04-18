package dk.unievent.app.application.service;

import dk.unievent.app.api.dto.FbLongLivedTokenResponse;
import dk.unievent.app.api.dto.FbPageResponse;
import dk.unievent.app.api.dto.FbShortLivedTokenResponse;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacebookOAuthServiceTests {

    @Mock
    private FacebookGraphApiService facebookGraphApiService;

    @Mock
    private PageService pageService;

    @Mock
    private VaultService vaultService;

    @InjectMocks
    private FacebookOAuthService facebookOAuthService;

    private FbShortLivedTokenResponse shortLivedResponse;
    private FbLongLivedTokenResponse longLivedResponse;
    private FbPageResponse fbPage;
    private PageEntity pageEntity;

    @BeforeEach
    void setUp() {
        shortLivedResponse = new FbShortLivedTokenResponse();
        shortLivedResponse.setAccessToken("short-token");
        shortLivedResponse.setExpiresIn(3600);

        longLivedResponse = new FbLongLivedTokenResponse();
        longLivedResponse.setAccessToken("long-token");
        longLivedResponse.setExpiresIn(5184000);

        fbPage = new FbPageResponse();
        fbPage.setId("page-1");
        fbPage.setName("Test Page");
        fbPage.setAccessToken("page-token");

        pageEntity = PageEntity.builder().id("page-1").name("Test Page").build();
    }

    @Test
    void processOAuthCallbackShouldReturnPagesOnSuccess() {
        when(facebookGraphApiService.getShortLivedToken("auth-code")).thenReturn(shortLivedResponse);
        when(facebookGraphApiService.getLongLivedToken("short-token")).thenReturn(longLivedResponse);
        when(facebookGraphApiService.getPagesFromUser("long-token")).thenReturn(List.of(fbPage));
        when(pageService.createOrUpdatePageFromFacebook(fbPage)).thenReturn(pageEntity);

        List<PageEntity> result = facebookOAuthService.processOAuthCallback("auth-code");

        assertEquals(1, result.size());
        assertEquals("page-1", result.get(0).getId());
        verify(vaultService).storePageToken("page-1", "page-token");
    }

    @Test
    void processOAuthCallbackShouldRethrowFacebookApiException() {
        when(facebookGraphApiService.getShortLivedToken("bad-code"))
            .thenThrow(new FacebookApiException("Token exchange failed", 400, "OAuthException"));

        assertThrows(FacebookApiException.class,
            () -> facebookOAuthService.processOAuthCallback("bad-code"));
    }

    @Test
    void processOAuthCallbackShouldWrapUnexpectedExceptionInFacebookApiException() {
        when(facebookGraphApiService.getShortLivedToken("code"))
            .thenThrow(new RuntimeException("network error"));

        FacebookApiException ex = assertThrows(FacebookApiException.class,
            () -> facebookOAuthService.processOAuthCallback("code"));

        assertEquals("OAUTH_FLOW_ERROR", ex.getErrorType());
    }

    @Test
    void validatePageTokenShouldReturnTrueWhenTokenIsValid() {
        when(vaultService.getPageToken("page-1")).thenReturn(Optional.of("valid-token"));
        when(facebookGraphApiService.validatePageToken("page-1", "valid-token")).thenReturn(true);

        assertTrue(facebookOAuthService.validatePageToken("page-1"));
    }

    @Test
    void validatePageTokenShouldReturnFalseWhenNoTokenInVault() {
        when(vaultService.getPageToken("page-1")).thenReturn(Optional.empty());

        assertFalse(facebookOAuthService.validatePageToken("page-1"));
        verifyNoInteractions(facebookGraphApiService);
    }

    @Test
    void validatePageTokenShouldReturnFalseWhenValidationThrows() {
        when(vaultService.getPageToken("page-1")).thenReturn(Optional.of("expired-token"));
        when(facebookGraphApiService.validatePageToken("page-1", "expired-token"))
            .thenThrow(new FacebookApiException("Invalid token", 401, "OAuthException"));

        assertFalse(facebookOAuthService.validatePageToken("page-1"));
    }
}
