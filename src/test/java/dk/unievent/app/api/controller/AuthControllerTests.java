package dk.unievent.app.api.controller;

import dk.unievent.app.api.handler.GlobalExceptionHandler;
import dk.unievent.app.application.dto.UserDTO;
import dk.unievent.app.application.service.RefreshTokenService;
import dk.unievent.app.application.service.UserService;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.infrastructure.security.UserDetailsAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private UserEntity testUser;
    private RefreshTokenService.TokenPair testTokenPair;

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
            "test@example.com"
        );
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
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-value"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
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
            .andExpect(jsonPath("$.email").value("test@example.com"));

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

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"old-refresh-token\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("access-token-value"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token-value"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void refreshShouldReturn400WhenTokenBlank() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void logoutShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"some-refresh-token\"}"))
            .andExpect(status().isNoContent());

        verify(refreshTokenService).logout("some-refresh-token");
    }

    @Test
    void logoutShouldReturn400WhenTokenBlank() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"\"}"))
            .andExpect(status().isBadRequest());
    }
}
