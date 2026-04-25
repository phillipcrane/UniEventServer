package dk.unievent.app.api.controller;

import dk.unievent.app.api.handler.GlobalExceptionHandler;
import tools.jackson.databind.json.JsonMapper;
import dk.unievent.app.application.dto.MediaDTO;
import dk.unievent.app.application.service.MediaService;
import dk.unievent.app.db.model.MediaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

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

    @InjectMocks
    private MediaController mediaController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(
            JsonMapper.builder().findAndAddModules().build()
        );
        mockMvc = MockMvcBuilders
            .standaloneSetup(mediaController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setMessageConverters(converter, new ResourceHttpMessageConverter())
            .build();
    }

    @Test
    void uploadShouldReturnOkWithMappedDto() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "poster.png", "image/png", "payload".getBytes());
        MediaDTO dto = MediaDTO.builder().id(10L).filename("poster.png").contentType("image/png").fileId("1,abc").uploadedAt(Instant.now()).build();
        when(mediaService.storeAndSave(any())).thenReturn(dto);

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
        when(mediaService.storeAndSave(any())).thenThrow(new IOException("store failed"));

        mockMvc.perform(multipart("/media").file(file))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void downloadShouldReturnResourceWithContentHeaders() throws Exception {
        MediaEntity media = MediaEntity.builder().id(42L).filename("my photo.jpg").contentType("image/jpeg").fileId("1,abc").uploadedAt(Instant.now()).build();
        when(mediaService.findById(42L)).thenReturn(media);
        when(mediaService.loadAsResource("1,abc")).thenReturn(new ByteArrayResource("hello".getBytes()));

        mockMvc.perform(get("/media/42"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"))
            .andExpect(header().string("Content-Disposition", "inline; filename=\"my+photo.jpg\""))
            .andExpect(content().bytes("hello".getBytes()));
    }

    @Test
    void downloadShouldReturnNotFoundWhenMediaMissing() throws Exception {
        when(mediaService.findById(999L)).thenThrow(new NoSuchElementException("Media not found: 999"));

        mockMvc.perform(get("/media/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void listShouldReturnAllMediaAsDtos() throws Exception {
        MediaDTO first = MediaDTO.builder().id(1L).filename("a.jpg").contentType("image/jpeg").fileId("1,a").uploadedAt(Instant.now()).build();
        MediaDTO second = MediaDTO.builder().id(2L).filename("b.png").contentType("image/png").fileId("1,b").uploadedAt(Instant.now()).build();
        when(mediaService.listAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 50), 2));

        mockMvc.perform(get("/media"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].fileId").value("1,a"))
            .andExpect(jsonPath("$.content[1].id").value(2))
            .andExpect(jsonPath("$.content[1].fileId").value("1,b"));
    }
}
