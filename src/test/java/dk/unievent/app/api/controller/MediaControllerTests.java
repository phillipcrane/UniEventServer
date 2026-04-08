package dk.unievent.app.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.app.api.handler.GlobalExceptionHandler;
import dk.unievent.app.application.service.MediaService;
import dk.unievent.app.db.model.MediaEntity;
import dk.unievent.app.db.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MediaControllerTests {

    @Mock
    private MediaService mediaService;

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private MediaController mediaController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(mediaController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void uploadShouldReturnOkWithMappedDto() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "poster.png", "image/png", "payload".getBytes());

        when(mediaService.store(any())).thenReturn("1,abc");
        when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(invocation -> {
            MediaEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        mockMvc.perform(multipart("/media").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10L))
            .andExpect(jsonPath("$.filename").value("poster.png"))
            .andExpect(jsonPath("$.contentType").value("image/png"))
            .andExpect(jsonPath("$.fileId").value("1,abc"));
    }

    @Test
    void uploadShouldReturnInternalServerErrorWhenStorageFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "poster.png", "image/png", "payload".getBytes());
        when(mediaService.store(any())).thenThrow(new IOException("store failed"));

        mockMvc.perform(multipart("/media").file(file))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void downloadShouldReturnResourceWithContentHeaders() throws Exception {
        MediaEntity media = MediaEntity.builder()
            .id(42L)
            .filename("my photo.jpg")
            .contentType("image/jpeg")
            .fileId("1,abc")
            .uploadedAt(Instant.now())
            .build();

        when(mediaRepository.findById(42L)).thenReturn(Optional.of(media));
        when(mediaService.loadAsResource("1,abc")).thenReturn(new ByteArrayResource("hello".getBytes()));

        mockMvc.perform(get("/media/42"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"))
            .andExpect(header().string("Content-Disposition", "inline; filename=\"my+photo.jpg\""))
            .andExpect(content().bytes("hello".getBytes()));
    }

    @Test
    void downloadShouldReturnInternalServerErrorWhenMediaMissing() throws Exception {
        when(mediaRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/media/999"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void listShouldReturnAllMediaAsDtos() throws Exception {
        MediaEntity first = MediaEntity.builder().id(1L).filename("a.jpg").contentType("image/jpeg").fileId("1,a").uploadedAt(Instant.now()).build();
        MediaEntity second = MediaEntity.builder().id(2L).filename("b.png").contentType("image/png").fileId("1,b").uploadedAt(Instant.now()).build();

        when(mediaRepository.findAll()).thenReturn(List.of(first, second));

        mockMvc.perform(get("/media"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].fileId").value("1,a"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].fileId").value("1,b"));
    }
}
