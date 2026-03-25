package dk.unievent.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.web.entity.Event;
import dk.unievent.web.entity.Page;
import dk.unievent.web.repository.EventRepository;
import dk.unievent.web.repository.PageRepository;
import dk.unievent.web.service.FacebookService;
import dk.unievent.web.service.SecretManagerService;
import dk.unievent.web.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventControllerTest {

    private FacebookService facebookService;
    private PageRepository pageRepository;
    private EventRepository eventRepository;
    private SecretManagerService secretManagerService;
    private StorageService storageService;
    private ObjectMapper objectMapper;

    private EventController controller;

    @BeforeEach
    void setUp() {
        pageRepository = mock(PageRepository.class);
        eventRepository = mock(EventRepository.class);
        objectMapper = new ObjectMapper(); // Use real ObjectMapper for tests

        controller = new EventController(pageRepository, eventRepository, objectMapper, "http://localhost:8081", "http://localhost:8082", "http://localhost:8083");
    }

    @Test
    void callbackStoresTokensAndReturnsStoredPages() throws Exception {
        when(facebookService.getShortLivedToken("code123")).thenReturn("short");
        when(facebookService.getLongLivedToken("short")).thenReturn(new FacebookService.LongLivedToken("long", 3600));
        when(facebookService.getPagesFromUser("long")).thenReturn(List.of(new FacebookService.FacebookPage("page1", "Page 1", "token1")));

        ResponseEntity<Map<String, Object>> response = controller.handleCallback(Map.of("code", "code123", "debug", "1"));

        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(1, response.getBody().get("storedPages"));

        verify(secretManagerService).addPageToken("page1", "token1", 3600);
        verify(pageRepository).save(any(Page.class));
    }

    @Test
    void ingestSavesEventsForPages() throws Exception {
        Page page = new Page();
        page.setId("page1");
        page.setName("Page 1");

        when(pageRepository.findAll()).thenReturn(List.of(page));
        when(secretManagerService.getPageToken("page1")).thenReturn("token1");
        when(facebookService.getPageEvents("page1", "token1"))
            .thenReturn(List.of(new FacebookService.FbEventResponse("e1", "Event 1", "Desc", "2026-03-18T00:00:00Z", "2026-03-18T01:00:00Z", null, null)));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ResponseEntity<Map<String, Object>> response = controller.handleIngest();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().get("totalPages"));
        assertEquals(1, response.getBody().get("totalEvents"));

        verify(eventRepository).saveAll(any());
    }

    @Test
    void refreshTokensUpdatesPageAndReturnsCounts() throws Exception {
        Page page = new Page();
        page.setId("page1");
        page.setName("Page 1");

        when(pageRepository.findAll()).thenReturn(List.of(page));
        when(secretManagerService.getPageToken("page1")).thenReturn("token1");
        when(facebookService.refreshPageToken("token1")).thenReturn(new FacebookService.LongLivedToken("newTok", 7200));

        ResponseEntity<Map<String, Object>> response = controller.handleRefreshTokens();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().get("tokensRefreshed"));

        verify(secretManagerService).updatePageToken("page1", "newTok", 7200);
        verify(pageRepository).save(any(Page.class));
    }
}

