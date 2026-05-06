package dk.unievent.app.application.scheduler;

import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.application.service.EventService;
import dk.unievent.app.application.service.PageService;
import dk.unievent.app.application.service.VaultService;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacebookIngestionSchedulerTests {

    @Mock
    private PageService pageService;

    @Mock
    private EventService eventService;

    @Mock
    private VaultService vaultService;

    @Test
    void ingestFacebookEventsShouldProcessAllActivePages() {
        PageDTO page1 = pageDto("p1", "Alpha");
        PageDTO page2 = pageDto("p2", "Beta");
        when(pageService.getActivePages(any()))
            .thenReturn(new PageImpl<>(List.of(page1, page2), PageRequest.of(0, 50), 2));

        FacebookIngestionScheduler scheduler =
            new FacebookIngestionScheduler(pageService, eventService, Optional.of(vaultService), 50);
        scheduler.ingestFacebookEvents();

        verify(eventService).ingestFacebookEvents("p1");
        verify(eventService).ingestFacebookEvents("p2");
    }

    @Test
    void ingestFacebookEventsShouldPageThroughAllActivePages() {
        PageDTO page1 = pageDto("p1", "Alpha");
        PageDTO page2 = pageDto("p2", "Beta");
        when(pageService.getActivePages(PageRequest.of(0, 1)))
            .thenReturn(new PageImpl<>(List.of(page1), PageRequest.of(0, 1), 2));
        when(pageService.getActivePages(PageRequest.of(1, 1)))
            .thenReturn(new PageImpl<>(List.of(page2), PageRequest.of(1, 1), 2));

        FacebookIngestionScheduler scheduler =
            new FacebookIngestionScheduler(pageService, eventService, Optional.of(vaultService), 1);
        scheduler.ingestFacebookEvents();

        verify(eventService).ingestFacebookEvents("p1");
        verify(eventService).ingestFacebookEvents("p2");
        verify(pageService).getActivePages(PageRequest.of(0, 1));
        verify(pageService).getActivePages(PageRequest.of(1, 1));
        verify(pageService, never()).getActivePages(PageRequest.of(2, 1));
    }

    @Test
    void ingestFacebookEventsShouldMarkPageTokenInvalidOnOAuthException() {
        PageDTO page = pageDto("p1", "Alpha");
        when(pageService.getActivePages(any()))
            .thenReturn(new PageImpl<>(List.of(page), PageRequest.of(0, 50), 1));
        doThrow(new FacebookApiException("Token invalid", 401, "OAuthException"))
            .when(eventService).ingestFacebookEvents("p1");

        FacebookIngestionScheduler scheduler =
            new FacebookIngestionScheduler(pageService, eventService, Optional.of(vaultService), 50);
        scheduler.ingestFacebookEvents();

        verify(vaultService).markPageTokenInvalid("p1");
    }

    @Test
    void ingestFacebookEventsShouldNotCallVaultOnNonOAuthException() {
        PageDTO page = pageDto("p1", "Alpha");
        when(pageService.getActivePages(any()))
            .thenReturn(new PageImpl<>(List.of(page), PageRequest.of(0, 50), 1));
        doThrow(new FacebookApiException("Rate limited", 429, "APIError"))
            .when(eventService).ingestFacebookEvents("p1");

        FacebookIngestionScheduler scheduler =
            new FacebookIngestionScheduler(pageService, eventService, Optional.of(vaultService), 50);
        scheduler.ingestFacebookEvents();

        verify(vaultService, never()).markPageTokenInvalid(any());
    }

    @Test
    void ingestFacebookEventsShouldNotMarkTokenInvalidWhenTokenIsMissing() {
        PageDTO page = pageDto("p1", "Alpha");
        when(pageService.getActivePages(any()))
            .thenReturn(new PageImpl<>(List.of(page), PageRequest.of(0, 50), 1));
        doThrow(new FacebookApiException("Token missing", 404, "TOKEN_NOT_FOUND"))
            .when(eventService).ingestFacebookEvents("p1");

        FacebookIngestionScheduler scheduler =
            new FacebookIngestionScheduler(pageService, eventService, Optional.of(vaultService), 50);
        scheduler.ingestFacebookEvents();

        verify(vaultService, never()).markPageTokenInvalid(any());
    }

    @Test
    void ingestFacebookEventsShouldContinueAfterGenericException() {
        PageDTO page1 = pageDto("p1", "Alpha");
        PageDTO page2 = pageDto("p2", "Beta");
        when(pageService.getActivePages(any()))
            .thenReturn(new PageImpl<>(List.of(page1, page2), PageRequest.of(0, 50), 2));
        doThrow(new RuntimeException("Network error")).when(eventService).ingestFacebookEvents("p1");

        FacebookIngestionScheduler scheduler =
            new FacebookIngestionScheduler(pageService, eventService, Optional.of(vaultService), 50);
        scheduler.ingestFacebookEvents();

        verify(eventService).ingestFacebookEvents("p2");
    }

    @Test
    void ingestFacebookEventsShouldHandleOAuthExceptionGracefullyWhenVaultAbsent() {
        PageDTO page = pageDto("p1", "Alpha");
        when(pageService.getActivePages(any()))
            .thenReturn(new PageImpl<>(List.of(page), PageRequest.of(0, 50), 1));
        doThrow(new FacebookApiException("Token invalid", 401, "OAuthException"))
            .when(eventService).ingestFacebookEvents("p1");

        FacebookIngestionScheduler scheduler =
            new FacebookIngestionScheduler(pageService, eventService, Optional.empty(), 50);
        scheduler.ingestFacebookEvents(); // Should not throw

        verifyNoInteractions(vaultService);
    }

    @Test
    void ingestFacebookEventsShouldDoNothingWhenNoPagesActive() {
        when(pageService.getActivePages(any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        FacebookIngestionScheduler scheduler =
            new FacebookIngestionScheduler(pageService, eventService, Optional.empty(), 50);
        scheduler.ingestFacebookEvents();

        verifyNoInteractions(eventService);
    }

    @Test
    void ingestFacebookEventsShouldSwallowPageListingFailuresSoSchedulerCanRunAgain() {
        PageDTO page = pageDto("p1", "Alpha");
        when(pageService.getActivePages(any()))
            .thenThrow(new RuntimeException("database unavailable"))
            .thenReturn(new PageImpl<>(List.of(page), PageRequest.of(0, 50), 1));

        FacebookIngestionScheduler scheduler =
            new FacebookIngestionScheduler(pageService, eventService, Optional.empty(), 50);

        assertDoesNotThrow(scheduler::ingestFacebookEvents);
        assertDoesNotThrow(scheduler::ingestFacebookEvents);

        verify(eventService).ingestFacebookEvents("p1");
    }

    private PageDTO pageDto(String id, String name) {
        PageDTO dto = new PageDTO();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }
}
