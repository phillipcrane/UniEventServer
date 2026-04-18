package dk.unievent.app.tools.controller;

import dk.unievent.app.application.service.EventService;
import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IngestControllerTests {

    @Mock
    private EventService eventService;

    @Mock
    private PageRepository pageRepository;

    @InjectMocks
    private IngestController ingestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ingestController).build();
    }

    @Test
    void ingestShouldReturn404WhenPageNotFound() throws Exception {
        when(pageRepository.existsById("missing-page")).thenReturn(false);

        mockMvc.perform(post("/admin/tools/ingest/missing-page"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Page not found"));
    }

    @Test
    void ingestShouldReturn200WithEventsOnSuccess() throws Exception {
        EventEntity event1 = EventEntity.builder().id("evt-1").title("React Workshop").build();
        EventEntity event2 = EventEntity.builder().id("evt-2").title("Spring Boot").build();

        when(pageRepository.existsById("page-1")).thenReturn(true);
        when(eventService.ingestFacebookEvents("page-1")).thenReturn(List.of(event1, event2));

        mockMvc.perform(post("/admin/tools/ingest/page-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageId").value("page-1"))
            .andExpect(jsonPath("$.eventCount").value(2))
            .andExpect(jsonPath("$.eventTitles[0]").value("React Workshop"));
    }

    @Test
    void ingestShouldReturn502OnFacebookApiException() throws Exception {
        when(pageRepository.existsById("page-1")).thenReturn(true);
        when(eventService.ingestFacebookEvents("page-1"))
            .thenThrow(new FacebookApiException("Rate limited", 429, "OAuthException"));

        mockMvc.perform(post("/admin/tools/ingest/page-1"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.errorType").value("OAuthException"));
    }

    @Test
    void ingestShouldReturn500OnUnexpectedException() throws Exception {
        when(pageRepository.existsById("page-1")).thenReturn(true);
        when(eventService.ingestFacebookEvents("page-1"))
            .thenThrow(new RuntimeException("DB connection lost"));

        mockMvc.perform(post("/admin/tools/ingest/page-1"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("DB connection lost"));
    }
}
