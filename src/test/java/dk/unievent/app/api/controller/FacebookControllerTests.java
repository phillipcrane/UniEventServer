package dk.unievent.app.api.controller;

import dk.unievent.app.application.service.FacebookOAuthService;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.infrastructure.exception.FacebookApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FacebookControllerTests {

    @Mock
    private FacebookOAuthService facebookOAuthService;

    @InjectMocks
    private FacebookController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:3000");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void healthCheckShouldReturn200WithStatusOk() throws Exception {
        mockMvc.perform(get("/api/facebook/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void callbackShouldRedirectToFrontendWithSuccessWhenCodePresent() throws Exception {
        PageEntity page = PageEntity.builder().id("page-1").name("Test Page").build();
        when(facebookOAuthService.processOAuthCallback("auth-code")).thenReturn(List.of(page));

        mockMvc.perform(get("/api/facebook/callback").param("code", "auth-code"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:3000?success=true&pages=1"));
    }

    @Test
    void callbackShouldRedirectWithErrorWhenCodeMissing() throws Exception {
        mockMvc.perform(get("/api/facebook/callback"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:3000?error=Missing+authorization+code"));

        verifyNoInteractions(facebookOAuthService);
    }

    @Test
    void callbackShouldRedirectWithErrorWhenServiceThrows() throws Exception {
        when(facebookOAuthService.processOAuthCallback("bad-code"))
            .thenThrow(new FacebookApiException("Token exchange failed", 400, "OAuthException"));

        mockMvc.perform(get("/api/facebook/callback").param("code", "bad-code"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("http://localhost:3000?error=*"));
    }
}
