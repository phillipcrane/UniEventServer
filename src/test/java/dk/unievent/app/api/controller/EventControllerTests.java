package dk.unievent.app.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.app.application.dto.EventDTO;
import dk.unievent.app.application.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTests {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void getAllEventsShouldReturnOk() throws Exception {
        EventDTO event = sampleEvent("event-1");
        when(eventService.getAllEvents()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("event-1"));
    }

    @Test
    void getEventByIdShouldReturnNotFoundWhenMissing() throws Exception {
        when(eventService.getEventById("missing")).thenReturn(null);

        mockMvc.perform(get("/api/events/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createEventShouldReturnCreated() throws Exception {
        EventDTO input = sampleEvent(null);
        EventDTO created = sampleEvent("evt-created");

        when(eventService.createEvent(any(EventDTO.class))).thenReturn(created);

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("evt-created"));
    }

    @Test
    void updateEventShouldReturnNotFoundWhenMissing() throws Exception {
        when(eventService.updateEvent(eq("evt-404"), any(EventDTO.class))).thenReturn(null);

        mockMvc.perform(put("/api/events/evt-404")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleEvent(null))))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteEventShouldReturnNoContentWhenDeleted() throws Exception {
        when(eventService.deleteEvent("evt-1")).thenReturn(true);

        mockMvc.perform(delete("/api/events/evt-1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void uploadCoverImageShouldReturnInternalServerErrorOnIoFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", "abc".getBytes());
        when(eventService.uploadCoverImage(eq("evt-1"), any())).thenThrow(new IOException("disk error"));

        mockMvc.perform(multipart("/api/events/evt-1/coverImage").file(file))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void updateEventShouldPassPathIdToService() throws Exception {
        EventDTO updated = sampleEvent("evt-path");
        when(eventService.updateEvent(eq("evt-path"), any(EventDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/api/events/evt-path")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleEvent("different-body-id"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("evt-path"));

        ArgumentCaptor<EventDTO> captor = ArgumentCaptor.forClass(EventDTO.class);
        verify(eventService).updateEvent(eq("evt-path"), captor.capture());
        assertEquals("different-body-id", captor.getValue().getId());
    }

    private EventDTO sampleEvent(String id) {
        EventDTO dto = new EventDTO();
        dto.setId(id);
        dto.setPageId("page-1");
        dto.setTitle("Sample Event");
        dto.setDescription("Sample description");
        dto.setStartTime(LocalDateTime.of(2030, 1, 1, 18, 0));
        dto.setEndTime(LocalDateTime.of(2030, 1, 1, 20, 0));
        dto.setEventUrl("https://example.com/events/1");
        return dto;
    }
}
