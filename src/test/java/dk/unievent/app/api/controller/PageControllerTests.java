package dk.unievent.app.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.app.application.dto.PageDTO;
import dk.unievent.app.application.service.PageService;
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
class PageControllerTests {

    @Mock
    private PageService pageService;

    @InjectMocks
    private PageController pageController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pageController).build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void getPageByIdShouldReturnNotFoundWhenMissing() throws Exception {
        when(pageService.getPageById("missing")).thenReturn(null);

        mockMvc.perform(get("/api/pages/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createPageShouldReturnCreated() throws Exception {
        PageDTO created = samplePage("page-1", "UniEvent");
        when(pageService.savePage(any(PageDTO.class))).thenReturn(created);

        mockMvc.perform(post("/api/pages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePage(null, "UniEvent"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("page-1"));
    }

    @Test
    void searchPagesShouldUseNameParameter() throws Exception {
        when(pageService.searchPagesByName("uni")).thenReturn(List.of(samplePage("page-1", "UniEvent")));

        mockMvc.perform(get("/api/pages/search").param("name", "uni"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("page-1"));

        verify(pageService).searchPagesByName("uni");
    }

    @Test
    void updatePageShouldOverrideBodyIdWithPathId() throws Exception {
        when(pageService.savePage(any(PageDTO.class))).thenReturn(samplePage("path-id", "Updated"));

        mockMvc.perform(put("/api/pages/path-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePage("body-id", "Updated"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("path-id"));

        ArgumentCaptor<PageDTO> captor = ArgumentCaptor.forClass(PageDTO.class);
        verify(pageService).savePage(captor.capture());
        assertEquals("path-id", captor.getValue().getId());
    }

    @Test
    void uploadPagePictureShouldReturnInternalServerErrorOnIoFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "pic.jpg", "image/jpeg", "bytes".getBytes());
        when(pageService.uploadPicture(eq("page-1"), any())).thenThrow(new IOException("upload failed"));

        mockMvc.perform(multipart("/api/pages/page-1/picture").file(file))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void deletePageShouldReturnNoContentWhenDeleted() throws Exception {
        when(pageService.deletePage("page-1")).thenReturn(true);

        mockMvc.perform(delete("/api/pages/page-1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deletePageShouldReturnNotFoundWhenMissing() throws Exception {
        when(pageService.deletePage("missing")).thenReturn(false);

        mockMvc.perform(delete("/api/pages/missing"))
            .andExpect(status().isNotFound());
    }

    private PageDTO samplePage(String id, String name) {
        PageDTO dto = new PageDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setUrl("https://facebook.com/unievent");
        dto.setActive(true);
        return dto;
    }
}
