package dk.unievent.app.service;

import dk.unievent.app.application.service.JwtService;
import dk.unievent.app.application.service.RefreshTokenService;
import dk.unievent.app.application.service.UserService;
import dk.unievent.app.db.model.RefreshTokenEntity;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.db.repository.RefreshTokenRepository;
import dk.unievent.app.infrastructure.config.JwtConfig;
import dk.unievent.app.infrastructure.exception.UnauthorizedTokenException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTests {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private UserEntity user;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .username("test-user")
                .email("user@example.com")
                .password("encoded")
                .role("USER")
                .build();

        userDetails = org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();

        lenient().when(jwtConfig.getExpirationMs()).thenReturn(60_000L);
        lenient().when(jwtConfig.getRefreshExpirationMs()).thenReturn(120_000L);
        lenient().when(userService.loadUserByUsername(user.getEmail())).thenReturn(userDetails);
        lenient().when(userService.findByEmail(user.getEmail())).thenReturn(user);
    }

    @Test
    void issueTokenPairShouldPersistRefreshToken() {
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(eq(userDetails), anyString(), anyString())).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issueTokenPair(user);

        assertEquals("access-token", tokenPair.accessToken());
        assertEquals("refresh-token", tokenPair.refreshToken());

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertEquals(user.getId(), captor.getValue().getUserId());
        assertEquals(user.getEmail(), captor.getValue().getUserEmail());
        assertNotNull(captor.getValue().getTokenId());
        assertNotNull(captor.getValue().getFamilyId());
        assertNotNull(captor.getValue().getTokenHash());
    }

    @Test
    void rotateShouldReplaceActiveRefreshToken() {
        String currentRefresh = "current-refresh-token";
        when(jwtService.extractRefreshUsername(currentRefresh)).thenReturn(user.getEmail());
        when(jwtService.extractRefreshTokenId(currentRefresh)).thenReturn("token-1");
        when(jwtService.extractRefreshFamilyId(currentRefresh)).thenReturn("family-1");
        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token-2");
        when(jwtService.generateRefreshToken(eq(userDetails), anyString(), eq("family-1"))).thenReturn("refresh-token-2");
        when(refreshTokenRepository.findByTokenId("token-1")).thenReturn(Optional.of(
                RefreshTokenEntity.builder()
                        .id(10L)
                        .tokenId("token-1")
                        .familyId("family-1")
                        .tokenHash(javaHash(currentRefresh))
                        .userId(user.getId())
                        .userEmail(user.getEmail())
                        .expiresAt(Instant.now().plusSeconds(60))
                        .build()
        ));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.TokenPair rotated = refreshTokenService.rotate(currentRefresh);

        assertEquals("access-token-2", rotated.accessToken());
        assertEquals("refresh-token-2", rotated.refreshToken());
        verify(refreshTokenRepository, times(2)).save(any(RefreshTokenEntity.class));
    }

    @Test
    void logoutShouldRevokeCurrentTokenAndFamily() {
        String refreshToken = "refresh-token";
        when(jwtService.extractRefreshTokenId(refreshToken)).thenReturn("token-1");
        when(jwtService.extractRefreshFamilyId(refreshToken)).thenReturn("family-1");
        when(refreshTokenRepository.findByTokenId("token-1")).thenReturn(Optional.of(
                RefreshTokenEntity.builder()
                        .id(10L)
                        .tokenId("token-1")
                        .familyId("family-1")
                        .tokenHash(javaHash(refreshToken))
                        .userId(user.getId())
                        .userEmail(user.getEmail())
                        .expiresAt(Instant.now().plusSeconds(60))
                        .build()
        ));
        when(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull("family-1")).thenReturn(List.of(
                RefreshTokenEntity.builder()
                        .id(10L)
                        .tokenId("token-1")
                        .familyId("family-1")
                        .tokenHash(javaHash(refreshToken))
                        .userId(user.getId())
                        .userEmail(user.getEmail())
                        .expiresAt(Instant.now().plusSeconds(60))
                        .build()
        ));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.logout(refreshToken);

        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
        verify(refreshTokenRepository).saveAll(any());
    }

    @Test
    void rotateShouldRejectUnknownToken() {
        when(jwtService.extractRefreshUsername("bad-token")).thenReturn("user@example.com");
        when(jwtService.extractRefreshTokenId("bad-token")).thenReturn("token-1");
        when(jwtService.extractRefreshFamilyId("bad-token")).thenReturn("family-1");
        when(refreshTokenRepository.findByTokenId("token-1")).thenReturn(Optional.empty());
        when(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull("family-1")).thenReturn(List.of());

        assertThrows(UnauthorizedTokenException.class, () -> refreshTokenService.rotate("bad-token"));
    }

    private String javaHash(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}