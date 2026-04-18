package dk.unievent.app.tools.controller;

import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.db.repository.PageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PagesControllerTests {

    @Mock
    private PageRepository pageRepository;

    @InjectMocks
    private PagesController pagesController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pagesController).build();
    }

    @Test
    void listShouldReturnPageSummaries() throws Exception {
        PageEntity page1 = PageEntity.builder().id("p1").name("Alpha").tokenStatus("valid").tokenExpiresInDays(30).build();
        PageEntity page2 = PageEntity.builder().id("p2").name("Beta").tokenStatus("expired").tokenExpiresInDays(0).build();

        when(pageRepository.findAllByOrderByNameAsc(any()))
            .thenReturn(new PageImpl<>(List.of(page1, page2), PageRequest.of(0, 500), 2));

        mockMvc.perform(get("/admin/tools/pages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("p1"))
            .andExpect(jsonPath("$[0].name").value("Alpha"))
            .andExpect(jsonPath("$[0].tokenStatus").value("valid"))
            .andExpect(jsonPath("$[1].id").value("p2"))
            .andExpect(jsonPath("$[1].tokenStatus").value("expired"));
    }

    @Test
    void listShouldReturnEmptyListWhenNoPagesExist() throws Exception {
        when(pageRepository.findAllByOrderByNameAsc(any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 500), 0));

        mockMvc.perform(get("/admin/tools/pages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}
