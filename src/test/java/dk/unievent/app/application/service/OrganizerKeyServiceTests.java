package dk.unievent.app.application.service;

import dk.unievent.app.api.dto.OrganizerKeyVerifyResponse;
import dk.unievent.app.db.model.OrganizerKeyEntity;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.db.repository.OrganizerKeyRepository;
import dk.unievent.app.db.repository.UserRepository;
import dk.unievent.app.infrastructure.exception.EmailAlreadyRegisteredException;
import dk.unievent.app.infrastructure.exception.InvalidConfirmationTokenException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyAlreadyUsedException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyExpiredException;
import dk.unievent.app.infrastructure.exception.OrganizerKeyNotFoundException;
import dk.unievent.app.infrastructure.exception.UsernameAlreadyTakenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizerKeyServiceTests {

    private static final String TEST_JWT_SECRET = "12345678901234567890123456789012";
    private static final String TEST_EMAIL = "organizer@example.com";
    private static final String TEST_ADMIN_EMAIL = "admin@example.com";
    private static final Long TEST_ADMIN_ID = 1L;
    private static final Long TEST_KEY_ID = 42L;

    @Mock
    private OrganizerKeyRepository organizerKeyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OrganizerKeyService organizerKeyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(organizerKeyService, "jwtSecret", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(organizerKeyService, "keyExpirationHours", 24L);
        ReflectionTestUtils.setField(organizerKeyService, "confirmationTokenExpirationMinutes", 10L);
    }

    // ── generateOrganizerKey ──────────────────────────────────────────────────

    @Test
    void generateShouldSaveKeyEntityWithCorrectFields() {
        when(userRepository.findByEmail(TEST_ADMIN_EMAIL)).thenReturn(Optional.of(adminUser()));
        when(organizerKeyRepository.save(any(OrganizerKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        organizerKeyService.generateOrganizerKey(TEST_EMAIL, TEST_ADMIN_EMAIL);

        verify(organizerKeyRepository).save(argThat(entity ->
            TEST_EMAIL.equals(entity.getEmail()) &&
            TEST_ADMIN_ID.equals(entity.getCreatedBy()) &&
            entity.getUsedAt() == null &&
            entity.getExpiresAt().isAfter(Instant.now())
        ));
    }

    @Test
    void generateShouldReturn32CharAlphanumericKey() {
        when(userRepository.findByEmail(TEST_ADMIN_EMAIL)).thenReturn(Optional.of(adminUser()));
        when(organizerKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String key = organizerKeyService.generateOrganizerKey(TEST_EMAIL, TEST_ADMIN_EMAIL);

        assertEquals(32, key.length());
        assertTrue(key.matches("[A-Za-z0-9]{32}"));
    }

    // ── verifyOrganizerKey ────────────────────────────────────────────────────

    @Test
    void verifyShouldReturnConfirmationTokenForValidKey() {
        when(organizerKeyRepository.findByKeyValue("VALIDKEY")).thenReturn(Optional.of(activeKey()));

        OrganizerKeyVerifyResponse response = organizerKeyService.verifyOrganizerKey("VALIDKEY");

        assertNotNull(response.confirmationToken());
        assertEquals(TEST_EMAIL, response.email());
        assertEquals(600, response.expiresIn());
    }

    @Test
    void verifyShouldThrowIllegalArgumentWhenKeyNotFound() {
        when(organizerKeyRepository.findByKeyValue(any())).thenReturn(Optional.empty());

        assertThrows(OrganizerKeyNotFoundException.class,
            () -> organizerKeyService.verifyOrganizerKey("BADKEY"));
    }

    @Test
    void verifyShouldThrowIllegalStateWhenKeyAlreadyUsed() {
        OrganizerKeyEntity usedKey = activeKey();
        usedKey.setUsedAt(Instant.now().minusSeconds(3600));
        when(organizerKeyRepository.findByKeyValue("USEDKEY")).thenReturn(Optional.of(usedKey));

        assertThrows(OrganizerKeyAlreadyUsedException.class,
            () -> organizerKeyService.verifyOrganizerKey("USEDKEY"));
    }

    @Test
    void verifyShouldThrowIllegalStateWhenKeyExpired() {
        OrganizerKeyEntity expiredKey = activeKey();
        expiredKey.setExpiresAt(Instant.now().minusSeconds(3600));
        when(organizerKeyRepository.findByKeyValue("EXPIREDKEY")).thenReturn(Optional.of(expiredKey));

        assertThrows(OrganizerKeyExpiredException.class,
            () -> organizerKeyService.verifyOrganizerKey("EXPIREDKEY"));
    }

    // ── completeOrganizerRegistration ─────────────────────────────────────────

    @Test
    void completeShouldCreateOrganizerAndMarkKeyUsed() {
        String token = buildConfirmationToken(TEST_KEY_ID, TEST_EMAIL, 10);
        OrganizerKeyEntity keyEntity = activeKey();
        when(organizerKeyRepository.findById(TEST_KEY_ID)).thenReturn(Optional.of(keyEntity));
        when(userRepository.existsByUsername("neworg")).thenReturn(false);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("securepassword123")).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(organizerKeyRepository.save(any(OrganizerKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity result = organizerKeyService.completeOrganizerRegistration(token, "neworg", "securepassword123", TEST_EMAIL);

        assertEquals("neworg", result.getUsername());
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals("organizer", result.getRole());
        assertNotNull(keyEntity.getUsedAt());
    }

    @Test
    void completeShouldThrowWhenConfirmationTokenIsInvalid() {
        assertThrows(InvalidConfirmationTokenException.class,
            () -> organizerKeyService.completeOrganizerRegistration("not-a-jwt", "user", "password123456", TEST_EMAIL));
    }

    @Test
    void completeShouldThrowWhenKeyAlreadyUsed() {
        String token = buildConfirmationToken(TEST_KEY_ID, TEST_EMAIL, 10);
        OrganizerKeyEntity usedKey = activeKey();
        usedKey.setUsedAt(Instant.now().minusSeconds(60));
        when(organizerKeyRepository.findById(TEST_KEY_ID)).thenReturn(Optional.of(usedKey));

        assertThrows(OrganizerKeyAlreadyUsedException.class,
            () -> organizerKeyService.completeOrganizerRegistration(token, "user", "password123456", TEST_EMAIL));
    }

    @Test
    void completeShouldThrowWhenKeyExpiredAtRegistrationTime() {
        String token = buildConfirmationToken(TEST_KEY_ID, TEST_EMAIL, 10);
        OrganizerKeyEntity expiredKey = activeKey();
        expiredKey.setExpiresAt(Instant.now().minusSeconds(60));
        when(organizerKeyRepository.findById(TEST_KEY_ID)).thenReturn(Optional.of(expiredKey));

        assertThrows(OrganizerKeyExpiredException.class,
            () -> organizerKeyService.completeOrganizerRegistration(token, "user", "password123456", TEST_EMAIL));
    }

    @Test
    void completeShouldThrowWhenEmailMismatch() {
        String token = buildConfirmationToken(TEST_KEY_ID, TEST_EMAIL, 10);
        when(organizerKeyRepository.findById(TEST_KEY_ID)).thenReturn(Optional.of(activeKey()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> organizerKeyService.completeOrganizerRegistration(token, "user", "password123456", "wrong@example.com"));
        assertTrue(ex.getMessage().contains("Email does not match"));
    }

    @Test
    void completeShouldThrowWhenUsernameAlreadyTaken() {
        String token = buildConfirmationToken(TEST_KEY_ID, TEST_EMAIL, 10);
        when(organizerKeyRepository.findById(TEST_KEY_ID)).thenReturn(Optional.of(activeKey()));
        when(userRepository.existsByUsername("takenuser")).thenReturn(true);

        assertThrows(UsernameAlreadyTakenException.class,
            () -> organizerKeyService.completeOrganizerRegistration(token, "takenuser", "password123456", TEST_EMAIL));
    }

    @Test
    void completeShouldThrowWhenEmailAlreadyRegistered() {
        String token = buildConfirmationToken(TEST_KEY_ID, TEST_EMAIL, 10);
        when(organizerKeyRepository.findById(TEST_KEY_ID)).thenReturn(Optional.of(activeKey()));
        when(userRepository.existsByUsername("neworg")).thenReturn(false);
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
            () -> organizerKeyService.completeOrganizerRegistration(token, "neworg", "password123456", TEST_EMAIL));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserEntity adminUser() {
        UserEntity admin = UserEntity.builder()
            .username("admin").email(TEST_ADMIN_EMAIL).role("admin").build();
        admin.setId(TEST_ADMIN_ID);
        return admin;
    }

    private OrganizerKeyEntity activeKey() {
        OrganizerKeyEntity entity = OrganizerKeyEntity.builder()
            .keyValue("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456")
            .email(TEST_EMAIL)
            .createdBy(TEST_ADMIN_ID)
            .expiresAt(Instant.now().plusSeconds(86400))
            .build();
        entity.setId(TEST_KEY_ID);
        return entity;
    }

    private String buildConfirmationToken(Long keyId, String email, long expiresInMinutes) {
        SecretKey signingKey = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(email)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiresInMinutes * 60 * 1000))
            .claim("type", "organizer-registration-confirmation")
            .claim("keyId", keyId)
            .signWith(signingKey)
            .compact();
    }
}
