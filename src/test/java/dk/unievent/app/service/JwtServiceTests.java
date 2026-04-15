package dk.unievent.app.service;

import dk.unievent.app.application.service.JwtService;
import dk.unievent.app.infrastructure.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtServiceTests {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = User.withUsername("user@example.com")
                .password("secret")
                .roles("USER")
                .build();

        lenient().when(jwtConfig.getSecret()).thenReturn("12345678901234567890123456789012");
        lenient().when(jwtConfig.getRefreshSecret()).thenReturn("abcdefghijklmnopqrstuvwx12345678");
        lenient().when(jwtConfig.getExpirationMs()).thenReturn(60_000L);
        lenient().when(jwtConfig.getRefreshExpirationMs()).thenReturn(120_000L);
    }

    @Test
    void generateAccessTokenShouldCreateValidJwt() {
        String token = jwtService.generateAccessToken(userDetails);

        assertNotNull(token);
        assertEquals("user@example.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertFalse(jwtService.isRefreshTokenValid(token, userDetails));
    }

    @Test
    void generateRefreshTokenShouldCreateValidRefreshJwt() {
        String token = jwtService.generateRefreshToken(userDetails, "token-1", "family-1");

        assertNotNull(token);
        assertEquals("user@example.com", jwtService.extractRefreshUsername(token));
        assertEquals("token-1", jwtService.extractRefreshTokenId(token));
        assertEquals("family-1", jwtService.extractRefreshFamilyId(token));
        assertTrue(jwtService.isRefreshTokenValid(token, userDetails));
        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void invalidTokenShouldBeRejected() {
        assertNull(jwtService.extractUsername("not-a-token"));
        assertFalse(jwtService.isTokenValid("not-a-token", userDetails));
        assertFalse(jwtService.isRefreshTokenValid("not-a-token", userDetails));
    }
}