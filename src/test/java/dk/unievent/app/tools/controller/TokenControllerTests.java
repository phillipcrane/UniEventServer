package dk.unievent.app.tools.controller;

import dk.unievent.app.application.service.TokenRefreshService;
import dk.unievent.app.db.repository.PageRepository;
import dk.unievent.app.tools.models.RefreshResult;
import dk.unievent.app.tools.models.RefreshSummary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TokenRefreshControllerTests {

    @Mock
    private TokenRefreshService tokenRefreshService;

    @Mock
    private PageRepository pageRepository;

    @InjectMocks
    private TokenController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void refreshAllShouldReturn200WithSummary() throws Exception {
        when(tokenRefreshService.refreshAllForce()).thenReturn(new RefreshSummary(3, 1, 420L));

        mockMvc.perform(post("/admin/tools/refresh-tokens"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshedCount").value(3))
            .andExpect(jsonPath("$.failedCount").value(1));
    }

    @Test
    void refreshOneShouldReturn404WhenPageNotFound() throws Exception {
        when(pageRepository.existsById("missing")).thenReturn(false);

        mockMvc.perform(post("/admin/tools/refresh-tokens/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Page not found"));
    }

    @Test
    void refreshOneShouldReturn200OnSuccess() throws Exception {
        when(pageRepository.existsById("page-1")).thenReturn(true);
        when(tokenRefreshService.refreshOne("page-1"))
            .thenReturn(new RefreshResult("page-1", true, "Token refreshed"));

        mockMvc.perform(post("/admin/tools/refresh-tokens/page-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Token refreshed"));
    }

    @Test
    void refreshOneShouldReturn500OnFailure() throws Exception {
        when(pageRepository.existsById("page-1")).thenReturn(true);
        when(tokenRefreshService.refreshOne("page-1"))
            .thenReturn(new RefreshResult("page-1", false, "Facebook API error: OAuthException (status 401)"));

        mockMvc.perform(post("/admin/tools/refresh-tokens/page-1"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false));
    }
}
