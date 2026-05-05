package dk.unievent.app.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.unievent.app.api.handler.GlobalExceptionHandler;
import dk.unievent.app.application.service.CsrfTokenService;
import dk.unievent.app.application.service.EmailService;
import dk.unievent.app.application.service.JwtService;
import dk.unievent.app.application.service.OrganizerKeyService;
import dk.unievent.app.application.service.RefreshTokenService;
import dk.unievent.app.application.service.UserService;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.infrastructure.config.CookieConfig;
import dk.unievent.app.infrastructure.config.JwtConfig;
import dk.unievent.app.infrastructure.filter.CookieAuthenticationFilter;
import dk.unievent.app.infrastructure.filter.CsrfValidationFilter;
import dk.unievent.app.infrastructure.security.UserDetailsAdapter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AuthControllerIntegrationTest.TestApplication.class)
@TestPropertySource(properties = "spring.aop.auto=false")
class AuthControllerIntegrationTest {

    private static final String ACCESS_COOKIE = "auth_access";
    private static final String REFRESH_COOKIE = "auth_refresh";
    private static final String CSRF_COOKIE = "csrf_token";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CookieAuthenticationFilter cookieAuthenticationFilter;

    @Autowired
    private CsrfValidationFilter csrfValidationFilter;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private OrganizerKeyService organizerKeyService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private CsrfTokenService csrfTokenService;

    @MockitoBean
    private CookieConfig cookieConfig;

    @MockitoBean
    private JwtService jwtService;

    private MockMvc mockMvc;
    private UserEntity testUser;
    private RefreshTokenService.TokenPair tokenPair;

    @BeforeEach
    void setUp() {
        reset(userService, refreshTokenService, authenticationManager, organizerKeyService,
                emailService, csrfTokenService, cookieConfig, jwtService);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(cookieAuthenticationFilter, csrfValidationFilter)
                .build();

        testUser = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encodedpassword")
                .role("user")
                .build();

        tokenPair = new RefreshTokenService.TokenPair(
                "access-token-value",
                "refresh-token-value",
                3_600_000L,
                86_400_000L,
                "testuser",
                "test@example.com",
                "user"
        );

        when(cookieConfig.getAccessName()).thenReturn(ACCESS_COOKIE);
        when(cookieConfig.getRefreshName()).thenReturn(REFRESH_COOKIE);
        when(cookieConfig.getCsrfName()).thenReturn(CSRF_COOKIE);
        when(cookieConfig.getAccessMaxAge()).thenReturn(3_600);
        when(cookieConfig.getRefreshMaxAge()).thenReturn(86_400);
        when(cookieConfig.getCsrfMaxAge()).thenReturn(3_600);
        when(cookieConfig.getPath()).thenReturn("/");
        when(cookieConfig.getSameSite()).thenReturn("Strict");
        when(cookieConfig.isSecure()).thenReturn(true);
        when(cookieConfig.getDomain()).thenReturn("");
    }

    @Test
        void csrfTokenEndpointShouldIssueTokenAndCookie() throws Exception {
        when(csrfTokenService.generateToken()).thenReturn("bootstrap-csrf-token");

        MvcResult result = mockMvc.perform(get("/api/auth/csrf-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.csrfToken").value("bootstrap-csrf-token"))
            .andReturn();

        List<String> setCookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertCookieHeader(setCookieHeaders, CSRF_COOKIE, "bootstrap-csrf-token", 3_600, false);
        }

        @Test
    void loginShouldSetAuthCookiesAndReturnCsrfToken() throws Exception {
        UserDetailsAdapter principal = new UserDetailsAdapter(testUser);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(refreshTokenService.issueTokenPair(testUser)).thenReturn(tokenPair);

        when(csrfTokenService.generateToken()).thenReturn("bootstrap-csrf-token", "login-csrf-token");
        when(csrfTokenService.validateToken("bootstrap-csrf-token", "bootstrap-csrf-token")).thenReturn(true);

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.csrfToken").value("bootstrap-csrf-token"))
            .andReturn();

        assertCookieHeader(csrfResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE), CSRF_COOKIE, "bootstrap-csrf-token", 3_600, false);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .cookie(new Cookie(CSRF_COOKIE, "bootstrap-csrf-token"))
                .header(CSRF_HEADER, "bootstrap-csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"password123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.csrfToken").value("login-csrf-token"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();

        List<String> setCookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertCookieHeader(setCookieHeaders, ACCESS_COOKIE, "access-token-value", 3_600, true);
        assertCookieHeader(setCookieHeaders, REFRESH_COOKIE, "refresh-token-value", 86_400, true);
        assertCookieHeader(setCookieHeaders, CSRF_COOKIE, "login-csrf-token", 3_600, false);
    }

    @Test
    void refreshShouldRotateTokenPairAndGenerateNewCsrfToken() throws Exception {
        RefreshTokenService.TokenPair rotatedPair = new RefreshTokenService.TokenPair(
                "rotated-access-token",
                "rotated-refresh-token",
                3_600_000L,
                86_400_000L,
                "testuser",
                "test@example.com",
                "user"
        );

        when(csrfTokenService.validateToken("old-csrf-token", "old-csrf-token")).thenReturn(true);
        when(refreshTokenService.rotate(eq("old-refresh-token"), eq("JUnit"), eq("203.0.113.10"))).thenReturn(rotatedPair);
        when(csrfTokenService.generateToken()).thenReturn("new-csrf-token");
        when(userService.findByEmail("test@example.com")).thenReturn(testUser);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE, "old-refresh-token"))
                        .cookie(new Cookie(CSRF_COOKIE, "old-csrf-token"))
                        .header(CSRF_HEADER, "old-csrf-token")
                        .header("User-Agent", "JUnit")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.csrfToken").value("new-csrf-token"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();

        List<String> setCookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertCookieHeader(setCookieHeaders, ACCESS_COOKIE, "rotated-access-token", 3_600, true);
        assertCookieHeader(setCookieHeaders, REFRESH_COOKIE, "rotated-refresh-token", 86_400, true);
        assertCookieHeader(setCookieHeaders, CSRF_COOKIE, "new-csrf-token", 3_600, false);

        verify(refreshTokenService).rotate("old-refresh-token", "JUnit", "203.0.113.10");
        verify(csrfTokenService).generateToken();
    }

    @Test
    void logoutShouldClearCookiesAndRevokeRefreshToken() throws Exception {
        when(csrfTokenService.validateToken("logout-csrf-token", "logout-csrf-token")).thenReturn(true);

        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie(REFRESH_COOKIE, "refresh-token-to-revoke"))
                        .cookie(new Cookie(CSRF_COOKIE, "logout-csrf-token"))
                        .header(CSRF_HEADER, "logout-csrf-token"))
                .andExpect(status().isNoContent())
                .andReturn();

        List<String> setCookieHeaders = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertClearedCookieHeader(setCookieHeaders, ACCESS_COOKIE, true);
        assertClearedCookieHeader(setCookieHeaders, REFRESH_COOKIE, true);
        assertClearedCookieHeader(setCookieHeaders, CSRF_COOKIE, false);

        verify(refreshTokenService).logout("refresh-token-to-revoke");
    }

    @Test
    void postWithoutCsrfHeaderShouldReturnForbidden() throws Exception {
        when(csrfTokenService.validateToken(isNull(), eq("csrf-token"))).thenReturn(false);

        mockMvc.perform(post("/csrf-probe")
                        .cookie(new Cookie(ACCESS_COOKIE, "access-token"))
                        .cookie(new Cookie(CSRF_COOKIE, "csrf-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("CSRF token validation failed"));

        verify(csrfTokenService).validateToken(isNull(), eq("csrf-token"));
        verify(refreshTokenService, never()).rotate(any(), any(), any());
    }

    @Test
    void postWithValidCsrfHeaderShouldReachController() throws Exception {
        when(csrfTokenService.validateToken("csrf-token", "csrf-token")).thenReturn(true);

        mockMvc.perform(post("/csrf-probe")
                        .cookie(new Cookie(ACCESS_COOKIE, "access-token"))
                        .cookie(new Cookie(CSRF_COOKIE, "csrf-token"))
                        .header(CSRF_HEADER, "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(csrfTokenService).validateToken("csrf-token", "csrf-token");
    }

    @Test
    void getRequestsShouldBypassCsrfValidation() throws Exception {
        mockMvc.perform(get("/csrf-probe")
                        .cookie(new Cookie(ACCESS_COOKIE, "access-token"))
                        .cookie(new Cookie(CSRF_COOKIE, "csrf-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(csrfTokenService, never()).validateToken(any(), any());
    }

    private void assertCookieHeader(List<String> setCookieHeaders, String name, String value, int maxAge, boolean httpOnly) {
        String header = findSetCookieHeader(setCookieHeaders, name);

        assertTrue(header.startsWith(name + "=" + value));
        assertTrue(header.contains("Path=/"));
        assertTrue(header.contains("Max-Age=" + maxAge));
        assertTrue(header.contains("Secure"));
        assertTrue(header.contains("SameSite=Strict"));
        if (httpOnly) {
            assertTrue(header.contains("HttpOnly"));
        } else {
            assertFalse(header.contains("HttpOnly"));
        }
    }

    private void assertClearedCookieHeader(List<String> setCookieHeaders, String name, boolean httpOnly) {
        String header = findSetCookieHeader(setCookieHeaders, name);

        assertTrue(header.startsWith(name + "="));
        assertTrue(header.contains("Path=/"));
        assertTrue(header.contains("Max-Age=0"));
        assertTrue(header.contains("Secure"));
        assertTrue(header.contains("SameSite=Strict"));
        if (httpOnly) {
            assertTrue(header.contains("HttpOnly"));
        } else {
            assertFalse(header.contains("HttpOnly"));
        }
    }

    private String findSetCookieHeader(List<String> setCookieHeaders, String name) {
        return setCookieHeaders.stream()
                .filter(header -> header.startsWith(name + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Set-Cookie header for " + name + ": " + setCookieHeaders));
    }

    @RestController
    static class CsrfProbeController {

        @PostMapping("/csrf-probe")
        Map<String, String> post() {
            return Map.of("status", "ok");
        }

        @GetMapping("/csrf-probe")
        Map<String, String> get() {
            return Map.of("status", "ok");
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            SecurityAutoConfiguration.class,
            ServletWebSecurityAutoConfiguration.class,
            ManagementWebSecurityAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            TransactionAutoConfiguration.class
    })
    @Import({
            AuthController.class,
            GlobalExceptionHandler.class,
            JwtService.class,
            JwtConfig.class,
            CookieAuthenticationFilter.class,
            CsrfValidationFilter.class,
            CsrfProbeController.class
    })
    static class TestApplication {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
