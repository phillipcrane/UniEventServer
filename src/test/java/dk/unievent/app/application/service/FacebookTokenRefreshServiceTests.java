package dk.unievent.app.application.service;

import dk.unievent.app.api.dto.FbLongLivedTokenResponse;
import dk.unievent.app.application.dto.TokenRefreshResult;
import dk.unievent.app.application.dto.TokenRefreshSummary;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacebookTokenRefreshServiceTests {

    @Mock
    private PageService pageService;

    @Mock
    private FacebookGraphApiService facebookGraphApiService;

    @Mock
    private VaultService vaultService;

    @InjectMocks
    private FacebookTokenRefreshService tokenRefreshService;

    @Test
    void refreshOneShouldReturnFailureWhenNoTokenInVault() {
        when(vaultService.getPageToken("page-1")).thenReturn(Optional.empty());

        TokenRefreshResult result = tokenRefreshService.refreshOne("page-1");

        assertFalse(result.success());
        assertEquals("No token found in Vault", result.message());
        verify(pageService).logRefreshFailure("page-1", "No token found in Vault");
    }

    @Test
    void refreshOneShouldReturnSuccessWhenTokenRefreshed() {
        FbLongLivedTokenResponse response = new FbLongLivedTokenResponse();
        response.setAccessToken("new-token");

        when(vaultService.getPageToken("page-1")).thenReturn(Optional.of("old-token"));
        when(facebookGraphApiService.refreshPageToken("old-token")).thenReturn(response);

        TokenRefreshResult result = tokenRefreshService.refreshOne("page-1");

        assertTrue(result.success());
        assertEquals("Token refreshed", result.message());
        verify(vaultService).updatePageToken("page-1", "new-token");
        verify(pageService).refreshToken("page-1");
    }

    @Test
    void refreshOneShouldReturnFailureOnFacebookApiException() {
        when(vaultService.getPageToken("page-1")).thenReturn(Optional.of("old-token"));
        when(facebookGraphApiService.refreshPageToken("old-token"))
            .thenThrow(new FacebookApiException("Token invalid", 401, "OAuthException"));

        TokenRefreshResult result = tokenRefreshService.refreshOne("page-1");

        assertFalse(result.success());
        assertTrue(result.message().contains("OAuthException"));
        verify(pageService).logRefreshFailure(eq("page-1"), anyString());
    }

    @Test
    void refreshOneShouldReturnFailureOnUnexpectedException() {
        when(vaultService.getPageToken("page-1")).thenThrow(new RuntimeException("Vault down"));

        TokenRefreshResult result = tokenRefreshService.refreshOne("page-1");

        assertFalse(result.success());
        assertEquals("Vault down", result.message());
    }

    @Test
    void refreshAllForceShouldIterateAllPagesAndReturnSummary() {
        PageEntity page1 = PageEntity.builder().id("p1").name("Alpha").build();
        PageEntity page2 = PageEntity.builder().id("p2").name("Beta").build();

        FbLongLivedTokenResponse response = new FbLongLivedTokenResponse();
        response.setAccessToken("new-token");

        when(pageService.getAllPageEntities(any()))
            .thenReturn(new PageImpl<>(List.of(page1, page2), PageRequest.of(0, 50), 2));
        when(vaultService.getPageToken(any())).thenReturn(Optional.of("current-token"));
        when(facebookGraphApiService.refreshPageToken("current-token")).thenReturn(response);

        TokenRefreshSummary summary = tokenRefreshService.refreshAllForce();

        assertEquals(2, summary.refreshedCount());
        assertEquals(0, summary.failedCount());
    }

    @Test
    void refreshAllShouldOnlyRefreshPagesToRefresh() {
        PageEntity page = PageEntity.builder().id("p1").name("Expiring").build();

        FbLongLivedTokenResponse response = new FbLongLivedTokenResponse();
        response.setAccessToken("new-token");

        when(pageService.getPagesToRefresh(any()))
            .thenReturn(new PageImpl<>(List.of(page), PageRequest.of(0, 50), 1));
        when(vaultService.getPageToken("p1")).thenReturn(Optional.of("old-token"));
        when(facebookGraphApiService.refreshPageToken("old-token")).thenReturn(response);

        TokenRefreshSummary summary = tokenRefreshService.refreshAll();

        assertEquals(1, summary.refreshedCount());
        assertEquals(0, summary.failedCount());
    }
}
