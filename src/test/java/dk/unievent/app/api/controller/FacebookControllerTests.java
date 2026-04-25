package dk.unievent.app.api.controller;

import dk.unievent.app.application.service.FacebookOAuthService;
import dk.unievent.app.db.model.PageEntity;
import dk.unievent.app.infrastructure.config.FacebookConfig;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FacebookControllerTests {

    private static final String TEST_SECRET = "test-secret-key-for-hmac";

    @Mock
    private FacebookOAuthService facebookOAuthService;

    @Mock
    private FacebookConfig facebookConfig;

    @InjectMocks
    private FacebookController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:3000");
        lenient().when(facebookConfig.getAppSecret()).thenReturn(TEST_SECRET);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private String buildSignedState() {
        try {
            String nonce = "testnonce12345678";
            long epoch = java.time.Instant.now().getEpochSecond();
            String data = nonce + "." + epoch;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hmacBytes.length * 2);
            for (byte b : hmacBytes) sb.append(String.format("%02x", b));
            return data + "." + sb;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        String state = buildSignedState();

        mockMvc.perform(get("/api/facebook/callback")
                .param("code", "auth-code")
                .param("state", state))
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
    void callbackShouldRedirectWithErrorWhenStateInvalid() throws Exception {
        mockMvc.perform(get("/api/facebook/callback")
                .param("code", "some-code")
                .param("state", "invalid.state.value"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:3000?error=Invalid+or+expired+state"));

        verifyNoInteractions(facebookOAuthService);
    }

    @Test
    void callbackShouldRedirectWithErrorWhenServiceThrows() throws Exception {
        when(facebookOAuthService.processOAuthCallback("bad-code"))
            .thenThrow(new FacebookApiException("Token exchange failed", 400, "OAuthException"));
        String state = buildSignedState();

        mockMvc.perform(get("/api/facebook/callback")
                .param("code", "bad-code")
                .param("state", state))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost:3000?error=oauth_error"));
    }
}
