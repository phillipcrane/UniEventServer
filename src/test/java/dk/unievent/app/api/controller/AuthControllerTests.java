package dk.unievent.app.api.controller;

import dk.unievent.app.api.dto.OrganizerKeyVerifyResponse;
import dk.unievent.app.api.handler.GlobalExceptionHandler;
import dk.unievent.app.application.dto.UserDTO;
import dk.unievent.app.application.service.CsrfTokenService;
import dk.unievent.app.application.service.EmailService;
import dk.unievent.app.application.service.OrganizerKeyService;
import dk.unievent.app.application.service.RefreshTokenService;
import dk.unievent.app.application.service.UserService;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.db.repository.UserRepository;
import dk.unievent.app.infrastructure.config.CookieConfig;
import dk.unievent.app.infrastructure.exception.InvalidConfirmationTokenException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyAlreadyUsedException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyExpiredException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyNotFoundException;
import dk.unievent.app.infrastructure.exception.UsernameAlreadyTakenException;
import dk.unievent.app.infrastructure.security.UserDetailsAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTests {

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private OrganizerKeyService organizerKeyService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private CsrfTokenService csrfTokenService;

    @Mock
    private CookieConfig cookieConfig;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private UserEntity testUser;
    private UserEntity adminUser;
    private RefreshTokenService.TokenPair testTokenPair;
    private UsernamePasswordAuthenticationToken adminAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        testUser = UserEntity.builder()
            .username("testuser")
            .email("test@example.com")
            .password("encodedpassword")
            .role("user")
            .build();

        testTokenPair = new RefreshTokenService.TokenPair(
            "access-token-value",
            "refresh-token-value",
            3600000L,
            86400000L,
            "testuser",
            "test@example.com",
            "user"
        );

        adminUser = UserEntity.builder()
            .username("admin")
            .email("admin@unievent.internal")
            .password("encodedpassword")
            .role("admin")
            .build();
        adminUser.setId(1L);

        adminAuth = new UsernamePasswordAuthenticationToken(
            "admin@unievent.internal", null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        lenient().when(csrfTokenService.generateToken()).thenReturn("csrf-token-value");
        lenient().when(cookieConfig.getAccessName()).thenReturn("auth_access");
        lenient().when(cookieConfig.getRefreshName()).thenReturn("auth_refresh");
        lenient().when(cookieConfig.getCsrfName()).thenReturn("csrf_token");
        lenient().when(cookieConfig.getAccessMaxAge()).thenReturn(3600);
        lenient().when(cookieConfig.getRefreshMaxAge()).thenReturn(86400);
        lenient().when(cookieConfig.getCsrfMaxAge()).thenReturn(3600);
        lenient().when(cookieConfig.getPath()).thenReturn("/");
        lenient().when(cookieConfig.getSameSite()).thenReturn("Strict");
        lenient().when(cookieConfig.isSecure()).thenReturn(false);
        lenient().when(cookieConfig.getDomain()).thenReturn("");
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void registerShouldReturnTokensOnSuccess() throws Exception {
        when(userService.register(any(UserDTO.class))).thenReturn(testUser);
        when(refreshTokenService.issueTokenPair(testUser)).thenReturn(testTokenPair);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("access-token-value"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
            .andExpect(jsonPath("$.csrfToken").value("csrf-token-value"));
    }

    @Test
    void registerShouldReturn400WhenUsernameBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"email\":\"test@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldReturn400WhenEmailBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"\",\"password\":\"password123\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldReturn400WhenPasswordBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldReturnTokensOnSuccess() throws Exception {
        UserDetailsAdapter principal = new UserDetailsAdapter(testUser);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(refreshTokenService.issueTokenPair(testUser)).thenReturn(testTokenPair);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("access-token-value"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
            .andExpect(jsonPath("$.csrfToken").value("csrf-token-value"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginShouldReturn400WhenEmailBlank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"password123\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldReturn400WhenPasswordBlank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void refreshShouldReturnNewTokenPair() throws Exception {
        when(refreshTokenService.rotate(eq("old-refresh-token"), any(), any())).thenReturn(testTokenPair);
        when(userService.findByEmail("test@example.com")).thenReturn(testUser);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("auth_refresh", "old-refresh-token")))
                //.cookie(new Cookie("auth_refresh", "old-refresh-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("access-token-value"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
            .andExpect(jsonPath("$.csrfToken").value("csrf-token-value"));
    }

    @Test
    void refreshShouldReturn400WhenTokenBlank() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON))
    /*void refreshShouldReturn400WhenNoCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))*/
            .andExpect(status().isBadRequest());
    }

    @Test
    void logoutShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("auth_refresh", "some-refresh-token")))
                //.cookie(new Cookie("auth_refresh", "some-refresh-token")))
            .andExpect(status().isNoContent());

        verify(refreshTokenService).logout("some-refresh-token");
    }

    @Test
    void logoutShouldReturn400WhenTokenBlank() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
            //.andExpect(status().isBadRequest());
    }

    // ── /organizer-key/generate ───────────────────────────────────────────────

    @Test
    void generateOrganizerKeyShouldReturn200WhenAdminAuthenticated() throws Exception {
        when(organizerKeyService.generateOrganizerKey("organizer@example.com", "admin@unievent.internal")).thenReturn("GENERATEDKEY123456789012345ABCDE");
        when(organizerKeyService.getKeyExpirationSeconds()).thenReturn(86400L);

        mockMvc.perform(post("/api/auth/organizer-key/generate")
                .principal(adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"organizer@example.com\",\"organizationName\":\"Test Org\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Invitation key has been sent to organizer@example.com"))
            .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    @Test
    void generateOrganizerKeyShouldReturn400WhenEmailBlank() throws Exception {
        mockMvc.perform(post("/api/auth/organizer-key/generate")
                .principal(adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"organizationName\":\"Test Org\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void generateOrganizerKeyShouldReturn400WhenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/organizer-key/generate")
                .principal(adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"organizationName\":\"Test Org\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── /organizer-key/verify ─────────────────────────────────────────────────

    @Test
    void verifyOrganizerKeyShouldReturn200OnValidKey() throws Exception {
        OrganizerKeyVerifyResponse verifyResponse = new OrganizerKeyVerifyResponse("confirm-token", 600, "organizer@example.com");
        when(organizerKeyService.verifyOrganizerKey("VALIDKEY12345678901234567890ABCD")).thenReturn(verifyResponse);

        mockMvc.perform(post("/api/auth/organizer-key/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"VALIDKEY12345678901234567890ABCD\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.confirmationToken").value("confirm-token"))
            .andExpect(jsonPath("$.email").value("organizer@example.com"))
            .andExpect(jsonPath("$.expiresIn").value(600));
    }

    @Test
    void verifyOrganizerKeyShouldReturn400WhenKeyBlank() throws Exception {
        mockMvc.perform(post("/api/auth/organizer-key/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOrganizerKeyShouldReturn404WhenKeyNotFound() throws Exception {
        when(organizerKeyService.verifyOrganizerKey(any())).thenThrow(new OrganizerKeyNotFoundException());

        mockMvc.perform(post("/api/auth/organizer-key/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"BADKEY123456789012345678901234\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void verifyOrganizerKeyShouldReturn410WhenKeyAlreadyUsed() throws Exception {
        when(organizerKeyService.verifyOrganizerKey(any())).thenThrow(new OrganizerKeyAlreadyUsedException());

        mockMvc.perform(post("/api/auth/organizer-key/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"USEDKEY1234567890123456789012AB\"}"))
            .andExpect(status().isGone());
    }

    @Test
    void verifyOrganizerKeyShouldReturn401WhenKeyExpired() throws Exception {
        when(organizerKeyService.verifyOrganizerKey(any())).thenThrow(new OrganizerKeyExpiredException());

        mockMvc.perform(post("/api/auth/organizer-key/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"EXPIREDKEY12345678901234567890A\"}"))
            .andExpect(status().isUnauthorized());
    }

    // ── /register-with-key ────────────────────────────────────────────────────

    @Test
    void registerWithKeyShouldReturn201OnSuccess() throws Exception {
        RefreshTokenService.TokenPair organizerPair = new RefreshTokenService.TokenPair(
            "org-access-token", "org-refresh-token", 3600000L, 86400000L, "neworg", "organizer@example.com", "organizer");
        UserEntity organizer = UserEntity.builder()
            .username("neworg").email("organizer@example.com").password("encoded").role("organizer").build();
        when(organizerKeyService.completeOrganizerRegistration(eq("valid-token"), eq("neworg"), eq("securepassword1"), eq("organizer@example.com")))
            .thenReturn(organizer);
        when(refreshTokenService.issueTokenPair(organizer)).thenReturn(organizerPair);

        mockMvc.perform(post("/api/auth/register-with-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationToken\":\"valid-token\",\"username\":\"neworg\",\"password\":\"securepassword1\",\"email\":\"organizer@example.com\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("org-access-token"))
            .andExpect(jsonPath("$.username").value("neworg"))
            .andExpect(jsonPath("$.email").value("organizer@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_ORGANIZER"))
            .andExpect(jsonPath("$.csrfToken").value("csrf-token-value"));
    }

    @Test
    void registerWithKeyShouldReturn400WhenConfirmationTokenBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register-with-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationToken\":\"\",\"username\":\"neworg\",\"password\":\"securepassword1\",\"email\":\"organizer@example.com\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithKeyShouldReturn400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register-with-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationToken\":\"token\",\"username\":\"neworg\",\"password\":\"short\",\"email\":\"organizer@example.com\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithKeyShouldReturn400WhenUsernameTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register-with-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationToken\":\"token\",\"username\":\"ab\",\"password\":\"securepassword1\",\"email\":\"organizer@example.com\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithKeyShouldReturn401WhenTokenInvalid() throws Exception {
        when(organizerKeyService.completeOrganizerRegistration(any(), any(), any(), any()))
            .thenThrow(new InvalidConfirmationTokenException());

        mockMvc.perform(post("/api/auth/register-with-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationToken\":\"bad-token\",\"username\":\"neworg\",\"password\":\"securepassword1\",\"email\":\"organizer@example.com\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registerWithKeyShouldReturn409WhenUsernameAlreadyTaken() throws Exception {
        when(organizerKeyService.completeOrganizerRegistration(any(), any(), any(), any()))
            .thenThrow(new UsernameAlreadyTakenException());

        mockMvc.perform(post("/api/auth/register-with-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationToken\":\"token\",\"username\":\"takenuser\",\"password\":\"securepassword1\",\"email\":\"organizer@example.com\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void registerWithKeyShouldReturn422WhenKeyAlreadyUsed() throws Exception {
        when(organizerKeyService.completeOrganizerRegistration(any(), any(), any(), any()))
            .thenThrow(new OrganizerKeyAlreadyUsedException());

        mockMvc.perform(post("/api/auth/register-with-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmationToken\":\"token\",\"username\":\"neworg\",\"password\":\"securepassword1\",\"email\":\"organizer@example.com\"}"))
            .andExpect(status().isGone());
    }
}
